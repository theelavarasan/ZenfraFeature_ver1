package com.zenfra.dataframe.service;

import static com.google.common.collect.Streams.zip;
import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.sum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.RelationalGroupedDataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mapping.AccessOptions.GetOptions.GetNulls;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.zenfra.configuration.AwsInventoryPostgresConnection;
import com.zenfra.dao.AwsInstanceCcrDataRepository;
import com.zenfra.dao.FavouriteDao_v2;
import com.zenfra.dao.ReportDao;
import com.zenfra.dataframe.filter.ColumnFilter;
import com.zenfra.dataframe.filter.NumberColumnFilter;
import com.zenfra.dataframe.filter.SetColumnFilter;
import com.zenfra.dataframe.filter.TextColumnFilter;
import com.zenfra.dataframe.request.AwsInstanceData;
import com.zenfra.dataframe.request.ColumnVO;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.request.SortModel;
import com.zenfra.dataframe.response.DataResult;
import com.zenfra.dataframe.util.DataframeUtil;
import com.zenfra.model.AwsInstanceCcrData;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.model.ZenfraJSONObject;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.CommonUtils;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@Repository
public class DataframeService {

	public static final Logger logger = LoggerFactory.getLogger(DataframeService.class);

	private List<String> rowGroups, groupKeys;
	private List<ColumnVO> valueColumns, pivotColumns;
	private List<SortModel> sortModel;
	private Map<String, ColumnFilter> filterModel;
	private boolean isGrouping, isPivotMode;

	private List<String> actualColumnNames = null;
	private List<String> renamedColumnNames = null;
	private Map<String, List<String>> serverDiscoveryNumbericalColumns = new HashMap<String, List<String>>();

	private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	JSONParser parser = new JSONParser();
	
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	SparkSession sparkSession;

	// @Value("${db.url}")
	// private String dbUrl;

	private String commonPath;

	@PostConstruct
	public void init() {
		commonPath = ZKModel.getProperty(ZKConstants.DATAFRAME_PATH);
	}

	// @Value("${zenfra.path}")
	// private String commonPath;

	@Value("${zenfra.permisssion}")
	private String fileOwnerGroupName;

	@Autowired
	EolService eolService;

	@Autowired
	private ReportDao reportDao;

	@Autowired
	private FavouriteDao_v2 favouriteDao_v2;
	
	@Autowired
	JdbcTemplate jdbc;
	
	@Autowired
	AwsInstanceCcrDataRepository awsInstanceCcrDataRepository;
	
	@Autowired
	CommonFunctions commonFunctions;

	private String dbUrl = DBUtils.getPostgres().get("dbUrl");

	// ---------------------SSRM Code-----------------------------------//

	public static Dataset<Row> union(final Dataset<Row> ds1, final Dataset<Row> ds2) {
		Set<String> ds1Cols = Sets.newHashSet(ds1.columns());
		Set<String> ds2Cols = Sets.newHashSet(ds2.columns());
		final Set<String> total = Sets.newHashSet(ds1Cols);
		total.addAll(ds2Cols);
		return ds1.select(expr(ds1Cols, total)).union(ds2.select(expr(ds2Cols, total)));
	}

	private static Column[] expr(final Set<String> cols, final Set<String> allCols) {
		return allCols.stream().map(x -> {
			if (cols.contains(x)) {
				return col(x);
			} else {
				return lit(null).as(x);
			}
		}).toArray(Column[]::new);
	}

	private static Dataset<Row> unionDatasets(Dataset<Row> one, Dataset<Row> another) {
		StructType firstSchema = one.schema();
		List<String> anotherFields = Arrays.asList(another.schema().fieldNames());
		another = balanceDataset(another, firstSchema, anotherFields);
		StructType secondSchema = another.schema();
		List<String> oneFields = Arrays.asList(one.schema().fieldNames());
		one = balanceDataset(one, secondSchema, oneFields);
		return another.union(one);
	}

	private static Dataset<Row> balanceDataset(Dataset<Row> dataset, StructType schema, List<String> fields) {
		for (StructField e : schema.fields()) {
			if (!fields.contains(e.name())) {
				dataset = dataset.withColumn(e.name(), functions.lit(null));
				dataset = dataset.withColumn(e.name(),
						dataset.col(e.name()).cast(Optional.ofNullable(e.dataType()).orElse(DataTypes.StringType)));
			}
		}
		return dataset;
	}

	private String selectSql() {
		if (!isGrouping)
			return "select *";

		Stream<String> groupCols = rowGroups.stream().limit(groupKeys.size() + 1);

		Stream<String> valCols = valueColumns.stream().map(ColumnVO::getField);

		Stream<String> pivotCols = isPivotMode ? pivotColumns.stream().map(ColumnVO::getField) : Stream.empty();

		return "select " + concat(groupCols, concat(pivotCols, valCols)).collect(joining(","));
	}

	private Dataset<Row> groupBy(Dataset<Row> df) {
		if (!isGrouping)
			return df;

		  System.out.println("-------------groupBy-------- " + df.count());
		Column[] groups = rowGroups.stream().limit(groupKeys.size() + 1).map(functions::col).toArray(Column[]::new);

		return agg(pivot(df.groupBy(groups)));
	}

	private RelationalGroupedDataset pivot(RelationalGroupedDataset groupedDf) {
		if (!isPivotMode)
			return groupedDf;

		// spark sql only supports a single pivot column
		Optional<String> pivotColumn = pivotColumns.stream().map(ColumnVO::getField).findFirst();

		return pivotColumn.map(groupedDf::pivot).orElse(groupedDf);
	}

	private Dataset<Row> agg(RelationalGroupedDataset groupedDf) {
		if (valueColumns.isEmpty())
			return groupedDf.count();

		Column[] aggCols = valueColumns.stream().map(ColumnVO::getField).map(field -> sum(field).alias(field))
				.toArray(Column[]::new);

		return groupedDf.agg(aggCols[0], copyOfRange(aggCols, 1, aggCols.length));
	}

