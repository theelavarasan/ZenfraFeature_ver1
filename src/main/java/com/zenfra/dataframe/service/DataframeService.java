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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.RelationalGroupedDataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
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

import scala.collection.JavaConversions;
import scala.collection.JavaConverters;
import scala.collection.Seq;

@Repository
public class DataframeService {

	public static final Logger logger = LoggerFactory.getLogger(DataframeService.class);

	private List<String> rowGroups, groupKeys;
	private List<ColumnVO> valueColumns, pivotColumns;
	private List<SortModel> sortModel;
	private Map<String, ColumnFilter> filterModel;
	private boolean isGrouping, isPivotMode;

 
	private Map<String, List<String>> serverDiscoveryNumbericalColumns = new HashMap<String, List<String>>();

	 

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
	 
	private Dataset<Row> groupBy(Dataset<Row> df) {
		if (!isGrouping)
			return df;

		
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
			columnName = "`" + columnName + "`";

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

		Stream<String> columnFilters = filterModel.entrySet().stream().map(applyColumnFilters);

		Stream<String> groupToFilter = zip(groupKeys.stream(), rowGroups.stream(),
				(key, group) -> group + " = '" + key + "'");

		String filters = concat(columnFilters, groupToFilter).collect(joining(" AND "));

		return filters.isEmpty() ? df : df.filter(filters);
	}
	 