	private Dataset<Row> orderBy(Dataset<Row> df) {
		try {
			Stream<String> groupCols = rowGroups.stream().limit(groupKeys.size() + 1);

			Stream<String> valCols = valueColumns.stream().map(ColumnVO::getField);

			List<String> allCols = concat(groupCols, valCols).collect(toList());

			Column[] cols = sortModel.stream().map(model -> Pair.of(model.getColId(), model.getSort().equals("asc")))
					.filter(p -> !isGrouping || allCols.contains(p.getKey()))
					.map(p -> p.getValue() ? col(p.getKey()).asc() : col(p.getKey()).desc()).toArray(Column[]::new);

			return df.orderBy(cols);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return df;
	}

	
	private Dataset<Row> filter(Dataset<Row> df) {
		Function<Map.Entry<String, ColumnFilter>, String> applyColumnFilters = entry -> {
			String columnName = entry.getKey();
			ColumnFilter filter = entry.getValue();
			columnName = "`"+columnName+"`";

			if (filter instanceof SetColumnFilter) {
				return setFilter().apply(columnName, (SetColumnFilter) filter);
			}

			if (filter instanceof NumberColumnFilter) {
				return numberFilter().apply(columnName, (NumberColumnFilter) filter);
			}

			if (filter instanceof TextColumnFilter) {
				return textFilter().apply(columnName, (TextColumnFilter) filter);
			}

			return "";
		};
	  
	  
	  Stream<String> columnFilters = filterModel.entrySet().stream()
	  .map(applyColumnFilters);
	  	
	  
	  
	  Stream<String> groupToFilter = zip(groupKeys.stream(), rowGroups.stream(),
	  (key, group) -> group + " = '" + key + "'");
	  
	  String filters = concat(columnFilters, groupToFilter)
	  .collect(joining(" AND "));	 
	  
	  return filters.isEmpty() ? df : df.filter(filters); }
	 

	/*private Dataset<Row> filter(Dataset<Row> df, String viewName) {

		Function<Map.Entry<String, ColumnFilter>, String> applyColumnFilters = entry -> {
			String columnName = entry.getKey();
			ColumnFilter filter = entry.getValue();

			System.out.println("-----------------filter ---------genfilter-------- " + filter);
			
			if (filter instanceof SetColumnFilter) {
				return setFilter().apply(columnName, (SetColumnFilter) filter);
			}

			if (filter instanceof NumberColumnFilter) {

				return numberFilter().apply(columnName, (NumberColumnFilter) filter);
			}

			
			  if (filter instanceof TextColumnFilter) {
			  
			 // return ///formTextQuery(columnName, (TextColumnFilter) filter);
					System.out.println("-----------------filter ---------genfilter->>>>------- " + textFilter().apply(columnName, (TextColumnFilter) filter));
			   return textFilter().apply(columnName, (TextColumnFilter) filter);
			   
			  
			 }
			 

			return "";
		};

		System.out.println("------------------ :filterModel: ---------gen-------- " + filterModel);
		String finalTextFilterQueryTemp = "";
		boolean customQuery = false;

		if(filterModel != null) {
			if (filterModel.toString().contains("condition1")) { // custom query

				for (Map.Entry<String, ColumnFilter> entry : filterModel.entrySet()) {
					String columnName = entry.getKey();
					columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
					ColumnFilter filter = entry.getValue();

					if (filter instanceof NumberColumnFilter) {
						customQuery = true;

						df.select(col(columnName).cast("int").as(columnName));

						String str = formNumberQuery(columnName, (NumberColumnFilter) filter);

						if (finalTextFilterQueryTemp.isEmpty()) {
							finalTextFilterQueryTemp = str;
						} else {
							finalTextFilterQueryTemp = finalTextFilterQueryTemp + " AND " + str;
						}
					}

					if (filter instanceof TextColumnFilter) {
						customQuery = true;
						String str = formTextQuery(columnName, (TextColumnFilter) filter);

						if (finalTextFilterQueryTemp.isEmpty()) {
							finalTextFilterQueryTemp = str;
						} else {
							finalTextFilterQueryTemp = finalTextFilterQueryTemp + " AND " + str;
						}

					}

				}
			} else if (filterModel.toString().contains("defaultFilter")) { // && filterModel.toString().contains("text")
				for (Map.Entry<String, ColumnFilter> entry : filterModel.entrySet()) {
					String columnName = entry.getKey();
					columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
					columnName = "lower(" + "" + columnName + "" + ")";

					ColumnFilter filter = entry.getValue();
					if (filter instanceof TextColumnFilter) {
						customQuery = true;
						TextColumnFilter textColumnFilter = (TextColumnFilter) filter;

						String expression = textColumnFilter.getType();
						String filterText = textColumnFilter.getFilter();

						finalTextFilterQueryTemp = formatTextFilterType(columnName, expression, filterText);

					}
				}
			}

			if (customQuery) {
				Dataset<Row> filteredData = df.sqlContext()
						.sql("select * from " + viewName + " where " + finalTextFilterQueryTemp);

				return filteredData;
			} else {
				System.out.println("------------------ :filterModel: ---------else-------- " + filterModel);

				Stream<String> columnFilters = filterModel.entrySet().stream().map(applyColumnFilters);

				Stream<String> groupToFilter = zip(groupKeys.stream(), rowGroups.stream(),
						(key, group) -> group + " = '" + key + "'");

				String filters = concat(columnFilters, groupToFilter).collect(joining(" AND "));

				return filters.isEmpty() ? df : df.filter(filters);
			}
				
		}
		return df;
	}
*/
	private String formNumberQuery(String columnName, NumberColumnFilter filter) {
		Integer filterValue = filter.getFilter();
		String filerType = filter.getType();
		
		 
		
		String filterQuery = formatNumberFilterType(columnName, filerType, filterValue, 0);
		
		System.out.println("-----Number Query--------- " + filterQuery);
		
		  if(filter.getCondition1() != null && filter.getCondition1().containsKey("filter")) {
			  JSONObject condition1 = filter.getCondition1();
				JSONObject condition2 = filter.getCondition2();
				String operator = filter.getOperator();

				String condition1Type = (String) condition1.get("type");
				int condition1Filter = (Integer) condition1.get("filter");
				int condition1FilterTo = (Integer) condition1.get("filterTo");

				String condition2Type = (String) condition2.get("type");
				int condition2Filter = (Integer) condition2.get("filter");
				int condition2FilterTo = (Integer) condition2.get("filterTo");

				String query1 = formatNumberFilterType(columnName, condition1Type, condition1Filter, condition1FilterTo);
				String query2 = formatNumberFilterType(columnName, condition2Type, condition2Filter, condition2FilterTo);
				filterQuery = "( " + query1 + " " + operator + " " + query2 + " )";
		  }
		
		
		return filterQuery;
	}

	private String formTextQuery(String columnName, TextColumnFilter filter) {
		columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
		columnName = "lower(`" + columnName + "`)";

		JSONObject condition1 = filter.getCondition1();
		JSONObject condition2 = filter.getCondition2();
		String operator = filter.getOperator();

		String condition1Type = (String) condition1.get("type");
		String condition1Filter = (String) condition1.get("filter");

		String condition2Type = (String) condition2.get("type");
		String condition2Filter = (String) condition2.get("filter");

		String query1 = formatTextFilterType(columnName, condition1Type, condition1Filter);
		String query2 = formatTextFilterType(columnName, condition2Type, condition2Filter);

		String filterQuery = "( " + query1 + " " + operator + " " + query2 + " )";

		return filterQuery;
	}

	private String formatTextFilterType(String columnName, String expression, String filterText) {
		String query = "";
		if (expression.equals("contains")) {
			query = columnName + " like lower('%" + filterText + "%'" + ")";
		} else if (expression.equalsIgnoreCase("startsWith")) {
			query = columnName + " like lower('" + filterText + "%'" + ")";
		} else if (expression.equalsIgnoreCase("endsWith")) {
			query = columnName + " like lower('%" + filterText + "'" + ")";
		} else if (expression.equalsIgnoreCase("equals")) {
			query = columnName + "=lower('" + filterText + "'" + ")";
		} else if (expression.equalsIgnoreCase("notEqual")) {
			query = columnName + "!=lower('" + filterText + "'" + ")";
		}

		return query;
	}

	private String formatNumberFilterType(String columnName, String expression, int filter, int filterTo) {
		String query = "";
		if (expression.equalsIgnoreCase("equals")) {
			query = columnName + "=" + filter;
		} else if (expression.equalsIgnoreCase("notEqual")) {
			query = columnName + "!=" + filter;
		} else if (expression.equalsIgnoreCase("lessThan")) {
			query = columnName + "<" + filter;
		} else if (expression.equalsIgnoreCase("lessThanOrEqual")) {
			query = columnName + "<=" + filter;
		} else if (expression.equalsIgnoreCase("greaterThan")) {
			query = columnName + ">" + filter;
		} else if (expression.equalsIgnoreCase("greaterThanOrEqual")) {
			query = columnName + ">=" + filter;
		} else if (expression.equalsIgnoreCase("inRange")) {
			query = columnName + "BETWEEN " + filter + " AND " + filterTo;
		}

		return query;
	}

	private BiFunction<String, SetColumnFilter, String> setFilter() {
		return (String columnName, SetColumnFilter filter) -> columnName
				+ (filter.getValues().isEmpty() ? " IN ('') " : " IN " + asString(filter.getValues()));
	}

	
	  private BiFunction<String, TextColumnFilter, String> textFilter() { 
			/*
			 * return (String columnName, TextColumnFilter filter) ->
			 * textFilterInput(columnName, filter.getCondition1(), filter.getCondition2(),
			 * filter.getOperator());
			 */
		  
		  return (String columnName, TextColumnFilter filter) -> textFilterInput(filter, columnName);
	  }
	 

	
	  private String textFilterInput(TextColumnFilter textFilter, String columnName) {	
		    columnName = "lower(" + columnName + ")";
			String query = formatTextFilterType(columnName, textFilter.getType(), textFilter.getFilter());		
		  
		  if(textFilter.getCondition1() != null && textFilter.getCondition1().containsKey("filter")) {
			

				JSONObject condition1 = textFilter.getCondition1();
				JSONObject condition2 = textFilter.getCondition2();
				String operator = textFilter.getOperator();

				String condition1Type = (String) condition1.get("type");
				String condition1Filter = (String) condition1.get("filter");

				String condition2Type = (String) condition2.get("type");
				String condition2Filter = (String) condition2.get("filter");

				String query1 = formatTextFilterType(columnName, condition1Type, condition1Filter);
				String query2 = formatTextFilterType(columnName, condition2Type, condition2Filter);

				query = "( " + query1 + " " + operator + " " + query2 + " )";
		  }
		
		return query;
	}

	private String textFilterInput(String columnName, JSONObject condition1,
	  JSONObject condition2, String operator) {
	  
		  System.out.println("=======columnName============================" +columnName + " : " + condition1 + " : " + operator);
	  
	  String query = columnName +"=\"" +condition1.get("filter").toString()+"\"" +
	  " " + operator + " " + columnName +"=\""
	  +condition2.get("filter").toString()+"\"";
	  System.out.println("============query=====================================" +
	  query); return query; }
	 

	private BiFunction<String, NumberColumnFilter, String> numberFilter() {
		return (String columnName, NumberColumnFilter filter) -> {
			
			
			/*
			 * return columnName + (filerType.equals("inRange") ? " BETWEEN " + filterValue
			 * + " AND " + filter.getFilterTo() : " " + operator + " " + filterValue);
			 */
			
			return formNumberQuery(columnName, (NumberColumnFilter) filter);

			
		};
	}

	private DataResult paginate(Dataset<Row> df, ServerSideGetRowsRequest request, List<String> countData ) {

		int startRow = request.getStartRow();
		int endRow = request.getEndRow();

		// save schema to recreate data frame
		StructType schema = df.schema();

	
		// obtain row count
		long rowCount = df.count();

	

		// convert data frame to RDD and introduce a row index so we can filter results
		// by range
		JavaPairRDD<Row, Long> zippedRows = df.toJavaRDD().zipWithIndex();

		// filter rows by row index using the requested range (startRow, endRow), this
		// ensures we don't run out of memory
		JavaRDD<Row> filteredRdd = zippedRows.filter(pair -> pair._2 >= startRow && pair._2 <= endRow)
				.map(pair -> pair._1);

		// collect paginated results into a list of json objects
		List<String> paginatedResults = sparkSession.sqlContext().createDataFrame(filteredRdd, schema).toJSON()
				.collectAsList(); 
		 

		// calculate last row
		long lastRow = endRow >= rowCount ? rowCount : -1;

		return new DataResult(paginatedResults, lastRow, getSecondaryColumns(df), df.count(), countData );
	}

	private List<String> getSecondaryColumns(Dataset<Row> df) {
		return stream(df.schema().fieldNames()).filter(f -> !rowGroups.contains(f)) // filter out group fields
				.collect(toList());
	}

	private String asString(List<String> l) {
		Function<String, String> addQuotes = s -> "\"" + s + "\"";
		return "(" + l.stream().map(addQuotes).collect(joining(", ")) + ")";
	}

	private String textFilterInput(JSONObject textFilterJson) {

		String query = "";
		try {

			if (textFilterJson != null) {
				if (textFilterJson.containsKey("condition1") && textFilterJson.containsKey("condition2")
						&& textFilterJson.containsKey("operator")) { // multiple filter condition
					JSONObject cond1 = (JSONObject) textFilterJson.get("condition1");
					JSONObject cond2 = (JSONObject) textFilterJson.get("condition2");

					query = cond1.get("filter").toString() + " " + textFilterJson.get("operator") + " "
							+ cond2.get("filter").toString();

				} else {
					query = "'%" + textFilterJson.get("filter") + "%'";
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		System.out.println("-------------Query--------------------" + query);

		return query;
	}

	private static Map<String, String> operatorMap = new HashMap<String, String>() {
		{
			put("equals", "=");
			put("notEqual", "<>");
			put("lessThan", "<");
			put("lessThanOrEqual", "<=");
			put("greaterThan", ">");
			put("greaterThanOrEqual", ">=");
		}
	};

	// ---------------------SSRM Code-----------------------------------//

	public String createDataframeForLocalDiscovery(String tableName) {

		logger.info("create dataframe for local discovery table");
		try {
			String path = commonPath + File.separator + "Dataframe" + File.separator + "DF" + File.separator;

			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", tableName);

			@SuppressWarnings("deprecation")
			Dataset<Row> localDiscoveryDF = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));

			Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");
			formattedDataframe.createOrReplaceTempView("local_discovery");

			/*Dataset<Row> siteKeDF = formattedDataframe.sqlContext()
					.sql("select distinct(site_key) from local_discovery");
			List<String> siteKeys = siteKeDF.as(Encoders.STRING()).collectAsList();		
			*/
			List<String> siteKeys = new ArrayList<String>();
			
			siteKeys.add("ddccdf5f-674f-40e6-9d05-52ab36b10d0e");
		

			// String DataframePath = dataframePath + File.separator;
			siteKeys.forEach(siteKey -> {
				try {
					Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
							"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"
									+ siteKey + "'");

					File f = new File(path + siteKey);
					if (!f.exists()) {
						f.mkdirs();
					}

					dataframeBySiteKey.write().option("escape", "").option("quotes", "")
							.option("ignoreLeadingWhiteSpace", true).partitionBy("site_key", "source_type")
							.format("org.apache.spark.sql.json").mode(SaveMode.Overwrite).save(f.getPath());

				} catch (Exception e) {
					logger.error("Not able to create dataframe for local discovery table site key " + siteKey,
							e.getMessage(), e);
				}
			});

			// remove double quotes from json file
			File[] files = new File(path).listFiles();
			if (files != null) {
				DataframeUtil.formatJsonFile(files);
			}

			createDataframeGlobalView();

			// boolean fileOwnerChanged =
			// DataframeUtil.changeOwnerForFile(fileOwnerGroupName);

			return ZKConstants.SUCCESS;
		} catch (Exception exp) {
			logger.error("Not able to create dataframe {}", exp.getMessage(), exp);
		}

		return ZKConstants.ERROR;
	}

	public DataResult getReportData(ServerSideGetRowsRequest request) {

		String siteKey = request.getSiteKey();
		String source_type = request.getSourceType().toLowerCase();

		if (source_type != null && !source_type.trim().isEmpty() && source_type.contains("hyper")) {
			source_type = source_type + "-" + request.getReportBy().toLowerCase();
		} else if (source_type != null && !source_type.trim().isEmpty()
				&& (source_type.contains("vmware") && request.getReportBy().toLowerCase().contains("host"))) {
			source_type = source_type + "-" + request.getReportBy().toLowerCase();
		} else if (source_type != null && !source_type.trim().isEmpty()
				&& (source_type.contains("nutanix") && request.getReportBy().toLowerCase().contains("host"))) {
			source_type = source_type + "-" + request.getReportBy().toLowerCase();
		} else if (source_type != null && !source_type.trim().isEmpty()
				&& (source_type.contains("nutanix") && request.getReportBy().toLowerCase().equalsIgnoreCase("vm"))) {
			source_type = source_type + "-" + "guest";
		}

		System.out.println("---------source_type------" + source_type);

		boolean isDiscoveryDataInView = false;
		Dataset<Row> dataset = null;
		String viewName = siteKey + "_" + source_type.toLowerCase();
		viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");
		try {
			dataset = sparkSession.sql("select * from global_temp." + viewName);
			dataset.cache();
			isDiscoveryDataInView = true;
		} catch (Exception e) {
			System.out.println("---------View Not exists--------");
		}

		if (!isDiscoveryDataInView) {
			File verifyDataframePath = new File(commonPath + File.separator + "Dataframe" + File.separator
					+ siteKey + File.separator + "site_key=" + siteKey + File.separator + "source_type=" + source_type);

			if (verifyDataframePath.exists()) {
				createSingleDataframe(siteKey, source_type, verifyDataframePath.getAbsolutePath());
				dataset = sparkSession.sql("select * from global_temp." + viewName);
				dataset.cache();
			} else {
				createDataframeOnTheFly(siteKey, source_type);
				dataset = sparkSession.sql("select * from global_temp." + viewName);
				dataset.cache();
			}
		}

	
		dataset.printSchema();
		
		rowGroups = request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
		groupKeys = request.getGroupKeys();
		valueColumns = request.getValueCols();
		pivotColumns = request.getPivotCols();
		filterModel = request.getFilterModel();
		sortModel = request.getSortModel();
		isPivotMode = request.isPivotMode();
		isGrouping = rowGroups.size() > groupKeys.size();

		rowGroups = formatInputColumnNames(rowGroups);
		groupKeys = formatInputColumnNames(groupKeys);
		sortModel = formatSortModel(sortModel);
			
		dataset = orderBy(groupBy(filter(dataset)));	
		Dataset<Row> countData = sparkSession.emptyDataFrame(); 
		
		try {
			List<String> numericColumns = getReportNumericalHeaders(request.getReportType(), source_type, request.getReportBy(),request.getSiteKey());
			if(numericColumns != null) {
				dataset.createOrReplaceTempView("tmpReport");
				String numericCol = String.join(",", numericColumns
			            .stream()
			            .map(col -> ("sum(`" + col + "`) as `"+col+"`"))
			            .collect(Collectors.toList()));	
				
				countData = sparkSession.sqlContext().sql("select "+numericCol+"  from tmpReport");//.sqlContext().sql("select `Total Size` group by `Total Size`").groupBy(new Column("`Total Size`""));
				 
				 
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	
		return paginate(dataset, request, countData.toJSON().collectAsList());

	}

	public DataResult getReportData_old(ServerSideGetRowsRequest request) {

		String siteKey = request.getSiteKey();
		String source_type = request.getSourceType().toLowerCase();

		if (source_type != null && !source_type.trim().isEmpty() && source_type.contains("hyper")) {
			source_type = source_type + "-" + request.getReportBy().toLowerCase();
		} else if (source_type != null && !source_type.trim().isEmpty()
				&& (source_type.contains("vmware") && request.getReportBy().toLowerCase().contains("host"))) {
			source_type = source_type + "-" + request.getReportBy().toLowerCase();
		} else if (source_type != null && !source_type.trim().isEmpty()
				&& (source_type.contains("nutanix") && request.getReportBy().toLowerCase().contains("host"))) {
			source_type = source_type + "-" + request.getReportBy().toLowerCase();
		} else if (source_type != null && !source_type.trim().isEmpty()
				&& (source_type.contains("nutanix") && request.getReportBy().toLowerCase().equalsIgnoreCase("vm"))) {
			source_type = source_type + "-" + "guest";
		}

		boolean isDiscoveryDataInView = false;
		Dataset<Row> dataset = null;
		String viewName = siteKey + "_" + source_type.toLowerCase();
		viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");
		try {
			dataset = sparkSession.sql("select * from global_temp." + viewName);
			dataset.cache();
			isDiscoveryDataInView = true;
		} catch (Exception e) {
			System.out.println("---------View Not exists--------");
		}

		try {
			// isDiscoveryDataInView = false;
			if (!isDiscoveryDataInView) {
				File verifyDataframePath = new File(
						commonPath + File.separator + "Dataframe" + File.separator + siteKey + File.separator
								+ "site_key=" + siteKey + File.separator + "source_type=" + source_type);
				System.out.println(
						"----->>>>>>>>>>>>>>>>----View Not exists--verifyDataframePath------" + verifyDataframePath);
				if (!verifyDataframePath.exists()) {
					System.out.println("---------true-----");
					// createDataframeOnTheFly(siteKey, source_type);
				}
				System.out.println("---------false-----");
				String filePath = commonPath + File.separator + "Dataframe" + File.separator + siteKey
						+ File.separator + "site_key=" + siteKey + File.separator + "source_type=" + source_type
						+ File.separator + "*.json";
				System.out.println("---------true-filePath----" + filePath);
				dataset = sparkSession.read().json(filePath);
				dataset.createOrReplaceTempView("tmpView");
				dataset = sparkSession.sql(
						"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1 ");
				dataset.createOrReplaceGlobalTempView(viewName);
				dataset.cache();
			}

			// ---------------------EOL EOS---------------------------//

			int osCount = eolService.getEOLEOSData();
			int hwCount = eolService.getEOLEOSHW();

			String hwJoin = "";
			String hwdata = "";
			String osJoin = "";
			String osdata = "";

			if (osCount > 0) {
				if (Arrays.stream(dataset.columns()).anyMatch("Server Type"::equals)
						&& dataset.first().fieldIndex("Server Type") != -1) {
					Dataset<Row> eolos = sparkSession
							.sql("select * from global_temp.eolDataDF where lower(os_type)='" + source_type + "'"); // where
																													// lower(`Server
																													// Name`)="+source_type
					eolos.createOrReplaceTempView("eolos");
					eolos.show();

					if (eolos.count() > 0) {
						osJoin = " left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(ldView.`OS Version`) and lcase(eol.os_type)=lcase(ldView.`Server Type`) "; // where
																																													// lcase(eol.os_version)=lcase(ldView.`OS
																																													// Version`)
																																													// and
																																													// lcase(eol.os_type)=lcase(ldView.`Server
																																													// Type`)
						osdata = ",eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`";
					}
				}

			}

			if (hwCount > 0) {
				if (Arrays.stream(dataset.columns()).anyMatch("Server Model"::equals)
						&& dataset.first().fieldIndex("Server Model") != -1) {

					hwJoin = " left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(ldView.`Server Model`, ' ', ''))";
					hwdata = ",eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`";
				}
			}

			// sparkSession.sql("select * from (select *, row_number() over (partition by
			// source_id order by log_date desc) as rank from tmpView ) ld where
			// ld.rank=1");

			String sql = "select * from (" + " select ldView.*" + osdata + hwdata
					+ " ,ROW_NUMBER() OVER (PARTITION BY ldView.`Server Name` ORDER BY ldView.`log_date` desc) as my_rank"
					+ " from global_temp." + viewName + " ldView" + hwJoin + osJoin + " ) ld where ld.my_rank = 1";

			dataset = sparkSession.sql(sql).toDF();

			if ((osCount > 0 || hwCount > 0) && dataset.count() == 0) {
				hwJoin = "";
				hwdata = "";
				osJoin = "";
				osdata = "";
				String sqlDf = "select * from (" + " select ldView.*" + osdata + hwdata
						+ " ,ROW_NUMBER() OVER (PARTITION BY ldView.`Server Name` ORDER BY ldView.`log_date` desc) as my_rank"
						+ " from global_temp." + viewName + " ldView" + hwJoin + osJoin + " ) ld where ld.my_rank = 1";

				dataset = sparkSession.sql(sqlDf).toDF();
			}

			actualColumnNames = Arrays.asList(dataset.columns());
			Dataset<Row> renamedDataSet = renameDataFrame(dataset);
			renamedDataSet.createOrReplaceTempView(viewName + "renamedDataSet");
			if (request.getEndRow() == 0) { // temp code
				request.setEndRow((int) dataset.count());
			}

			rowGroups = request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
			groupKeys = request.getGroupKeys();
			valueColumns = request.getValueCols();
			pivotColumns = request.getPivotCols();
			filterModel = request.getFilterModel();
			sortModel = request.getSortModel();
			isPivotMode = request.isPivotMode();
			isGrouping = rowGroups.size() > groupKeys.size();

			rowGroups = formatInputColumnNames(rowGroups);
			groupKeys = formatInputColumnNames(groupKeys);
			sortModel = formatSortModel(sortModel);

			Dataset<Row> df = renamedDataSet.sqlContext().sql(selectSql() + " from " + viewName + "renamedDataSet");
			renamedColumnNames = Arrays.asList(df.columns());

			Dataset<Row> results = dataset = orderBy(groupBy(filter(df)));
			//dataset = orderBy(groupBy(filter(df)));
			results = reassignColumnName(actualColumnNames, renamedColumnNames, results);
			// results.printSchema();

			results = results.dropDuplicates();

			List<String> numericalHeaders = getReportNumericalHeaders("Discovery",
					request.getSourceType().toLowerCase(), "Discovery", siteKey);

			List<String> columns = Arrays.asList(results.columns());

			for (String column : numericalHeaders) {
				if (columns.contains(column)) {
					results = results.withColumn(column, results.col(column).cast("float"));
				}

			}

			if (source_type.equalsIgnoreCase("vmware-host")) {
				results = results.withColumn("Server Type", lit("vmware-host"));
			}

			return paginate(results, request, df.toJSON().collectAsList());
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			logger.error("Exception occured while fetching local discoverydata from DF{}", e.getMessage(), e);
		}

		return null;
	}

	public List<String> getReportNumericalHeaders(String reportName, String source_type, String reportBy,
			String siteKey) {
		// TODO Auto-generated method stub
		return reportDao.getReportNumericalHeaders(reportName, source_type, reportBy, siteKey);
	}

	private void createDataframeOnTheFly(String siteKey, String source_type) {
		try {
			source_type = source_type.toLowerCase();
			String path = commonPath + File.separator + "Dataframe" + File.separator;

			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable",
					"(select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"
							+ siteKey + "' and lower(source_type)='" + source_type + "') as foo");

			@SuppressWarnings("deprecation")
			Dataset<Row> localDiscoveryDF = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			localDiscoveryDF.show();
			Dataset<Row> dataframeBySiteKey = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");

			File f = new File(path + siteKey);
			if (!f.exists()) {
				f.mkdir();
			}

			dataframeBySiteKey.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true)
					.partitionBy("site_key", "source_type").format("org.apache.spark.sql.json").mode(SaveMode.Overwrite)
					.save(f.getPath());

			String viewName = siteKey + "_" + source_type.toLowerCase();
			viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");

			// remove double quotes from json file
			File[] files = new File(path).listFiles();
			if (files != null) {
				DataframeUtil.formatJsonFile(files);
			}

			for (File file : files) {
				if (file.isDirectory()) {
					createLocalDiscoveryView(file.listFiles());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			logger.error("Not able to create dataframe for local discovery table site key " + siteKey, e.getMessage(),
					e);
		}

	}

	private List<SortModel> formatSortModel(List<SortModel> sortModels) {
		List<SortModel> sortModel = new ArrayList<>();
		for (SortModel sm : sortModels) {
			sm.setColId(sm.getColId().replaceAll("\\s+", "_").toLowerCase());
			sortModel.add(sm);
		}
		return sortModel;
	}

	private List<String> formatInputColumnNames(List<String> list) {
		List<String> tmpList = new ArrayList<>();
		if (list != null && !list.isEmpty()) {
			for (String l : list) {
				tmpList.add(l.replaceAll("\\s+", "_").toLowerCase());
			}
		}
		return tmpList;
	}

	private Dataset<Row> reassignColumnName(List<String> actualColumnNames, List<String> renamedColumnNames,
			Dataset<Row> df) {
		Map<String, String> columnNameMap = new HashMap<>();
		actualColumnNames.forEach(actualName -> {
			String actualNameTmp = actualName.replaceAll("\\s+", "_");
			renamedColumnNames.forEach(newName -> {
				if (actualNameTmp.equalsIgnoreCase(newName)) {
					columnNameMap.put(newName, actualName);
				}
			});
		});

		for (Entry<String, String> map : columnNameMap.entrySet()) {
			df = df.withColumnRenamed(map.getKey(), map.getValue());
		}

		return df;
	}

	public static Dataset<Row> renameDataFrame(Dataset<Row> dataset) {
		for (String column : dataset.columns()) {
			dataset = dataset.withColumnRenamed(column, column.replaceAll(" ", "_").toLowerCase());
		}

		return dataset;
	}

	public String appendLocalDiscovery(String siteKey, String sourceType, JSONObject data) {
		String result = "";
		try {
			sourceType = sourceType.toLowerCase();
			String filePath = commonPath + File.separator + "Dataframe" + File.separator + siteKey
					+ File.separator + "site_key=" + siteKey + File.separator + "source_type=" + sourceType
					+ File.separator;
			File siteKeyAndSourceType = new File(filePath);

			JSONObject newJson = new JSONObject();
			newJson.put("source_id", data.get("sourceId").toString());
			newJson.put("site_key", data.get("siteKey").toString());
			newJson.put("data_temp", data.get("data").toString());
			newJson.put("log_date", data.get("logDate").toString());
			newJson.put("source_type", data.get("sourceType").toString());
			newJson.put("actual_os_type", data.get("actualOSType").toString());
			newJson.put("server_name", data.get("serverName").toString());

			System.out.println("---------------newJson-----------------------------" + newJson);

			String viewName = siteKey + "_" + sourceType;
			viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");

			if (siteKeyAndSourceType.exists()) { // site key and source type present

				Path path = Paths.get(filePath);
				List<String> filesExists = DataframeUtil.getAllFileNamesByType(path, ".json");

				if (filesExists != null && filesExists.size() > 0) { // append

					String tmpFile = writeJosnFile(commonPath + File.separator + "Dataframe" + File.separator
							+ siteKey + File.separator + "site_key=" + siteKey + File.separator + "tmp.json",
							newJson.toJSONString());

					if (!tmpFile.isEmpty()) {
						File[] files = new File[1];
						files[0] = new File(tmpFile);
						DataframeUtil.formatJsonFile(files);

						try {

							Dataset<Row> newDataframeToAppend = sparkSession.read().json(tmpFile);
							newDataframeToAppend = newDataframeToAppend.drop("site_key").drop("source_type");
							newDataframeToAppend.write().option("escape", "").option("quotes", "")
									.option("ignoreLeadingWhiteSpace", true).mode(SaveMode.Append)
									.format("org.apache.spark.sql.json").save(filePath);
							newDataframeToAppend.unpersist();

							System.out.println(
									"-----------newDataframeToAppend------------------" + newDataframeToAppend.count());

							Dataset<Row> mergedDataframe = sparkSession.read().json(filePath);
							mergedDataframe.createOrReplaceTempView("tmpView");
							// mergedDataframe.sqlContext().sql("select * from (select *, rank() over
							// (partition by source_id order by log_date desc) as rank from tmpView ) ld
							// where ld.rank=1 ");
							sparkSession.sql(
									"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
							mergedDataframe.createOrReplaceGlobalTempView(viewName);
							mergedDataframe.cache();

							sparkSession.sql("REFRESH TABLE global_temp." + viewName);
							System.out.println("----------------Dataframe Append--------------------------------");
							DataframeUtil.deleteFile(tmpFile);
							result = ZKConstants.SUCCESS;
						} catch (Exception e) {
							e.printStackTrace();
							StringWriter errors = new StringWriter();
							e.printStackTrace(new PrintWriter(errors));
							String ex = errors.toString();
							ExceptionHandlerMail.errorTriggerMail(ex);
						}
					}
				}
			} else {
				// create template

				// check if source type exits
				String sourceTypePath = commonPath + File.separator + "Dataframe" + File.separator + siteKey
						+ File.separator + "site_key=" + siteKey + File.separator + "source_type="
						+ sourceType.toLowerCase();

				File newSiteKey = new File(
						commonPath + File.separator + "Dataframe" + File.separator + siteKey + File.separator);
				boolean siteKeyPresent = true;
				if (!newSiteKey.exists()) {
					siteKeyPresent = false;
				}

				File sourceTypeFolder = new File(sourceTypePath);

				if (siteKeyPresent && !sourceTypeFolder.exists() || !siteKeyPresent) {
					sourceTypeFolder.mkdir();

					System.out.println("------------create new dataframe for new source type---------------- ");
					String tmpFile = writeJosnFile(newSiteKey.getAbsolutePath() + "tmp.json", newJson.toString());
					File[] files = new File[1];
					files[0] = new File(tmpFile);
					DataframeUtil.formatJsonFile(files);

					Dataset<Row> newDataframe = sparkSession.read().json(tmpFile);
					newDataframe = newDataframe.drop("site_key").drop("source_type");

					// String newFolderName = commonPath + File.separator + "Dataframe" +
					// File.separator + siteKey + File.separator + "site_key="+siteKey +
					// File.separator;

					newDataframe.write().option("escape", "").option("quotes", "")
							.option("ignoreLeadingWhiteSpace", true).format("org.apache.spark.sql.json")
							.mode(SaveMode.Overwrite).save(sourceTypePath);
					newDataframe.unpersist();

					Dataset<Row> mergedDataframe = sparkSession.read().json(filePath);
					mergedDataframe.createOrReplaceTempView("tmpView");
					// mergedDataframe.sqlContext().sql("select * from (select *, rank() over
					// (partition by source_id order by log_date desc) as rank from tmpView ) ld
					// where ld.rank=1 ");
					sparkSession.sql(
							"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
					mergedDataframe.createOrReplaceGlobalTempView(viewName);
					mergedDataframe.cache();
					mergedDataframe.printSchema();

					DataframeUtil.deleteFile(tmpFile);
					System.out.println("---------new Dataframe created with new source type-------------- ");
					result = ZKConstants.SUCCESS;
				}

			}

			// boolean fileOwnerChanged =
			// DataframeUtil.changeOwnerForFile(fileOwnerGroupName);

		} catch (Exception e) {
			// logger.error("Exception occured when append dataframe {}", e.getMessage(),
			// e);
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return result;
	}

	private String writeJosnFile(String filePath, String data) {
		String result = "";
		try {
			data = data.replaceAll("server_name", "sever_name_col");
			FileWriter file = new FileWriter(filePath);
			file.write(data.toString());
			file.close();

			// DataframeUtil.setFilePermission(filePath);

			File f = new File(filePath);
			if (f.exists()) {
				result = f.getAbsolutePath();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return result;
	}

	public void createDataframeGlobalView() {
		String path = commonPath + File.separator + "Dataframe" + File.separator + "DF" + File.separator;
		File[] files = new File(path).listFiles();
		if (files != null) {
			createLocalDiscoveryView(files);
		}

	}

	private void createLocalDiscoveryView(File[] files) {
		String path = commonPath + File.separator + "Dataframe" + File.separator + "DF" + File.separator;
		for (File file : files) {
			if (file.isDirectory()) {
				createLocalDiscoveryView(file.listFiles());
			} else {
				createDataframeGlobally(path, file);
			}
		}

	}

	private void createSingleDataframe(String siteKey, String source_type, String filePath) {
		try {
			String viewName = siteKey + "_" + source_type.toLowerCase();
			viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");

			Dataset<Row> dataset = sparkSession.read().json(filePath + File.separator + "*.json");
			dataset.createOrReplaceTempView("tmpView");
			Dataset<Row> filteredData = sparkSession.emptyDataFrame();

			// select * from (select *, row_number() over (partition by source_id order by
			// log_date desc) as rank from tmpView ) ld where ld.rank=1

			String sql = " select ldView.*, eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`,eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`"
					+ " from tmpView ldView  left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(ldView.`Server Model`, ' ', '')) left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(ldView.`OS Version`) and lcase(eol.os_type)=lcase(ldView.`Server Type`) ";
			try {
				dataset = sparkSession.sql(sql);
				dataset.createOrReplaceTempView("datawithoutFilter");
				filteredData = sparkSession.sql(
						"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from datawithoutFilter) ld where ld.rank=1 ");
			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
				sql = "select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1";
				dataset.createOrReplaceTempView("datawithoutFilter");
				filteredData = sparkSession.sql(
						"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from datawithoutFilter) ld where ld.rank=1 ");

			}
			List<String> numericalHeaders = new ArrayList<String>();
			if (!serverDiscoveryNumbericalColumns.containsKey("Discovery" + source_type.toLowerCase())) {
				numericalHeaders = getReportNumericalHeaders("Discovery", source_type.toLowerCase(), "Discovery",
						siteKey);
				serverDiscoveryNumbericalColumns.put("Discovery" + source_type.toLowerCase(), numericalHeaders);
			} else {
				numericalHeaders = serverDiscoveryNumbericalColumns.get("Discovery" + source_type.toLowerCase());
			}

			List<String> columns = Arrays.asList(filteredData.columns());

			for (String column : numericalHeaders) {
				if (columns.contains(column)) {
					filteredData = filteredData.withColumn(column, filteredData.col(column).cast("integer"));
				}
			}

			if (source_type.equalsIgnoreCase("vmware-host")) {
				filteredData = filteredData.withColumn("Server Type", lit("vmware-host"));
			}

			filteredData.createOrReplaceGlobalTempView(viewName);

			System.out.println("--------single-View created-------- :: " + viewName);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
	}

	private void createDataframeGlobally(String path, File file) {
		String filePath = file.getAbsolutePath();

		if (filePath.endsWith(".json")) {
			String source_type = file.getParentFile().getName().replace("source_type=", "").trim();
			String siteKey = file.getParentFile().getParentFile().getName().replace("site_key=", "").trim();
			String dataframeFilePath = path + siteKey + File.separator + "site_key=" + siteKey + File.separator
					+ "source_type=" + source_type + File.separator + "*.json";
			String viewName = siteKey + "_" + source_type.toLowerCase();
			viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");
			try {
				Dataset<Row> dataset = sparkSession.read().json(dataframeFilePath);
				dataset.createOrReplaceTempView("tmpView");
				Dataset<Row> filteredData = sparkSession.emptyDataFrame();

				dataset.printSchema();
				dataset.show();
				System.out.println("----viewName-----" + viewName + " : " + dataframeFilePath);

				System.out.println("----dataset-----" + dataset.count());
				// select * from (select *, row_number() over (partition by source_id order by
				// log_date desc) as rank from tmpView ) ld where ld.rank=1

				String sql = " select ldView.*, eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`,eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`"
							+ " from tmpView ldView  left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(ldView.`Server Model`, ' ', '')) left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(ldView.`OS Version`) and lcase(eol.os_name)=lcase(ldView.`OS`) ";

				try {
					dataset = sparkSession.sql(sql);
					dataset.createOrReplaceTempView("datawithoutFilter");
					filteredData = sparkSession.sql(
							"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from datawithoutFilter) ld where ld.rank=1 ");
				} catch (Exception e) {
					//e.printStackTrace();
					/*StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					String ex = errors.toString();
					ExceptionHandlerMail.errorTriggerMail(ex);*/
					sql = "select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1";
					dataset.createOrReplaceTempView("datawithoutFilter");
					filteredData = sparkSession.sql(
							"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from datawithoutFilter) ld where ld.rank=1 ");

				}
				List<String> numericalHeaders = new ArrayList<String>();
				if (!serverDiscoveryNumbericalColumns.containsKey("Discovery" + source_type.toLowerCase())) {
					numericalHeaders = getReportNumericalHeaders("Discovery", source_type.toLowerCase(), "Discovery",
							siteKey);
					serverDiscoveryNumbericalColumns.put("Discovery" + source_type.toLowerCase(), numericalHeaders);
				} else {
					numericalHeaders = serverDiscoveryNumbericalColumns.get("Discovery" + source_type.toLowerCase());
				}

				List<String> columns = Arrays.asList(filteredData.columns());

				for (String column : numericalHeaders) {
					if (columns.contains(column)) {
						filteredData = filteredData.withColumn(column, filteredData.col(column).cast("integer"));
					}
				}

				if (source_type.equalsIgnoreCase("vmware-host")) {
					filteredData = filteredData.withColumn("Server Type", lit("vmware-host"));
				}

				filteredData.createOrReplaceGlobalTempView(viewName);

				System.out.println("---------View created-------- :: " + viewName);
			} catch (Exception e) {
				//e.printStackTrace();
			/*	StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);*/
			}

		}
	}

	public JSONArray getReportHeader(String reportType, String deviceType, String reportBy) {
		JSONArray jSONArray = new JSONArray();
		// jSONArray = postgresCompServiceImpl.getReportHeader(reportType, deviceType,
		// reportBy);
		return jSONArray;
	}

	public String createReportHeaderDF() {
		try {
			System.out.println("---------------2--------------------");
			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", "report_columns");

			@SuppressWarnings("deprecation")
			Dataset<Row> headerDF = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			headerDF.createOrReplaceGlobalTempView("report_columns");
			System.out.println("---------------3--------------------");
			return ZKConstants.SUCCESS;

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return ZKConstants.ERROR;
	}

	private void createReportHeaderGlobalView(Dataset<Row> headerDF) {

		try {

		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public String createDataframeForReportHeader(String tableName) {
		logger.info("create dataframe for local discovery table");
		try {
			commonPath = commonPath + File.separator + "Dataframe" + File.separator;
			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", tableName);

			@SuppressWarnings("deprecation")
			Dataset<Row> localDiscoveryDF = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));

			Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");
			formattedDataframe.createOrReplaceTempView("local_discovery");

			Dataset<Row> siteKeDF = formattedDataframe.sqlContext()
					.sql("select distinct(site_key) from local_discovery");
			List<String> siteKeys = siteKeDF.as(Encoders.STRING()).collectAsList();

			// String DataframePath = dataframePath + File.separator;
			siteKeys.forEach(siteKey -> {
				try {
					Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
							"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"
									+ siteKey + "'");
					File f = new File(commonPath + File.separator + "Dataframe" + File.separator + siteKey);
					if (!f.exists()) {
						f.mkdir();
					}
					dataframeBySiteKey.write().option("escape", "").option("quotes", "")
							.option("ignoreLeadingWhiteSpace", true).partitionBy("site_key", "source_type")
							.format("org.apache.spark.sql.json").mode(SaveMode.Overwrite).save(f.getPath());
					dataframeBySiteKey.unpersist();
				} catch (Exception e) {
					logger.error("Not able to create dataframe for local discovery table site key " + siteKey,
							e.getMessage(), e);
				}
			});

			// remove double quotes from json file
			File[] files = new File(commonPath).listFiles();
			if (files != null) {
				DataframeUtil.formatJsonFile(files);
			}

			createDataframeGlobalView();

			return ZKConstants.SUCCESS;
		} catch (Exception exp) {
			logger.error("Not able to create dataframe {}", exp.getMessage(), exp);
		}

		return ZKConstants.ERROR;

	}

	/*
	 * @SuppressWarnings("unchecked") public JSONObject getSubReportList(String
	 * deviceType, String reportName) throws IOException, ParseException {
	 * System.out.println("!!!!! deviceType: " + deviceType);
	 * if(deviceType.equalsIgnoreCase("HP-UX")) { deviceType = "hpux"; } JSONParser
	 * parser = new JSONParser();
	 * 
	 * Map<String, JSONArray> columnsMap = new LinkedHashMap<String, JSONArray>();
	 * JSONObject result = new JSONObject();
	 * 
	 * String linkDevices = ZKModel.getProperty(ZKConstants.CRDevice); JSONArray
	 * devicesArray = (JSONArray) parser.parse(linkDevices);
	 * if(reportName.trim().equalsIgnoreCase("discovery")) { String linkColumns =
	 * ZKModel.getProperty(ZKConstants.CRCOLUMNNAMES); JSONArray columnsArray =
	 * (JSONArray) parser.parse(linkColumns);
	 * 
	 * for(int a = 0; a < devicesArray.size(); a++) { JSONArray columnsNameArray =
	 * new JSONArray(); for(int i = 0; i < columnsArray.size(); i++) { JSONObject
	 * jsonObject = (JSONObject) columnsArray.get(i);
	 * if(jsonObject.containsKey(devicesArray.get(a).toString().toLowerCase())) {
	 * columnsNameArray = (JSONArray)
	 * parser.parse(jsonObject.get(devicesArray.get(a).toString().toLowerCase()).
	 * toString()); columnsMap.put(devicesArray.get(a).toString().toLowerCase(),
	 * columnsNameArray); } } }
	 * 
	 * } else if(reportName.trim().equalsIgnoreCase("compatibility")) { JSONArray
	 * columnsNameArray = new JSONArray(); columnsNameArray.add("Host Name");
	 * for(int a = 0; a < devicesArray.size(); a++) {
	 * columnsMap.put(devicesArray.get(a).toString().toLowerCase(),
	 * columnsNameArray); }
	 * 
	 * }
	 * 
	 * 
	 * if(!columnsMap.isEmpty()) { Map<String, Properties> propMap = new
	 * TreeMap<String, Properties>(); if(deviceType.equalsIgnoreCase("all")) {
	 * for(int i = 0; i < devicesArray.size(); i++) { String deviceValue = "";
	 * if(devicesArray.get(i).toString().equalsIgnoreCase("HP-UX")) { deviceValue =
	 * "hpux"; } String path = "/opt/config/" + deviceValue.toLowerCase() +
	 * "ServerClickReport.properties"; System.out.println("!!!!! path: " + path);
	 * InputStream inputFile = null;
	 * 
	 * try {
	 * 
	 * File file = new File(path); if(file.exists()) {
	 * 
	 * inputFile = new FileInputStream(file); Properties prop = new Properties();
	 * prop.load(inputFile);
	 * 
	 * if(devicesArray.get(i).toString().equalsIgnoreCase("HP-UX")) { deviceValue =
	 * "hpux"; } propMap.put(deviceValue.toLowerCase(), prop); }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); StringWriter errors = new
	 * StringWriter(); e.printStackTrace(new PrintWriter(errors)); String ex =
	 * errors.toString(); ExceptionHandlerMail.errorTriggerMail(ex); }
	 * 
	 * 
	 * 
	 * } } else { String path = "/opt/config/" + deviceType.toLowerCase() +
	 * "ServerClickReport.properties"; System.out.println("!!!!! path: " + path);
	 * InputStream inputFile = null;
	 * 
	 * try {
	 * 
	 * File file = new File(path); if(file.exists()) {
	 * 
	 * inputFile = new FileInputStream(file); Properties prop = new Properties();
	 * prop.load(inputFile); propMap.put(deviceType.toLowerCase(), prop); }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); StringWriter errors = new
	 * StringWriter(); e.printStackTrace(new PrintWriter(errors)); String ex =
	 * errors.toString(); ExceptionHandlerMail.errorTriggerMail(ex); }
	 * 
	 * }
	 * 
	 * List<String> propKeys = new ArrayList<String>(propMap.keySet());
	 * System.out.println("!!!!! propKeys: " + propKeys);
	 * 
	 * 
	 * ZenfraJSONObject resultObject = new ZenfraJSONObject();
	 * 
	 * JSONArray postDataColumnArray = new JSONArray(); List<String> columnsKey =
	 * new ArrayList<String>(columnsMap.keySet());
	 * 
	 * 
	 * for(int i = 0; i < columnsKey.size(); i++) { JSONArray columnsNameArray =
	 * columnsMap.get(columnsKey.get(i)); JSONObject tabInfoObject = new
	 * JSONObject(); for(int j = 0; j < columnsNameArray.size(); j++) {
	 * if(!columnsNameArray.get(j).toString().equalsIgnoreCase("vCenter")) {
	 * ZenfraJSONObject tabArrayObject = new ZenfraJSONObject(); for(int k = 0; k <
	 * propKeys.size(); k++) { Properties prop = propMap.get(propKeys.get(k));
	 * List<Object> tabKeys = new ArrayList<Object>(prop.keySet()); JSONArray
	 * tabInnerArray = new JSONArray();
	 * 
	 * for(int l = 0; l < tabKeys.size(); l++) { ZenfraJSONObject tabValueObject =
	 * new ZenfraJSONObject(); String key = tabKeys.get(l).toString(); String keyId
	 * = tabKeys.get(l).toString(); //System.out.println("!!!!! key: " + key);
	 * String value = ""; String keyName = ""; String keyLabel = ""; String keyView
	 * = ""; String keyOrdered = ""; if(key.contains("$")) { String[] keyArray =
	 * key.split("\\$"); value = keyArray[0]; keyName = keyArray[0].replace("~",
	 * ""); keyLabel = value.replace("~", " "); keyView = keyArray[1]; keyOrdered =
	 * keyArray[2]; } else { keyName = key.replace("~", ""); keyLabel =
	 * key.replace("~", " "); keyView = "H"; keyOrdered = "0"; }
	 * 
	 * tabValueObject.put("value", keyId); tabValueObject.put("name", keyName);
	 * tabValueObject.put("label", keyLabel); tabValueObject.put("view", keyView);
	 * tabValueObject.put("ordered", Integer.parseInt(keyOrdered));
	 * tabInnerArray.add(tabValueObject); } if(!tabInnerArray.isEmpty()) {
	 * tabArrayObject.put(propKeys.get(k), tabInnerArray); }
	 * 
	 * } if(!tabArrayObject.isEmpty()) { tabInfoObject.put("tabInfo",
	 * tabArrayObject); tabInfoObject.put("tabInfo", tabArrayObject);
	 * tabInfoObject.put("skipValues", new JSONArray()); tabInfoObject.put("title",
	 * "Detailed Report for Server (" + columnsNameArray.get(j) + ")");
	 * if(!postDataColumnArray.contains(columnsNameArray.get(j))) {
	 * if(deviceType.equalsIgnoreCase("vmware")) { postDataColumnArray.add("VM");
	 * postDataColumnArray.add("vCenter"); } else
	 * if(deviceType.equalsIgnoreCase("vmware-host")) {
	 * postDataColumnArray.add("Server Name"); postDataColumnArray.add("vCenter"); }
	 * else { postDataColumnArray.add(columnsNameArray.get(j)); }
	 * 
	 * } resultObject.put(columnsNameArray.get(j), tabInfoObject);
	 * //resultObject.put("skipValues", new JSONArray());
	 * result.put("subLinkColumns", resultObject); } } }
	 * 
	 * }
	 * 
	 * 
	 * 
	 * result.put("postDataColumns", postDataColumnArray); result.put("deviceType",
	 * deviceType.toLowerCase().trim()); JSONArray refferedDeviceType = new
	 * JSONArray(); if(reportName.equalsIgnoreCase("compatibility")) {
	 * refferedDeviceType.add("OS Type"); } else
	 * if(reportName.equalsIgnoreCase("taskList")) {
	 * refferedDeviceType.add("Source Type"); } result.put("deviceTypeRefColumn",
	 * refferedDeviceType);
	 * 
	 * 
	 * 
	 * }
	 * 
	 * //System.out.println("!!!!! result: " + result); return result; }
	 */

	public JSONArray getReportHeaderForMigrationMethod(String siteKey, String deviceType) {
		JSONArray resultArray = new JSONArray();

		String viewName = siteKey + "_" + deviceType.toLowerCase();
		viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");
		Dataset<Row> dataset = sparkSession.emptyDataFrame();
		try {
			dataset = sparkSession.sql("select * from global_temp." + viewName);
		} catch (Exception e) {
			String filePath = commonPath + File.separator + "Dataframe" + File.separator + siteKey
					+ File.separator + "site_key=" + siteKey + File.separator + "source_type=" + deviceType
					+ File.separator + "*.json";
			dataset = sparkSession.read().json(filePath);
			dataset.createOrReplaceTempView("tmpView");
			dataset = sparkSession.sql(
					"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1 ");
			dataset.createOrReplaceGlobalTempView(viewName);
		}
		List<String> header = Arrays.asList(dataset.columns());

		if (header != null && !header.isEmpty()) {
			int rowCount = 1;
			for (String col : header) {
				JSONObject obj = new JSONObject();

				obj.put("displayName", col);
				obj.put("actualName", col);
				obj.put("dataType", "String");
				if (rowCount == 1) {
					obj.put("lockPinned", true);
					obj.put("lockPosition", true);
					obj.put("pinned", "left");
				} else {
					obj.put("lockPinned", false);
					obj.put("lockPosition", false);
					obj.put("pinned", "");
				}
				resultArray.add(obj);
			}
		}
		return resultArray;
	}

	public String recreateLocalDiscovery(String siteKey, String sourceType) {
		String result = "";
		try {
			sourceType = sourceType.toLowerCase();

			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", "local_discovery");

			
			sparkSession.sqlContext().load("jdbc", options).registerTempTable("local_discovery");
			
			boolean isMultipleSourceType = false;
			if (sourceType.contains("hyper") || sourceType.contains("vmware") || sourceType.contains("nutanix")) {
				isMultipleSourceType = true;
			}

			if (!isMultipleSourceType) {
				reinitiateDiscoveryDataframe(siteKey, sourceType);
			} else {
				if (sourceType.contains("hyper")) {
					reinitiateDiscoveryDataframe(siteKey, "hyper-v-host");
					reinitiateDiscoveryDataframe(siteKey, "hyper-v-vm");
				} else if (sourceType.contains("vmware")) {
					reinitiateDiscoveryDataframe(siteKey, "vmware");
					reinitiateDiscoveryDataframe(siteKey, "vmware-host");
				} else if (sourceType.contains("nutanix")) {
					reinitiateDiscoveryDataframe(siteKey, "nutanix-guest");
					reinitiateDiscoveryDataframe(siteKey, "nutanix-host");
				} else {
					reinitiateDiscoveryDataframe(siteKey, sourceType);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return result;
	}

	private void reinitiateDiscoveryDataframe(String siteKey, String sourceType) {
		String path = commonPath + File.separator + "Dataframe" + File.separator;
		Dataset<Row> localDiscoveryDF = sparkSession.sql(
				"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"
						+ siteKey + "' and LOWER(source_type)='" + sourceType + "'");

		Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");

		try {
			Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
					"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"
							+ siteKey + "' and LOWER(source_type)='" + sourceType + "'");

			String filePathSrc = commonPath + File.separator + "Dataframe" + File.separator + siteKey
					+ File.separator + "site_key=" + siteKey + File.separator + "source_type=" + sourceType
					+ File.separator;
			File f = new File(filePathSrc);
			if (!f.exists()) {
				f.mkdir();
			}

			dataframeBySiteKey.show();

			dataframeBySiteKey = dataframeBySiteKey.drop("site_key").drop("source_type");

			dataframeBySiteKey.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true)
					.format("org.apache.spark.sql.json").mode(SaveMode.Overwrite).save(f.getPath());
			dataframeBySiteKey.unpersist();

			// remove double quotes from json file
			File[] files = new File(filePathSrc).listFiles();
			if (files != null) {
				DataframeUtil.formatJsonFile(files);
			}
			createLocalDiscoveryView(files);
			
		} catch (Exception e) {
			logger.error("Not able to create dataframe for local discovery table site key " + siteKey, e.getMessage(),
					e);
		}
	}

	public JSONObject getUnitConvertDetails(String reportName, String deviceType) {
		logger.info("GetUnitConvertDetails Begins");
		JSONObject resultJSONObject = new JSONObject();
		try {
			JSONObject timeZoneMetricsObject = new JSONObject();
			List<Map<String, Object>> resultMap = new ArrayList<>();
			if (reportName != null && !reportName.isEmpty()) {
				if (reportName.equalsIgnoreCase("capacity")) {
					String query = "select column_name from report_capacity_columns where lower(device_type)= '"
							+ deviceType.toLowerCase() + "' and is_size_metrics = '1'";

					resultMap = favouriteDao_v2.getJsonarray(query);

				} else if (reportName.equalsIgnoreCase("optimization_All") || reportName.contains("optimization")) {
					String query = "select column_name from report_columns where lower(report_name) = 'optimization' and lower(device_type) = 'all'  and is_size_metrics = '1'";

					resultMap = favouriteDao_v2.getJsonarray(query);

				} else {
					String query = "select column_name from report_columns where lower(report_name) = '"
							+ reportName.toLowerCase() + "' and lower(device_type) = '" + deviceType.toLowerCase()
							+ "' and is_size_metrics = '1'";

					resultMap = favouriteDao_v2.getJsonarray(query);
				}

			}

			JSONArray capacityMetricsColumns = new JSONArray();
			JSONObject capacityMetricsColumnObject = new JSONObject();
			for (Map<String, Object> list : resultMap) {
				for (Map.Entry<String, Object> entry : list.entrySet()) {
					capacityMetricsColumns.add(entry.getValue());
				}
			}

			capacityMetricsColumnObject.put("column", capacityMetricsColumns);
			capacityMetricsColumnObject.put("metrics_in", "Gb");
			resultJSONObject.put("capacity_metrics", capacityMetricsColumnObject);
			resultJSONObject.put("timezone_metrics", timeZoneMetricsObject);
		} catch (Exception ex) {
			logger.error("Exception in GetUnitConvertDetails ", ex);
		}
		logger.info("GetUnitConvertDetails Ends");
		return resultJSONObject;
	}

	public DataResult getCloudCostData_old(ServerSideGetRowsRequest request) {
		Dataset<Row> dataset = null;
		try {
			String siteKey = request.getSiteKey();
			String deviceType = request.getDeviceType();

			String viewName = siteKey + "_" + deviceType.toLowerCase();
			viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "") + "_opt";

			boolean isDiscoveryDataInView = false;
			try {
				dataset = sparkSession.sql("select * from global_temp." + viewName);
				dataset.cache();
				isDiscoveryDataInView = true;
			} catch (Exception e) {

			}

			if (!isDiscoveryDataInView) {
				Map<String, String> options = new HashMap<String, String>();
				options.put("url", dbUrl);
				options.put("dbtable", "mview_aws_cost_report");
				sparkSession.sqlContext().load("jdbc", options).registerTempTable("mview_aws_cost_report");
				dataset = sparkSession.sql("select * from mview_aws_cost_report where site_key='" + siteKey + "'");
				dataset.createOrReplaceGlobalTempView(viewName);
				dataset.cache();
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		dataset = DataframeUtil.renameDataFrameColumn(dataset, "data_temp_", "");

		rowGroups = request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
		groupKeys = request.getGroupKeys();
		valueColumns = request.getValueCols();
		pivotColumns = request.getPivotCols();
		filterModel = request.getFilterModel();
		sortModel = request.getSortModel();
		isPivotMode = request.isPivotMode();
		isGrouping = rowGroups.size() > groupKeys.size();

		rowGroups = formatInputColumnNames(rowGroups);
		groupKeys = formatInputColumnNames(groupKeys);
		sortModel = formatSortModel(sortModel);
		request.setStartRow(1);
		request.setEndRow((int) dataset.count());

		dataset.show();

		return paginate(dataset, request, dataset.toJSON().collectAsList());
	}

	public List<Map<String, Object>> getCloudCostDataPostgresFn(ServerSideGetRowsRequest request) {
		
		List<Map<String, Object>> cloudCostData = getCloudCostDataFromPostgres(request);
		
		
		return cloudCostData;
		
	}
	public DataResult getCloudCostData(ServerSideGetRowsRequest request) {
		
		 
		
		
		Dataset<Row> dataset = sparkSession.emptyDataFrame();
		String viewName = request.getSiteKey().replaceAll("\\s+", "").replaceAll("-", "") + "_cloudcost";

		try {
			dataset = sparkSession.sql("select * from global_temp." + viewName);
			dataset.cache();
		} catch (Exception e) {
			dataset = getOptimizationReport(request);
			dataset.cache();
			
			/*String cloudCostDfPath = commonPath + "Dataframe" + File.separator + "CCR" + File.separator
					+ request.getSiteKey() + File.separator;
			File filePath = new File(commonPath + "Dataframe" + File.separator + "CCR" + File.separator
					+ request.getSiteKey() + File.separator);

			if (filePath.exists()) {
				dataset = sparkSession.read().json(cloudCostDfPath + "*.json");
				dataset.createOrReplaceGlobalTempView(viewName);
				dataset.cache();
			} else {
				dataset = getOptimizationReport(request);
				dataset.cache();
			}*/

		}

		try {
			String category = request.getCategoryOpt();

			String siteKey = request.getSiteKey();
			String deviceType = request.getDeviceType();
			String sourceId = request.getSource();

			if (deviceType.equalsIgnoreCase("All")) {
				deviceType = " lcase(`Server Type`) in ('windows','linux', 'vmware', 'ec2')";
			} else {
				deviceType = "lcase(`Server Type`)='" + deviceType.toLowerCase() + "'";
				if (category.toLowerCase().equalsIgnoreCase("AWS Instances")) {
					deviceType = "lcase(`Server Type`)='ec2' and lcase(`OS Name`) = '"
							+ request.getDeviceType().toLowerCase() + "'";
				}
			}

			List<String> taskListServers = new ArrayList<>();
			if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
				List<Map<String, Object>> resultMap = favouriteDao_v2.getJsonarray(
						"select server_name from tasklist where project_id='" + request.getProjectId() + "'");
				if (resultMap != null && !resultMap.isEmpty()) {
					for (Map<String, Object> map : resultMap) {
						taskListServers.add((String) map.get("server_name"));
					}
				}
			}

			if (!taskListServers.isEmpty()) {
				String serverNames = String.join(",", taskListServers.stream()
						.map(name -> ("'" + name.toLowerCase() + "'")).collect(Collectors.toList()));
				deviceType = " lcase(`Server Name`) in (" + serverNames + ")";
			}

			// dataset.show(false);

			String sourceQuery = "";

			if (sourceId != null && !sourceId.isEmpty() && !sourceId.equalsIgnoreCase("All")) {
				sourceQuery = " and customExcelSrcId='" + sourceId + "'";
			}

			if (deviceType.equalsIgnoreCase("All") && category.equalsIgnoreCase("All")
					&& sourceId.equalsIgnoreCase("All")) {
				//
			} else {
				if (category.equalsIgnoreCase("All")) {
					dataset = sparkSession
							.sql("select * from global_temp." + viewName + " where " + deviceType + sourceQuery);
				} else {
					dataset = sparkSession.sql("select * from global_temp." + viewName + " where " + deviceType
							+ " and lcase(report_by)='" + category.toLowerCase() + "'" + sourceQuery);
				}
			}

			request.setStartRow(0);
			request.setEndRow((int) dataset.count());
			rowGroups = request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
			groupKeys = request.getGroupKeys();
			valueColumns = request.getValueCols();
			pivotColumns = request.getPivotCols();
			filterModel = request.getFilterModel();
			sortModel = request.getSortModel();
			isPivotMode = request.isPivotMode();
			isGrouping = rowGroups.size() > groupKeys.size();

			return paginate(dataset, request, dataset.toJSON().collectAsList());

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		dataset = sparkSession.emptyDataFrame();
		return paginate(dataset, request, dataset.toJSON().collectAsList());

	}
	
	private List<Map<String, Object>> getCloudCostDataFromPostgres(ServerSideGetRowsRequest request) {
		JSONArray cloudCostData = new JSONArray();
		String siteKey = request.getSiteKey();
		String deviceType = request.getDeviceType();

		String reportName = request.getReportType();
		String deviceTypeHeder = "All";
		String reportBy = request.getReportType();
		JSONArray headers = reportDao.getReportHeader(reportName, deviceTypeHeder, reportBy);
		String discoveryFilterqry = "";

		List<String> columnHeaders = new ArrayList<>();
		List<String> numberColumnHeaders = new ArrayList<>();
		if (headers != null && headers.size() > 0) {
			for (Object o : headers) {
				if (o instanceof JSONObject) {
					String col = (String) ((JSONObject) o).get("actualName");
					String dataType = (String) ((JSONObject) o).get("dataType");
					/*if (dataType.equalsIgnoreCase("String")) {
						columnHeaders.add(col);
					} else {
						numberColumnHeaders.add(col);
					}*/
					columnHeaders.add(col);

				}
			}
		}

		List<String> taskListServers = new ArrayList<>();
		if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
			List<Map<String, Object>> resultMap = favouriteDao_v2
					.getJsonarray("select server_name from tasklist where project_id='" + request.getProjectId() + "'");
			if (resultMap != null && !resultMap.isEmpty()) {
				for (Map<String, Object> map : resultMap) {
					taskListServers.add((String) map.get("server_name"));
				}
			}
		}

		if (deviceType.equalsIgnoreCase("All")) {
			
			discoveryFilterqry = " lower(source_type) in ('windows','linux', 'vmware', 'ec2')";
			//deviceType = " lcase(aws.`Server Type`) in ('windows','linux', 'vmware')";
		} else {
			discoveryFilterqry = " lower(source_type)='" + deviceType.toLowerCase() + "'";
			//deviceType = "lcase(aws.`Server Type`)='" + deviceType.toLowerCase() + "'"; 
			
		}
		boolean isTaskListReport = false;

		if (!taskListServers.isEmpty()) {
			String serverNames = String.join(",", taskListServers.stream().map(name -> ("'" + name.toLowerCase() + "'"))
					.collect(Collectors.toList()));
			//deviceType = " lcase(aws.`Server Name`) in (" + serverNames + ")";
			discoveryFilterqry = " lower(server_name) in (" + serverNames + ")";
			isTaskListReport = true;
		}

		System.out.println("----------------------deviceTypeCondition--------------------------" + discoveryFilterqry);

		List<String> categoryList = new ArrayList<>();
		List<String> sourceList = new ArrayList<>();
		if (!isTaskListReport) {
			
			  categoryList.add(request.getCategoryOpt());
			  sourceList.add(request.getSource());
		} 		
		
		String categoryQuery = "";
		String sourceQuery = "";
		if(request.getCategoryOpt() != null && !request.getCategoryOpt().equalsIgnoreCase("All")) {
			categoryQuery = " and report_by='"+request.getCategoryOpt()+"'"; 			
		}

		if(request.getSource() != null && !request.getSource().equalsIgnoreCase("All") && request.getCategoryOpt() != null && request.getCategoryOpt().equalsIgnoreCase("Custom Excel Data")) {
			sourceQuery = " and source_id='"+request.getSource()+"'";
		}
		
		try {
			String sql = " SELECT cpu_ghz as \"CPU GHz\",\r\n" + 
					"    db_service As \"DB Service\",\r\n" + 
					"    hba_speed As \"HBA Speed\",\r\n" + 
					"    host As \"Host\",\r\n" + 
					"    cast(logical_processor_count as int) As \"Logical Processor Count\",\r\n" + 
					"    memory As \"Memory\",\r\n" + 
					"    cast(number_of_cores as int) As \"Number of Cores\",\r\n" + 
					"    cast(number_of_ports as int) As \"Number of Ports\",\r\n" + 
					"    cast(number_of_processors as int) As \"Number of Processors\",\r\n" + 
					"    os_name As \"OS Name\",\r\n" + 
					"    os_version As \"OS Version\",\r\n" + 
					"    processor_name As \"Processor Name\",\r\n" + 
					"    server_model As \"Server Model\",\r\n" + 
					"    server_name As \"Server Name\",\r\n" + 
					"    total_size As \"Total Size\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN aws_on_demand_price IS NOT NULL THEN aws_on_demand_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"AWS On Demand Price\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN aws_1_year_price IS NOT NULL THEN aws_1_year_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"AWS 1 Year Price\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN aws_3_year_price IS NOT NULL THEN aws_3_year_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"AWS 3 Year Price\",\r\n" + 
					"    aws_instance As \"AWS Instance Type\",\r\n" + 
					"    aws_region As \"AWS Region\",\r\n" + 
					"    aws_specs As \"AWS Specs\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN azure_on_demand_price IS NOT NULL THEN azure_on_demand_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"Azure On Demand Price\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN azure_1_year_price IS NOT NULL THEN azure_1_year_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"Azure 1 Year Price\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN azure_3_year_price IS NOT NULL THEN azure_3_year_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"Azure 3 Year Price\",\r\n" + 
					"    azure_instance As \"Azure Instance Type\",\r\n" + 
					"    azure_specs As \"Azure Specs\",\r\n" + 
					"    google_instance As \"Google Instance Type\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN google_on_demand_price IS NOT NULL THEN google_on_demand_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"Google On Demand Price\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN google_1_year_price IS NOT NULL THEN google_1_year_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"Google 1 Year Price\",\r\n" + 
					"        cast(CASE\r\n" + 
					"            WHEN google_3_year_price IS NOT NULL THEN google_3_year_price\r\n" + 
					"            ELSE 0::numeric\r\n" + 
					"        END as float) AS \"Google 3 Year Price\",\r\n" + 
					"     site_key,\r\n" + 
					"     server_type as \"Server Type\",\r\n" + 
					"    end_of_life_os As \"End Of Life - OS\",\r\n" + 
					"    end_of_extended_support_os As \"End Of Extended Support - OS\",\r\n" + 
					"    end_of_life_hw As \"End Of Life - HW\",\r\n" + 
					"    end_of_extended_support_hw As \"End Of Extended Support - HW\",\r\n" + 
					"    report_by  from cloud_cost_report_data where site_key='"+siteKey+"' and " + discoveryFilterqry + categoryQuery + sourceQuery;
			
			System.out.println("----------------------sql--------------------------" + sql);

			List<Map<String, Object>> localDiscDatas = jdbc.queryForList(sql);			
			
			if (!isTaskListReport && !taskListServers.isEmpty()) {
				String taskListQuery = "select * from cloud_cost_report_data where site_key='"+siteKey+"' and " + discoveryFilterqry;
				localDiscDatas = jdbc.queryForList(taskListQuery);
			}
			
			
			return localDiscDatas;

		} catch (Exception ex) {
			ex.printStackTrace();
			 
		}

		
		return cloudCostData; // paginate(dataCheck, request);
	}
	
	
public void putAwsInstanceDataToPostgres(String siteKey, String deviceType) {
		
		Connection conn = null;
		Statement stmt = null;
		if (deviceType.equalsIgnoreCase("All")) {
			deviceType = " (lower(img.platformdetails) like '%linux%' or lower(img.platformdetails) like '%windows%' or lower(img.platformdetails) like '%vmware%')";
		} else {
			deviceType = " lower(img.platformdetails) like '%" + deviceType.toLowerCase() + "%'";
		}
		try {
			String query = "select i.sitekey, i.region, i.instanceid, i.instancetype, i.imageid, it.vcpuinfo, it.memoryinfo, img.platformdetails, tag.value as description, i.updated_date from ec2_instances i  left join ec2_tags tag on i.instanceid=tag.resourceid left join ec2_instancetypes it on i.instancetype=it.instancetype  join ec2_images img on i.imageid=img.imageid where i.sitekey='"
					+ siteKey + "' and  " + deviceType; // i.sitekey='"+siteKey+" and // + " group by it.instancetype,
														// it.vcpuinfo, it.memoryinfo";
		
			
			conn = AwsInventoryPostgresConnection.dataSource.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			List<AwsInstanceData> resultRows = resultSetToList(rs);
			resultRows = resultRows.stream().distinct().collect(Collectors.toList());
			 
			for(AwsInstanceData aws : resultRows) {
				try {
					if(aws.getDescription() != null && aws.getMemoryinfo() != null && aws.getVcpuinfo() != null) {					
						AwsInstanceCcrData awsInstanceCcrData = new AwsInstanceCcrData();
						awsInstanceCcrData.setInstanceType(aws.getInstancetype());
						awsInstanceCcrData.setMemory(aws.getMemoryinfo());
						awsInstanceCcrData.setSourceType("EC2");
						awsInstanceCcrData.setOsName(aws.getPlatformdetails());
						awsInstanceCcrData.setNumberOfCores(aws.getVcpuinfo());
						awsInstanceCcrData.setServerName(aws.getDescription());
						awsInstanceCcrData.setRegion(aws.getRegion());
						awsInstanceCcrData.setSiteKey(siteKey);					
						awsInstanceCcrDataRepository.save(awsInstanceCcrData);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		} finally {
			try {
				conn.close();
				// AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		
	}
	

	public Dataset<Row> getOptimizationReport(ServerSideGetRowsRequest request) {

		String siteKey = request.getSiteKey();
		String deviceType = request.getDeviceType();

		String reportName = request.getReportType();
		String deviceTypeHeder = "All";
		String reportBy = request.getReportType();
		JSONArray headers = reportDao.getReportHeader(reportName, deviceTypeHeder, reportBy);

		// String [] categoryArray = request.getCategory().replaceAll( "^\\[|\\]$",
		// "").replaceAll("\"", "").trim().split( "," );
		// String [] sourceArray = request.getSource().replaceAll( "^\\[|\\]$",
		// "").replaceAll("\"", "").trim().split( "," );

		String discoveryFilterqry = "";

		List<String> columnHeaders = new ArrayList<>();
		List<String> numberColumnHeaders = new ArrayList<>();
		if (headers != null && headers.size() > 0) {
			for (Object o : headers) {
				if (o instanceof JSONObject) {
					String col = (String) ((JSONObject) o).get("actualName");
					String dataType = (String) ((JSONObject) o).get("dataType");
					if (dataType.equalsIgnoreCase("String")) {
						columnHeaders.add(col);
					} else {
						numberColumnHeaders.add(col);
					}

				}
			}
		}

		List<String> taskListServers = new ArrayList<>();
		if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
			List<Map<String, Object>> resultMap = favouriteDao_v2
					.getJsonarray("select server_name from tasklist where project_id='" + request.getProjectId() + "'");
			if (resultMap != null && !resultMap.isEmpty()) {
				for (Map<String, Object> map : resultMap) {
					taskListServers.add((String) map.get("server_name"));
				}
			}
		}

		if (deviceType.equalsIgnoreCase("All") || !deviceType.isEmpty()) {
			deviceType = " lcase(aws.`Server Type`) in ('windows','linux', 'vmware')";
			discoveryFilterqry = " lcase(source_type) in ('windows','linux', 'vmware')";
		} else {
			deviceType = "lcase(aws.`Server Type`)='" + deviceType.toLowerCase() + "'";
			discoveryFilterqry = " lcase(source_type)='" + deviceType.toLowerCase() + "'";
		}

		if (!taskListServers.isEmpty()) {
			String serverNames = String.join(",", taskListServers.stream().map(name -> ("'" + name.toLowerCase() + "'"))
					.collect(Collectors.toList()));
			deviceType = " lcase(aws.`Server Name`) in (" + serverNames + ")";
			discoveryFilterqry = " lcase(server_name) in (" + serverNames + ")";
		}

		System.out.println("----------------------deviceTypeCondition--------------------------" + deviceType);

		Dataset<Row> dataCheck = null;

		String cloudCostDfPath = commonPath + File.separator + "Dataframe" + File.separator + "CCR" + File.separator;

		File file = new File(cloudCostDfPath);
		if (!file.exists()) {
			file.mkdir();
			try {
				Path resultFilePath = Paths.get(cloudCostDfPath);
				UserPrincipal owner = resultFilePath.getFileSystem().getUserPrincipalLookupService()
						.lookupPrincipalByName("zenuser");
				Files.setOwner(resultFilePath, owner);
			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		try {

			constructReport(siteKey, discoveryFilterqry);

			int dataCount = getEOLEOSCount(siteKey);

			int eolHwcount = getEOLEHWCount(siteKey);

			String hwJoin = "";
			String hwdata = ",'' as `End Of Life - HW`,'' as `End Of Extended Support - HW`";
			if (eolHwcount != 0) {
				hwJoin = "left join global_temp.eolHWData eolHw on lcase(eolHw.`Server Name`) = lcase(aws.`Server Name`)";
				hwdata = ",eolHw.`End Of Life - HW`,eolHw.`End Of Extended Support - HW`";
			}

			String sql = "select * from (" + " select "
					+ " ROW_NUMBER() OVER (PARTITION BY aws.`Server Name` ORDER BY aws.`log_date` desc) as my_rank, 'Physical Servers' as report_by, '0' as customExcelSrcId,"
					+ " lower(aws.`Server Name`) as `Server Name`, aws.`OS Name`, aws.`Server Type`, aws.`Server Model`,"
					+ " aws.`Memory`, aws.`Total Size`, aws.`Number of Processors`, aws.`Logical Processor Count`, "
					+ " round(aws.`CPU GHz`,2) as `CPU GHz`, aws.`Processor Name`,aws.`Number of Cores`,aws.`DB Service`, "
					+ " aws.`HBA Speed`,aws.`Number of Ports`, aws.Host, "
					+ " round((round(aws.`AWS On Demand Price`,2) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS On Demand Price`,"
					+ " round((round(aws.`AWS 3 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 3 Year Price`,"
					+ " round((round(aws.`AWS 1 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 1 Year Price`,"
					+ " aws.`AWS Instance Type`,aws.`AWS Region`,aws.`AWS Specs`,"
					+ " round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`,"
					+ " round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`,"
					+ " round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`,"
					+ " azure.`Azure Instance Type`,azure.`Azure Specs`," + " google.`Google Instance Type`, "
					+ " google.`Google On Demand Price`," + " google.`Google 1 Year Price`,"
					+ " google.`Google 3 Year Price`,"
					+ " aws.`OS Version`, eol.`End Of Life - OS`,eol.`End Of Extended Support - OS`" + " " + hwdata
					+ " " + " from global_temp.awsReport aws "
					+ " left join global_temp.eoleosDataDF eol on eol.`Server Name`=aws.`Server Name` "
					+ " left join global_temp.azureReport azure on azure.`Server Name` = aws.`Server Name`"
					+ " left join global_temp.googleReport google on google.`Server Name` = aws.`Server Name` " + " "
					+ hwJoin + " " + " where aws.site_key='" + siteKey + "' and " + deviceType
					+ " order by aws.`Server Name` asc) ld where ld.my_rank = 1";

			if (dataCount == 0) {
				sql = "select * from (" + " select "
						+ " ROW_NUMBER() OVER (PARTITION BY aws.`Server Name` ORDER BY aws.`log_date` desc) as my_rank, 'Physical Servers' as report_by, '0' as customExcelSrcId, "
						+ "lower(aws.`Server Name`) as `Server Name`, aws.`OS Name`, aws.`Server Type`, aws.`Server Model`,"
						+ " aws.`Memory`, aws.`Total Size`, aws.`Number of Processors`, aws.`Logical Processor Count`, "
						+ " round(aws.`CPU GHz`,2) as `CPU GHz`, aws.`Processor Name`,aws.`Number of Cores`,aws.`DB Service`, "
						+ " aws.`HBA Speed`,aws.`Number of Ports`, aws.Host,"
						+ " round((round(aws.`AWS On Demand Price`,2) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS On Demand Price`,"
						+ " round((round(aws.`AWS 3 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 3 Year Price`,"
						+ " round((round(aws.`AWS 1 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 1 Year Price`,"
						+ " aws.`AWS Instance Type`,aws.`AWS Region`,aws.`AWS Specs`,"
						+ " round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`,"
						+ " round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`,"
						+ " round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`,"
						+ " azure.`Azure Instance Type`,azure.`Azure Specs`," + " google.`Google Instance Type`, "
						+ " google.`Google On Demand Price`," + " google.`Google 1 Year Price`,"
						+ " google.`Google 3 Year Price`,"
						+ " aws.`OS Version`, '' as `End Of Life - OS`,'' as `End Of Extended Support - OS`" + " "
						+ hwdata + " " + " from global_temp.awsReport aws "
						+ " left join global_temp.azureReport azure on azure.`Server Name` = aws.`Server Name`"
						+ " left join global_temp.googleReport google on google.`Server Name` = aws.`Server Name` "
						+ " " + hwJoin + " " + " where aws.site_key='" + siteKey + "' and " + deviceType
						+ " order by aws.`Server Name` asc) ld where ld.my_rank = 1";
			}

			boolean isTaskListReport = false;
			if (!taskListServers.isEmpty()) {
				isTaskListReport = true;
			}
			List<String> categoryList = new ArrayList<>();
			List<String> sourceList = new ArrayList<>();

			if (!isTaskListReport) {
				/*
				 * categoryList.add(request.getCategoryOpt());
				 * sourceList.add(request.getSource());
				 */
				categoryList.add("All");
				sourceList.add("All");
			}

			dataCheck = sparkSession.sql(sql).toDF();
			List<String> colHeaders = Arrays.asList(dataCheck.columns());

			if (!isTaskListReport && !categoryList.contains("All") && !categoryList.contains("Physical Servers")) {
				dataCheck = sparkSession.emptyDataFrame();
			}

			Dataset<Row> awsInstanceData = null;
			if (!isTaskListReport && (categoryList.contains("All") || categoryList.contains("AWS Instances"))) {
				awsInstanceData = getAwsInstanceData(colHeaders, siteKey, deviceTypeHeder);
				/*
				 * if(awsInstanceData != null && !awsInstanceData.isEmpty()) {
				 * if(dataCheck.isEmpty()) { dataCheck = awsInstanceData; } else { dataCheck =
				 * dataCheck.unionByName(awsInstanceData); }
				 * 
				 * }
				 */
			}

			List<String> physicalServerNames = new ArrayList<String>();
			if (categoryList.contains("All")) {
				List<Row> serverNames = dataCheck.select(functions.col("Server Name")).collectAsList();
				serverNames.forEach((Consumer<? super Row>) row -> physicalServerNames.add(row.getAs("Server Name")));
			}

			Dataset<Row> thirdPartyData = null;
			if (!isTaskListReport && (categoryList.contains("All") || categoryList.contains("Custom Excel Data"))) {

				thirdPartyData = getThirdPartyData(colHeaders, siteKey, deviceTypeHeder, sourceList,
						request.getDeviceType(), physicalServerNames);

				/*
				 * if(thirdPartyData != null && !thirdPartyData.isEmpty()) {
				 * if(dataCheck.isEmpty()) { dataCheck = thirdPartyData; } else { dataCheck =
				 * dataCheck.unionByName(thirdPartyData); } }
				 */

			}

			/*
			 * sparkSession.catalog().dropGlobalTempView("localDiscoveryTemp");
			 * sparkSession.catalog().dropGlobalTempView("awsReportForThirdParty");
			 * sparkSession.catalog().dropGlobalTempView("eoleosDataDF");
			 * sparkSession.catalog().dropGlobalTempView("eolHWData");
			 * sparkSession.catalog().dropGlobalTempView("awsReport");
			 * sparkSession.catalog().dropGlobalTempView("azureReport");
			 * sparkSession.catalog().dropGlobalTempView("googleReport");
			 * sparkSession.catalog().dropGlobalTempView("azureReportForAWSInstance");
			 * sparkSession.catalog().dropGlobalTempView("awsReportForThirdParty");
			 * sparkSession.catalog().dropGlobalTempView("googleReportForAWSInstance");
			 */
			dataCheck = dataCheck.unionByName(awsInstanceData).unionByName(thirdPartyData);
			// dataCheck = sparkSession.sql("SELECT * FROM global_temp.ccrPhysicalDF UNION
			// ALL SELECT * FROM global_temp.awsInstanceDF UNION ALL SELECT * FROM
			// global_temp.thirdPartyDataDF");

			// dataCheck.printSchema();
			// dataCheck.show();

			/*
			 * for(String col : columnHeaders) { dataCheck = dataCheck.withColumn(col,
			 * functions.when(col(col).equalTo(""),"N/A")
			 * .when(col(col).equalTo(null),"N/A").when(col(col).isNull(),"N/A")
			 * .otherwise(col(col))); }
			 */
			dataCheck = dataCheck
					.withColumn("AWS 1 Year Price",
							functions.round(dataCheck.col("AWS 1 Year Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("AWS 3 Year Price",
							functions.round(dataCheck.col("AWS 3 Year Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("AWS On Demand Price",
							functions.round(
									dataCheck.col("AWS On Demand Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("Google 1 Year Price",
							functions.round(
									dataCheck.col("Google 1 Year Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("Google 3 Year Price",
							functions.round(
									dataCheck.col("Google 3 Year Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("Google On Demand Price",
							functions.round(
									dataCheck.col("Google On Demand Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("Azure 1 Year Price",
							functions.round(
									dataCheck.col("Azure 1 Year Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("Azure 3 Year Price",
							functions.round(
									dataCheck.col("Azure 3 Year Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("Azure On Demand Price",
							functions.round(
									dataCheck.col("Azure On Demand Price").cast(DataTypes.createDecimalType(32, 2))))
					.withColumn("Logical Processor Count", dataCheck.col("Logical Processor Count").cast("integer"))
					.withColumn("Memory", dataCheck.col("Memory").cast("integer"))
					.withColumn("Number of Cores", dataCheck.col("Number of Cores").cast("integer"))
					.withColumn("Number of Processors", dataCheck.col("Number of Processors").cast("integer"))
					.withColumn("Number of Ports", dataCheck.col("Number of Ports").cast("integer"))
					.withColumn("Total Size", dataCheck.col("Total Size").cast("integer"))
					.withColumn("AWS Instance Type",
							functions.when(col("AWS Instance Type").equalTo(""), "N/A")
									.when(col("AWS Instance Type").equalTo(null), "N/A")
									.when(col("AWS Instance Type").isNull(), "N/A").otherwise(col("AWS Instance Type")))
					.withColumn("OS Version",
							functions.when(col("OS Version").equalTo(""), "N/A")
									.when(col("OS Version").equalTo(null), "N/A")
									.when(col("OS Version").isNull(), "N/A").otherwise(col("OS Version")))
					.withColumn("Server Name",
							functions.when(col("Server Name").equalTo(""), "N/A")
									.when(col("Server Name").equalTo(null), "N/A")
									.when(col("Server Name").isNull(), "N/A").otherwise(col("Server Name")))
					.withColumn("Server Model",
							functions.when(col("Server Model").equalTo(""), "N/A")
									.when(col("Server Model").equalTo(null), "N/A")
									.when(col("Server Model").isNull(), "N/A").otherwise(col("Server Model")))
					.withColumn("End Of Extended Support - OS",
							functions.when(col("End Of Extended Support - OS").equalTo(""), "N/A")
									.when(col("End Of Extended Support - OS").equalTo(null), "N/A")
									.when(col("End Of Extended Support - OS").isNull(), "N/A")
									.otherwise(col("End Of Extended Support - OS")))
					.withColumn("Google Instance Type", functions.when(col("Google Instance Type").equalTo(""), "N/A")
							.when(col("Google Instance Type").equalTo(null), "N/A")
							.when(col("Google Instance Type").isNull(), "N/A").otherwise(col("Google Instance Type")))
					.withColumn("End Of Life - HW",
							functions.when(col("End Of Life - HW").equalTo(""), "N/A")
									.when(col("End Of Life - HW").equalTo(null), "N/A")
									.when(col("End Of Life - HW").isNull(), "N/A").otherwise(col("End Of Life - HW")))
					.withColumn("OS Name",
							functions.when(col("OS Name").equalTo(""), "N/A").when(col("OS Name").equalTo(null), "N/A")
									.when(col("OS Name").isNull(), "N/A").otherwise(col("OS Name")))
					.withColumn("Server Type",
							functions.when(col("Server Type").equalTo(""), "N/A")
									.when(col("Server Type").equalTo(null), "N/A")
									.when(col("Server Type").isNull(), "N/A").otherwise(col("Server Type")))
					.withColumn("End Of Extended Support - HW",
							functions.when(col("End Of Extended Support - HW").equalTo(""), "N/A")
									.when(col("End Of Extended Support - HW").equalTo(null), "N/A")
									.when(col("End Of Extended Support - HW").isNull(), "N/A")
									.otherwise(col("End Of Extended Support - HW")))
					.withColumn("CPU GHz",
							functions.when(col("CPU GHz").equalTo(""), "N/A").when(col("CPU GHz").equalTo(null), "N/A")
									.when(col("CPU GHz").isNull(), "N/A").otherwise(col("CPU GHz")))
					.withColumn("DB Service",
							functions.when(col("DB Service").equalTo(""), "N/A")
									.when(col("DB Service").equalTo(null), "N/A")
									.when(col("DB Service").isNull(), "N/A").otherwise(col("DB Service")))
					.withColumn("AWS Region",
							functions.when(col("AWS Region").equalTo(""), "N/A")
									.when(col("AWS Region").equalTo(null), "N/A")
									.when(col("AWS Region").isNull(), "N/A").otherwise(col("AWS Region")))
					.withColumn("AWS Specs",
							functions.when(col("AWS Specs").equalTo(""), "N/A")
									.when(col("AWS Specs").equalTo(null), "N/A").when(col("AWS Specs").isNull(), "N/A")
									.otherwise(col("AWS Specs")))
					.withColumn("Processor Name",
							functions.when(col("Processor Name").equalTo(""), "N/A")
									.when(col("Processor Name").equalTo(null), "N/A")
									.when(col("Processor Name").isNull(), "N/A").otherwise(col("Processor Name")))
					.withColumn("HBA Speed",
							functions.when(col("HBA Speed").equalTo(""), "N/A")
									.when(col("HBA Speed").equalTo(null), "N/A").when(col("HBA Speed").isNull(), "N/A")
									.otherwise(col("HBA Speed")))
					.withColumn("End Of Life - OS",
							functions.when(col("End Of Life - OS").equalTo(""), "N/A")
									.when(col("End Of Life - OS").equalTo(null), "N/A")
									.when(col("End Of Life - OS").isNull(), "N/A").otherwise(col("End Of Life - OS")))
					.withColumn("Azure Instance Type", functions.when(col("Azure Instance Type").equalTo(""), "N/A")
							.when(col("Azure Instance Type").equalTo(null), "N/A")
							.when(col("Azure Instance Type").isNull(), "N/A").otherwise(col("Azure Instance Type")))
					.withColumn("Azure Specs",
							functions.when(col("Azure Specs").equalTo(""), "N/A")
									.when(col("Azure Specs").equalTo(null), "N/A")
									.when(col("Azure Specs").isNull(), "N/A").otherwise(col("Azure Specs")))
					.withColumn("my_rank",
							functions.when(col("my_rank").equalTo(""), "N/A").when(col("my_rank").equalTo(null), "N/A")
									.when(col("my_rank").isNull(), "N/A").otherwise(col("my_rank")))
					.withColumn("report_by",
							functions.when(col("report_by").equalTo(""), "N/A")
									.when(col("report_by").equalTo(null), "N/A").when(col("report_by").isNull(), "N/A")
									.otherwise(col("report_by")))
					.withColumn("customExcelSrcId",
							functions.when(col("customExcelSrcId").equalTo(""), "N/A")
									.when(col("customExcelSrcId").equalTo(null), "N/A")
									.when(col("customExcelSrcId").isNull(), "N/A").otherwise(col("customExcelSrcId")));

			if (!taskListServers.isEmpty()) { // add server~ for task list call
				List<String> allServers = dataCheck.select("Server Name").as(Encoders.STRING()).collectAsList();

				taskListServers.removeAll(allServers);

				if (taskListServers != null && !taskListServers.isEmpty()) {
					Dataset<Row> nonOptDataset = getNonOptDatasetData(siteKey, taskListServers);

					if (nonOptDataset != null && !nonOptDataset.isEmpty()) {
						dataCheck = dataCheck.unionByName(nonOptDataset);
					}
				}

				dataCheck = dataCheck.withColumnRenamed("End Of Life - HW", "server~End Of Life - HW")
						.withColumnRenamed("End Of Extended Support - HW", "server~End Of Extended Support - HW")
						.withColumnRenamed("End Of Life - OS", "server~End Of Life - OS")
						.withColumnRenamed("End Of Extended Support - OS", "server~End Of Extended Support - OS");

			}

			/*
			 * List<String> floatColumns = new ArrayList<String>();
			 * floatColumns.add("AWS 1 Year Price"); floatColumns.add("AWS 3 Year Price");
			 * floatColumns.add("AWS On Demand Price");
			 * floatColumns.add("Google 1 Year Price");
			 * floatColumns.add("Google 3 Year Price");
			 * floatColumns.add("Google On Demand Price");
			 * floatColumns.add("Azure 1 Year Price");
			 * floatColumns.add("Azure 3 Year Price");
			 * floatColumns.add("Azure On Demand Price");
			 */

			/*
			 * List<String> numericalHeaders = getReportNumericalHeaders("Optimization",
			 * "All", "Optimization", siteKey);
			 * 
			 * List<String> columns = Arrays.asList(dataCheck.columns());
			 * 
			 * for(String column : numericalHeaders) { if(columns.contains(column)) {
			 * if(column.toLowerCase().contains("price")) { dataCheck =
			 * dataCheck.withColumn(column,
			 * dataCheck.col(column).cast(DataTypes.createDecimalType(32,2))); }else {
			 * dataCheck = dataCheck.withColumn(column,
			 * dataCheck.col(column).cast("integer")); }
			 * 
			 * }
			 * 
			 * }
			 */
			/*
			 * for(String column : floatColumns) { if(columns.contains(column)) { dataCheck
			 * = dataCheck.withColumn(column, dataCheck.col(column).cast("float")); }
			 * 
			 * }
			 */

			logger.info("getReport Details Ends");

			try {
				File ccrPath = new File(cloudCostDfPath + siteKey + File.separator);
				ccrPath.delete();
			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}

			String viewName = siteKey.replaceAll("\\s+", "").replaceAll("-", "") + "_cloudcost";

			dataCheck.createOrReplaceGlobalTempView(viewName);
			dataCheck.cache();
			// dataCheck.coalesce(1).write().json(cloudCostDfPath+siteKey);
			dataCheck.coalesce(1).write().option("escape", "").option("quotes", "")
					.option("ignoreLeadingWhiteSpace", true).format("org.apache.spark.sql.json")
					.mode(SaveMode.Overwrite).save(cloudCostDfPath + siteKey);

			/*
			 * dataCheck.write().option("escape", "").option("quotes",
			 * "").option("ignoreLeadingWhiteSpace", true)
			 * .format("org.apache.spark.sql.json")
			 * .mode(SaveMode.Overwrite).save(cloudCostDfPath+siteKey);
			 */

			//// dataCheck.write().option("escape", "").option("quotes",
			//// "").option("ignoreLeadingWhiteSpace", true).option("multiline",
			//// true).option("nullValue", "").option("mode",
			//// "PERMISSIVE").json(cloudCostDfPath+siteKey);

			Path resultFilePath = Paths.get(cloudCostDfPath + siteKey);
			UserPrincipal owner = resultFilePath.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName("zenuser");
			Files.setOwner(resultFilePath, owner);

			Path resultFilePath1 = Paths.get(cloudCostDfPath);
			UserPrincipal owner1 = resultFilePath.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName("zenuser");
			Files.setOwner(resultFilePath1, owner1);

			/*
			 * request.setStartRow(0); request.setEndRow((int)dataCheck.count()); rowGroups
			 * =
			 * request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
			 * groupKeys = request.getGroupKeys(); valueColumns = request.getValueCols();
			 * pivotColumns = request.getPivotCols(); filterModel =
			 * request.getFilterModel(); sortModel = request.getSortModel(); isPivotMode =
			 * request.isPivotMode(); isGrouping = rowGroups.size() > groupKeys.size();
			 */
			return dataCheck;

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Exception in getReport ", ex);
			// ex.printStackTrace();
		}

		dataCheck = sparkSession.emptyDataFrame();
		return dataCheck; // paginate(dataCheck, request);
	}

	private Dataset<Row> getThirdPartyData(List<String> columnHeaders, String siteKey, String deviceTypeHeder,
			List<String> sourceList, String deviceType, List<String> physicalServerNames) {
		Dataset<Row> result = sparkSession.emptyDataFrame();
		try {
			List<AwsInstanceData> thirdPartyData = queryThirdPartyData(siteKey, sourceList, deviceType,
					physicalServerNames);

			Dataset<Row> data = sparkSession.createDataFrame(thirdPartyData, AwsInstanceData.class);

			data = data.withColumnRenamed("region", "AWS Region").withColumnRenamed("instancetype", "AWS Instance Type")
					.withColumnRenamed("memoryinfo", "Memory").withColumnRenamed("vcpuinfo", "Number of Cores")
					.withColumnRenamed("platformdetails", "OS Name").withColumnRenamed("description", "Server Name")
					.withColumnRenamed("instanceid", "instanceid").withColumnRenamed("updated_date", "updated_date")
					.withColumnRenamed("serverType", "Server Type");
			/*
			 * data = data.withColumnRenamed("instancetype", "AWS Instance Type"); data =
			 * data.withColumnRenamed("memoryinfo", "Memory"); data =
			 * data.withColumnRenamed("vcpuinfo", "Number of Cores"); data =
			 * data.withColumnRenamed("platformdetails", "OS Name"); data =
			 * data.withColumnRenamed("description", "Server Name"); data =
			 * data.withColumnRenamed("instanceid", "instanceid"); data =
			 * data.withColumnRenamed("updated_date", "updated_date"); data =
			 * data.withColumnRenamed("serverType", "Server Type");
			 */
			data.createOrReplaceGlobalTempView("awsInstanceDF");

			// getAwsPricingForThirdParty();
			getAWSPricingForThirdParty();
			getAzurePricingForAWS();
			getGooglePricingForAWS();

			Dataset<Row> data1 = data.drop("instanceid");
			data1.createOrReplaceGlobalTempView("thirdPartyDataDF");

			try {

				// Dataset<Row> dataCheck1 = sparkSession.sql("select * from (select g.`Google
				// Instance Type`, g.`Google On Demand Price`, g.`Google 1 Year Price`,
				// g.`Google 3 Year Price`, ai.`AWS Region`, ai.`AWS Instance Type`,
				// ai.`Memory`, ai.`Number of Cores`, ai.`Server Name`, ai.`OS Name`,
				// round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`,
				// round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`,
				// round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`, azure.`Azure
				// Instance Type`, azure.`Azure Specs`, ROW_NUMBER () over (partition BY
				// ai.`instanceid` ORDER BY a.`PricePerUnit` ASC) AS my_rank, concat_ws(',',
				// concat('Processor: ',a.`Physical Processor`),concat('vCPU:
				// ',a.vCPU),concat('Clock Speed: ',a.`Clock Speed`),concat('Processor
				// Architecture: ',a.`Processor Architecture`) ,concat('Memory:
				// ',a.Memory),concat('Storage: ',a.Storage),concat('Network Performance:
				// ',a.`Network Performance`)) as `AWS Specs`, round(((select
				// min(a.`PricePerUnit`) from global_temp.awsPricingDF a where
				// lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No
				// Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and
				// a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730
				// ),2) as `AWS 3 Year Price`, round(((select min(a.`PricePerUnit`) from
				// global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS
				// Name`) and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) =
				// lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '1yr' and
				// cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 1 Year Price`,
				// round((a.`PricePerUnit` * 730),2) as `AWS On Demand Price` from
				// global_temp.awsInstanceDF ai left join global_temp.googleReportForAWSInstance
				// g on ai.instanceid=g.instanceid left join
				// global_temp.azureReportForAWSInstance azure on lower(azure.`OS Name`) =
				// lower(ai.`actualOsType`) and cast(azure.Memory as float ) = cast(ai.Memory as
				// float) and cast(azure.VCPUs as int ) = cast(ai.`Number of Cores` as int) left
				// join global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)=
				// lower(a.`Instance Type`) and a.`License Model` = 'No License required' and
				// a.Location = 'US East (Ohio)' and a.Tenancy <> 'Host' and a.TermType =
				// 'OnDemand' and lower(a.`Operating System`) = lower(ai.`OS Name`) and
				// cast(a.`PricePerUnit` as float ) > 0.0 and ( a.`Product Family` = 'Compute
				// Instance (bare metal)' or a.`Product Family` = 'Compute Instance' )) AWS
				// where AWS.my_rank = 1").toDF();
				result = sparkSession.sql(
						"select * from (select    'N/A' as `End Of Extended Support - HW`, 'N/A' as `End Of Life - HW`, 'N/A' as `End Of Life - OS`, 'N/A' as `End Of Extended Support - OS`, 'N/A' as `HBA Speed`, 'N/A' as `Processor Name`, 'N/A' as `DB Service`, 'N/A' as `CPU GHz`, 'N/A' as `OS Version`, 'N/A' as `Host`, 'N/A' as `Number of Ports`, 'N/A' as `Number of Processors`, 'N/A' as `Logical Processor Count`, 'N/A' as `Total Size`, 'N/A' as `Server Model`,  ROW_NUMBER() OVER (PARTITION BY ai.`Server Name` order by ai.`Server Name` asc) as my_rank, 'Custom Excel Data' as report_by, ai.customExcelSrcId, g.`Google Instance Type`, g.`Google On Demand Price`, g.`Google 1 Year Price`, g.`Google 3 Year Price`, a.`AWS Region`, a.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`Server Name`, a.`OS Name`, ai.`Server Type`, round( azure.`Azure On Demand Price`, 2 ) as `Azure On Demand Price`, round(azure.`Azure 3 Year Price`, 2) as `Azure 3 Year Price`, round(azure.`Azure 1 Year Price`, 2) as `Azure 1 Year Price`, azure.`Azure Instance Type`, azure.`Azure Specs`, round(a.`AWS On Demand Price`,2) as `AWS On Demand Price`, round(a.`AWS 3 Year Price`,2) as `AWS 3 Year Price`, round(a.`AWS 1 Year Price`,2) as `AWS 1 Year Price`, a.`AWS Specs` from global_temp.thirdPartyDataDF ai left join global_temp.googleReportForAWSInstance g on ai.`Server Name` = g.`instanceid` left join global_temp.azureReportForAWSInstance azure on ai.`Server Name` = azure.`instanceid` left join global_temp.awsReportForThirdParty a on  ai.`Server Name` = a.`Server Name`) thirdParty where thirdParty.my_rank = 1")
						.toDF();

				/*
				 * List<String> dup = new ArrayList<>();
				 * dup.addAll(Arrays.asList(dataCheck1.columns())); List<String> original = new
				 * ArrayList<>(); original.addAll(columnHeaders); original.removeAll(dup);
				 */
				/*
				 * for(String col : original) {
				 * 
				 * dataCheck1 = dataCheck1.withColumn(col, lit("N/A"));
				 * 
				 * }
				 */

				// result = dataCheck1.toDF();

				/*
				 * Collections.sort(columnHeaders); List<String> tmp = new ArrayList<String>();
				 * tmp.addAll(Arrays.asList(dataCheck1.columns())); Collections.sort(tmp);
				 */

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return result;
	}

	private List<AwsInstanceData> queryThirdPartyData(String siteKey, List<String> sourceList, String deviceType,
			List<String> physicalServerNames) {
		List<AwsInstanceData> row = new ArrayList<>();
		try {
			/*
			 * List<Map<String, Object>> obj = favouriteDao_v2.getFavouriteList(
			 * "select data from source_data where source_id in (select source_id from source where is_active='true' and (link_to='All' or site_key='"
			 * +siteKey+"')) and site_key='"
			 * +siteKey+"' and (data like '%Memory%' and data like '%Number of Cores%' and data like '%OS Type%' and data like '%Server Name%')"
			 * );
			 */
			boolean isAllSource = false;
			String sources = "";
			if (sourceList.contains("All")) {
				isAllSource = true;
			} else {
				sources = String.join(",",
						sourceList.stream().map(source -> ("'" + source + "'")).collect(Collectors.toList()));
			}

			String sql = "select source_id, replace(data, '.0\"', '') as data from source_data where source_id in (" + sources + ") and site_key='"
					+ siteKey + "'";

			String getAllSourceSql = "select source_id, fields from source where is_active='true' and site_key='"
					+ siteKey
					+ "' and fields like '%memory%' and fields like '%numberOfCores%' and fields like '%osType%' and fields like '%name%' and fields like '%serverType%'"; // (link_to='All'
																																											// or

			List<Map<String, Object>> allSource = favouriteDao_v2.getFavouriteList(getAllSourceSql);

			Map<String, JSONArray> sourceMap = new HashMap<String, JSONArray>();
			if (allSource != null && !allSource.isEmpty()) {
				for (Map<String, Object> map : allSource) {
					sourceMap.put((String) map.get("source_id"), (JSONArray) parser.parse((String) map.get("fields")));
				}
			}

			if (isAllSource) {
				sources = String.join(",",
						sourceMap.keySet().stream().map(source -> ("'" + source + "'")).collect(Collectors.toList()));
				sql = "select source_id, replace(data, '.0\"', '') as data from source_data where source_id in (" + sources + ") and site_key='"
						+ siteKey + "'";
			}

			List<Map<String, Object>> obj = favouriteDao_v2.getFavouriteList(sql);

			List<String> deviceList = new ArrayList<>();
			if (deviceType.contentEquals("All")) {
				deviceList.add("vmware");
				deviceList.add("windows");
				deviceList.add("linux");

			} else {
				deviceList.add(deviceType.toLowerCase());
			}

			if (!obj.isEmpty()) {
				for (Map<String, Object> o : obj) {

					JSONObject json = (JSONObject) parser.parse((String) o.get("data"));
					String sourceId = (String) o.get("source_id");
					JSONArray fieldMapAry = sourceMap.get(sourceId);
					Map<String, String> mappingNames = new HashMap<String, String>();

					for (int i = 0; i < fieldMapAry.size(); i++) {
						JSONObject sourceFiledMap = (JSONObject) fieldMapAry.get(i);
						mappingNames.put((String) sourceFiledMap.get("primaryKey"),
								(String) sourceFiledMap.get("displayLabel"));
					}

					if (json.containsKey(mappingNames.get("memory"))
							&& json.containsKey(mappingNames.get("numberOfCores"))
							&& json.containsKey(mappingNames.get("osType"))
							&& json.containsKey(mappingNames.get("name"))
							&& json.containsKey(mappingNames.get("serverType"))) {

						String actualOsType = "";

						String serverType = (String) json.get(mappingNames.get("serverType"));

						String value = (String) json.get(mappingNames.get("osType"));
						if (StringUtils.containsIgnoreCase(value, "CentOS")) {
							value = "LINUX";
							actualOsType = "CentOS";
						} else if (StringUtils.containsIgnoreCase(value, "SUSE")) {
							value = "SUSE";
							actualOsType = "SUSE";
						} else if (StringUtils.containsIgnoreCase(value, "Red")) {
							value = "RHEL";
							actualOsType = "RHEL";
						} else if (StringUtils.containsIgnoreCase(value, "LINUX")) {
							value = "LINUX";
							actualOsType = "UBUNTU";
						} else if (StringUtils.containsIgnoreCase(value, "WINDOWS")) {
							value = "WINDOWS";
							actualOsType = "WINDOWS";
						}
						float mem = Float.parseFloat((String) json.get(mappingNames.get("memory")));
						float vcpu = Float.parseFloat((String) json.get(mappingNames.get("numberOfCores")));
						String instanceId = mem + "_" + vcpu;
						if (deviceList.contains(serverType.toLowerCase())) { // &&
																				// !physicalServerNames.stream().anyMatch(d
																				// ->
																				// d.equalsIgnoreCase((String)json.get(mappingNames.get("name"))))
							AwsInstanceData awsInstanceData = new AwsInstanceData("US East (Ohio)", "",
									(String) json.get(mappingNames.get("memory")),
									(String) json.get(mappingNames.get("numberOfCores")), value,
									(String) json.get(mappingNames.get("name")),
									(String) json.get(mappingNames.get("name")), "", actualOsType,
									(String) json.get(mappingNames.get("serverType")), "Custom Excel Data", sourceId);
							row.add(awsInstanceData);
						}
						//
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return row;
	}

	private Dataset<Row> getNonOptDatasetData(String siteKey, List<String> taskListServers) {
		try {
			String serverList = "";
			serverList = String.join(",", taskListServers.stream().map(server -> ("'" + server.toLowerCase() + "'"))
					.collect(Collectors.toList()));

			// , eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support
			// as `End Of Extended Support - HW`, eol.end_of_life_cycle as `End Of Life -
			// OS`, eol.end_of_extended_support as `End Of Extended Support - OS` from
			// global_temp.localDiscoveryTemp l left join global_temp.eolDataDF eol on
			// lcase(eol.os_version)=lcase(l.`OS Version`) and
			// lcase(eol.os_type)=lcase(l.`Server Type`) left join global_temp.eolHWDataDF
			// eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) =
			// lcase(REPLACE(l.`Server Model`, ' ', ''))
			Dataset<Row> data = sparkSession.sql(
					"select l.rank as `my_rank`, l.`Server Name`, l.OS as `OS Name`, l.`Server Type`, l.`Server Model`, l.Memory, l.`Total Size`, l.`Number of Processors`, l.`Logical Processor Count`, l.`CPU GHz`, l.`Processor Name`, l.`Number of Cores`, l.`DB Service`, l.`HBA Speed`, l.`Number of Ports`, l.`Host`, 'AWS On Demand Price', 'AWS 3 Year Price', 'AWS 1 Year Price', 'AWS Instance Type', 'AWS Region', 'AWS Specs', 'Azure On Demand Price', 'Azure 3 Year Price', 'Azure 1 Year Price', 'Azure Instance Type', 'Azure Specs', 'Google Instance Type', 'Google On Demand Price', 'Google 1 Year Price', 'Google 3 Year Price', l.`OS Version`, eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`, eol.end_of_life_cycle as `End Of Life - OS`, eol.end_of_extended_support as `End Of Extended Support - OS`  from global_temp.localDiscoveryTemp l  left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(l.`OS Version`) and lcase(eol.os_type)=lcase(l.`Server Type`)  left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(l.`Server Model`, ' ', '')) where lower(l.`Server Name`) in ("
							+ serverList + ")");

			data = data.withColumn("AWS On Demand Price", lit("N/A"));
			data = data.withColumn("AWS 3 Year Price", lit("N/A"));
			data = data.withColumn("AWS 1 Year Price", lit("N/A"));
			data = data.withColumn("AWS Instance Type", lit("N/A"));
			data = data.withColumn("AWS Region", lit("N/A"));
			data = data.withColumn("AWS Specs", lit("N/A"));
			data = data.withColumn("Azure On Demand Price", lit("N/A"));
			data = data.withColumn("Azure 3 Year Price", lit("N/A"));
			data = data.withColumn("Azure 1 Year Price", lit("N/A"));
			data = data.withColumn("Azure Instance Type", lit("N/A"));
			data = data.withColumn("Azure Specs", lit("N/A"));
			data = data.withColumn("Google Instance Type", lit("N/A"));
			data = data.withColumn("Google On Demand Price", lit("N/A"));
			data = data.withColumn("Google 1 Year Price", lit("N/A"));
			data = data.withColumn("Google 3 Year Price", lit("N/A"));
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		Dataset<Row> df = sparkSession.emptyDataFrame();
		return df;

	}
	
	
	private List<Map<String, Object>> getAwsInstanceDataPostgres(List<String> columnHeaders, String siteKey, String deviceType) {
		List<Map<String, Object>> result = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;
		if (deviceType.equalsIgnoreCase("All")) {
			deviceType = " (lower(img.platformdetails) like '%linux%' or lower(img.platformdetails) like '%windows%' or lower(img.platformdetails) like '%vmware%')";
		} else {
			deviceType = " lower(img.platformdetails) like '%" + deviceType.toLowerCase() + "%'";
		}
		try {
			String query = "select i.sitekey, i.region, i.instanceid, i.instancetype, i.imageid, it.vcpuinfo, it.memoryinfo, img.platformdetails, tag.value as description, i.updated_date from ec2_instances i  left join ec2_tags tag on i.instanceid=tag.resourceid left join ec2_instancetypes it on i.instancetype=it.instancetype  join ec2_images img on i.imageid=img.imageid where i.sitekey='"
					+ siteKey + "' and  " + deviceType; // i.sitekey='"+siteKey+" and // + " group by it.instancetype,
														// it.vcpuinfo, it.memoryinfo";

			conn = AwsInventoryPostgresConnection.dataSource.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			List<Map<String, String>> awsData = parseAwsData(rs);
			
			
			
			
			
			
 
		} catch (Exception e) {
			e.printStackTrace();
			 
		} finally {
			try {
				conn.close();
				// AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		return result;
	}
	
	
	
	private Dataset<Row> getAwsInstanceData(List<String> columnHeaders, String siteKey, String deviceType) {
		Dataset<Row> result = sparkSession.emptyDataFrame();
		Connection conn = null;
		Statement stmt = null;
		if (deviceType.equalsIgnoreCase("All")) {
			deviceType = " (lower(img.platformdetails) like '%linux%' or lower(img.platformdetails) like '%windows%' or lower(img.platformdetails) like '%vmware%')";
		} else {
			deviceType = " lower(img.platformdetails) like '%" + deviceType.toLowerCase() + "%'";
		}
		try {
			String query = "select i.sitekey, i.region, i.instanceid, i.instancetype, i.imageid, it.vcpuinfo, it.memoryinfo, img.platformdetails, tag.value as description, i.updated_date from ec2_instances i  left join ec2_tags tag on i.instanceid=tag.resourceid left join ec2_instancetypes it on i.instancetype=it.instancetype  join ec2_images img on i.imageid=img.imageid where i.sitekey='"
					+ siteKey + "' and  " + deviceType; // i.sitekey='"+siteKey+" and // + " group by it.instancetype,
														// it.vcpuinfo, it.memoryinfo";

			conn = AwsInventoryPostgresConnection.dataSource.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			List<AwsInstanceData> resultRows = resultSetToList(rs);
			Dataset<Row> data = sparkSession.createDataFrame(resultRows, AwsInstanceData.class);

			// Dataset<Row> data =
			// sparkSession.read().format("csv").option("header","true").load("E:\\opt\\aws_inventory1.csv").distinct();

			data = data.withColumnRenamed("region", "AWS Region").withColumnRenamed("instancetype", "AWS Instance Type")
					.withColumnRenamed("memoryinfo", "Memory").withColumnRenamed("vcpuinfo", "Number of Cores")
					.withColumnRenamed("platformdetails", "OS Name").withColumnRenamed("description", "Server Name")
					.withColumnRenamed("instanceid", "instanceid").withColumnRenamed("updated_date", "updated_date");
			/*
			 * data = data.withColumnRenamed("instancetype", "AWS Instance Type"); data =
			 * data.withColumnRenamed("memoryinfo", "Memory"); data =
			 * data.withColumnRenamed("vcpuinfo", "Number of Cores"); data =
			 * data.withColumnRenamed("platformdetails", "OS Name"); data =
			 * data.withColumnRenamed("description", "Server Name"); data =
			 * data.withColumnRenamed("instanceid", "instanceid"); data =
			 * data.withColumnRenamed("updated_date", "updated_date");
			 */
			data.createOrReplaceGlobalTempView("awsInstanceDF");

			getAzurePricingForAWS();
			getGooglePricingForAWS();
			try {

				/*
				 * Dataset<Row> dataCheck1 = sparkSession.sql(
				 * " select googlePricing.InstanceType as `Google Instance Type`, round(googlePricing.pricePerUnit*730 + (case when awsData.`Operating System` like '%Windows%' then 67.16  when awsData.`Operating System` like '%Red Hat%' then 43.8 else 0  end),2) as `Google On Demand Price`,  round(googlePricing.1YrPrice*730 + (case when awsData.`Operating System` like '%Windows%' then 67.16  when awsData.`Operating System` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 1 Year Price`, round(googlePricing.3YrPrice*730 + (case when awsData.`Operating System` like '%Windows%' then 67.16  when awsData.`Operating System` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 3 Year Price`, awsData.`AWS Region`, awsData.`Memory`, awsData.`Number of Cores`, awsData.`OS Version`, awsData.`Operating System`, awsData.`AWS 3 Year Price`, awsData.`AWS On Demand Price`, round(az.demandPrice,2) as `Azure On Demand Price`, round(az.3YrPrice,2) as `Azure 3 Year Price`, round(az.1YrPrice,2) as `Azure 1 Year Price`, az.InstanceType as `Azure Instance Type` from (select  ai.`AWS Region`, ai.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`OS Version`, ai.`Operating System`, ((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where a.`Operating System` = ai.`Operating System` and a.PurchaseOption='No Upfront' and a.`Instance Type`= ai.`AWS Instance Type` and a.LeaseContractLength='3yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 3 Year Price`,"
				 * +
				 * "((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where a.`Operating System` = ai.`Operating System` and a.PurchaseOption='No Upfront' and a.`Instance Type`=ai.`AWS Instance Type` and a.LeaseContractLength='1yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 1 Year Price`,"
				 * +
				 * "(a.`PricePerUnit` * 730) as `AWS On Demand Price`  from awsInstanceDF ai left join global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)=lower(a.`Instance Type`) and  cast(ai.Memory as int) = CAST(a.Memory as int) and cast(ai.`Number of Cores` as int) = CAST(a.vCPU as int) and  a.`License Model`='No License required'  and a.Location='US East (Ohio)' and a.Tenancy <> 'Host' and (a.`Product Family` = 'Compute Instance (bare metal)' or a.`Product Family` = 'Compute Instance')) awsData "
				 * +
				 * " left join global_temp.azurePricingDF az on cast(awsData.Memory as int) = CAST(az.Memory as int) and cast(awsData.`Number of Cores` as int) = CAST(az.VCPUs as int) and lcase(az.OperatingSystem) = lcase(awsData.`Operating System`)"
				 * +
				 * " left join (select cast(OnDemandPrice as float) as pricePerUnit,VCPUs,Memory,InstanceType,1YrPrice,3YrPrice from global_temp.googlePricingDF where  Region='US East' order by cast(OnDemandPrice as float) asc) googlePricing on cast(googlePricing.VCPUs as float)=cast(awsData.`Number of Cores` as float) and cast(googlePricing.Memory as float)=cast(awsData.Memory as float)"
				 * ) .toDF();
				 */
				// Dataset<Row> dataCheck1 = sparkSession.sql("select * from (select ai.`AWS
				// Region`, ai.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`,
				// ai.`Server Name`, ai.`OS Name`, round(azure.`Azure On Demand Price`,2) as
				// `Azure On Demand Price`, round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year
				// Price`, round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`,
				// azure.`Azure Instance Type`, azure.`Azure Specs`, ROW_NUMBER () over
				// (partition BY ai.`instanceid` ORDER BY a.`PricePerUnit` ASC) AS my_rank,
				// round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where
				// lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No
				// Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and
				// a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730
				// ),2) as `AWS 3 Year Price`, round(((select min(a.`PricePerUnit`) from
				// global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS
				// Name`) and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) =
				// lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '1yr' and
				// cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 1 Year Price`,
				// round((a.`PricePerUnit` * 730),2) as `AWS On Demand Price` from
				// global_temp.awsInstanceDF ai left join global_temp.azureReportForAWSInstance
				// azure on lower(azure.`OS Name`) = lower(ai.`actualOsType`) and
				// cast(azure.Memory as float ) = cast(ai.Memory as float) and cast(azure.VCPUs
				// as int ) = cast(ai.`Number of Cores` as int) left join
				// global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)=
				// lower(a.`Instance Type`) and a.`License Model` = 'No License required' and
				// a.Location = 'US East (Ohio)' and a.Tenancy <> 'Host' and a.TermType =
				// 'OnDemand' and lower(a.`Operating System`) = lower(ai.`OS Name`) and
				// cast(a.`PricePerUnit` as float ) > 0.0 and ( a.`Product Family` = 'Compute
				// Instance (bare metal)' or a.`Product Family` = 'Compute Instance' )) AWS
				// where AWS.my_rank = 1").toDF();

				Dataset<Row> dataCheck1 = sparkSession.sql(
						"select  * from (select   'N/A' as `End Of Extended Support - HW`, 'N/A' as `End Of Life - HW`, 'N/A' as `End Of Life - OS`, 'N/A' as `End Of Extended Support - OS`, 'N/A' as `HBA Speed`, 'N/A' as `Processor Name`, 'N/A' as `DB Service`, 'N/A' as `CPU GHz`, 'N/A' as `OS Version`, 'N/A' as `Host`, 'N/A' as `Number of Ports`, 'N/A' as `Number of Processors`, 'N/A' as `Logical Processor Count`, 'N/A' as `Total Size`, 'N/A' as `Server Model`, 'EC2' as `Server Type`, g.`Google Instance Type`, g.`Google On Demand Price`, g.`Google 1 Year Price`, g.`Google 3 Year Price`, ai.`AWS Region`, ai.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`Server Name`, ai.`OS Name`, round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`, round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`, round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`, azure.`Azure Instance Type`, azure.`Azure Specs`,  ROW_NUMBER () over (partition BY ai.`instanceid` ORDER BY a.`PricePerUnit` ASC) AS my_rank, 'AWS Instances' as report_by, '0' as customExcelSrcId, concat_ws(',', concat('Processor: ',a.`Physical Processor`),concat('vCPU: ',a.vCPU),concat('Clock Speed: ',a.`Clock Speed`),concat('Processor Architecture: ',a.`Processor Architecture`) ,concat('Memory: ',a.Memory),concat('Storage: ',a.Storage),concat('Network Performance: ',a.`Network Performance`)) as `AWS Specs`, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a  where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 3 Year Price`, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`)  and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '1yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 1 Year Price`, round((a.`PricePerUnit` * 730),2) as `AWS On Demand Price` from global_temp.awsInstanceDF ai left join global_temp.googleReportForAWSInstance g on ai.instanceid=g.instanceid  left join global_temp.azureReportForAWSInstance azure on lower(azure.`OS Name`) = lower(ai.`actualOsType`) and cast(azure.Memory as float ) = cast(ai.Memory as float)  and cast(azure.VCPUs as int ) = cast(ai.`Number of Cores` as int)  left join global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)= lower(a.`Instance Type`) and a.`License Model` = 'No License required' and a.Location = 'US East (Ohio)' and a.Tenancy <> 'Host' and a.TermType = 'OnDemand' and lower(a.`Operating System`) = lower(ai.`OS Name`) and cast(a.`PricePerUnit` as float ) > 0.0 and (  a.`Product Family` = 'Compute Instance (bare metal)'   or a.`Product Family` = 'Compute Instance'  )) AWS where AWS.my_rank = 1")
						.toDF();

				/*
				 * List<String> dup = new ArrayList<>();
				 * dup.addAll(Arrays.asList(dataCheck1.columns())); List<String> original = new
				 * ArrayList<>(); original.addAll(columnHeaders); original.removeAll(dup);
				 */
				/*
				 * for(String col : original) { if(col.equalsIgnoreCase("Server Type")) {
				 * dataCheck1 = dataCheck1.withColumn(col, lit("EC2")); } else { dataCheck1 =
				 * dataCheck1.withColumn(col, lit("N/A")); }
				 * 
				 * }
				 */

				result = dataCheck1.toDF();

				/*
				 * Collections.sort(columnHeaders); List<String> tmp = new ArrayList<String>();
				 * tmp.addAll(Arrays.asList(dataCheck1.columns())); Collections.sort(tmp);
				 */

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		} finally {
			try {
				conn.close();
				// AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		return result;
	}

	private List<AwsInstanceData> resultSetToList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		List<AwsInstanceData> rows = new ArrayList<>();
		while (rs.next()) {
			Map<String, String> row = new HashMap<String, String>(columns);

			for (int i = 1; i <= columns; ++i) {
				String colName = md.getColumnName(i);
				String value = rs.getString(i);
				if (md.getColumnName(i).equals("vcpuinfo")) {
					value = getValueFromJson("DefaultVCpus", rs.getString(i));
				}
				if (md.getColumnName(i).equals("memoryinfo")) {
					value = getValueFromJson("SizeInMiB", rs.getString(i));
					value = Integer.parseInt(value) / 1024 + "";
				}

				if (md.getColumnName(i).equals("region")) {
					if (rs.getString(i).equalsIgnoreCase("us-east-2")) {
						value = "US East (Ohio)";
					}
				}
				if (md.getColumnName(i).equals("platformdetails")) {
					String actualOsType = "";
					value = rs.getString(i);
					if (StringUtils.containsIgnoreCase(value, "CentOS")) {
						value = "LINUX";
						actualOsType = "CentOS";
					} else if (StringUtils.containsIgnoreCase(value, "SUSE")) {
						value = "SUSE";
						actualOsType = "SUSE";
					} else if (StringUtils.containsIgnoreCase(value, "Red")) {
						value = "RHEL";
						actualOsType = "RHEL";
					} else if (StringUtils.containsIgnoreCase(value, "LINUX")) {
						value = "LINUX";
						actualOsType = "UBUNTU";
					} else if (StringUtils.containsIgnoreCase(value, "WINDOWS")) {
						value = "WINDOWS";
						actualOsType = "WINDOWS";
					}
					row.put("actualOsType", actualOsType);
				}
				row.put(colName, value);

			}
			AwsInstanceData awsInstanceData = new AwsInstanceData(row.get("region"), row.get("instancetype"),
					row.get("memoryinfo"), row.get("vcpuinfo"), row.get("platformdetails"), row.get("description"),
					row.get("instanceid"), row.get("updated_date"), row.get("actualOsType"), "", "AWS Instances", "0");
			// System.out.println("----json----------" +awsInstanceData.toString() );
			awsInstanceData.setReport_by("AWS Instances");
			awsInstanceData.setCustomExcelSrcId("0");
			rows.add(awsInstanceData);
		}
		return rows;
	}

	
	private List<Map<String, String>> parseAwsData(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		List<Map<String, String>> rows = new ArrayList<>();
		while (rs.next()) {
			Map<String, String> row = new HashMap<String, String>(columns);

			for (int i = 1; i <= columns; ++i) {
				String colName = md.getColumnName(i);
				String value = rs.getString(i);
				if (md.getColumnName(i).equals("vcpuinfo")) {
					value = getValueFromJson("DefaultVCpus", rs.getString(i));
				}
				if (md.getColumnName(i).equals("memoryinfo")) {
					value = getValueFromJson("SizeInMiB", rs.getString(i));
					value = Integer.parseInt(value) / 1024 + "";
				}

				if (md.getColumnName(i).equals("region")) {
					if (rs.getString(i).equalsIgnoreCase("us-east-2")) {
						value = "US East (Ohio)";
					}
				}
				if (md.getColumnName(i).equals("platformdetails")) {
					String actualOsType = "";
					value = rs.getString(i);
					if (StringUtils.containsIgnoreCase(value, "CentOS")) {
						value = "LINUX";
						actualOsType = "CentOS";
					} else if (StringUtils.containsIgnoreCase(value, "SUSE")) {
						value = "SUSE";
						actualOsType = "SUSE";
					} else if (StringUtils.containsIgnoreCase(value, "Red")) {
						value = "RHEL";
						actualOsType = "RHEL";
					} else if (StringUtils.containsIgnoreCase(value, "LINUX")) {
						value = "LINUX";
						actualOsType = "UBUNTU";
					} else if (StringUtils.containsIgnoreCase(value, "WINDOWS")) {
						value = "WINDOWS";
						actualOsType = "WINDOWS";
					}
					row.put("actualOsType", actualOsType);
				}
				row.put(colName, value);
				row.put("report_by", "AWS Instances");
				row.put("custom_excel_src_id", "0");
			}
			
			rows.add(row);
		}
		return rows;
	}
	
	private String getValueFromJson(String key, String jsonString) {
		try {
			JSONParser jSONParser = new JSONParser();
			JSONObject json = (JSONObject) jSONParser.parse(jsonString);

			if (json.get(key) instanceof String) {
				return (String) json.get(key);
			} else if (json.get(key) instanceof Long) {
				Long rs = (Long) json.get(key);
				return rs.toString();
			}
			return "";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return "";
	}

	private void constructReport(String siteKey, String discoveryFilterqry) {
		logger.info("ConstructReport Starts");
		try {
			sparkSession.sqlContext().clearCache();
			getLocalDiscovery(siteKey, discoveryFilterqry);
			getAWSPricing();
			getAzurePricing();
			getGooglePricing();

		} catch (Exception ex) {
			logger.error("Exception in ConstructReport", ex);
			ex.printStackTrace();
		}
		logger.info("ConstructReport Ends");
	}

	private void getLocalDiscovery(String siteKey, String discoveryFilterqry) {			
	       
        try {			        	
        	sparkSession.catalog().dropGlobalTempView("localDiscoveryTemp");
			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);						
			options.put("dbtable", "local_discovery");						
			
			sparkSession.sqlContext().load("jdbc", options).registerTempTable("local_discovery");
			Dataset<Row> localDiscoveryDF = sparkSession.sql("select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"+ siteKey + "' and " + discoveryFilterqry);
			Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");				
			
			try {
				Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
						"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"+ siteKey + "'");
			   String path = commonPath + File.separator + "cloud_cost" + File.separator + siteKey;
				File f = new File(path);
				
				if (!f.exists()) {
					f.mkdir();
				}			
				 
				try {
					 Path resultFilePath = Paths.get(f.getAbsolutePath());
					    UserPrincipal owner = resultFilePath.getFileSystem().getUserPrincipalLookupService()
				                .lookupPrincipalByName("zenuser");
				        Files.setOwner(resultFilePath, owner);
				} catch (Exception e) {
					// TODO: handle exception
				}
				
			        
			        
				dataframeBySiteKey.write().option("ignoreNullFields", false)
						.format("org.apache.spark.sql.json")
						.mode(SaveMode.Overwrite).save(f.getPath());
				
				File[] files = new File(path).listFiles();
				if (files != null) {
					DataframeUtil.formatJsonFile(files);
				}
				
				
				 Dataset<Row> dataset = sparkSession.read().json(f.getPath() + File.separator + "*.json"); 
   	        	 dataset.createOrReplaceTempView("tmpView");
   	        	 dataset = sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
   	        	//following three conditions only applied for windows logs. windows only following 3 fields, other logs should have column with empty
   	        	
   	        	 List<String> columns = Arrays.asList(dataset.columns());
   	        	 if(!columns.contains("Logical Processor Count")) {
   	        		dataset = dataset.withColumn("Logical Processor Count", lit(""));
   	        	 }
				 if(!columns.contains("DB Service")) {
					 dataset = dataset.withColumn("DB Service", lit(""));			   	        		 
					}
				 if(!columns.contains("Processor Name")) {
					 dataset =  dataset.withColumn("Processor Name", lit("")); 
				  }
				 
				 if(!columns.contains("Host")) {
					 dataset =  dataset.withColumn("Host", lit("")); 
				  }
   	        	
   	        	 dataset.createOrReplaceGlobalTempView("localDiscoveryTemp"); 
   	        	dataset.show();
   	        	System.out.println("----------localDiscoveryTemp---------- " +dataset.count());
   		        // dataset.cache();	
   		         
   		      //dataset.printSchema();
   		 	
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        } catch (Exception e) {
        	e.printStackTrace();
        }
}


	public void getAWSPricingForThirdParty() {
		try {
			Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from ("
					+ " select report.`Server Name`, report.`OS Name`, "
					+ "  (report.`PricePerUnit` * 730) as `AWS On Demand Price`,"
					+ " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='3yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 3 Year Price`, "
					+ " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='1yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 1 Year Price`, "
					+ " report.`AWS Instance Type`,report.`AWS Region`,report.`AWS Specs`,"
					+ " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`PricePerUnit` as float) asc) as my_rank"
					+ " from (SELECT localDiscoveryDF.`Server Name`, localDiscoveryDF.instanceid, "
					+ " localDiscoveryDF.`OS Name`, "
					+ " cast(localDiscoveryDF.`Number of Cores` as int) as `Number of Cores`,"
					+ " cast(localDiscoveryDF.`Memory` as int) as `Memory`, "
					+ " awsPricing2.`Instance Type` as `AWS Instance Type`, awsPricing2.Location as `AWS Region`"
					+ " ,concat_ws(',', concat('Processor: ',awsPricing2.`Physical Processor`),concat('vCPU: ',awsPricing2.vCPU)"
					+ " ,concat('Clock Speed: ',awsPricing2.`Clock Speed`),concat('Processor Architecture: ',awsPricing2.`Processor Architecture`)"
					+ " ,concat('Memory: ',awsPricing2.Memory),concat('Storage: ',awsPricing2.Storage),concat('Network Performance: ',awsPricing2.`Network Performance`)) as `AWS Specs`"
					+ " , cast(localDiscoveryDF.`Number of Cores` as int) as `vCPU`, (case when localDiscoveryDF.`Memory` is null then 0 else cast(localDiscoveryDF.`Memory` as int) end) as `MemorySize`, awsPricing2.PricePerUnit as `PricePerUnit`,awsPricing2.`Operating System` as `OperatingSystem` "
					+ " FROM global_temp.awsInstanceDF localDiscoveryDF"
					+ " join (Select localDiscoveryDF1.`Server Name` "
					+ " from global_temp.awsInstanceDF localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`) localDiscoveryTemp2 ON "
					+ " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name`"
					+ " left join (select `Operating System`,Memory,min(PricePerUnit) as pricePerUnit, vCPU,TermType from global_temp.awsPricingDF where `License Model`='No License required'"
					+ " and Location='US East (Ohio)' and Tenancy <> 'Host' and (`Product Family` = 'Compute Instance (bare metal)' or `Product Family` = 'Compute Instance') and cast(PricePerUnit as float) > 0 group by `Operating System`,Memory,vCPU,TermType) awsPricing on"
					+ " lcase(awsPricing.`Operating System`) = lcase(localDiscoveryDF.`OS Name`) and"
					+ " awsPricing.Memory >= (case when localDiscoveryDF.Memory is null then 0 else cast(localDiscoveryDF.Memory as int) end)"
					+ " and awsPricing.vCPU >= (cast(localDiscoveryDF.`Number of Cores` as int))"
					+ " left join global_temp.awsPricingDF awsPricing2 on awsPricing2.`Operating System` = awsPricing.`Operating System` and awsPricing2.PricePerUnit = awsPricing.pricePerUnit and awsPricing.Memory = "
					+ " awsPricing2.Memory and awsPricing.vCPU = awsPricing2.vCPU and awsPricing2.TermType='OnDemand' where cast(awsPricing2.PricePerUnit as float) > 0) report) reportData"
					+ " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
			dataCheck.createOrReplaceGlobalTempView("awsReportForThirdParty");

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getAWSPricing() {
		try {
		Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
		" select report.Host, report.log_date,report.`vCPU`, report.site_key, report.`Server Type`, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`," +
		" report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`, report.`Number of Processors`, report.`Logical Processor Count`, " +
		" report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`," +
		" report.`Number of Ports`, (report.`PricePerUnit` * 730) as `AWS On Demand Price`," +
		" ((select min(cast (a.PricePerUnit as float)) as `PricePerUnit` from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='3yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 3 Year Price`, " +
		" ((select min(cast (a.PricePerUnit as float)) as `PricePerUnit` from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='1yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 1 Year Price`, " +
		" report.`AWS Instance Type`,report.`AWS Region`,report.`AWS Specs`," +
		" ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`PricePerUnit` as float) asc) as my_rank" +
		" from (SELECT localDiscoveryDF.log_date,localDiscoveryDF.site_key, localDiscoveryDF.`Server Type`, localDiscoveryDF.`Server Name` , localDiscoveryDF.`Server Type` ," +
		" localDiscoveryDF.OS as `OS Name`,localDiscoveryDF.`OS Version`,localDiscoveryDF.`Server Model` ," +
		" cast(localDiscoveryDF.`Logical Processor Count` as int) as `Logical Processor Count`,cast(localDiscoveryDF.`Number of Processors` as int) as `Number of Processors`," +
		" cast(localDiscoveryDF.`Memory` as int) as `Memory`, round(localDiscoveryDF.`Total Size`,2) as `Total Size`, " +
		" cast(localDiscoveryDF.`CPU GHz` as int) as `CPU GHz`, localDiscoveryDF.`Processor Name`, " +
		" cast(localDiscoveryDF.`Number of Cores` as int) as `Number of Cores`," +
		" localDiscoveryDF.`DB Service`, localDiscoveryDF.`HBA Speed`, cast(localDiscoveryDF.`Number of Ports` as int) as `Number of Ports`," +
		" awsPricing2.`Instance Type` as `AWS Instance Type`, awsPricing2.Location as `AWS Region`" +
		" ,concat_ws(',', concat('Processor: ',awsPricing2.`Physical Processor`),concat('vCPU: ',awsPricing2.vCPU)" +
		" ,concat('Clock Speed: ',awsPricing2.`Clock Speed`),concat('Processor Architecture: ',awsPricing2.`Processor Architecture`)" +
		" ,concat('Memory: ',awsPricing2.Memory),concat('Storage: ',awsPricing2.Storage),concat('Network Performance: ',awsPricing2.`Network Performance`)) as `AWS Specs`" +
		" , (case when localDiscoveryDF.`Logical Processor Count` is null and localDiscoveryDF.`Number of Processors` is not null then " +
		" cast(localDiscoveryDF.`Number of Processors` as int) when localDiscoveryDF.`Logical Processor Count` is not null then " +
		" cast(localDiscoveryDF.`Logical Processor Count` as int) else 0 end) as `vCPU`, (case when localDiscoveryDF.`Memory` is null then 0 else cast(localDiscoveryDF.`Memory` as int) end) as `MemorySize`, cast(awsPricing2.PricePerUnit as float) as `PricePerUnit`,awsPricing2.`Operating System` as `OperatingSystem`, localDiscoveryDF.Host" +
		" FROM global_temp.localDiscoveryTemp localDiscoveryDF" +
		" join (Select localDiscoveryDF1.site_key, localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate " +
		" from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and " +
		" localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key" +
		" left join (select `Operating System`,cast(Memory as int), min(cast(PricePerUnit as float)) as pricePerUnit, cast(vCPU as int),TermType from global_temp.awsPricingDF where `License Model`='No License required'" +
		" and Location='US East (Ohio)' and Tenancy <> 'Host' and (`Product Family` = 'Compute Instance (bare metal)' or `Product Family` = 'Compute Instance') and cast(PricePerUnit as float) > 0 group by `Operating System`,Memory,vCPU,TermType) awsPricing on" +
		" lcase(awsPricing.`Operating System`) = lcase((case when localDiscoveryDF.OS like '%Red Hat%' then 'RHEL'" +
		" when localDiscoveryDF.OS like '%SUSE%' then 'SUSE' when localDiscoveryDF.OS like '%Linux%' OR localDiscoveryDF.OS like '%CentOS%' then 'Linux'" +
		" when localDiscoveryDF.OS like '%Windows%' then 'Windows' else localDiscoveryDF.`Server Type` end)) and" +
		" (cast(awsPricing.Memory as int) >= (case when localDiscoveryDF.Memory is null then 0 else cast(localDiscoveryDF.Memory as int) end)" +
		" or cast(awsPricing.vCPU as int) >= (case when localDiscoveryDF.`Logical Processor Count` is null and localDiscoveryDF.`Number of Processors` is not null then " +
		" cast(localDiscoveryDF.`Number of Processors` as int) when localDiscoveryDF.`Logical Processor Count` is not null then " +
		" cast(localDiscoveryDF.`Logical Processor Count` as int) else 0 end))" +
		" left join global_temp.awsPricingDF awsPricing2 on awsPricing2.`Operating System` = awsPricing.`Operating System` and cast(awsPricing2.PricePerUnit as float) = cast(awsPricing.pricePerUnit as float) and cast(awsPricing.Memory as int) = " +
		" cast(awsPricing2.Memory as int) and cast(awsPricing.vCPU as int) = cast(awsPricing2.vCPU as int) and awsPricing2.TermType='OnDemand' where cast(awsPricing2.PricePerUnit as float) > 0) report) reportData" +
		" where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
		dataCheck.createOrReplaceGlobalTempView("awsReport");		
		} catch (Exception ex) {
		ex.printStackTrace();
		}
		}



	public void getAzurePricing() {
		try {
			Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from ("
					+ " select report.log_date,report.`vCPU`, report.site_key, "
					+ " report.`Server Name`, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`,"
					+ " report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`,"
					+ " report.`Number of Processors`, report.`Logical Processor Count`, "
					+ " report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`,"
					+ " report.`Number of Ports`,report.`Azure On Demand Price`,report.`Azure 3 Year Price`, report.`Azure 1 Year Price`, report.`Azure Instance Type`, report.`Azure Specs`, "
					+ " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`Azure On Demand Price` as float) asc) as my_rank"
					+ " from (SELECT localDiscoveryDF.log_date,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Type`,"
					+ " localDiscoveryDF.OS as `OS Name`,localDiscoveryDF.`OS Version`,localDiscoveryDF.`Server Model`,"
					+ " localDiscoveryDF.`Logical Processor Count`,localDiscoveryDF.`Number of Processors`,"
					+ " cast(localDiscoveryDF.`Memory` as int) as `Memory`, round(localDiscoveryDF.`Total Size`,2) as `Total Size`, "
					+ " cast(localDiscoveryDF.`CPU GHz` as int) as `CPU GHz`, localDiscoveryDF.`Processor Name`, "
					+ " cast(localDiscoveryDF.`Number of Cores` as int) as `Number of Cores`,"
					+ " localDiscoveryDF.`DB Service`, localDiscoveryDF.`HBA Speed`, cast(localDiscoveryDF.`Number of Ports` as int) as `Number of Ports`,"
					+ " round(azurePricingDF.demandPrice,2) as `Azure On Demand Price`,"
					+ " round(azurePricingDF.3YrPrice,2) as `Azure 3 Year Price`,"
					+ " round(azurePricingDF.1YrPrice,2) as `Azure 1 Year Price`,"
					+ " azurePricingDF.InstanceType as `Azure Instance Type`,azurePricingDF.`Azure Specs`,"
					+ " (case when localDiscoveryDF.`Logical Processor Count` is null  and localDiscoveryDF.`Number of Processors` is not null then "
					+ " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Logical Processor Count` is not null then "
					+ " localDiscoveryDF.`Logical Processor Count` else 0 end) as `vCPU`"
					+ " FROM global_temp.localDiscoveryTemp localDiscoveryDF"
					+ " left join global_temp.azurePricingDF azurePricingDF on azurePricingDF.VCPUs >= (case when localDiscoveryDF.`Logical Processor Count` is null  and localDiscoveryDF.`Number of Processors` is not null then"
					+ " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Logical Processor Count` is not null then"
					+ " localDiscoveryDF.`Logical Processor Count` else 0 end)"
					+ " and azurePricingDF.Memory >= (case when localDiscoveryDF.Memory is null then 0 else cast(localDiscoveryDF.Memory as int) end) and"
					+ " lcase(azurePricingDF.OperatingSystem) = lcase((case when localDiscoveryDF.OS like '%Red Hat%' then 'RHEL' "
					+ " when localDiscoveryDF.OS like '%SUSE%' then 'SUSE' when localDiscoveryDF.OS like '%Linux%' OR localDiscoveryDF.OS like '%CentOS%' then 'Linux'"
					+ " when localDiscoveryDF.OS like '%Windows%' then 'Windows' else localDiscoveryDF.`Server Type` end))) report ) reportData "
					+ " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
			dataCheck.createOrReplaceGlobalTempView("azureReport");
			dataCheck.cache();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getAzurePricingForAWS() {
		try {
			Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" + " select report.`VCPUs`, "
					+ " report.`instanceid`, report.`OS Name`, " + " report.`Memory` as `Memory`, "
					+ " report.`Azure On Demand Price`,report.`Azure 3 Year Price`, report.`Azure 1 Year Price`, report.`Azure Instance Type`, report.`Azure Specs`, "
					+ " ROW_NUMBER() OVER (PARTITION BY report.`instanceid` ORDER BY cast(report.`Azure On Demand Price` as float) asc) as my_rank"
					+ " from (SELECT ai.`instanceid`, " + " ai.`Number of Cores`, ai.`actualOsType` as `OS Name`, "
					+ " round(azurePricingDF.demandPrice,2) as `Azure On Demand Price`,"
					+ " round(azurePricingDF.3YrPrice,2) as `Azure 3 Year Price`,"
					+ " round(azurePricingDF.1YrPrice,2) as `Azure 1 Year Price`," + " azurePricingDF.VCPUs,"
					+ " azurePricingDF.Memory, azurePricingDF.`InstanceType` as `Azure Instance Type`, azurePricingDF.`Azure Specs` "
					+ " FROM global_temp.awsInstanceDF ai"
					+ " left join global_temp.azurePricingDF azurePricingDF on cast(azurePricingDF.VCPUs as int) >= cast(ai.`Number of Cores` as int) "
					+ " and cast(azurePricingDF.Memory as float) >= cast(ai.Memory as float) "
					+ " and lower(azurePricingDF.`OperatingSystem`) = lower(ai.`actualOsType`)"
					+ " ) report ) reportData " + " where reportData.my_rank= 1 order by reportData.`instanceid` asc")
					.toDF();
			dataCheck.createOrReplaceGlobalTempView("azureReportForAWSInstance");

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getAwsPricingForThirdParty() {
		try {
			// cast(a.vCPU as int) = cast(ai.`Number of Cores` as int) and cast(a.Memory as
			// int) = cast (ai.Memory as int) and
			Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from ("
					+ " select report.`vCPU`,  report.`Server Name`, "
					+ " report.`AWS Instance Type`, report.`OS Name`, report.instanceid, "
					+ " report.`Memory` as `Memory`, "
					+ " report.`AWS On Demand Price`,report.`AWS 3 Year Price`, report.`AWS 1 Year Price`, report.`AWS Instance Type`, report.`AWS Specs`, "
					+ " ROW_NUMBER() OVER (PARTITION BY report.`Server Name`  ORDER BY cast(report.`PricePerUnit` as float) asc) as my_rank"
					+ " from (SELECT ai.`instanceid`, " + " ai.`Number of Cores`, ai.`actualOsType` as `OS Name`, "
					+ " round( ( ( select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and  a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ), 2 ) as `AWS 3 Year Price`, "
					+ " round( ( ( select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and  a.LeaseContractLength = '1yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ), 2 ) as `AWS 1 Year Price`, "
					+ " round( (a.`PricePerUnit` * 730), 2 ) as `AWS On Demand Price`, a.`PricePerUnit`, "
					+ " a.vCPU, ai.`Server Name`, "
					+ " a.Memory, a.`Instance Type` as `AWS Instance Type`, concat_ws(',', concat('Processor: ',a.`Physical Processor`),concat('vCPU: ',a.vCPU),concat('Clock Speed: ',a.`Clock Speed`),concat('Processor Architecture: ',a.`Processor Architecture`) ,concat('Memory: ',a.Memory),concat('Storage: ',a.Storage),concat('Network Performance: ',a.`Network Performance`)) as `AWS Specs` "
					+ " FROM global_temp.awsInstanceDF ai"
					+ " left join global_temp.awsPricingDF a on cast(a.vCPU as int) >=  cast(ai.`Number of Cores` as int) and  cast(a.Memory as int) >= cast (ai.Memory as int) "
					+ " and a.`TermType`='OnDemand' and Location='US East (Ohio)' and Tenancy <> 'Host'  and (`Product Family` = 'Compute Instance (bare metal)' or `Product Family` = 'Compute Instance') and lower(a.`Operating System`) = lower(ai.`actualOsType`) and cast(a.`PricePerUnit` as float ) > 0.0"
					+ " ) report ) reportData " + " where reportData.my_rank= 1 order by reportData.`instanceid` asc")
					.toDF();
			dataCheck.createOrReplaceGlobalTempView("awsReportForThirdParty");

			dataCheck.show();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getGooglePricingForAWS() {
		try {
			Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from ("
					+ " select report.`VCPUs`, report.`instanceid`, " + " report.`OS Name`," + " report.`Memory`, "
					+ " report.`Number of Cores`, "
					+ " report.`Google Instance Type`,report.`Google On Demand Price`,report.`Google 1 Year Price`,report.`Google 3 Year Price`,"
					+ " ROW_NUMBER() OVER (PARTITION BY report.`instanceid` ORDER BY cast(report.`Google On Demand Price` as float) asc) as my_rank"
					+ " from (SELECT ai.`instanceid`, " + " ai.`Number of Cores`, ai.`actualOsType` as `OS Name`,"
					+ " ai.Memory, ai.instanceid," + " ai.`Number of Cores`, "
					+ " googlePricing.InstanceType as `Google Instance Type`, googlePricing.`VCPUs`,"
					+ " round(googlePricing.pricePerUnit * 730 + "
					+ " (case when ai.`actualOsType` like '%Windows%' then 67.16  when ai.`actualOsType` like '%Red Hat%' then 43.8 else 0  end),2) as `Google On Demand Price`,"
					+ " round(googlePricing.1YrPrice * 730 + "
					+ " (case when ai.`actualOsType` like '%Windows%' then 67.16  when ai.`actualOsType` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 1 Year Price`,"
					+ " round(googlePricing.3YrPrice * 730 + "
					+ " (case when ai.`actualOsType` like '%Windows%' then 67.16  when ai.`actualOsType` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 3 Year Price`"
					+ " FROM global_temp.awsInstanceDF ai "
					+ " left join (select cast(OnDemandPrice as float) as pricePerUnit,VCPUs,Memory,InstanceType,1YrPrice,3YrPrice from global_temp.googlePricingDF where "
					+ " Region='US East' order by cast(OnDemandPrice as float) asc) googlePricing on cast(googlePricing.VCPUs as float) >=  cast(ai.`Number of Cores` as float) and "
					+ " cast(googlePricing.Memory as float) >= cast (ai.Memory as float)) report ) reportData "
					+ " where reportData.my_rank= 1 order by reportData.`instanceid` asc").toDF();
			dataCheck.createOrReplaceGlobalTempView("googleReportForAWSInstance");
			dataCheck.cache();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getGooglePricing() {
		try {
			Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from ("
					+ " select report.log_date,report.`vCPU`, report.site_key, "
					+ " report.`Server Name`, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`,"
					+ " report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`,"
					+ " report.`Number of Processors`, report.`Logical Processor Count`, "
					+ " report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`,"
					+ " report.`Number of Ports`,"
					+ " report.`Google Instance Type`,report.`Google On Demand Price`,report.`Google 1 Year Price`,report.`Google 3 Year Price`,"
					+ " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`Google On Demand Price` as float) asc) as my_rank"
					+ " from (SELECT localDiscoveryDF.log_date,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Type`,"
					+ " localDiscoveryDF.OS as `OS Name`,localDiscoveryDF.`OS Version`,localDiscoveryDF.`Server Model`,"
					+ " cast(localDiscoveryDF.`Logical Processor Count` as int) as `Logical Processor Count`,cast(localDiscoveryDF.`Number of Processors` as int) as `Number of Processors`,"
					+ " cast(localDiscoveryDF.Memory as int) as `Memory`, round(localDiscoveryDF.`Total Size`,2) as `Total Size`, "
					+ " cast(localDiscoveryDF.`CPU GHz` as int) as `CPU GHz`, localDiscoveryDF.`Processor Name` as `Processor Name`, "
					+ " cast(localDiscoveryDF.`Number of Cores` as int) as `Number of Cores`,"
					+ " localDiscoveryDF.`DB Service` as `DB Service`, localDiscoveryDF.`HBA Speed` as `HBA Speed`, cast(localDiscoveryDF.`Number of Ports` as int) as `Number of Ports`,"
					+ " localDiscoveryDF.`Logical Processor Count` as `vCPU`,"
					+ " googlePricing.InstanceType as `Google Instance Type`,"
					+ " round(googlePricing.pricePerUnit*730 + "
					+ " ((case when localDiscoveryDF.`Total Size` is null then 0 else localDiscoveryDF.`Total Size` end) * 0.08) + "
					+ " (case when localDiscoveryDF.OS like '%Windows%' then 67.16  when localDiscoveryDF.OS like '%Red Hat%' then 43.8 else 0  end),2) as `Google On Demand Price`,"
					+ " round(googlePricing.1YrPrice*730 + "
					+ " ((case when localDiscoveryDF.`Total Size` is null then 0 else localDiscoveryDF.`Total Size` end) * 0.05) + "
					+ " (case when localDiscoveryDF.OS like '%Windows%' then 67.16  when localDiscoveryDF.OS like '%Red Hat%' then 43.8 else 0  end),2) as `Google 1 Year Price`,"
					+ " round(googlePricing.3YrPrice*730 + "
					+ " ((case when localDiscoveryDF.`Total Size` is null then 0 else localDiscoveryDF.`Total Size` end) * 0.04) + "
					+ " (case when localDiscoveryDF.OS like '%Windows%' then 67.16  when localDiscoveryDF.OS like '%Red Hat%' then 43.8 else 0  end),2) as `Google 3 Year Price`"
					+ " FROM global_temp.localDiscoveryTemp localDiscoveryDF "
					+ " join (Select localDiscoveryDF1.site_key,localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate "
					+ " from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and "
					+ " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key"
					+ " left join (select cast(OnDemandPrice as float) as pricePerUnit,VCPUs,Memory,InstanceType,1YrPrice,3YrPrice from global_temp.googlePricingDF where "
					+ " Region='US East' order by cast(OnDemandPrice as float) asc) googlePricing on cast(googlePricing.VCPUs as float) >= "
					+ " (case when localDiscoveryDF.`Number of Processors` is not null then"
					+ " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Number of Processors` is not null then"
					+ " localDiscoveryDF.`Logical Processor Count` else 0 end) and "
					+ " cast(googlePricing.Memory as float) >= (case when localDiscoveryDF.Memory is null then 0 else "
					+ " cast(localDiscoveryDF.Memory as int) end)) report ) reportData "
					+ " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
			dataCheck.createOrReplaceGlobalTempView("googleReport");
			dataCheck.cache();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private int getEOLEOSCount(String siteKey) {
		logger.info("Construct EOL/EOS Dataframe Begins");
		int dataCount = 0;
		try {
			int count = eolService.getEOLEOSData();

			if (count != 0) {
				String sql = " select report.source_id, report.site_key, report.`Server Name`,report.`Server Type`,report.`OS Name`,report.`OS Version`, "
						+ " eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`"
						+ " from (select localDiscoveryDF.source_id,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Type`,"
						+ " localDiscoveryDF.OS as `OS Name`,localDiscoveryDF.`OS Version`"
						+ " FROM global_temp.localDiscoveryTemp localDiscoveryDF"
						+ " join (Select localDiscoveryDF1.site_key,localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate "
						+ " from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and "
						+ " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key) report"
						+ " left join global_temp.eolDataDF eol on eol.os_type=report.`Server Type` and replace(trim(report.`OS Name`),'(R)' ' ') like concat('%',trim(eol.os_type),'%')"
						+
						// " and trim(report.`OS Version`) like concat('%',trim(eol.osversion),'%') " +
						" and trim(report.`OS Version`) = trim(eol.os_version) " + " where report.site_key='" + siteKey
						+ "'";

				Dataset<Row> dataCheck = sparkSession.sql(sql).toDF();

				dataCount = Integer.parseInt(String.valueOf(dataCheck.count()));
				if (dataCount > 0) {
					dataCheck.createOrReplaceGlobalTempView("eoleosDataDF");
					// dataCheck.cache();
				}
			}
		} catch (Exception ex) {
			logger.error("Exception in generating dataframe for EOL/EOS OS data", ex);
		}
		logger.info("Construct EOL/EOS Dataframe Ends");
		return dataCount;
	}

	private int getEOLEHWCount(String siteKey) {
		logger.info("Construct EOL/EOS - HW Dataframe Begins");
		int dataCount = 0;
		try {
			int count = eolService.getEOLEOSHW();
			if (count > 0) {
				String sql = "select report.source_id, report.site_key, report.`Server Name`,report.`Server Model`, "
						+ " eol.end_of_life_cycle as `End Of Life - HW`,eol.end_of_extended_support as `End Of Extended Support - HW`"
						+ " from (select localDiscoveryDF.source_id,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`,"
						+ " localDiscoveryDF.`Server Model`" + " FROM global_temp.localDiscoveryTemp localDiscoveryDF"
						+ " join (Select localDiscoveryDF1.site_key,localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate "
						+ " from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and "
						+ " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key) report"
						+ " left join global_temp.eolHWDataDF eol on lcase(concat(eol.vendor,' ',eol.model)) = lcase(report.`Server Model`)"
						+ " where report.site_key='" + siteKey + "'";

				Dataset<Row> dataCheck = sparkSession.sql(sql).toDF();

				// dataCheck.printSchema();

				dataCount = Integer.parseInt(String.valueOf(dataCheck.count()));
				if (dataCount > 0) {
					dataCheck.createOrReplaceGlobalTempView("eolHWData");
					// dataCheck.cache();
				}
			}
		} catch (Exception ex) {
			logger.error("Exception in generating dataframe for EOL/EOS - HW data", ex);
		}
		logger.info("Construct EOL/EOS - HW Dataframe Ends");
		return dataCount;
	}

	public JSONObject getMigrationReport(String filePath) throws IOException, ParseException {
		if (filePath.contains(",")) {
			filePath = filePath.split(",")[0];
		}
		System.out.println("-----------filePath-get----" + filePath);
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(new FileReader(filePath));
		JSONObject jsonObject = (JSONObject) obj;

		/*
		 * JSONObject json = new JSONObject(); File f = new File(filePath);
		 * System.out.println("-----------filePath-----" + filePath); String viewName =
		 * f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
		 * System.out.println("------------ODB View Name get------------" + viewName);
		 * try { String datas =
		 * sparkSession.sql("select * from global_temp."+viewName).toJSON().
		 * collectAsList().toString(); JSONParser parser = new JSONParser(); Object obj
		 * = parser.parse(datas); JSONArray jsonArray = (JSONArray) obj; json =
		 * (JSONObject) jsonArray.get(0); } catch (Exception e) { e.printStackTrace();
		 * StringWriter errors = new StringWriter(); e.printStackTrace(new
		 * PrintWriter(errors)); String ex = errors.toString();
		 * ExceptionHandlerMail.errorTriggerMail(ex); if(f.exists()) {
		 * createDataframeForJsonData(filePath); json = getMigrationReport(filePath); }
		 * }
		 */

		return jsonObject;
	}

	public void createDataframeForJsonData(String filePath) {
		if (filePath.contains(",")) {
			filePath = filePath.split(",")[0];
		}
		try {
			
			//repalceEmptyFromJson(filePath);
			
			ObjectMapper mapper = new ObjectMapper();
			JSONObject jsonObject = mapper.readValue(new File(filePath), JSONObject.class);
			JSONArray jData = (JSONArray) jsonObject.get("data");
			
			mapper.writeValue(new File(filePath), jData);
			Dataset<Row> dataset = sparkSession.read().option("multiline", true).option("nullValue", "")
					.option("mode", "PERMISSIVE").json(filePath);
			File f = new File(filePath);
			String viewName = f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
			dataset.createOrReplaceGlobalTempView(viewName);
			
			setFileOwner(new File(filePath));
		       	
			System.out.println("------------ODB View Name create------------" + viewName);
			
		} catch (Exception e) {
			e.printStackTrace();			
		}

	}
	
private void repalceEmptyFromJson(String filePath) {
        
        Path path = Paths.get(filePath);       
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {          
            List<String> list = stream.map(line -> line.replaceAll("\":,\"", "\":null,\"")).collect(Collectors.toList());         
            Files.write(path, list, StandardCharsets.UTF_8);
        } catch (IOException e) {         
            e.printStackTrace();
        }
    }


	public void createCloudCostDataframeFromJsonData(String filePath, String viewName) {

		try {
			Dataset<Row> dataset = sparkSession.read().option("multiline", true).option("nullValue", "")
					.option("mode", "PERMISSIVE").json(filePath);
			dataset.createOrReplaceGlobalTempView(viewName);
			System.out.println("------------cloud cost view----------" + viewName);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	public void destroryCloudCostDataframe(String siteKey) {
		try {
			String viewName = siteKey.replaceAll("\\s+", "").replaceAll("-", "") + "_cloudcost";
			sparkSession.catalog().dropGlobalTempView(viewName);
			String cloudCostDfPath = commonPath + File.separator + "Dataframe" + File.separator + "CCR" + File.separator
					+ siteKey + File.separator;
			File dfFolder = new File(cloudCostDfPath);
			FileSystemUtils.deleteRecursively(dfFolder);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	public DataResult getReportDataFromOdbDf(ServerSideGetRowsRequest request) {

		String siteKey = request.getSiteKey();
		
		String componentName = "";
		if(request.getOstype() != null && !request.getOstype().isEmpty()) { //server
			componentName = request.getOstype();
		} else if(request.getSwitchtype() != null && !request.getSwitchtype().isEmpty()) { //switch
			componentName = request.getSwitchtype();
		} else if(request.getStorage() != null && !request.getStorage().isEmpty()) { //Storage
			componentName = request.getStorage();
		} else if(request.getThirdPartyId() != null && !request.getThirdPartyId().isEmpty()) { //Project
			componentName = request.getThirdPartyId();
		} else if(request.getProviders() != null && !request.getProviders().isEmpty()) { //Providers
			componentName = request.getProviders();
		} else if(request.getProject() != null && !request.getProject().isEmpty()) { //Project
			componentName = request.getProject();
		}
		
		
		String viewNameWithHypen = siteKey + "_" + request.getAnalyticstype().toLowerCase() + "_"
				+ request.getCategory() + "_" + componentName + "_" + request.getReportList() + "_"
				+ request.getReportBy();
		String viewName = viewNameWithHypen.replaceAll("-", "").replaceAll("\\s+", "");
		
	
		
		File verifyDataframePath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ "OrientDB" + File.separator + siteKey + File.separator + componentName
				+ File.separator + viewNameWithHypen + ".json");
		
		System.out.println("------verifyDataframePath-------" + verifyDataframePath);
		
		File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ "OrientDB" + File.separator + siteKey + File.separator + componentName + File.separator );
		

		
		Dataset<Row> dataset = null;
		
		
		if(!componentName.toLowerCase().contains("tanium")) {  
			boolean isDiscoveryDataInView = false;	
			try {
				dataset = sparkSession.sql("select * from global_temp." + viewName);		
				isDiscoveryDataInView = true;
			} catch (Exception e) {
				System.out.println("---------View Not exists--------");
			}

			if (!isDiscoveryDataInView) {			
				
				if (verifyDataframePath.exists()) {
					
					createDataframeFromJsonFile(viewName, verifyDataframePath.getAbsolutePath());
					dataset = sparkSession.sql("select * from global_temp." + viewName);
				
				} else {				
						createDataframeFromOdb(request, verifyDataframePath, verifyDataframeParentPath);				
					if (verifyDataframePath.exists()) {
						createDataframeFromJsonFile(viewName, verifyDataframePath.getAbsolutePath());
						dataset = sparkSession.sql("select * from global_temp." + viewName); 
					}
					
				} 
			}

		} else { //tanium logic
			dataset = getTaniumReport(siteKey);		
		}
	
		
		
	
		
		
		rowGroups = request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
		groupKeys = request.getGroupKeys();
		valueColumns = request.getValueCols();
		pivotColumns = request.getPivotCols();
		filterModel = request.getFilterModel();
		sortModel = request.getSortModel();
		isPivotMode = request.isPivotMode();
		isGrouping = rowGroups.size() > groupKeys.size();

		rowGroups = formatInputColumnNames(rowGroups);
		groupKeys = formatInputColumnNames(groupKeys);
		sortModel = formatSortModel(sortModel);
			
		dataset = orderBy(groupBy(filter(dataset)));	
		
		Dataset<Row> countData = sparkSession.emptyDataFrame();
		try {
			List<String> numericColumns = getReportNumericalHeaders(request.getReportType(), componentName, request.getReportBy(),request.getSiteKey());
			if(numericColumns != null) {
				dataset.createOrReplaceTempView("tmpReport");
				String numericCol = String.join(",", numericColumns
			            .stream()
			            .map(col -> ("sum(`" + col + "`) as `"+col+"`"))
			            .collect(Collectors.toList()));	
				
			if(numericCol != null && !numericCol.isEmpty()) {
				countData = sparkSession.sqlContext().sql("select "+numericCol+"  from tmpReport");//.sqlContext().sql("select `Total Size` group by `Total Size`").groupBy(new Column("`Total Size`""));
			}
				
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		return paginate(dataset, request, countData.toJSON().collectAsList());

	
	 
	}

	
	private Dataset<Row> getTaniumReport(String siteKey) {
		
		Dataset<Row> taniumDataset = sparkSession.emptyDataFrame();
		
		try {
			File taniumDataframFile = new File(commonPath + File.separator + "Dataframe" + File.separator
					+ "OrientDB" + File.separator + "Tanium"  + siteKey + File.separator );
			
			File customDataframFile = new File(commonPath + File.separator + "Dataframe" + File.separator
					+ "OrientDB" +  File.separator + "Tanium"
					+ File.separator + siteKey+"_" + "custom_data" + ".json");
			
			String logDataViewName = siteKey  + "tanium_log"; 
			logDataViewName = logDataViewName.replaceAll("-", "").replaceAll("\\s+", "").toLowerCase();
			
			String customDataViewName = siteKey  + "tanium_custom"; 
			customDataViewName = customDataViewName.replaceAll("-", "").replaceAll("\\s+", "").toLowerCase();
			
			Dataset<Row> logDataset = sparkSession.read().option("multiline", true).json(taniumDataframFile.getAbsolutePath()+"*.json");
			logDataset.createOrReplaceGlobalTempView(logDataViewName);
			
			Dataset<Row> customDataset = sparkSession.read().option("multiline", true).json(customDataframFile.getAbsolutePath());
			customDataset.createOrReplaceGlobalTempView(customDataViewName);
			taniumDataset = sparkSession.sqlContext().sql("select * from global_temp." + logDataViewName + " ld left join global_temp." + customDataViewName + " cd on ld.server_name=cd.primary_key_value");
		 } catch (Exception e) {
			e.printStackTrace();
		}
		
		return taniumDataset;
	}

	private void createDataframeFromJsonFile(String viewName, String filePath) {
		if (filePath.endsWith(".json")) {
			try {
				Dataset<Row> dataset = sparkSession.read().option("multiline", true).json(filePath);
				dataset.createOrReplaceGlobalTempView(viewName);
				dataset.show();			
				System.out.println("---------View created-------- :: " + viewName);
			} catch (Exception e) {
				e.printStackTrace();

			}

		}

	}
	
	
	private void createDataframeFromOdb(ServerSideGetRowsRequest request, File filePath, File verifyDataframeParentPath) {
		
		System.out.println("-----initate migration API--");
		
		String protocol = ZKModel.getProperty(ZKConstants.APP_SERVER_PROTOCOL);
    	String appServerIp = ZKModel.getProperty(ZKConstants.APP_SERVER_IP);
    	String port = ZKModel.getProperty(ZKConstants.APP_SERVER_PORT);
      // String uri = protocol + "://" + appServerIp + ":" + port + "/ZenfraV2/rest/reports/getReportData/migrationreport";
    	String uri = "https://uat.zenfra.co/ZenfraV2/rest/reports/getReportData/migrationreport";
    	uri = uri+"?authUserId="+request.getStartRow()
    	+"&reportCategory="+request.getReportCategory()
    	+"&reportType="+request.getReportType()
    	+"&siteKey="+request.getSiteKey()
    	+"&category="+request.getCategory()
    	+"&project="+request.getProject()
    	+"&filters="+request.getFilterModel()
    	+"&analyticstype="+request.getAnalyticstype()
    	+"&migrationtype="
    	+"&method="
    	+"&reportview="
    	+"&logDate="
    	+"&skip=0"
    	+"&limit=0"
    	+"&mode=" 
    	+"&ostype="+request.getOstype()
    	+"&arraytype=" 
    	+"&vendor="
    	+"&providers="+request.getProviders()
    	+"&type="
    	+"&switchtype="+request.getSwitchtype()
    	+"&thirdPartyId="+request.getThirdPartyId()
    	+"&isSubReportAccess=0"
    	+"&reportList="+request.getReportList()
    	+"&destinationtype="
    	+"&collectiondate="
    	+"&reportBy="+request.getReportBy()
    	+"&isTasklist=0"
    	+"&storage="+request.getStorage()    	
    	;
        uri = CommonUtils.checkPortNumberForWildCardCertificate(uri);
      
        Map<String, Object> map =   mapper.convertValue(request, new TypeReference<Map<String, Object>>() {});
        map.put("skip", 0);
        map.put("limit", 0);
       	  Map<String, Object> body= new LinkedHashMap<>();
	    body.putAll(map); 
	   
	    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
	    		builder.build(map);
	    System.out.println(builder.buildAndExpand(map).toUri());
	   

	 
	    
	    
		  
	 RestTemplate restTemplate = new RestTemplate();
	 
	 
	
	 HttpEntity<Object> httpRequest = new HttpEntity<>(body);
	 ResponseEntity<String> restResult = restTemplate.exchange(builder.buildAndExpand(map).toUri() , HttpMethod.POST,
	    		httpRequest, String.class);
	   
	///// ResponseEntity<String> restResult = restTemplate.exchange(uri, HttpMethod.POST, httpRequest, String.class);
	
	 JSONObject resultObj = new JSONObject();
	try {
		resultObj = (JSONObject) parser.parse(restResult.getBody());
	} catch (ParseException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	 
	  try {		
         if(!verifyDataframeParentPath.exists()) {
        	 verifyDataframeParentPath.mkdirs();
        	
         }
         setFileOwner(verifyDataframeParentPath);
		  mapper.writeValue(filePath, resultObj.get("data"));
		 setFileOwner(filePath);
		 System.out.println("-----------------Write DF PAth----------" + filePath.getAbsolutePath());
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
		
	}
	
	
	private void setFileOwner(File filePath) {
		try {
			Path resultFilePath = Paths.get("/opt/ZENfra/Dataframe/");
			UserPrincipal owner = resultFilePath.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName("zenuser");
			Files.setOwner(resultFilePath, owner);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}

	public void recreateReportForDataframe(String siteKey, String sourceType, String userId) {

		List<Map<String, Object>> reportCombination = reportDao.getReportCombinationByLogType(sourceType);
		
		if(!reportCombination.isEmpty()) {
			for(Map<String, Object> reportInput : reportCombination) {
				String reportList = (String) reportInput.get("reportList");
				String reportBy = (String) reportInput.get("reportBy");
				String reportCategory = (String) reportInput.get("category");
				String deviceType = (String) reportInput.get("device");
				
				 if(reportCategory.equalsIgnoreCase("server") || reportCategory.equalsIgnoreCase("switch") || reportCategory.equalsIgnoreCase("Storage")) {
					 ServerSideGetRowsRequest request = new ServerSideGetRowsRequest();
						if(reportCategory.equalsIgnoreCase("server")) { //server
							request.setOstype(deviceType);
						} else if(reportCategory.equalsIgnoreCase("switch")) { //switch
							request.setSwitchtype(deviceType);
						} else if(reportCategory.equalsIgnoreCase("Storage")) { //Storage
							request.setStorage(deviceType);
						} 
						
					
						request.setReportCategory("migration");
						request.setSiteKey(siteKey);
						request.setCategory(reportCategory);						
						request.setAnalyticstype("Discovery");
						request.setReportList(reportList);
						request.setReportBy(reportBy);
						request.setReportType("discovery");
						
						
						String viewNameWithHypen = siteKey + "_" + request.getAnalyticstype().toLowerCase() + "_"
								+ request.getCategory() + "_" + deviceType + "_" + request.getReportList() + "_"
								+ request.getReportBy();
						String viewName = viewNameWithHypen.replaceAll("-", "").replaceAll("\\s+", "");
						
						 
						
						File verifyDataframePath = new File(commonPath + File.separator + "Dataframe" + File.separator
								+ "OrientDB" + File.separator + siteKey + File.separator + deviceType
								+ File.separator + viewNameWithHypen + ".json");
						
						File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
								+ "OrientDB" + File.separator + siteKey + File.separator + deviceType + File.separator );
						
						createDataframeFromOdb(request, verifyDataframePath, verifyDataframeParentPath);
						
				 }
				
			}
		}
		
	}

	
	
	
	//------------------------ Tanium Report------------------------------------------------//
	@SuppressWarnings("unchecked")
	private JSONArray getPrivillegeAccessReportData(String siteKey, File filePath) {
		
		JSONArray resultArray = new JSONArray(); 
		JSONParser parser = new JSONParser();
		String query = "select source_id, server_name, privillege_data, json_agg(source_data) as source_data from (\r\n" + 
				"select a.source_id, server_name, a.data as privillege_data, td.data as source_data from ( \r\n" + 
				"select source_id, server_name, replace(data, '.0\"', '\"') as data from privillege_data \r\n" + 
				"where site_key = '" + siteKey + "' \r\n" + 
				") a\r\n" + 
				"LEFT JOIN (select primary_key_value, json_object_agg(source_name, data::json) as data from (\r\n" + 
				"select source_id, source_name, primary_key_value, data - 'sourceId' - 'siteKey' - 'User Name' - 'Server Name' as data from ( \r\n" +
				"select source_id, source_name, primary_key_value, data::jsonb || concat('{\"Last Updated Time\":\"', update_time, '\"}')::jsonb as data from (\r\n" + 
				"select sd.source_id, s.source_name, primary_key_value, \r\n" + 
				"to_char(to_timestamp(update_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as update_time, \r\n" + 
				"replace(data, '.0\"', '\"') as data, \r\n" + 
				"row_number() over(partition by sd.source_id, primary_key_value order by update_time desc) as row_num from source_data sd  \r\n" + 
				"JOIN source s on s.source_id = sd.source_id and s.is_active = true and s.site_key = '" + siteKey + "'  \r\n" + 
				"where sd.site_key = '" + siteKey + "' \r\n" + 
				") b  \r\n" + 
				"where row_num = 1 \r\n" + 
				") c\r\n" + 
				") e \r\n" +
				"group by primary_key_value \r\n" + 
				") td on td.primary_key_value ilike (a.source_id || '%') or td.primary_key_value ilike (a.server_name || '%') \r\n" + 
				") c group by source_id, server_name, privillege_data";
				
		System.out.println("!!!!! privillege data query: " + query);
		try {
			
			List<Map<String, Object>> taniumData = reportDao.getListOfMapByQuery(query);
			
			System.out.println("!!!!! --------------------------------------  !!!!! ");
			
			System.out.println(taniumData.size());
			
			System.out.println("!!!!! --------------------------------------  !!!!! ");
			
			for(Map<String, Object> rs : taniumData) {
				ZenfraJSONObject dataObject = new ZenfraJSONObject();
				JSONArray privilegeDataArray = (JSONArray) parser.parse(rs.get("privillege_data") == null ? "[]" : (String) rs.get("privillege_data"));
				if(!privilegeDataArray.isEmpty()) {
					JSONObject jsonObject = (JSONObject) privilegeDataArray.get(0);
					Set<String> keySet = jsonObject.keySet();
					for(String key : keySet) {
						dataObject.put("Server Data~" + key, jsonObject.get(key).toString());
					}
				}
				
				JSONArray sourceDataArray = (JSONArray) parser.parse((rs.get("source_data") == null || (String.valueOf(rs.get("source_data"))).equalsIgnoreCase("[null]")) ? "[]" : String.valueOf(rs.get("source_data")));
				if(!sourceDataArray.isEmpty()) {
					for(int i = 0; i < sourceDataArray.size(); i++) {
						JSONObject sourceDataObject = (JSONObject) sourceDataArray.get(i);
						Set<String> keySet = sourceDataObject.keySet();
						for(String key : keySet) {
							JSONObject jsonObject1 = (JSONObject) sourceDataObject.get(key);
							Set<String> innerKeySet = jsonObject1.keySet();
							for(String key1 : innerKeySet) {
								if(key1 != null && key1.isEmpty() && (key1.equalsIgnoreCase("Processed Date") || key1.equalsIgnoreCase("Date of Last Password Change"))) {
									String value = jsonObject1.get(key1).toString();
									value = formatDateStringToUtc(value);
									dataObject.put(key + "~" + key1, value);
								} else {
									dataObject.put(key + "~" + key1, jsonObject1.get(key1).toString());
								}
								dataObject.put(key + "~" + key1, jsonObject1.get(key1).toString());
							}
						}
					}
					
				}
				
				if(!dataObject.isEmpty()) {
					resultArray.add(dataObject);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		if(!resultArray.isEmpty()) {
			 
			try {
				  mapper.writeValue(filePath, resultArray);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		
		return resultArray;
	}
	
	private String formatDateStringToUtc(String value) {
		try {
			value = value.replaceAll("UTC", "").replaceAll("utc", "").trim();
			value = commonFunctions.convertToUtc(TimeZone.getDefault(), value);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return value;
	}


	public JSONObject getReportHeaderForLinuxTanium(ServerSideGetRowsRequest request) {
		
		JSONObject header = new JSONObject();
		
		String viewNameWithHypen = request.getSiteKey() + "_" + request.getAnalyticstype().toLowerCase() + "_"
				+ request.getCategory() + "_" + "Tanium" + "_" + request.getReportList() + "_"
				+ request.getReportBy();
		
	
		
		File dfFilePath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ "OrientDB" + File.separator + request.getSiteKey() + File.separator + "Tanium"
				+ File.separator + viewNameWithHypen + ".json");
		
		File verifyDataframePath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ "OrientDB" + File.separator + request.getSiteKey() + File.separator + "Tanium"
				+ File.separator + viewNameWithHypen + ".json");
		
		File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ "OrientDB" + File.separator + request.getSiteKey() + File.separator + "Tanium" + File.separator );
		
		System.out.println("------Tanium verifyDataframeParentPath-------------- " + verifyDataframeParentPath);
		
		if(!dfFilePath.exists()) {
			createDataframeFromOdb(request, verifyDataframePath, verifyDataframeParentPath);
		}
		
		JSONArray taniumData = new JSONArray();
		if(dfFilePath.exists()) {
			try {
			 taniumData = mapper.readValue(dfFilePath, JSONArray.class);
			 System.out.println("------Tanium taniumData-------------- " + taniumData.size());
			 if(!taniumData.isEmpty()) {
				 JSONArray taniumHeader =  getPrivillegedAccessHeaderInfofromData(taniumData, request.getSiteKey(), request.getUserId());
				 header.put("headerInfo", taniumHeader);	
				 header.put("report_label", "Server Tanium by Privileged Access");
				 header.put("report_name", "Local_Tanium_by_Privileged Access");
				 header.put("unit_conv_details", new JSONArray());
				 return header;
				 }
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} 
		
		return null;
	} 


	
	@SuppressWarnings("unchecked")
	private JSONArray getPrivillegedAccessHeaderInfofromData(JSONArray dataArray, String siteKey, String userId) {
		
		
		JSONArray resultArray = new JSONArray();
		Set<String> checkKeys = new HashSet<String>();
		
		try {
			
			JSONArray columnsArray = getPrivillegedAccessHeaderInfo(siteKey, userId);
			JSONArray columnsGroupArray = new JSONArray();
			for(int i = 0; i < columnsArray.size(); i++) {
				columnsGroupArray.add(columnsArray.get(i).toString().substring(0, columnsArray.get(i).toString().indexOf("~")));
			}
			for(int i = 0; i < dataArray.size(); i++) {
				LinkedHashMap dataObject = (LinkedHashMap) dataArray.get(i);
				Set<String> dataKeys = dataObject.keySet();
				for(String key: dataKeys) {
					if(columnsGroupArray.contains(key.substring(0, key.indexOf("~"))) && (columnsArray.contains(key) || key.contains("Last Updated Time"))) {
						if(!key.contains("rid") && !key.contains("sourceId") && !key.contains("siteKey")) {
							if(!checkKeys.contains(key)) {
								ZenfraJSONObject jsonObject = new ZenfraJSONObject();
								jsonObject.put("actualName", key);
								if(key.equalsIgnoreCase("Server Data~Processed Date") || key.contains("Last Updated Time")) {
									jsonObject.put("dataType", "date");
								} else {
									jsonObject.put("dataType", "String");
								}
								
								jsonObject.put("displayName", key.substring(key.indexOf("~") + 1, key.length()));
								
									if(key.equalsIgnoreCase("Server Data~Server Name") || key.equalsIgnoreCase("Server Data~User Name")) {
										jsonObject.put("lockPinned", true);
										jsonObject.put("lockPosition", true);
										jsonObject.put("pinned", "left");
									} else {
										jsonObject.put("lockPinned", false);
										jsonObject.put("lockPosition", false);
										jsonObject.put("pinned", "");
									}
									resultArray.add(jsonObject);
								
								
								checkKeys.add(key);
							}
						}
					}
					
				}
				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
	}
 


	private JSONArray getPrivillegedAccessHeaderInfo(String siteKey, String userId) {
		
		
		JSONArray resultArray = new JSONArray();
		
		String query = "select 'Server Data' as category, concat('Server Data~', column_names) as actual_name, column_names as display_name from (\r\n" + 
				"select column_name as column_names from report_columns where report_name = 'Discovery' and report_by = 'Privileged Access' \r\n" + 
				") a\r\n" + 
				"union all\r\n" + 
				"select source_name as category, concat(source_name, '~', display_label) as actual_name, display_label as display_name from ( \r\n" + 
				"select source_name, created_by, display_label, read_policy, update_policy, ut.user_id, is_tenant_admin from ( \r\n" + 
				"select source_name, created_by, display_label, json_array_elements_text((case when read_policy = '[]' then '[\"test\"]' else read_policy end)::json) as read_policy, \r\n" + 
				"json_array_elements_text((case when update_policy = '[]' then '[\"test\"]' else update_policy end)::json) as update_policy from ( \r\n" + 
				"select source_name, created_by, json_array_elements(fields::json) ->> 'displayLabel' as display_label, \r\n" + 
				"json_array_elements(fields::json) ->> 'read' as read_policy, \r\n" + 
				"json_array_elements(fields::json) ->> 'update' as update_policy \r\n" + 
				"from source where is_active = true and site_key = '" + siteKey + "'  \r\n" + 
				") a1  \r\n" + 
				") a  \r\n" + 
				"LEFT JOIN ( \r\n" + 
				"select user_id, is_tenant_admin, first_name, last_name, site_key, json_array_elements_text(policy_set::json) as policy_set from ( \r\n" + 
				"select user_id, is_tenant_admin, first_name, last_name, json_array_elements(custom_policy::json) ->> 'siteKey'  as site_key,  \r\n" + 
				"json_array_elements(custom_policy::json) ->> 'policset' as policy_set from user_temp  \r\n" + 
				"where user_id = '" + userId + "'  \r\n" + 
				") a where is_tenant_admin = true or site_key = '" + siteKey + "'  \r\n" + 
				") ut on ut.policy_set = a.read_policy or ut.policy_set = a.update_policy or is_tenant_admin = true  \r\n" + 
				") a where user_id is not null or is_tenant_admin = true or created_by = '" + userId + "'";
		
		System.out.println("!!!!! privilleges report headerInfo query: " + query);
		
		try {
			List<Map<String, Object>> resultList = reportDao.getListOfMapByQuery(query);
		for(Map<String, Object> rs : resultList) { 
				resultArray.add(rs.get("actual_name"));
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
	}

	public void recreateTaniumReportForDataframe(String siteKey, String sourceType, String userId) {

		File dfFilePath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ "OrientDB" + File.separator  + File.separator + "Tanium"
				+ File.separator + siteKey + File.separator);  
	 
		
		try {
			
			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", "privillege_data");

			@SuppressWarnings("deprecation")
			Dataset<Row> privillegeData = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));

			//Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(privillegeData, "data", "");
			
			privillegeData.schema();
			privillegeData.show();
			
			String privilageTempView = (siteKey+"privillege_data").replaceAll("-", "");
			privillegeData.createOrReplaceTempView(privilageTempView);
			
	       Dataset<Row> pvData = privillegeData.sqlContext().sql("select * from "+privilageTempView+" where site_key='"+siteKey+"'");
	       
	   
	 
	        pvData.write().option("escape", "").option("quotes", "")
							.option("ignoreLeadingWhiteSpace", true)
							.format("org.apache.spark.sql.json").mode(SaveMode.Overwrite).save(dfFilePath.getAbsolutePath());
	        
	        pvData.show();
			 
	    	File[] files = new File(dfFilePath.getAbsolutePath()).listFiles();

			if (files != null) {
				DataframeUtil.formatJsonFile(files);
			}

		} catch (Exception exp) {
			logger.error("Not able to create dataframe {}", exp.getMessage(), exp);
		}
		
	}

	public void recreateCustomExcelReportForDataframe(String siteKey, String userId) {
		
		File dfFilePath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ "OrientDB" + File.separator  + File.separator + "Tanium"
				+ File.separator + siteKey + File.separator);
		String pvDataDfFilePath = dfFilePath + "_custom_data" + ".json";
		
		try {
			String query = "select primary_key_value, json_object_agg(source_name, data::json) as data from ( " + 
					" select source_id, source_name, primary_key_value, data - 'sourceId' - 'siteKey' - 'User Name' - 'Server Name' as data from (  "+ 
					" select source_id, source_name, primary_key_value, data::jsonb || concat('{\"Last Updated Time\":\"', update_time, '\"}')::jsonb as data from ( " + 
					" select sd.source_id, s.source_name, primary_key_value, " + 
					" to_char(to_timestamp(update_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as update_time,  " + 
					" replace(data, '.0\"', '\"') as data,  " + 
					" row_number() over(partition by sd.source_id, primary_key_value order by update_time desc) as row_num from source_data sd   " + 
					" JOIN source s on s.source_id = sd.source_id and s.is_active = true and s.site_key = '" + siteKey + "'  " + 
					" where sd.site_key = '" + siteKey + "' " + 
					" ) b  " + 
					" where row_num = 1 " + 
					" ) c " + 
					" ) e " + 
					" group by primary_key_value";
			
		
			JSONArray jsonArray = new JSONArray();
			List<Map<String, Object>> dataList = reportDao.getListOfMapByQuery(query);
			
			for(Map<String, Object> data : dataList) {
				
			
				
				if(data.containsKey("primary_key_value")) {
					JSONObject customData = new JSONObject();
					customData.put("primary_key_value", (String) data.get("primary_key_value"));
					JSONObject dataObj = mapper.readValue(data.get("data").toString(), JSONObject.class);
						Set<String> keySet = dataObj.keySet();
						String srcName = keySet.iterator().next();
						customData.put("sourceName", srcName);
						customData.putAll((Map) dataObj.get(srcName));					
					jsonArray.add(customData);
				}
			}
				 
		
		
			
			mapper.writeValue(new File(pvDataDfFilePath), jsonArray);
			
			String privilageTempView = (siteKey+"_custom_data").replaceAll("-", "");
			
			createDataframeFromJsonFile(privilageTempView, pvDataDfFilePath);
	        

		} catch (Exception exp) {
			logger.error("Not able to create dataframe {}", exp.getMessage(), exp);
		}
		
		
	}

	
	
	
	

	
	//------------------------ Tanium Report------------------------------------------------//
	
	
	
	//------------------------Write dataframe to excel start-------------------------------------//
	
	public String writeDfToCsv(ServerSideGetRowsRequest request) {
		String outputFilePath = "";
		System.out.println("----request------" + request.getAnalyticstype());
		
		try {
			String siteKey = request.getSiteKey();
			
			String componentName = "";
			if(request.getOstype() != null && !request.getOstype().isEmpty()) { //server
				componentName = request.getOstype();
			} else if(request.getSwitchtype() != null && !request.getSwitchtype().isEmpty()) { //switch
				componentName = request.getSwitchtype();
			} else if(request.getStorage() != null && !request.getStorage().isEmpty()) { //Storage
				componentName = request.getStorage();
			} else if(request.getThirdPartyId() != null && !request.getThirdPartyId().isEmpty()) { //Project
				componentName = request.getThirdPartyId();
			} else if(request.getProviders() != null && !request.getProviders().isEmpty()) { //Providers
				componentName = request.getProviders();
			} else if(request.getProject() != null && !request.getProject().isEmpty()) { //Project
				componentName = request.getProject();
			}
			
			
			String viewNameWithHypen = siteKey + "_" + request.getAnalyticstype().toLowerCase() + "_"
					+ request.getCategory() + "_" + componentName + "_" + request.getReportList() + "_"
					+ request.getReportBy();
			String viewName = viewNameWithHypen.replaceAll("-", "").replaceAll("\\s+", "");		
			
			
			if(request.getReportBy().equalsIgnoreCase("server")) {
				viewName = (siteKey + "_linux").toLowerCase().replaceAll("-", "").replaceAll("\\s+", "");
			}
			
			
			File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
					+ "exportDF" + File.separator + siteKey + File.separator + componentName + File.separator );
			
			if(!verifyDataframeParentPath.exists()) {
				verifyDataframeParentPath.mkdirs();
			}
			
			System.out.println("-------write Path -------" + viewName + " :: " + verifyDataframeParentPath + " : "  );
			
			Dataset<Row> dataset = null;
			
			
			if(!componentName.toLowerCase().contains("tanium")) {  
				boolean isDiscoveryDataInView = false;	
				try {
					dataset = sparkSession.sql("select * from global_temp." + viewName);		
					isDiscoveryDataInView = true;
				} catch (Exception e) {
					System.out.println("---------View Not exists--------");
				}

				System.out.println("------isDiscoveryDataInView-------" + isDiscoveryDataInView);	

			} else { //tanium logic
				dataset = getTaniumReport(siteKey);		
			}
			
			rowGroups = request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
			groupKeys = request.getGroupKeys();
			valueColumns = request.getValueCols();
			pivotColumns = request.getPivotCols();
			filterModel = request.getFilterModel();
			sortModel = request.getSortModel();
			isPivotMode = request.isPivotMode();
			isGrouping = rowGroups.size() > groupKeys.size();

			rowGroups = formatInputColumnNames(rowGroups);
			groupKeys = formatInputColumnNames(groupKeys);
			sortModel = formatSortModel(sortModel);
				
			dataset = orderBy(groupBy(filter(dataset)));	
			
			String writePath = verifyDataframeParentPath + File.separator + viewName+ "_export";
			dataset.coalesce(1).write().mode("overwrite").option("header",true).option("sep","|").option("lineSep","\n")	       
	        .csv(writePath);
			
			System.out.println("--------writePath---------- " + writePath);
			
			String filePath = getCsvPath(writePath);
			System.out.println("--------filePathfilePath---------- " + filePath);
			return csvToExcel(filePath, writePath, viewName);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	
	private String getCsvPath(String writePath) {
		File folder = new File(writePath);
		String csvPath = ""; 
		try {
			for (final File fileEntry : folder.listFiles()) {
		            if(fileEntry.getName().endsWith(".csv")){
		            	return fileEntry.getAbsolutePath();
		            }  
		       
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return csvPath;
	}

	private String  csvToExcel(String csvPath, String csvParentPath, String viewName) {
		try {
			
			System.out.println("--------csvPath---------- " + csvPath);
			System.out.println("--------csvParentPath---------- " + csvParentPath);
			System.out.println("--------viewName---------- " + viewName);
			
			//open input file
			BufferedReader br = new BufferedReader(new FileReader(csvPath));
			//create sheet
			XSSFWorkbook wb = new XSSFWorkbook();
			XSSFSheet sheet = wb.createSheet();
			//read from file
			String line = br.readLine();
			for (int rows=0; line != null; rows++) {
			    //create one row per line
			    XSSFRow row = sheet.createRow(rows);
			    //split by semicolon
			    String[] items = line.split("\\|");
			    //ignore first item
			    
			    for (int i=0, col=0; i<items.length; i++) {
			       try {
			    	   String item = items[i];
				        Cell cell = row.createCell(col++);
				        //set item
				        cell.setCellValue(item.replaceAll("\"", ""));
				} catch (Exception e) {
					// TODO: handle exception
				}
			      
			    }
			    //read next line
			    line = br.readLine();
			}
			//write to xlsx
			File parentPath = new File(csvParentPath);
			String xlsxPath = parentPath.getParentFile().getAbsolutePath()+File.separator+viewName+".xlsx";
			System.out.println("-----------xlsxPath------------ " + xlsxPath);
			FileOutputStream out = new FileOutputStream(xlsxPath);
			wb.write(out);
			//close resources
			br.close();
			out.close();
			
			FileUtils.deleteDirectory(new File(parentPath.getAbsolutePath()));
			return xlsxPath;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
		
	}
	//------------------------Write dataframe to excel end-------------------------------------//
	
	
}