	private String formNumberQuery(String columnName, NumberColumnFilter filter) {
		Integer filterValue = filter.getFilter();
		String filerType = filter.getType();		 
		
		String filterQuery = formatNumberFilterType(columnName, filerType, filterValue, 0);
		
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
	 

	private BiFunction<String, NumberColumnFilter, String> numberFilter() {
		return (String columnName, NumberColumnFilter filter) -> {		
			
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

	// ---------------------SSRM Code-----------------------------------//
	
	
	//--------------------- Server report data frame creation start------------------------//

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

			Dataset<Row> siteKeDF = formattedDataframe.sqlContext()
					.sql("select distinct(site_key) from local_discovery");

			//List<String> siteKeys = siteKeDF.as(Encoders.STRING()).collectAsList();	
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
			
			File[] files = new File(path).listFiles();
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
	
	private void createDataframeGlobally(String path, File file) {
		String filePath = file.getAbsolutePath();

		if (filePath.endsWith(".json")) {
			String source_type = file.getParentFile().getName().replace("source_type=", "").trim();
			String siteKey = file.getParentFile().getParentFile().getName().replace("site_key=", "").trim();
			String dataframeFilePath = path + siteKey + File.separator + "site_key=" + siteKey + File.separator
					+ "source_type=" + source_type + File.separator + "*.json";
			String viewName = siteKey + "_" + source_type.toLowerCase();
			viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");
			prepareDataframe(source_type, siteKey, dataframeFilePath, viewName);

		}
	}

	private void prepareDataframe(String source_type, String siteKey, String dataframeFilePath, String viewName) {
		try {
			Dataset<Row> dataset = sparkSession.read().json(dataframeFilePath);
			dataset.createOrReplaceTempView("tmpView");
			Dataset<Row> filteredData = sparkSession.emptyDataFrame();
			
			System.out.println("----viewName-----" + viewName + " : " + dataframeFilePath);			

			String sql = " select ldView.*, eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`,eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`"
						+ " from tmpView ldView  left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(ldView.`Server Model`, ' ', '')) left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(ldView.`OS Version`) and lcase(eol.os_name)=lcase(ldView.`OS`) ";

			try {
				dataset = sparkSession.sql(sql);
				dataset.createOrReplaceTempView("datawithoutFilter");
				filteredData = sparkSession.sql(
						"select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from datawithoutFilter) ld where ld.rank=1 ");
			} catch (Exception e) {
				
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
			
			filteredData = addNonExistColumn(filteredData, "End Of Life - OS");
			filteredData = addNonExistColumn(filteredData, "End Of Extended Support - OS");
			filteredData = addNonExistColumn(filteredData, "End Of Life - HW");
			filteredData = addNonExistColumn(filteredData, "End Of Extended Support - HW");

			if(!Arrays.stream(filteredData.columns()).anyMatch(""::equals)) {
				
			}
			
			filteredData.createOrReplaceGlobalTempView(viewName);

			System.out.println("---------View created-------- :: " + viewName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//--------------------- Server report data frame creation end------------------------//
	
	private Dataset<Row> addNonExistColumn(Dataset<Row> filteredData, String colName) {
		try {
			if(!Arrays.stream(filteredData.columns()).anyMatch(colName::equals)) {
				filteredData = filteredData.withColumn(colName, lit(""));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return filteredData;
	}

	//------------------------getReportData API Start------------------------------------//
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

	

		boolean isDiscoveryDataInView = false;
		Dataset<Row> dataset = null;
		String viewName = siteKey + "_" + source_type.toLowerCase();
		viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");
		System.out.println("---------viewName------" + viewName);
		
		try {
			dataset = sparkSession.sql("select * from global_temp." + viewName);			
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
			} else {
				createDataframeOnTheFly(siteKey, source_type);
				dataset = sparkSession.sql("select * from global_temp." + viewName);				
			}
		}

	
		//type cast numeric columns in dataframe
		
		
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
		
		List<String> numericColumns = getReportNumericalHeaders(request.getReportType(), source_type, request.getReportBy(),request.getSiteKey());
		List<String> dataframeColumns  = Arrays.asList(dataset.columns()); 
		countData = getDataframeNumericColAgg(dataset, viewName, numericColumns, dataframeColumns);	
		countData.show();
	
		return paginate(dataset, request, countData.toJSON().collectAsList());

	}

	private Dataset<Row> getDataframeNumericColAgg(Dataset<Row> dataset, String viewName, List<String> numericColumns, List<String> dataframeColumns) {
		Dataset<Row> countData = sparkSession.emptyDataFrame();
		try {
		
			if(numericColumns != null && !numericColumns.isEmpty()) {
				dataset.createOrReplaceGlobalTempView(viewName+"_tmpReport");
				numericColumns.retainAll(dataframeColumns);
			
				
				String numericCol = String.join(",", numericColumns
			            .stream()
			            .map(col -> ("sum(`" + col + "`) as `"+col+"`"))
			            .collect(Collectors.toList()));	
				
			
				if(numericCol != null && !numericCol.trim().isEmpty()) {
					countData = sparkSession.sqlContext().sql("select "+numericCol+"  from global_temp."+viewName+"_tmpReport");//.sqlContext().sql("select `Total Size` group by `Total Size`").groupBy(new Column("`Total Size`""));
					
				}
				 
				 
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return countData;
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
			sm.setColId(sm.getColId());  //.replaceAll("\\s+", "_").toLowerCase()
			sortModel.add(sm);
		}
		return sortModel;
	}

	private List<String> formatInputColumnNames(List<String> list) {
		List<String> tmpList = new ArrayList<>();
		if (list != null && !list.isEmpty()) {
			for (String l : list) {
				tmpList.add(l);  //.replaceAll("\\s+", "_").toLowerCase()
			}
		}
		return tmpList;
	}
	
	private void createSingleDataframe(String siteKey, String source_type, String filePath) {
		try {
			
			String dataframeFilePath = filePath + File.separator + "*.json";
			String viewName = siteKey + "_" + source_type.toLowerCase();
			viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");
			prepareDataframe(source_type, siteKey, dataframeFilePath, viewName);

			System.out.println("--------single-View created-------- :: " + viewName);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
	}

	
	
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
		
		Dataset<Row> localDiscoveryDF = sparkSession.sql(
				"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"
						+ siteKey + "' and LOWER(source_type)='" + sourceType + "'");

		Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");

		try {
			Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
					"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"
							+ siteKey + "' and LOWER(source_type)='" + sourceType + "'");

			String filePathSrc = commonPath + File.separator + "Dataframe" + File.separator + "tmp" + File.separator + siteKey
					+ File.separator + "site_key=" + siteKey + File.separator + "source_type=" + sourceType
					+ File.separator;
			File f = new File(filePathSrc);
			if (!f.exists()) {
				f.mkdir();
			}

		

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
					String query = "select distinct(column_name) from report_capacity_columns where lower(device_type)= '"
							+ deviceType.toLowerCase() + "' and is_size_metrics = '1'";

					resultMap = favouriteDao_v2.getJsonarray(query);

				} else if (reportName.equalsIgnoreCase("optimization_All") || reportName.contains("optimization")) {
					String query = "select distinct(column_name) from report_columns where lower(report_name) = 'optimization' and lower(device_type) = 'all'  and is_size_metrics = '1'";

					resultMap = favouriteDao_v2.getJsonarray(query);

				} else {
					String query = "select distinct(column_name) from report_columns where lower(report_name) = '"
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

	

	public List<Map<String, Object>> getCloudCostDataPostgresFn(ServerSideGetRowsRequest request) {
		
		List<Map<String, Object>> cloudCostData = getCloudCostDataFromPostgres(request);
		
		
		return cloudCostData;
		
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
					"	cast((case when logical_processor_count = '' then null else logical_processor_count end) as int) As \"Logical Processor Count\",\r\n" + 
					"    memory As \"Memory\",\r\n" + 
					"    cast((case when number_of_cores = '' then null else number_of_cores end) as int) As \"Number of Cores\",\r\n" + 
					"   	cast((case when number_of_ports = '' then null else number_of_ports end) as int) As \"Number of Ports\",\r\n" + 
					"	cast((case when number_of_processors = '' then null else number_of_processors end) as int) As \"Number of Processors\",\r\n" + 
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

	

	
	public JSONObject getMigrationReport(String filePath) throws IOException, ParseException {
		
		try {
			if (filePath.contains(",")) {
				filePath = filePath.split(",")[0];
			}
				 
			 JSONObject jsonObject = mapper.readValue(new File(filePath), JSONObject.class);

			 return jsonObject; 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return new JSONObject(); 
   
 
	}

	public void createDataframeForJsonData(String filePath) {
		if (filePath.contains(",")) {
			filePath = filePath.split(",")[0];
		}
		try {
			if(filePath.contains("VMAX_Local_Disk-SAN")) {
				reprocessVmaxDiskSanData(filePath);
			}	 
			
			try {
				File f = new File(filePath);
				
				Dataset<Row> dataset = sparkSession.read().option("multiline", true).option("nullValue", "")
						.option("mode", "PERMISSIVE").json(filePath);
			
				String viewName = f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
				dataset.createOrReplaceGlobalTempView(viewName);
				
			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}
				  

		} catch (Exception e) {
			e.printStackTrace();			
		}

	}
	
private void reprocessVmaxDiskSanData(String filePath) {
	try {

		String dataPath = filePath.replace(".json", "_data.json");

		JSONObject jsonObject = mapper.readValue(new File(filePath), JSONObject.class);
		List<Map<String, Object>> dataArray = (List<Map<String, Object>>) jsonObject.get("data");
		mapper.writeValue(new File(dataPath), dataArray);

		try {
			Path level = Paths.get(filePath).getParent().getParent();
			UserPrincipal owner = level.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName(ZKConstants.ZENFRA_USER_GROUP_NAME);
			Files.setOwner(level, owner);

		} catch (Exception e) {
			// TODO: handle exception
		}
		File f = new File(dataPath);
		Dataset<Row> datasetA = sparkSession.read().option("nullValue", "").json(f.getAbsolutePath());
		String viewName = f.getName().split("_")[0].replaceAll("-", "") + "vmax_disk_san";

		datasetA.createOrReplaceGlobalTempView(viewName);
		Dataset<Row> result = sparkSession.sqlContext().sql("select " + "a.`Local Device ID`, "
				+ "a.`Local Serial Number`, " + "a.`Local Device Configuration`, " + "a.`Local Device Capacity`, "
				+ "a.`Local Device WWN`, " + "a.`Local Device Status`, " + "a.`Local Host Access Mode`, "
				+ "a.`Local Clone Source Device (SRC)`, " + "a.`Local Clone Target Device (TGT)`, "
				+ "a.`Local BCV Device Name`, " + "a.`Local BCV Device Status`, " + "a.`Local BCV State of Pair`, "
				+ "a.`Local Storage Group`, " + "a.`Local Masking View`, " + "a.`Local Initiator Group`, "
				+ "a.`Local Initiator Name`, " + "a.`Local Initiator WWN`, " + "a.`Local Possible Server Name`, "
				+ "a.`Local FA Port`," + "a.`Local FA Port WWN`,  " + "b.`Local Device ID` as `Remote Device Name`,"
				+ "b.`Local Serial Number` as `Remote Target ID`,"
				+ "b.`Local Device Configuration` as `Remote Device Configuration`,"
				+ "b.`Local Device Capacity` as `Remote Device Capacity`,"
				+ "b.`Local Device WWN` as `Remote Device WWN`," + "b.`Local Device Status` as `Remote Device Status`,"
				+ "b.`Local Host Access Mode` as `Remote Host Access Mode`,"
				+ "b.`Local Clone Source Device (SRC)` as `Remote Clone Source Device (SRC)`,"
				+ "b.`Local Clone Target Device (TGT)` as `Remote Clone Target Device (TGT)`,"
				+ "b.`Local BCV Device Name` as `Remote BCV Device Name`,"
				+ "b.`Local BCV Device Status` as `Remote BCV Device Status`,"
				+ "b.`Local BCV State of Pair` as `Remote BCV State of Pair`,"
				+ "b.`Local Storage Group` as `Remote Storage Group`,"
				+ "b.`Local Masking View` as `Remote Masking View`,"
				+ "b.`Local Initiator Group` as `Remote Initiator Group`,"
				+ "b.`Local Initiator Name` as `Remote Initiator Name`,"
				+ "b.`Local Initiator WWN` as `Remote Initiator WWN`,"
				+ "b.`Local Possible Server Name` as `Remote Possible Server Name`,"
				+ "b.`Local FA Port` as `Remote FA Port`," + "b.`Local FA Port WWN` as `Remote FA Port WWN` "
				+ "from global_temp." + viewName + " a  " + "left join global_temp." + viewName
				+ " b on a.`Remote Device Name` = b.`Local Device ID` and a.`Remote Target ID` = b.`Local Serial Number`");

		result.createOrReplaceGlobalTempView(viewName);

		jsonObject.put("data", parser.parse(result.toJSON().collectAsList().toString()));

		try (JsonGenerator jGenerator = mapper.getFactory().createGenerator(new File(filePath), JsonEncoding.UTF8)) {

			jGenerator.writeObject(jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Path level = Paths.get(filePath);
			UserPrincipal owner = level.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName(ZKConstants.ZENFRA_USER_GROUP_NAME);
			Files.setOwner(level, owner);

		} catch (Exception e) {
			// TODO: handle exception
		}

		System.out.println("-----------VMAX Disk SAN report completed--------");
	} catch (Exception e) {
		e.printStackTrace();
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
		
		componentName = componentName.toLowerCase();
		String viewNameWithHypen = siteKey + "_" + request.getAnalyticstype().toLowerCase() + "_"
				+ request.getCategory() + "_" + componentName + "_" + request.getReportList() + "_"
				+ request.getReportBy();
		String viewName = viewNameWithHypen.replaceAll("-", "").replaceAll("\\s+", "");
		
	
		
		File verifyDataframePath = new File(commonPath + File.separator + "Dataframe" + File.separator  + siteKey + File.separator + componentName
				+ File.separator + viewNameWithHypen + ".json");
		
	
		
		System.out.println("------verifyDataframePath-------" + verifyDataframePath);
		
		File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ siteKey + File.separator + componentName + File.separator );
		
		
		
		Dataset<Row> dataset = null;
		
		
		if(!componentName.toLowerCase().contains("tanium")) {  
			boolean isDiscoveryDataInView = false;	
			try {
				if(verifyDataframePath.exists()) {
					dataset = sparkSession.sql("select * from global_temp." + viewName);		
					isDiscoveryDataInView = true;
				}
				
			} catch (Exception e) {
				System.out.println("---------View Not exists--------");
			}

			if (!isDiscoveryDataInView) {			
				
				if (verifyDataframePath.exists()) {
					
					createDataframeFromJsonFile(viewName, verifyDataframePath.getAbsolutePath());
					dataset = sparkSession.sql("select * from global_temp." + viewName);   //we need apply filter order pagination start and end 
				
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
		
		 setFileOwner(verifyDataframePath);		
		 
		List<String> numericColumns = getReportNumericalHeaders(request.getReportType(), componentName, request.getReportBy(),request.getSiteKey());
		List<String> dataframeColumns  = Arrays.asList(dataset.columns()); 
		
		//type cast to numeric columns
		try {			
			dataset = typeCastNumericColumns(dataset, numericColumns, viewName, dataframeColumns);
		} catch (Exception e) {
			e.printStackTrace();
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
		
		
		Dataset<Row> countData = getDataframeNumericColAgg(dataset, viewName, numericColumns, dataframeColumns);	
	
		return paginate(dataset, request, countData.toJSON().collectAsList());

	
	 
	}
	
	
 

	 

	
	private Dataset<Row> typeCastNumericColumns(Dataset<Row> dataset, List<String> numericColumns, String viewName, List<String> dataframeColumns) {
		try {
			
			System.out.println("-----numericColumns-------- " + numericColumns);			
             // have to find way to type cast without iteration.... following code take some time and memory for type cast
			
			for (String numericColumn : numericColumns) {	
				if(dataframeColumns.contains(numericColumn)) {
					dataset = dataset.withColumn(numericColumn, new Column(numericColumn).cast("double"));
				}
					
				
			}
 
		
		/*	
		 * List<Column> numericCol = new ArrayList<>();
			List<Column> colNames = new ArrayList<>();
		 * 
		 * dataset = dataset.select(JavaConversions.asScalaBuffer(colNames));
		 */
			
			/*Seq<String> seqColumnNames = DataframeUtil.convertListToSeq(numericColumns);
			Seq<Column> numericColumnsSeq = JavaConverters.collectionAsScalaIterableConverter(numericCol).asScala().toStream().map(f, bf)
					.asScala().toSeq();			
		 
			
		 
			dataset.withColumns(seqColumnNames, numericColumnsSeq).
			 
         */
			
			//dataset = dataset.select(dataset.col)
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataset;
	}

	private Dataset<Row> getTaniumReport(String siteKey) {
		
		Dataset<Row> taniumDataset = sparkSession.emptyDataFrame();
		
		try {
			File taniumDataframFile = new File(commonPath + File.separator + "Dataframe" + File.separator
					+ "Tanium"  + siteKey + File.separator );
			
			File customDataframFile = new File(commonPath + File.separator + "Dataframe" + File.separator
					+ "Tanium"
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
       String uri = protocol + "://" + appServerIp + ":" + port + "/ZenfraV2/rest/reports/getReportData/migrationreport";
    	//String uri = "https://uat.zenfra.co/ZenfraV2/rest/reports/getReportData/migrationreport";
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
        
        
         if(resultObj.get("data") != null && !resultObj.get("data").toString().equalsIgnoreCase("null")) {
        	  mapper.writeValue(filePath, resultObj.get("data")); 
        	  setFileOwner(filePath);
         }		  
		 System.out.println("-----------------Write DF PAth----------" + filePath.getAbsolutePath());
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
		
	}
	
	
	private void setFileOwner(File filePath) {
		try {
			
			Path resultFilePath = Paths.get(filePath.getAbsolutePath());
			UserPrincipal owner = resultFilePath.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName("zenuser");
			Files.setOwner(Paths.get(filePath.getAbsolutePath()), owner);
			
			if(filePath.getParentFile() != null) {
				setFileOwner(filePath.getParentFile());
			}
			
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
				
				
				if(reportCategory.equalsIgnoreCase("server") && reportBy.equalsIgnoreCase("server")) { //dataframe created from postgres db				
					recreateLocalDiscovery(siteKey, sourceType);	
					//write server_server dataframe into common path /opt/ZENfra/Dataframe/siteKey/{logType}/jsonFile
					writeServerDataframeToCommonPath(siteKey, sourceType);
					
				} else if(reportCategory.equalsIgnoreCase("server") || reportCategory.equalsIgnoreCase("switch") 
						|| reportCategory.equalsIgnoreCase("Storage")) { //dataframe created from V2 repo /migrationReport API call... mostly report created from orient DB
					ServerSideGetRowsRequest request = new ServerSideGetRowsRequest();
						if(reportCategory.equalsIgnoreCase("server")) { //server
							request.setOstype(deviceType);
						} else if(reportCategory.equalsIgnoreCase("switch")) { //switch
							request.setSwitchtype(deviceType);
						} else if(reportCategory.equalsIgnoreCase("Storage")) { //Storage
							request.setStorage(deviceType);
						} 					
						request.setAnalyticstype("Discovery");
						request.setReportCategory("migration");
						request.setSiteKey(siteKey);
						request.setCategory(reportCategory);						
						request.setAnalyticstype("Discovery");
						request.setReportList(reportList);
						request.setReportBy(reportBy);
						request.setReportType("discovery");						
						
						deviceType = deviceType.toLowerCase();
						
						String viewNameWithHypen = siteKey + "_" + request.getAnalyticstype().toLowerCase() + "_"
								+ request.getCategory() + "_" + deviceType + "_" + request.getReportList() + "_"
								+ request.getReportBy();						 
						
						File verifyDataframePath = new File(commonPath + File.separator + "Dataframe" + File.separator
								+ siteKey + File.separator + deviceType
								+ File.separator + viewNameWithHypen + ".json");
						
						File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
								+ siteKey + File.separator + deviceType + File.separator );
						
						createDataframeFromOdb(request, verifyDataframePath, verifyDataframeParentPath);
						
				 }
			
			}
		}
		
	}

	
	
	

	private void writeServerDataframeToCommonPath(String siteKey, String sourceType) {
		String srcDirPath =  commonPath  + "Dataframe" + File.separator + "tmp" + File.separator + siteKey
		+ File.separator + "site_key=" + siteKey + File.separator + "source_type=" + sourceType.toLowerCase()
		+ File.separator;
		
		String analyticBy = "discovery";
		String category = "Server";
		String reportList = "Local";
		String reportBy = "Server";
		
		String destDirPath = commonPath + File.separator + "Dataframe" +  File.separator + siteKey + File.separator + sourceType + File.separator + siteKey + "_" + analyticBy + "_" + category + "_" + sourceType + "_" + reportList + "_" + reportBy + ".json";
		System.out.println("srcDirPath :: " + srcDirPath);
		System.out.println("destDirPath :: " + destDirPath);
		try {
			Optional<Path> path = Files.walk(Paths.get(srcDirPath))
			        .filter(Files::isRegularFile)
			        .filter(p -> p.toFile().getName().contains(".json"))
			        .findFirst();
			if(path.isPresent()) {
				Path filePath = path.get();
				
				System.out.println("-------filePath tttt--------- " + filePath );
				
				FileUtils.copyFile(filePath.toFile(), new File(destDirPath));
				
				setFileOwner(new File(destDirPath));
				
			}
		} catch (IOException e) {			
			e.printStackTrace();
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
				 + request.getSiteKey() + File.separator + "Tanium"
				+ File.separator + viewNameWithHypen + ".json");
		
		File verifyDataframePath = new File(commonPath + File.separator + "Dataframe" + File.separator
				 + request.getSiteKey() + File.separator + "Tanium"
				+ File.separator + viewNameWithHypen + ".json");
		
		File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
				+ request.getSiteKey() + File.separator + "Tanium" + File.separator );
		
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
				+ "Tanium"
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
				+ "Tanium"
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
			String userId = request.getUserId();
			
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
				viewName = (siteKey + "_" + componentName).toLowerCase().replaceAll("-", "").replaceAll("\\s+", "");
			}
			
			
			File verifyDataframeParentPath = new File(commonPath + File.separator + "Dataframe" + File.separator
					+ "exportDF" + File.separator + siteKey + File.separator + componentName + File.separator );
			
			if(!verifyDataframeParentPath.exists()) {
				verifyDataframeParentPath.mkdirs();
			}
			
			System.out.println("-------write Path -------" + viewName + " :: " + verifyDataframeParentPath + " : "  );
			
			Dataset<Row> dataset = null;
			
			
			
			String reportName = request.getReportList() + "_" + componentName + "_by_" + request.getReportBy();
			
            String exportType = request.getExportType();
			
						
			JSONArray reportColumns = reportDao.getReportHeader(request.getReportType(), componentName, request.getReportBy());
			List<String> reportCols = new ArrayList<String>();
			for(int i=0; i<reportColumns.size(); i++) {
				JSONObject colObj = (JSONObject) reportColumns.get(i);
				String colName = (String) colObj.get("displayName");
				reportCols.add(colName);
			}
			         
			 
			
			String columnsToExport = String.join(",", reportCols
		            .stream()
		            .map(col -> ("`" + col + "`"))
		            .collect(Collectors.toList()));
			
			System.out.println("-------columnsToExport-------" + exportType + " : " + columnsToExport);;
			if(exportType.equalsIgnoreCase("ARVC") || exportType.equalsIgnoreCase("VRVC")) {
				JSONObject reportUserCustom =  reportDao.getReportUserCustomData(userId, siteKey, reportName);
				
				if(reportUserCustom.containsKey("columnOrder")) {
					JSONArray visibleColumns = (JSONArray) reportUserCustom.get("columnOrder");
					if(visibleColumns != null && !visibleColumns.isEmpty()) {
						columnsToExport =  String.join(",", ((List<String>) visibleColumns.stream().map(json -> json.toString()).collect(Collectors.toList()))
					            .stream()
					            .map(col -> ("`" + col + "`"))
					            .collect(Collectors.toList()));
					}
				}
			}
			
			
			System.out.println("-------columnsToExport----final---" + componentName + " : " + viewName + " : "+ columnsToExport);;
			
			if(!componentName.toLowerCase().contains("tanium")) { 
				try {
					dataset = sparkSession.sql("select "+columnsToExport+" from global_temp." + viewName);		
				
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("---------View Not exists--------");
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
			
			//filter rows for VRAC VRAC
			if(exportType.equalsIgnoreCase("VRAC") || exportType.equalsIgnoreCase("VRAC")) {
				int startRow = request.getStartRow();
				int endRow = request.getEndRow();
				StructType schema = dataset.schema();			
				JavaPairRDD<Row, Long> zippedRows = dataset.toJavaRDD().zipWithIndex();
				JavaRDD<Row> filteredRdd = zippedRows.filter(pair -> pair._2 >= startRow && pair._2 <= endRow)
						.map(pair -> pair._1);
				dataset = sparkSession.sqlContext().createDataFrame(filteredRdd, schema).toDF();
			}
			
			String writePath = verifyDataframeParentPath + File.separator + viewName+ "_export";
			dataset.coalesce(1).write().mode("overwrite").option("header",true).option("sep","|").option("lineSep","\n")	       
	        .csv(writePath);
			
			setFileOwner(new File(writePath));
			String filePath = getCsvPath(writePath);			
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
			
			setFileOwner(new File(xlsxPath));
			
			FileUtils.deleteDirectory(new File(parentPath.getAbsolutePath()));
			return xlsxPath;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
		
	}
	//------------------------Write dataframe to excel end-------------------------------------//

	public JSONObject prepareChart(String siteKey, String component, String reportList, String reportBy, String xaxis,
			String yaxis, String chartType) {
		JSONObject jsonObject = new JSONObject();
		try {
			File f = new File("C:\\opt\\ZENfra\\Dataframe\\DF\\ddccdf5f-674f-40e6-9d05-52ab36b10d0e\\site_key=ddccdf5f-674f-40e6-9d05-52ab36b10d0e\\source_type=linux\\");
			 Dataset<Row> dataset = sparkSession.read().json(f.getPath() + File.separator + "*.json"); 
			dataset.createOrReplaceGlobalTempView("kkk");
			
			/*Dataset<Row> dataset = sparkSession.emptyDataFrame();
			String viewName = siteKey+"_"+component+"_"+reportList+"_"+reportBy;
			viewName = viewName.toLowerCase().replaceAll("-", "").replaceAll("\\s+", "");		
			*/
			dataset = sparkSession.sqlContext().sql("select `"+xaxis+"`, `"+yaxis+"` from global_temp.kkk");
			   StructType structure = dataset.schema();
			   StructField[] sf =  structure.fields();
			   DataType xaxisCol = sf[0].dataType();
			   DataType yaxisCol = sf[1].dataType();
			   
			   jsonObject.put("xaxisField", sf[0].name());
			   jsonObject.put("yaxisField", sf[1].name());
			   
			   if(xaxisCol.typeName().equalsIgnoreCase("string")) {
				   
			   }
			   
			   System.out.println("----------- " + xaxisCol + " : " + yaxisCol + " : "+ sf[0].name() +  " : "  +sf[1].name());
		 
			System.out.println("chart :: " + dataset);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		
		
		return null;
	}
	
	
	

	public JSONArray getVmaxSubreport(String filePath, String serverName, String sid) {
		JSONArray resultArray = new JSONArray();
		ObjectMapper mapper = new ObjectMapper();
		JSONParser parser = new JSONParser();
		 File f = new File(filePath);	
	
		 Dataset<Row> subReportData  = sparkSession.emptyDataFrame();
		try {			 
			 String viewName = f.getName().split("_")[0].replaceAll("-", "")+"vmax_disk_san";
			  subReportData = sparkSession.sqlContext().sql("select * from global_temp."+viewName+" where lower(`Local Possible Server Name`) like '%"+serverName.toLowerCase()+"%' and `Local Serial Number`='"+sid+"' and lower(`Local Device Configuration`) like 'rdf%'").toDF();
			  System.out.println("-----------getVmaxSubreport----view exists-----" ); 
		} catch (Exception e) { //view not present
			  System.out.println("-----------getVmaxSubreport----view NOT exists-----" ); 
			 createDataframeForJsonData(filePath);			
			 
			 String viewName = f.getName().split("_")[0].replaceAll("-", "")+"vmax_disk_san";
			  subReportData = sparkSession.sqlContext().sql("select * from global_temp."+viewName+" where lower(`Local Possible Server Name`) like '%"+serverName.toLowerCase()+"%' and `Local Serial Number`='"+sid+"'  and lower(`Local Device Configuration`) like 'rdf%'").toDF();
		}
		try {
			
			  System.out.println("-------serverName--- :: " +serverName + " :: sid :: " + sid); 
			  
			  
			  resultArray =  (JSONArray) parser.parse(subReportData.toJSON().collectAsList().toString());	
			  System.out.println("----------VmaxSubreport size----" + resultArray.size()); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
	}

	public JSONArray getReportHeaderFromData(String siteKey, String category, String reportList, String componentName,
			String reportBy, String analyticsType) {
		JSONArray columnArray = new JSONArray();
		try {
			
			String dataframePath = commonPath + File.separator + "Dataframe" + File.separator
					+ siteKey + "_" + analyticsType + "_"
					+ category + "_" + componentName + "_" + reportList + "_" + reportBy + ".json";
			
			File f = new File(dataframePath);
			
			Dataset<Row> dataset = sparkSession.read().option("multiline", true).option("nullValue", "")
					.option("mode", "PERMISSIVE").json(dataframePath);
		
			String viewName = f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
			dataset.createOrReplaceGlobalTempView(viewName);
			
			String[] dfColumnArray = dataset.columns();
			for(int i=0; i<dfColumnArray.length; i++) {
				JSONObject columnObj = new JSONObject();				
				columnObj.put("actualName", dfColumnArray[i]);
				columnObj.put("displayName", dfColumnArray[i]);
				columnObj.put("dataType", dfColumnArray[i]);
				columnObj.put("lockPinned", false);
				columnObj.put("lockPosition", false);
				columnObj.put("pinned", "");				
				columnArray.add(columnObj);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return columnArray;
	}

	public void prepareDsrReport(String siteKey, String sourceType) {
		try {
			
			String protocol = ZKModel.getProperty(ZKConstants.APP_SERVER_PROTOCOL);
	    	String appServerIp = ZKModel.getProperty(ZKConstants.APP_SERVER_IP);
	    	String port = ZKModel.getProperty(ZKConstants.APP_SERVER_PORT);
	        String uri = protocol + "://" + appServerIp + ":" + port + "/ZenfraV2/rest/reports/prepareSubreportData?siteKey="+siteKey+"&logType="+sourceType;
	    	
	        uri = CommonUtils.checkPortNumberForWildCardCertificate(uri);
	      
	        Map<String, String> map =  new HashMap<String, String>();
	        map.put("siteKey", siteKey);
	        map.put("logType", sourceType);
	       	  Map<String, Object> body= new LinkedHashMap<>();
		    body.putAll(map); 
		   
		    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
		    		builder.build(map);
		    System.out.println(builder.buildAndExpand(map).toUri());
			  
		 RestTemplate restTemplate = new RestTemplate();
		
		 HttpEntity<Object> httpRequest = new HttpEntity<>(body);
		 ResponseEntity<String> restResult = restTemplate.exchange(builder.buildAndExpand(map).toUri() , HttpMethod.POST,
		    		httpRequest, String.class);
		String dsrPath = commonPath +"Dataframe" + File.separator + siteKey + File.separator + sourceType + File.separator;
		
		 File filesList[] = new File(dsrPath).listFiles();
	      System.out.println("List of files and directories in the specified directory:");
	      for(File file : filesList) {
	    	  if(file.getAbsolutePath().contains("_dsr_")) {
	    		  Dataset<Row> dataset = sparkSession.read().option("multiline", true).option("nullValue", "")
							.option("mode", "PERMISSIVE").json(file.getAbsolutePath());
				
					String viewName = file.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
					System.out.println("--------DSR View -------- " + viewName);
					dataset.createOrReplaceGlobalTempView(viewName);
					dataset.printSchema();
					dataset.show();
		    	  setFileOwner(file.getAbsoluteFile());
	    	  }
	    	
	      }
		   
		///// ResponseEntity<String> restResult = restTemplate.exchange(uri, HttpMethod.POST, httpRequest, String.class);
		
		  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
			
		  
		
	}

	public JSONArray getDsrData(String dsrReportName, String siteKey, String serverName, String deviceType) {
		JSONArray resultArray = new JSONArray();
		dsrReportName = siteKey+"_dsr_"+dsrReportName.replaceAll("~", "").replaceAll("\\$", "");
		try {
			Dataset<Row> dsrData = sparkSession.sql("select * from global.temp"+dsrReportName+" where lower(`Server Name`)="+serverName.toLowerCase());
			resultArray =  (JSONArray) parser.parse(dsrData.toJSON().collectAsList().toString());	
		} catch (Exception e) {
			String dsrPath = commonPath +"Dataframe" + File.separator + siteKey + File.separator + deviceType.toLowerCase() + File.separator + dsrReportName+".json";
			System.out.println("----------------dsrPath---------- " + dsrPath);
			File file = new File(dsrPath);
			  Dataset<Row> dataset = sparkSession.read().option("multiline", true).option("nullValue", "")
						.option("mode", "PERMISSIVE").json(file.getAbsolutePath());
			
				dataset.createOrReplaceGlobalTempView(dsrReportName);
				
				Dataset<Row> dsrData = sparkSession.sql("select * from global.temp"+dsrReportName+" where lower(`Server Name`)="+serverName.toLowerCase());
				resultArray =  (JSONArray) parser.parse(dsrData.toJSON().collectAsList().toString());
		}
		return resultArray;
	}
	
}
