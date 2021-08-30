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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Sets;
import com.zenfra.configuration.AwsInventoryPostgresConnection;
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
import com.zenfra.model.ZKConstants;
import com.zenfra.utils.DBUtils;


@Repository
public class DataframeService{
	
	
	
	public static final Logger logger = LoggerFactory.getLogger(DataframeService.class);

	private List<String> rowGroups, groupKeys;
	private List<ColumnVO> valueColumns, pivotColumns;
	private List<SortModel> sortModel;
	private Map<String, ColumnFilter> filterModel;
	private boolean isGrouping, isPivotMode;
	
	private List<String> actualColumnNames = null;
	private List<String> renamedColumnNames = null;

	private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	JSONParser parser = new JSONParser();

	 @Autowired
	 SparkSession sparkSession;
	
	 //@Value("${db.url}")
	// private String dbUrl;
	 
	 @Value("${zenfra.path}")
	 private String commonPath;
	 
	 @Value("${zenfra.permisssion}")
	 private String fileOwnerGroupName;
	 
	 @Autowired
	 EolService eolService;
	 
	 
	 @Autowired
	 private ReportDao reportDao;
	 
	 @Autowired
	 private FavouriteDao_v2 favouriteDao_v2;
	 
	
	 
	
	 
	 private String dbUrl = DBUtils.getPostgres().get("dbUrl");
	
	 //---------------------SSRM Code-----------------------------------//


	    public static Dataset<Row> union(final Dataset<Row> ds1, final Dataset<Row> ds2) {
	    	  Set<String> ds1Cols = Sets.newHashSet(ds1.columns());
	    	  Set<String> ds2Cols = Sets.newHashSet(ds2.columns());
	    	  final Set<String> total = Sets.newHashSet(ds1Cols);
	    	  total.addAll(ds2Cols);
	    	  return ds1.select(expr(ds1Cols, total)).union(ds2.select(expr(ds2Cols, total)));
	    	}
	    
	    
	    
	    private static Column[] expr(final Set<String> cols, final Set<String> allCols) {
	        return allCols.stream()
	                .map(x -> {
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
	                dataset = dataset
	                        .withColumn(e.name(),
	                        		functions.lit(null));
	                dataset = dataset.withColumn(e.name(),
	                        dataset.col(e.name()).cast(Optional.ofNullable(e.dataType()).orElse(DataTypes.StringType)));
	            }
	        }
	        return dataset;
	    }
	    
	    private String selectSql() {
	        if (!isGrouping) return "select *";

	        Stream<String> groupCols = rowGroups.stream()
	                .limit(groupKeys.size() + 1);

	        Stream<String> valCols = valueColumns.stream()
	                .map(ColumnVO::getField);

	        Stream<String> pivotCols = isPivotMode ?
	                pivotColumns.stream().map(ColumnVO::getField) : Stream.empty();

	        return "select " + concat(groupCols, concat(pivotCols, valCols)).collect(joining(","));
	    }

	    private Dataset<Row> groupBy(Dataset<Row> df) {
	        if (!isGrouping) return df;

	        Column[] groups = rowGroups.stream()
	                .limit(groupKeys.size() + 1)
	                .map(functions::col)
	                .toArray(Column[]::new);

	        return agg(pivot(df.groupBy(groups)));
	    }

	    private RelationalGroupedDataset pivot(RelationalGroupedDataset groupedDf) {
	        if (!isPivotMode) return groupedDf;

	        // spark sql only supports a single pivot column
	        Optional<String> pivotColumn = pivotColumns.stream()
	                .map(ColumnVO::getField)
	                .findFirst();

	        return pivotColumn.map(groupedDf::pivot).orElse(groupedDf);
	    }

	    private Dataset<Row> agg(RelationalGroupedDataset groupedDf) {
	        if (valueColumns.isEmpty()) return groupedDf.count();

	        Column[] aggCols = valueColumns
	                .stream()
	                .map(ColumnVO::getField)
	                .map(field -> sum(field).alias(field))
	                .toArray(Column[]::new);

	        return groupedDf.agg(aggCols[0], copyOfRange(aggCols, 1, aggCols.length));
	    }

	    private Dataset<Row> orderBy(Dataset<Row> df) {
	        Stream<String> groupCols = rowGroups.stream()
	                .limit(groupKeys.size() + 1);

	        Stream<String> valCols = valueColumns.stream()
	                .map(ColumnVO::getField);

	        List<String> allCols = concat(groupCols, valCols).collect(toList());

	        Column[] cols = sortModel.stream()
	                .map(model -> Pair.of(model.getColId(), model.getSort().equals("asc")))
	                .filter(p -> !isGrouping || allCols.contains(p.getKey()))
	                .map(p -> p.getValue() ? col(p.getKey()).asc() : col(p.getKey()).desc())
	                .toArray(Column[]::new);

	        return df.orderBy(cols);
	    }

	  /*  private Dataset<Row> filter(Dataset<Row> df) {
	        Function<Map.Entry<String, ColumnFilter>, String> applyColumnFilters = entry -> {
	            String columnName = entry.getKey();
	            ColumnFilter filter = entry.getValue();

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
	        
	        
	        Stream<String> columnFilters = filterModel.entrySet()
	                .stream()
	                .map(applyColumnFilters);

	        Stream<String> groupToFilter = zip(groupKeys.stream(), rowGroups.stream(),
	                (key, group) -> group + " = '" + key + "'");

	        String filters = concat(columnFilters, groupToFilter)
	                .collect(joining(" AND "));

	        return filters.isEmpty() ? df : df.filter(filters);
	    } */
	    

	    private Dataset<Row> filter(Dataset<Row> df, String viewName) {	    	
		    
			  Function<Map.Entry<String, ColumnFilter>, String> applyColumnFilters = entry -> {
	            String columnName = entry.getKey();
	            ColumnFilter filter = entry.getValue();
	            
	            if (filter instanceof SetColumnFilter) {
	                return setFilter().apply(columnName, (SetColumnFilter) filter);
	            }

	            if (filter instanceof NumberColumnFilter) {
	            
	                return numberFilter().apply(columnName, (NumberColumnFilter) filter);
	            }	        
	            
	          /* if (filter instanceof TextColumnFilter) {	  
	        	 
	        	 return formTextQuery(columnName, (TextColumnFilter) filter); //textFilter().apply(columnName, (TextColumnFilter) filter);
	        	  
	            } */
	            
	            return "";
	        };	
	       
	        
	        String finalTextFilterQueryTemp =  "";
	        boolean customQuery = false;	     
	        
	        if(filterModel.toString().contains("condition1")) { // custom query

		        for(Map.Entry<String, ColumnFilter> entry : filterModel.entrySet()) {
		        	 String columnName = entry.getKey();
		        	 columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
			         ColumnFilter filter = entry.getValue();
			         
			         if (filter instanceof NumberColumnFilter) {
			        	 customQuery = true;
			        	
		 	             df.select(col(columnName).cast("int").as(columnName));
		 	          
		        		 String str =  formNumberQuery(columnName, (NumberColumnFilter) filter);
		        		 
		        		 if(finalTextFilterQueryTemp.isEmpty()) {
		        			 finalTextFilterQueryTemp =  str;
		        		 } else {
		        			 finalTextFilterQueryTemp =  finalTextFilterQueryTemp +  " AND " + str;	  	        			
		        		 }
			         } 
			         
			         if (filter instanceof TextColumnFilter) {	  
			        	 customQuery = true;
			        	 String str =  formTextQuery(columnName, (TextColumnFilter) filter);
		        		 
		        		 if(finalTextFilterQueryTemp.isEmpty()) {
		        			 finalTextFilterQueryTemp =  str;
		        		 } else {
		        			 finalTextFilterQueryTemp =  finalTextFilterQueryTemp +  " AND " + str;	  	        			
		        		 }
			        	
			        	  
			            }
			         
			        	
		        }
	        } else if (filterModel.toString().contains("defaultFilter")) { // && filterModel.toString().contains("text")
	        	  for(Map.Entry<String, ColumnFilter> entry : filterModel.entrySet()) {
	        		  String columnName = entry.getKey();
			          columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
			     	  columnName = "lower("+""+columnName+"" +")";
			     	  
				      ColumnFilter filter = entry.getValue();
		        	 if (filter instanceof TextColumnFilter) {
			        	 customQuery = true;
			        	 TextColumnFilter textColumnFilter =  (TextColumnFilter) filter;
			        	
			        	 String expression = textColumnFilter.getType();
			        	 String filterText = textColumnFilter.getFilter();
		        		 
			        	 finalTextFilterQueryTemp =  formatTextFilterType(columnName, expression, filterText);
			        	  
			            }
	        	  }
	        }
	       
	        
	        if(customQuery) { 	           
	             Dataset<Row> filteredData =  df.sqlContext().sql("select * from "+viewName+" where " + finalTextFilterQueryTemp);
	           
	             return filteredData;	  	   
	        } else {
	        	
	        	Stream<String> columnFilters = filterModel.entrySet()
		                .stream()
		                .map(applyColumnFilters);  	        
		        

		        Stream<String> groupToFilter = zip(groupKeys.stream(), rowGroups.stream(),
		                (key, group) -> group + " = '" + key + "'");

		        String filters = concat(columnFilters, groupToFilter)
		                .collect(joining(" AND "));		  	    
		    
		     return filters.isEmpty() ? df : df.filter(filters);
	        }
	    }

	    private String formNumberQuery(String columnName, NumberColumnFilter filter) {
	    	   columnName = "`"+columnName+"`";
	    		columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
	    	  JSONObject condition1 =  filter.getCondition1();
	    		JSONObject condition2 =  filter.getCondition2();
	    		String operator =  filter.getOperator();
	    		
	    		String condition1Type = (String) condition1.get("type");
	    		int condition1Filter =  (Integer) condition1.get("filter");
	    		int condition1FilterTo = (Integer) condition1.get("filterTo");
	    		
	    		String condition2Type = (String) condition2.get("type");
	    		int condition2Filter =  (Integer) condition2.get("filter");
	    		int condition2FilterTo = (Integer) condition2.get("filterTo");
	    		
	    		String query1 = formatNumberFilterType(columnName, condition1Type, condition1Filter, condition1FilterTo);
	    		String  query2 = formatNumberFilterType(columnName, condition2Type, condition2Filter, condition2FilterTo);
	    		String filterQuery = "( " + query1 + " " + operator + " " + query2 + " )";
	    		return filterQuery;
	    	} 


	    	private String formTextQuery(String columnName, TextColumnFilter filter) {
	    		columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
	    		 columnName = "lower(`"+columnName+"`)";
	    		
	    		JSONObject condition1 =  filter.getCondition1();
	    		JSONObject condition2 =  filter.getCondition2();
	    		String operator =  filter.getOperator();
	    		
	    		String condition1Type = (String) condition1.get("type");
	    		String condition1Filter = (String) condition1.get("filter");
	    		
	    		String condition2Type = (String) condition2.get("type");
	    		String condition2Filter = (String) condition2.get("filter");
	    		
	    		String query1 = formatTextFilterType(columnName, condition1Type, condition1Filter);
	    		String  query2 = formatTextFilterType(columnName, condition2Type, condition2Filter);
	    		
	    		
	    		String filterQuery = "( " + query1 + " " + operator + " " + query2 + " )";

	    		
	    		return filterQuery;
	    	}


	    	private String formatTextFilterType(String columnName, String expression, String filterText) {
	    		String query = "";
	    		if(expression.equals("contains")) {	    		
	    			query = columnName + " like lower('%" + filterText + "%'"+")";
	    		} else if(expression.equalsIgnoreCase("startsWith")) {	    		
	    			query = columnName + " like lower('" + filterText + "%'"+")";
	    		} else if(expression.equalsIgnoreCase("endsWith")) {	    		
	    			query = columnName + " like lower('%" + filterText + "'"+")";
	    		}  else if(expression.equalsIgnoreCase("equals")) {	    		
	    			query = columnName + "=lower('" + filterText + "'"+")";
	    		} else if(expression.equalsIgnoreCase("notEqual")) {	    		
	    			query = columnName + "!=lower('" + filterText + "'"+")";
	    		} 
	    		
	    		return query;
	    	}


	    	private String formatNumberFilterType(String columnName, String expression, int filter, int filterTo) {
	    		String query = "";
	    		if(expression.equalsIgnoreCase("equals")) {	    		
	    			query = columnName + "=" + filter;
	    		} else if(expression.equalsIgnoreCase("notEqual")) {	    		
	    			query = columnName + "!=" + filter;
	    		} else if(expression.equalsIgnoreCase("lessThan")) {	    		
	    			query = columnName + "<" + filter ;
	    		} else if(expression.equalsIgnoreCase("lessThanOrEqual")) {	    		
	    			query = columnName + "<=" + filter;
	    		} else if(expression.equalsIgnoreCase("greaterThan")) {	    		
	    			query = columnName + ">" + filter;
	    		} else if(expression.equalsIgnoreCase("greaterThanOrEqual")) {	    		
	    			query = columnName + ">=" + filter;
	    		} else if(expression.equalsIgnoreCase("inRange")) {	    		
	    			query = columnName + "BETWEEN " + filter + " AND " + filterTo;
	    		} 
	    		
	    		return query;
	    	}


	    private BiFunction<String, SetColumnFilter, String> setFilter() {
	        return (String columnName, SetColumnFilter filter) ->
	                columnName + (filter.getValues().isEmpty() ? " IN ('') " : " IN " + asString(filter.getValues()));
	    }
	    
	 /*   private BiFunction<String, TextColumnFilter, String> textFilter() {	    	
	        return (String columnName, TextColumnFilter filter) -> textFilterInput(columnName, filter.getCondition1(), filter.getCondition2(), filter.getOperator());
	    } */

	   /* private String textFilterInput(String columnName, JSONObject condition1, JSONObject condition2, String operator) {
	    	 
        
        
        	String query = columnName +"=\"" +condition1.get("filter").toString()+"\"" + " " + operator + " " +  columnName +"=\"" +condition2.get("filter").toString()+"\"";
        	System.out.println("============query=====================================" + query);
        	return query;
		} */


		private BiFunction<String, NumberColumnFilter, String> numberFilter() {
	        return (String columnName, NumberColumnFilter filter) -> {
	            Integer filterValue = filter.getFilter();
	            String filerType = filter.getType();
	            String operator = operatorMap.get(filerType);

	            return columnName + (filerType.equals("inRange") ?
	                    " BETWEEN " + filterValue + " AND " + filter.getFilterTo() : " " + operator + " " + filterValue);
	        };
	    } 

	    private DataResult paginate(Dataset<Row> df, ServerSideGetRowsRequest request) {
	    	
	    	int startRow = request.getStartRow();
	    	int endRow = request.getEndRow();
	    	
	        // save schema to recreate data frame
	        StructType schema = df.schema();

	        // obtain row count
	        long rowCount = df.count();

	        // convert data frame to RDD and introduce a row index so we can filter results by range
	        JavaPairRDD<Row, Long> zippedRows = df.toJavaRDD().zipWithIndex();

	        // filter rows by row index using the requested range (startRow, endRow), this ensures we don't run out of memory
	        JavaRDD<Row> filteredRdd =
	                zippedRows.filter(pair -> pair._2 >= startRow && pair._2 <= endRow).map(pair -> pair._1);

	        // collect paginated results into a list of json objects
	        List<String> paginatedResults = sparkSession.sqlContext()
	                .createDataFrame(filteredRdd, schema)
	                .toJSON()
	                .collectAsList();

	        // calculate last row
	        long lastRow = endRow >= rowCount ? rowCount : -1;	        
	       
	        
	        return new DataResult(paginatedResults, lastRow, getSecondaryColumns(df), df.count());
	    }

	    private List<String> getSecondaryColumns(Dataset<Row> df) {
	        return stream(df.schema().fieldNames())
	                .filter(f -> !rowGroups.contains(f)) // filter out group fields
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
			    		if(textFilterJson.containsKey("condition1") && textFilterJson.containsKey("condition2") && textFilterJson.containsKey("operator")) { // multiple filter condition
			    			JSONObject cond1 = (JSONObject) textFilterJson.get("condition1");
			    			JSONObject cond2 = (JSONObject) textFilterJson.get("condition2");
			    			
			    			query = cond1.get("filter").toString() + " " + textFilterJson.get("operator") + " " + cond2.get("filter").toString();
			    			
			    			
			    		} else {
			    			query = "'%"+textFilterJson.get("filter")+"%'";
			    		}
	    		
	    		
		    	}
			} catch (Exception e) {
				e.printStackTrace();
			}
	    	

	    	System.out.println("-------------Query--------------------" + query);
	    
	    	
	        return query;
	    }

	    private static Map<String, String> operatorMap = new HashMap<String, String>() {{
	        put("equals", "=");
	        put("notEqual", "<>");
	        put("lessThan", "<");
	        put("lessThanOrEqual", "<=");
	        put("greaterThan", ">");
	        put("greaterThanOrEqual", ">=");
	    }};

	 //---------------------SSRM Code-----------------------------------//
	
	public String createDataframeForLocalDiscovery(String tableName) {   		
	
		logger.info("create dataframe for local discovery table" );		
		try {
			String path = commonPath + File.separator + "LocalDiscoveryDF" + File.separator;
		
			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", tableName);
			
			@SuppressWarnings("deprecation")
			Dataset<Row> localDiscoveryDF = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			
			Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");
			formattedDataframe.createOrReplaceTempView("local_discovery");

			Dataset<Row> siteKeDF = formattedDataframe.sqlContext().sql("select distinct(site_key) from local_discovery");
			List<String> siteKeys = siteKeDF.as(Encoders.STRING()).collectAsList();

			//String DataframePath = dataframePath + File.separator;
			siteKeys.forEach(siteKey -> {
				try {
					Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
							"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"+ siteKey + "'");
					
					File f = new File(path + siteKey);
					if (!f.exists()) {
						f.mkdir();
					}
					
					dataframeBySiteKey.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true)
							.partitionBy("site_key", "source_type").format("org.apache.spark.sql.json")
							.mode(SaveMode.Overwrite).save(f.getPath());
					dataframeBySiteKey.unpersist();
				} catch (Exception e) {
					logger.error("Not able to create dataframe for local discovery table site key " + siteKey, e.getMessage(), e);
				}
			});

			// remove double quotes from json file
			File[] files = new File(path).listFiles();
			if (files != null) {
				DataframeUtil.formatJsonFile(files);
			}

			createDataframeGlobalView();
			
			//boolean fileOwnerChanged = DataframeUtil.changeOwnerForFile(fileOwnerGroupName);
			
			return ZKConstants.SUCCESS;
		} catch (Exception exp) {
			logger.error("Not able to create dataframe {}",  exp.getMessage(), exp);
		}
		
		return ZKConstants.ERROR;
	}
		
	
	
	 public DataResult getReportData(ServerSideGetRowsRequest request) {	    
		 
		 
		 String siteKey = request.getSiteKey();
         String source_type = request.getSourceType().toLowerCase();
        
       
 		if(source_type != null && !source_type.trim().isEmpty() && source_type.contains("hyper")) {
 			source_type = source_type + "-" + request.getReportBy().toLowerCase();
 		} else if(source_type != null && !source_type.trim().isEmpty() && (source_type.contains("vmware") && request.getReportBy().toLowerCase().contains("host"))) {
 			source_type = source_type + "-" + request.getReportBy().toLowerCase();
 		} else if(source_type != null && !source_type.trim().isEmpty() && (source_type.contains("nutanix") && request.getReportBy().toLowerCase().contains("host"))) {
 			source_type = source_type + "-" + request.getReportBy().toLowerCase();
 		} else if(source_type != null && !source_type.trim().isEmpty() && (source_type.contains("nutanix") && request.getReportBy().toLowerCase().equalsIgnoreCase("vm"))) {
 			source_type = source_type + "-" + "guest";
 		} 		
 		
 		
         
		 boolean isDiscoveryDataInView = false;
		 Dataset<Row> dataset = null;
		 String viewName = siteKey+"_"+source_type.toLowerCase();
		 viewName = viewName.replaceAll("-", "");
		 try {
			 dataset = sparkSession.sql("select * from global_temp."+viewName);
			 dataset.cache();
			 isDiscoveryDataInView = true;			
		} catch (Exception e) {
			System.out.println("---------View Not exists--------");
		} 
		 
		 try {	       
			 isDiscoveryDataInView = false;
	         if(!isDiscoveryDataInView) {
	        	 File verifyDataframePath = new File(commonPath + File.separator + "LocalDiscoveryDF" + File.separator + siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + source_type);
	        	 if(!verifyDataframePath.exists()) {
	        		 createDataframeOnTheFly(siteKey, source_type);
	        	 }
	        	 String filePath = commonPath + File.separator + "LocalDiscoveryDF" + File.separator + siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + source_type + File.separator + "*.json";
	        	 dataset = sparkSession.read().json(filePath); 	 
	        	 dataset.createOrReplaceTempView("tmpView");
	        	 dataset =  sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1 ");
	        	 dataset.createOrReplaceGlobalTempView(viewName); 
		         dataset.cache();
	         }		        
	         
	         //---------------------EOL EOS---------------------------//	     
	        
	        	 int osCount = eolService.getEOLEOSData();
	        	 int hwCount = eolService.getEOLEOSHW();
	        	 
	       
	                String hwJoin = "";
	                String hwdata = "";
	                String osJoin = "";
	                String osdata = "";	                
	                
	                
	        	if(osCount > 0) {	 
	        		 if(Arrays.stream(dataset.columns()).anyMatch("Server Type"::equals) && dataset.first().fieldIndex("Server Type") != -1) {
	        			 Dataset<Row> eolos = sparkSession.sql("select * from global_temp.eolDataDF where lower(os_type)='"+source_type+"'");  // where lower(`Server Name`)="+source_type
		        		 eolos.createOrReplaceTempView("eolos");
		        		 eolos.show();
		        		  
		        		 if(eolos.count() > 0) { 		        			
		        			 osJoin = " left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(ldView.`OS Version`) and lcase(eol.os_type)=lcase(ldView.`Server Type`) ";   // where lcase(eol.os_version)=lcase(ldView.`OS Version`) and lcase(eol.os_type)=lcase(ldView.`Server Type`)
		                     osdata = ",eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`";
			        		 		                     
		 	        		/*String eosQuery = "Select * from ( Select ldView.* ,eol.end_of_life_cycle as `End Of Life - OS` ,eol.end_of_extended_support as `End Of Extended Support - OS`  from global_temp."+viewName+" ldView left join eolos eol on lcase(eol.os_type)=lcase(ldView.actual_os_type) where lcase(eol.os_version)=lcase(ldView.`OS Version`) )";
		 	        		Dataset<Row> datasetTmp =  sparkSession.sql(eosQuery);
		 	        		 System.out.println("----------->>>>>>>>>>>>>>>>>>>>>>--------------" + datasetTmp.count());
		 	        		datasetTmp.show(); */
				        	
				         }
	        		 }
	        		 
	        	}
	        	
	        	 if(hwCount > 0) {
	        		 if(Arrays.stream(dataset.columns()).anyMatch("Server Model"::equals) && dataset.first().fieldIndex("Server Model") != -1) {
	        			 
	        			 hwJoin = " left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(ldView.`Server Model`, ' ', ''))";
	                     hwdata = ",eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`";	                  
	     	        	/*String hwModel =  dataset.first().getAs("Server Model");
	        		 Dataset<Row> eolhw = sparkSession.sql("select end_of_life_cycle as `End Of Life - HW`, end_of_extended_support as `End Of Extended Support - HW` from global_temp.eolHWDataDF where lower(concat(vendor,' ',model))='"+hwModel.toLowerCase()+"'");  // where lower(`Server Name`)="+source_type
		        	 if(eolhw.count() > 0) {		        	
			        	 dataset = dataset.join(eolhw);
			         } */
	        	 }
	        	 }
	        	
	        	//sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
	        	 
	        	 String sql = "select * from (" +
	                        " select ldView.*" +osdata + hwdata+
	                        " ,ROW_NUMBER() OVER (PARTITION BY ldView.`Server Name` ORDER BY ldView.`log_date` desc) as my_rank" +
	                        " from global_temp."+viewName+" ldView" + hwJoin + osJoin +
	                        " ) ld where ld.my_rank = 1";	        	  
	        	 
	        	 dataset = sparkSession.sql(sql).toDF(); 
	        
	        	 
	        	 if((osCount > 0 || hwCount > 0) && dataset.count() == 0) {
	        		  hwJoin = "";
		              hwdata = "";
		              osJoin = "";
		              osdata = "";
	        		 String sqlDf = "select * from (" +
		                        " select ldView.*" +osdata + hwdata+
		                        " ,ROW_NUMBER() OVER (PARTITION BY ldView.`Server Name` ORDER BY ldView.`log_date` desc) as my_rank" +
		                        " from global_temp."+viewName+" ldView" + hwJoin + osJoin +
		                        " ) ld where ld.my_rank = 1";
		        	 
		        	 dataset = sparkSession.sql(sqlDf).toDF(); 
	        	 }
	        	
	         
	         actualColumnNames = Arrays.asList(dataset.columns());	
	         Dataset<Row> renamedDataSet = renameDataFrame(dataset); 
	         renamedDataSet.createOrReplaceTempView(viewName+"renamedDataSet");
	        if(request.getEndRow() == 0) { // temp code
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
	       
	        rowGroups =  formatInputColumnNames(rowGroups);
	        groupKeys =  formatInputColumnNames(groupKeys);
	        sortModel =  formatSortModel(sortModel);

	        Dataset<Row> df = renamedDataSet.sqlContext().sql(selectSql() + " from "+viewName+"renamedDataSet"); 	        	
	        renamedColumnNames = Arrays.asList(df.columns());		       
	        	
	        Dataset<Row> results = orderBy(groupBy(filter(df, viewName+"renamedDataSet")));	
	        
	        results =  reassignColumnName(actualColumnNames, renamedColumnNames, results);	        
	        //results.printSchema();	 	
	        
	        results = results.dropDuplicates();	        

	        List<String> numericalHeaders = getReportNumericalHeaders("Discovery", request.getSourceType().toLowerCase(), "Discovery", siteKey);	    	
	    	
	    	List<String> columns = Arrays.asList(results.columns());
	    	
            for(String column : numericalHeaders) {                        	
            	if(columns.contains(column)) { 
            		results = results.withColumn(column, results.col(column).cast("float"));
            	}
            	
            }
	        
            if(source_type.equalsIgnoreCase("vmware-host")) { 
            	results = results.withColumn("Server Type", lit("vmware-host"));
            }

	       
	       /* List<String> headers = reportDao.getReportHeaderForFilter("discovery", source_type.toLowerCase(), request.getReportBy().toLowerCase());	  
	        List<String> actualHeadets = new ArrayList<>();
	        actualHeadets.addAll(Arrays.asList(results.columns()));	      
	        actualHeadets.removeAll(headers);	       
	        results =  results.drop(actualHeadets.stream().toArray(String[]::new)); */
	        return paginate(results, request);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured while fetching local discoverydata from DF{}", e.getMessage(), e);
		}
	         
		
		 return null;
	    } 	   
	 
	 public List<String> getReportNumericalHeaders(String reportName, String source_type, String reportBy, String siteKey) {
			// TODO Auto-generated method stub
			return reportDao.getReportNumericalHeaders(reportName, source_type, reportBy, siteKey);
		}

	 
private void createDataframeOnTheFly(String siteKey, String source_type) {
	try {
		source_type = source_type.toLowerCase();
		String path = commonPath + File.separator + "LocalDiscoveryDF" + File.separator;
		
		Map<String, String> options = new HashMap<String, String>();
		options.put("url", dbUrl);
		options.put("dbtable", "(select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"+ siteKey + "' and lower(source_type)='"+source_type+"') as foo");
		
		@SuppressWarnings("deprecation")
		Dataset<Row> localDiscoveryDF = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
		localDiscoveryDF.show();
		Dataset<Row> dataframeBySiteKey = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");
		
		
		File f = new File(path + siteKey);
		if (!f.exists()) {
			f.mkdir();
		}
		
		dataframeBySiteKey.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true)
				.partitionBy("site_key", "source_type").format("org.apache.spark.sql.json")
				.mode(SaveMode.Overwrite).save(f.getPath());
		
		 String viewName = siteKey+"_"+source_type.toLowerCase();
		 viewName = viewName.replaceAll("-", "");
		 
		// remove double quotes from json file
					File[] files = new File(path).listFiles();
					if (files != null) {
						DataframeUtil.formatJsonFile(files);
					}
		 
		 String filePath = commonPath + File.separator + "LocalDiscoveryDF" + File.separator + siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + source_type + File.separator + "*.json";
		 dataframeBySiteKey = sparkSession.read().json(filePath); 	 
		 dataframeBySiteKey.createOrReplaceTempView("tmpView");
		 dataframeBySiteKey =  sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1 ");
		 dataframeBySiteKey.createOrReplaceGlobalTempView(viewName); 
		 dataframeBySiteKey.cache();
		 System.out.println("------------dataframeBySiteKey-------------------" + dataframeBySiteKey.count());
         
	} catch (Exception e) {
		e.printStackTrace();
		logger.error("Not able to create dataframe for local discovery table site key " + siteKey, e.getMessage(), e);
	}
		
	}


	 
	 private List<SortModel> formatSortModel(List<SortModel> sortModels) {
		 List<SortModel> sortModel = new ArrayList<>();
		 for(SortModel sm : sortModels) {
			 sm.setColId(sm.getColId().replaceAll("\\s+", "_").toLowerCase());
			 sortModel.add(sm);
		 }
		return sortModel;
	}



	private List<String> formatInputColumnNames(List<String> list) {
			List<String> tmpList = new ArrayList<>();
	          if(list != null && !list.isEmpty()) {
	        	 for(String l : list) {
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
	    		renamedColumnNames.forEach(newName ->{
	    			if(actualNameTmp.equalsIgnoreCase(newName)) {
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
		         String filePath = commonPath +  File.separator + "LocalDiscoveryDF" + File.separator + siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + sourceType + File.separator;
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
		         
		         String viewName = siteKey+"_"+sourceType;
		   	       viewName = viewName.replaceAll("-", "");	
		   	       
		         if(siteKeyAndSourceType.exists()) {  // site key and source type present		        	
		        	
		        	 Path path = Paths.get(filePath);			         
			         List<String> filesExists = DataframeUtil.getAllFileNamesByType(path, ".json");				        	
			        	 
				         if(filesExists != null && filesExists.size()>0) { //append					        	
				        	  
				        	String tmpFile =  writeJosnFile(commonPath +  File.separator + "LocalDiscoveryDF" + File.separator + siteKey +  File.separator + "site_key="+siteKey + File.separator+"tmp.json", newJson.toJSONString());
				        					        	  
				        	if(!tmpFile.isEmpty()) {
				        		 File[] files =  new File[1];
				        		 files[0] = new File(tmpFile);
				        		 DataframeUtil.formatJsonFile(files);					        		
				        		 
				        		  try {		
				        			  
				        			  Dataset<Row> newDataframeToAppend =   sparkSession.read().json(tmpFile); 				        			  
				        			  newDataframeToAppend = newDataframeToAppend.drop("site_key").drop("source_type");	
				        			  newDataframeToAppend.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true).mode(SaveMode.Append).format("org.apache.spark.sql.json").save(filePath);
				        			  newDataframeToAppend.unpersist();
				        			  
				        			  System.out.println("-----------newDataframeToAppend------------------" + newDataframeToAppend.count());
				        			  
				        			  Dataset<Row> mergedDataframe = sparkSession.read().json(filePath);	
				        			  mergedDataframe.createOrReplaceTempView("tmpView");
				        			  //mergedDataframe.sqlContext().sql("select * from (select *, rank() over (partition by  source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1 ");
				        			  sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
				        			  mergedDataframe.createOrReplaceGlobalTempView(viewName); 
				        			  mergedDataframe.cache();
				        			  
				        			  sparkSession.sql("REFRESH TABLE global_temp."+viewName);				                 
				                 System.out.println("----------------Dataframe Append--------------------------------");	
				                 DataframeUtil.deleteFile(tmpFile);
				                 result = ZKConstants.SUCCESS;				                
				        	} catch(Exception e) {
				        		e.printStackTrace();
				        	}
				         }
				         }
		         } else {
		        	 //create template
		        	 
		        	
		        	 //check if source type exits		        	 
		        	  String sourceTypePath = commonPath +  File.separator + "LocalDiscoveryDF" + File.separator +  siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + sourceType.toLowerCase();
		        	
		        	  File newSiteKey = new File(commonPath +  File.separator + "LocalDiscoveryDF" + File.separator +  siteKey + File.separator);
		        	  boolean siteKeyPresent = true;
						if (!newSiteKey.exists()) {							
							siteKeyPresent = false;
						} 
						
						File sourceTypeFolder = new File(sourceTypePath);
						  
						if(siteKeyPresent && !sourceTypeFolder.exists() || !siteKeyPresent) { 
							sourceTypeFolder.mkdir();
							
							System.out.println("------------create new dataframe for new source type---------------- ");							 
							 String tmpFile =  writeJosnFile(newSiteKey.getAbsolutePath()  + "tmp.json", newJson.toString());	
							 File[] files =  new File[1];
			        		 files[0] = new File(tmpFile);
			        		 DataframeUtil.formatJsonFile(files);	       
			        		 
							 Dataset<Row> newDataframe = sparkSession.read().json(tmpFile);							
							 newDataframe = newDataframe.drop("site_key").drop("source_type");
							 
							 //String newFolderName = commonPath +  File.separator + "LocalDiscoveryDF" + File.separator +  siteKey +  File.separator + "site_key="+siteKey + File.separator;   
							 
							 newDataframe.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true)
								.format("org.apache.spark.sql.json")
								.mode(SaveMode.Overwrite).save(sourceTypePath);
							 newDataframe.unpersist();
							 
							  Dataset<Row> mergedDataframe = sparkSession.read().json(filePath);	
		        			  mergedDataframe.createOrReplaceTempView("tmpView");
		        			  //mergedDataframe.sqlContext().sql("select * from (select *, rank() over (partition by  source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1 ");
		        			  sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
		        			  mergedDataframe.createOrReplaceGlobalTempView(viewName); 
		        			  mergedDataframe.cache();		        			  
		        			  mergedDataframe.printSchema();
							
							 DataframeUtil.deleteFile(tmpFile);
							 System.out.println("---------new Dataframe created with new source type-------------- ");
							 result = ZKConstants.SUCCESS;
						} 		        	  
		         
		         }
		         
		        // boolean fileOwnerChanged = DataframeUtil.changeOwnerForFile(fileOwnerGroupName);
		        
			} catch (Exception e) {
				//logger.error("Exception occured when append dataframe {}", e.getMessage(), e);
				e.printStackTrace();
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
                if(f.exists()) {
                	result = f.getAbsolutePath();
                }
             } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
             }
			
			return result;
		}



		public void createDataframeGlobalView() {
			String path = commonPath + File.separator + "LocalDiscoveryDF" +  File.separator;
			File[] files = new File(path).listFiles();
		    if (files != null) {
		    	   createLocalDiscoveryView(files);
		    }
		    
		   
		}



		private void createLocalDiscoveryView(File[] files) {
			String path = commonPath + File.separator + "LocalDiscoveryDF" +  File.separator;
			  for (File file : files) {
			        if (file.isDirectory()) {
			        	createLocalDiscoveryView(file.listFiles());
			        } else {
			            String filePath =  file.getAbsolutePath();
			            if(filePath.endsWith(".json")) {			            	
			            	String source_type = file.getParentFile().getName().replace("source_type=", "").trim();
			            	String siteKey = file.getParentFile().getParentFile().getName().replace("site_key=", "").trim();
			   	         	String dataframeFilePath = path + siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + source_type + File.separator + "*.json";
			   	         	String viewName = siteKey+"_"+source_type.toLowerCase();
			   	         	viewName = viewName.replaceAll("-", "");
			   	       
			   	      try {
			   	    	 System.out.println("--------dataframeFilePath-------- :: " + viewName + " : " + dataframeFilePath);
			   	        	 Dataset<Row> dataset = sparkSession.read().json(dataframeFilePath); 
			   	        	 dataset.createOrReplaceTempView("tmpView");
			   	        	 sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
			   		         dataset.createOrReplaceGlobalTempView(viewName); 
			   		         dataset.cache();
			   		 	 System.out.println("---------View created-------- :: " + viewName);
		        		} catch (Exception e) {
		        			e.printStackTrace();
		        			 System.out.println("---------Not able to create View-------- :: " + viewName);
		        		}  
			            	
			            }
			        }
			    }
			
		}



		public JSONArray getReportHeader(String reportType, String deviceType, String reportBy) {
			JSONArray jSONArray = new JSONArray();			
			//jSONArray = postgresCompServiceImpl.getReportHeader(reportType, deviceType, reportBy);
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
				commonPath = commonPath + File.separator + "LocalDiscoveryDF" + File.separator;
				Map<String, String> options = new HashMap<String, String>();
				options.put("url", dbUrl);
				options.put("dbtable", tableName);
				
				@SuppressWarnings("deprecation")
				Dataset<Row> localDiscoveryDF = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
				
				Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");
				formattedDataframe.createOrReplaceTempView("local_discovery");

				Dataset<Row> siteKeDF = formattedDataframe.sqlContext().sql("select distinct(site_key) from local_discovery");
				List<String> siteKeys = siteKeDF.as(Encoders.STRING()).collectAsList();

				//String DataframePath = dataframePath + File.separator;
				siteKeys.forEach(siteKey -> {
					try {
						Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
								"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"+ siteKey + "'");
						File f = new File(commonPath +  File.separator + "LocalDiscoveryDF" + File.separator +  siteKey);
						if (!f.exists()) {
							f.mkdir();
						}
						dataframeBySiteKey.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true)
								.partitionBy("site_key", "source_type").format("org.apache.spark.sql.json")
								.mode(SaveMode.Overwrite).save(f.getPath());
						dataframeBySiteKey.unpersist();
					} catch (Exception e) {
						logger.error("Not able to create dataframe for local discovery table site key " + siteKey, e.getMessage(), e);
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
				logger.error("Not able to create dataframe {}",  exp.getMessage(), exp);
			}
			
			return ZKConstants.ERROR;
			
		}





		/*@SuppressWarnings("unchecked")
		public JSONObject getSubReportList(String deviceType, String reportName) throws IOException, ParseException {
			System.out.println("!!!!! deviceType: " + deviceType);
			if(deviceType.equalsIgnoreCase("HP-UX")) {
				deviceType = "hpux";
			}
			JSONParser parser = new JSONParser();
			
			Map<String, JSONArray> columnsMap = new LinkedHashMap<String, JSONArray>(); 
			JSONObject result = new JSONObject();

			String linkDevices = ZKModel.getProperty(ZKConstants.CRDevice);
			JSONArray devicesArray = (JSONArray) parser.parse(linkDevices);			
			if(reportName.trim().equalsIgnoreCase("discovery")) {
				String linkColumns = ZKModel.getProperty(ZKConstants.CRCOLUMNNAMES);
				JSONArray columnsArray = (JSONArray) parser.parse(linkColumns);
				
				for(int a = 0; a < devicesArray.size(); a++) {
					JSONArray columnsNameArray = new JSONArray();
					for(int i = 0; i < columnsArray.size(); i++) {
						JSONObject jsonObject = (JSONObject) columnsArray.get(i);
						if(jsonObject.containsKey(devicesArray.get(a).toString().toLowerCase())) {
							columnsNameArray = (JSONArray) parser.parse(jsonObject.get(devicesArray.get(a).toString().toLowerCase()).toString());
							columnsMap.put(devicesArray.get(a).toString().toLowerCase(), columnsNameArray);
						}
					}
				}
				
			} else if(reportName.trim().equalsIgnoreCase("compatibility")) {
				JSONArray columnsNameArray = new JSONArray();
				columnsNameArray.add("Host Name");
				for(int a = 0; a < devicesArray.size(); a++) {
					columnsMap.put(devicesArray.get(a).toString().toLowerCase(), columnsNameArray);
				}
				
			}
			
		
			if(!columnsMap.isEmpty()) {
				Map<String, Properties> propMap = new TreeMap<String, Properties>();
				if(deviceType.equalsIgnoreCase("all")) {
					for(int i = 0; i < devicesArray.size(); i++) {
						String deviceValue = "";
						if(devicesArray.get(i).toString().equalsIgnoreCase("HP-UX")) {
							deviceValue = "hpux";
						}
						String path = "/opt/config/" + deviceValue.toLowerCase() + "ServerClickReport.properties";
						System.out.println("!!!!! path: " + path);
						InputStream inputFile = null;
						
						try {
							
							File file = new File(path);
							if(file.exists()) {
								
								inputFile = new FileInputStream(file);
								Properties prop = new Properties();
								prop.load(inputFile);
								
								if(devicesArray.get(i).toString().equalsIgnoreCase("HP-UX")) {
									deviceValue = "hpux";
								}
								propMap.put(deviceValue.toLowerCase(), prop);
							}
							
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						
						
					}
				} else {
					String path = "/opt/config/" + deviceType.toLowerCase() + "ServerClickReport.properties";
					System.out.println("!!!!! path: " + path);
					InputStream inputFile = null;
					
					try {
						
						File file = new File(path);
						if(file.exists()) {
							
							inputFile = new FileInputStream(file);
							Properties prop = new Properties();
							prop.load(inputFile);
							propMap.put(deviceType.toLowerCase(), prop);
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				
				List<String> propKeys = new ArrayList<String>(propMap.keySet());
				System.out.println("!!!!! propKeys: " + propKeys);
				
				
				ZenfraJSONObject resultObject = new ZenfraJSONObject();
				
				JSONArray postDataColumnArray = new JSONArray();
				List<String> columnsKey = new ArrayList<String>(columnsMap.keySet());
				
				
				for(int i = 0; i < columnsKey.size(); i++) {
					JSONArray columnsNameArray = columnsMap.get(columnsKey.get(i));
					JSONObject tabInfoObject = new JSONObject();
					for(int j = 0; j < columnsNameArray.size(); j++) {
						if(!columnsNameArray.get(j).toString().equalsIgnoreCase("vCenter")) {
						ZenfraJSONObject tabArrayObject = new ZenfraJSONObject();
						for(int k = 0; k < propKeys.size(); k++) {
							Properties prop = propMap.get(propKeys.get(k));
							List<Object> tabKeys = new ArrayList<Object>(prop.keySet());
							JSONArray tabInnerArray = new JSONArray();
							
							for(int l = 0; l < tabKeys.size(); l++) {
								ZenfraJSONObject tabValueObject = new ZenfraJSONObject();
								String key = tabKeys.get(l).toString();
								String keyId = tabKeys.get(l).toString();
								//System.out.println("!!!!! key: " + key);
								String value = "";
								String keyName = "";
								String keyLabel = "";
								String keyView = "";
								String keyOrdered = "";
								if(key.contains("$")) {
									String[] keyArray = key.split("\\$");									
									value = keyArray[0];
									keyName = keyArray[0].replace("~", "");
									keyLabel = value.replace("~", " ");
									keyView = keyArray[1];
									keyOrdered = keyArray[2];
								} else {
									keyName = key.replace("~", "");
									keyLabel = key.replace("~", " ");
									keyView = "H";
									keyOrdered = "0";
								}
								
								tabValueObject.put("value", keyId);
								tabValueObject.put("name", keyName);
								tabValueObject.put("label", keyLabel);
								tabValueObject.put("view", keyView);
								tabValueObject.put("ordered", Integer.parseInt(keyOrdered));
								tabInnerArray.add(tabValueObject);
							}
							if(!tabInnerArray.isEmpty()) {
								tabArrayObject.put(propKeys.get(k), tabInnerArray);
							}
							
						}
						if(!tabArrayObject.isEmpty()) {
							tabInfoObject.put("tabInfo", tabArrayObject);
							tabInfoObject.put("tabInfo", tabArrayObject);
							tabInfoObject.put("skipValues", new JSONArray());
							tabInfoObject.put("title", "Detailed Report for Server (" + columnsNameArray.get(j) + ")");
							if(!postDataColumnArray.contains(columnsNameArray.get(j))) {
								if(deviceType.equalsIgnoreCase("vmware")) {
									postDataColumnArray.add("VM");
									postDataColumnArray.add("vCenter");
								} else if(deviceType.equalsIgnoreCase("vmware-host")) {
									postDataColumnArray.add("Server Name");
									postDataColumnArray.add("vCenter");
								} else {
									postDataColumnArray.add(columnsNameArray.get(j));
								}
								
							}
							resultObject.put(columnsNameArray.get(j), tabInfoObject);
							//resultObject.put("skipValues", new JSONArray());
							result.put("subLinkColumns", resultObject);
						}
					}
					}
					
				}
				
				
				
				result.put("postDataColumns", postDataColumnArray);
				result.put("deviceType", deviceType.toLowerCase().trim());
				JSONArray refferedDeviceType = new JSONArray();
				if(reportName.equalsIgnoreCase("compatibility")) {
					refferedDeviceType.add("OS Type");
				} else if(reportName.equalsIgnoreCase("taskList")) {
					refferedDeviceType.add("Source Type");
				}
				result.put("deviceTypeRefColumn", refferedDeviceType);
				
				
				
			}
			
			//System.out.println("!!!!! result: " + result);
			return result;
		} */
		
		
		public JSONArray getReportHeaderForMigrationMethod(String siteKey, String deviceType) {
			 JSONArray resultArray = new JSONArray();
			 
			 String viewName = siteKey+"_"+deviceType.toLowerCase();
			 viewName = viewName.replaceAll("-", "");
			 Dataset<Row> dataset = sparkSession.emptyDataFrame();
			 try {
				 dataset = sparkSession.sql("select * from global_temp."+viewName);
			} catch (Exception e) {
				 String filePath = commonPath +  File.separator + "LocalDiscoveryDF" + File.separator +  siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + deviceType + File.separator + "*.json";
	        	 dataset = sparkSession.read().json(filePath); 	 
	        	 dataset.createOrReplaceTempView("tmpView");
	        	 dataset =  sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1 ");
		         dataset.createOrReplaceGlobalTempView(viewName); 
			} 
			 List<String> header = Arrays.asList(dataset.columns());
			
			 if(header != null && !header.isEmpty()) {
				 int rowCount = 1;
				 for(String col : header) {
					JSONObject obj = new JSONObject();
					
					obj.put("displayName", col);
					obj.put("actualName", col);
					obj.put("dataType", "String");
					if(rowCount == 1) {
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
				if(sourceType.contains("hyper") || sourceType.contains("vmware") || sourceType.contains("nutanix")) {
					isMultipleSourceType = true;
				} 
				
				if(!isMultipleSourceType) {
					reinitiateDiscoveryDataframe(siteKey, sourceType);
				} else {
					if(sourceType.contains("hyper")) {
						reinitiateDiscoveryDataframe(siteKey, "hyper-v-host");
						reinitiateDiscoveryDataframe(siteKey, "hyper-v-vm");
					} else if(sourceType.contains("vmware")) {
						reinitiateDiscoveryDataframe(siteKey,"vmware");
						reinitiateDiscoveryDataframe(siteKey, "vmware-host");
					}  else if(sourceType.contains("nutanix")) {
						reinitiateDiscoveryDataframe(siteKey,"nutanix-guest");
						reinitiateDiscoveryDataframe(siteKey, "nutanix-host");
					} else {
						reinitiateDiscoveryDataframe(siteKey, sourceType);
					}
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}	
		

		private void reinitiateDiscoveryDataframe(String siteKey, String sourceType) {
			String path = commonPath + File.separator + "LocalDiscoveryDF" + File.separator;
			Dataset<Row> localDiscoveryDF = sparkSession.sql("select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"+ siteKey + "' and LOWER(source_type)='"+sourceType+"'");
			
			Dataset<Row> formattedDataframe = DataframeUtil.renameDataFrameColumn(localDiscoveryDF, "data_temp_", "");				
			
			try {
				Dataset<Row> dataframeBySiteKey = formattedDataframe.sqlContext().sql(
						"select source_id, data_temp, log_date, source_category, server_name as sever_name_col, site_key, LOWER(source_type) as source_type, actual_os_type  from local_discovery where site_key='"+ siteKey + "' and LOWER(source_type)='"+sourceType+"'");
				
				 String filePathSrc = commonPath +  File.separator + "LocalDiscoveryDF" + File.separator + siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + sourceType + File.separator;
				File f = new File(filePathSrc);
				if (!f.exists()) {
					f.mkdir();
				}
				
				dataframeBySiteKey.show();
				
				dataframeBySiteKey = dataframeBySiteKey.drop("site_key").drop("source_type");	
				 
				dataframeBySiteKey.write().option("escape", "").option("quotes", "").option("ignoreLeadingWhiteSpace", true)
						.format("org.apache.spark.sql.json")
						.mode(SaveMode.Overwrite).save(f.getPath());
				dataframeBySiteKey.unpersist();
				
				// remove double quotes from json file
				File[] files = new File(filePathSrc).listFiles();
				if (files != null) {
					DataframeUtil.formatJsonFile(files);
				}
				
				for(File file : files) {
					
		            String filePath =  file.getAbsolutePath();
		            if(filePath.endsWith(".json")) {			            	
		   	         	String dataframeFilePath = path + siteKey +  File.separator + "site_key="+siteKey + File.separator + "source_type=" + sourceType + File.separator + "*.json";
		   	         	String viewName = siteKey+"_"+sourceType.toLowerCase();
		   	         	viewName = viewName.replaceAll("-", "");
		   	       
		   	      try {			   	    	 
		   	        	 Dataset<Row> dataset = sparkSession.read().json(dataframeFilePath); 
		   	        	 dataset.createOrReplaceTempView("tmpView");
		   	        	 sparkSession.sql("select * from (select *, row_number() over (partition by source_id order by log_date desc) as rank from tmpView ) ld where ld.rank=1");
		   		         dataset.createOrReplaceGlobalTempView(viewName); 
		   		         dataset.cache();
		   		 	 System.out.println("---------View created-------- :: " + viewName);
	        		} catch (Exception e) {
	        			e.printStackTrace();
	        			 System.out.println("---------Not able to create View-------- :: " + viewName);
	        		}  
		            	
		            }
		        
				}
		}  catch (Exception e) {
			logger.error("Not able to create dataframe for local discovery table site key " + siteKey, e.getMessage(), e);
		}
 }
		
		
		 public JSONObject getUnitConvertDetails(String reportName, String deviceType) {
		        logger.info("GetUnitConvertDetails Begins");
		        JSONObject resultJSONObject = new JSONObject();
		        try {
		            JSONObject timeZoneMetricsObject = new JSONObject();
		            List<Map<String, Object>> resultMap = new ArrayList<>();
		            if(reportName != null && !reportName.isEmpty()) {
		            	 if (reportName.equalsIgnoreCase("capacity")) {
				                String query = "select column_name from report_capacity_columns where lower(device_type)= '"+ deviceType.toLowerCase() + "' and is_size_metrics = '1'";
				                
				                resultMap = favouriteDao_v2.getJsonarray(query);

				                
				            } else if (reportName.equalsIgnoreCase("optimization_All") || reportName.contains("optimization")) {
				            	  String query = "select column_name from report_columns where lower(report_name) = 'optimization' and lower(device_type) = 'all'  and is_size_metrics = '1'";
				                
				                resultMap = favouriteDao_v2.getJsonarray(query);

				                
				            } else {
				                String query = "select column_name from report_columns where lower(report_name) = '"+ reportName.toLowerCase() + "' and lower(device_type) = '" + deviceType.toLowerCase()
				                        + "' and is_size_metrics = '1'";

				                resultMap = favouriteDao_v2.getJsonarray(query);
				            }
				           
		            }
		           
		            JSONArray capacityMetricsColumns = new JSONArray();
		            JSONObject capacityMetricsColumnObject = new JSONObject();
		            for(Map<String, Object> list : resultMap) {
		            	for (Map.Entry<String,Object> entry : list.entrySet()) {			                                
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



		public DataResult getCloudCostData(ServerSideGetRowsRequest request) {
			 Dataset<Row> dataset = null;
			try {
				String siteKey = request.getSiteKey();
				String deviceType = request.getDeviceType();
				
				 String viewName = siteKey+"_"+deviceType.toLowerCase();
				 viewName = viewName.replaceAll("-", "")+"_opt";
				
				 boolean isDiscoveryDataInView = false;
				 try {
					 dataset = sparkSession.sql("select * from global_temp."+viewName);
					 dataset.cache();
					 isDiscoveryDataInView = true;			
				} catch (Exception e) {
					
				} 
				 
				 if(!isDiscoveryDataInView) {
					 Map<String, String> options = new HashMap<String, String>();
						options.put("url", dbUrl);						
						options.put("dbtable", "mview_aws_cost_report");						
						sparkSession.sqlContext().load("jdbc", options).registerTempTable("mview_aws_cost_report"); 
						dataset = sparkSession.sql("select * from mview_aws_cost_report where site_key='"+ siteKey + "'");
						dataset.createOrReplaceGlobalTempView(viewName); 
						dataset.cache();					
				 }
				 
				   
				 
			} catch (Exception e) {
				e.printStackTrace();
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
	       
	        rowGroups =  formatInputColumnNames(rowGroups);
	        groupKeys =  formatInputColumnNames(groupKeys);
	        sortModel =  formatSortModel(sortModel);
	        request.setStartRow(1);
	        request.setEndRow((int)dataset.count());
	        
	        dataset.show();
	        
	        return paginate(dataset, request);
		}



		public DataResult getOptimizationReport(ServerSideGetRowsRequest request) {
			 
			 String siteKey = request.getSiteKey();
	         String deviceType = request.getDeviceType();
	        	         
	         
	         String reportName = request.getReportType();
			 String deviceTypeHeder = "All";
			 String reportBy = request.getReportType();			
			 JSONArray headers = reportDao.getReportHeader(reportName, deviceTypeHeder, reportBy);
			 
			 //String [] categoryArray =  request.getCategory().replaceAll( "^\\[|\\]$", "").replaceAll("\"", "").trim().split( "," );
			// String [] sourceArray =  request.getSource().replaceAll( "^\\[|\\]$", "").replaceAll("\"", "").trim().split( "," );
			
			 
			 String discoveryFilterqry ="";
			
			   List<String> columnHeaders = new ArrayList<>();
			   List<String> numberColumnHeaders = new ArrayList<>();
			   if(headers != null && headers.size() > 0) {
				   for(Object o : headers){
					    if ( o instanceof JSONObject ) {
					    	String col = (String) ((JSONObject) o).get("actualName");
					    	String dataType = (String) ((JSONObject) o).get("dataType");
					    	if(dataType.equalsIgnoreCase("String")) {
					    		columnHeaders.add(col);
					    	} else {
					    		numberColumnHeaders.add(col);
					    	}
					    	
					    }
					}
			   }
			   
			   List<String> taskListServers = new ArrayList<>();
			 if(request.getProjectId() != null && !request.getProjectId().isEmpty()) {
				 List<Map<String, Object>> resultMap = favouriteDao_v2.getJsonarray("select server_name from tasklist where project_id='"+request.getProjectId()+"'");
				 if(resultMap != null && !resultMap.isEmpty()) {
					 for(Map<String, Object> map : resultMap) {
						 taskListServers.add((String) map.get("server_name"));
					 }
				 }
			 }
	        
           
             if (deviceType.equalsIgnoreCase("All")) {
            	 deviceType = " lcase(aws.`Server Type`) in ('windows','linux', 'vmware')";
            	 discoveryFilterqry = " lcase(source_type) in ('windows','linux', 'vmware')";
             } else {
            	 discoveryFilterqry = " lcase(source_type)='" + deviceType.toLowerCase() + "'";
            	 deviceType = "lcase(aws.`Server Type`)='" + deviceType.toLowerCase() + "'";
             }
             
             if(!taskListServers.isEmpty()) {
            	 String serverNames = String.join(",", taskListServers
 			            .stream()
 			            .map(name -> ("'" + name.toLowerCase() + "'"))
 			            .collect(Collectors.toList()));
            	 deviceType =  " lcase(aws.`Server Name`) in ("+serverNames+")";
            	 discoveryFilterqry = " lcase(server_name) in ("+serverNames+")";
             }
             
             System.out.println("----------------------deviceTypeCondition--------------------------" + deviceType);
             
             Dataset<Row> dataCheck = null;
          
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

                 String sql = "select * from (" +
                         " select " +
                         " ROW_NUMBER() OVER (PARTITION BY aws.`Server Name` ORDER BY aws.`log_date` desc) as my_rank," +
                         " lower(aws.`Server Name`) as `Server Name`, aws.`OS Name`, aws.`Server Type`, aws.`Server Model`," +
                         " aws.`Memory`, aws.`Total Size`, aws.`Number of Processors`, aws.`Logical Processor Count`, " +
                         " round(aws.`CPU GHz`,2) as `CPU GHz`, aws.`Processor Name`,aws.`Number of Cores`,aws.`DB Service`, " +
                         " aws.`HBA Speed`,aws.`Number of Ports`, aws.Host, " +
                         " round((round(aws.`AWS On Demand Price`,2) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS On Demand Price`," +
                         " round((round(aws.`AWS 3 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 3 Year Price`," +
                         " round((round(aws.`AWS 1 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 1 Year Price`," +
                         " aws.`AWS Instance Type`,aws.`AWS Region`,aws.`AWS Specs`," +
                         " round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`," +
                         " round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`," +
                         " round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`," +
                         " azure.`Azure Instance Type`,azure.`Azure Specs`," +
                         " google.`Google Instance Type`, " +
                         " google.`Google On Demand Price`," +
                         " google.`Google 1 Year Price`," +
                         " google.`Google 3 Year Price`," +
                         " aws.`OS Version`, eol.`End Of Life - OS`,eol.`End Of Extended Support - OS`" +
                         " " + hwdata + " " +
                         " from global_temp.awsReport aws " +
                         " left join global_temp.eoleosDataDF eol on eol.`Server Name`=aws.`Server Name` " +
                         " left join global_temp.azureReport azure on azure.`Server Name` = aws.`Server Name`" +
                         " left join global_temp.googleReport google on google.`Server Name` = aws.`Server Name` " +
                         " " + hwJoin + " " +
                         " where aws.site_key='" + siteKey + "' and "+ deviceType +" order by aws.`Server Name` asc) ld where ld.my_rank = 1";

                 if (dataCount == 0) {
                     sql = "select * from (" +
                             " select " +
                             " ROW_NUMBER() OVER (PARTITION BY aws.`Server Name` ORDER BY aws.`log_date` desc) as my_rank," +
                             "lower(aws.`Server Name`) as `Server Name`, aws.`OS Name`, aws.`Server Type`, aws.`Server Model`," +
                             " aws.`Memory`, aws.`Total Size`, aws.`Number of Processors`, aws.`Logical Processor Count`, " +
                             " round(aws.`CPU GHz`,2) as `CPU GHz`, aws.`Processor Name`,aws.`Number of Cores`,aws.`DB Service`, " +
                             " aws.`HBA Speed`,aws.`Number of Ports`, aws.Host," +
                             " round((round(aws.`AWS On Demand Price`,2) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS On Demand Price`," +
                             " round((round(aws.`AWS 3 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 3 Year Price`," +
                             " round((round(aws.`AWS 1 Year Price`) +((case when aws.`Total Size` >16384 then 16384 else aws.`Total Size` end)*0.10)),2) as `AWS 1 Year Price`," +
                             " aws.`AWS Instance Type`,aws.`AWS Region`,aws.`AWS Specs`," +
                             " round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`," +
                             " round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`," +
                             " round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`," +
                             " azure.`Azure Instance Type`,azure.`Azure Specs`," +
                             " google.`Google Instance Type`, " +
                             " google.`Google On Demand Price`," +
                             " google.`Google 1 Year Price`," +
                             " google.`Google 3 Year Price`," +
                             " aws.`OS Version`, '' as `End Of Life - OS`,'' as `End Of Extended Support - OS`" +
                             " " + hwdata + " " +
                             " from global_temp.awsReport aws " +
                             " left join global_temp.azureReport azure on azure.`Server Name` = aws.`Server Name`" +
                             " left join global_temp.googleReport google on google.`Server Name` = aws.`Server Name` " +
                             " " + hwJoin + " " +
                             " where aws.site_key='" + siteKey  + "' and "+ deviceType +" order by aws.`Server Name` asc) ld where ld.my_rank = 1";
                 }                    
                 
                 boolean isTaskListReport = false;
    			 if(!taskListServers.isEmpty()) {
    				 isTaskListReport = true;
    			 }
    			 List<String> categoryList = new ArrayList<>();			 
    			 List<String> sourceList = new ArrayList<>(); 
    			
    			 if(!isTaskListReport) {
    				 categoryList.add(request.getCategoryOpt());
    				 sourceList.add(request.getSource());
    			 }
    			 	 
    			 System.out.println("------categoryList---------- " + categoryList);
    			 System.out.println("------sourceList---------- " + sourceList);
    			 
    			 dataCheck = sparkSession.sql(sql).toDF();    
    			 List<String> colHeaders = Arrays.asList(dataCheck.columns());  
					
				 if(!isTaskListReport && !categoryList.contains("All") && !categoryList.contains("Physical Servers")) {
					 dataCheck = sparkSession.emptyDataFrame();
				 }     
    	        
              
               System.out.println("------colHeaders---------- " + colHeaders);
               if(!isTaskListReport && (categoryList.contains("All") || categoryList.contains("AWS Instances"))) {            	   
            	   Dataset<Row> awsInstanceData = getAwsInstanceData(colHeaders, siteKey, deviceTypeHeder);
            	   if(awsInstanceData != null && !awsInstanceData.isEmpty()) {
            		   if(dataCheck.isEmpty()) {
            			   dataCheck = awsInstanceData;
            		   } else {
            			   dataCheck = dataCheck.unionByName(awsInstanceData);
            		   }
                   	
                   }
               }
    	        	
               if(!isTaskListReport && (categoryList.contains("All") || categoryList.contains("Custom Excel Data"))) {            	 
            	   Dataset<Row> thirdPartyData = getThirdPartyData(colHeaders, siteKey, deviceTypeHeder, sourceList);
            	   if(thirdPartyData != null && !thirdPartyData.isEmpty()) {                   	
                    if(dataCheck.isEmpty()) {
         			   dataCheck = thirdPartyData;
         		   } else {
         			  dataCheck = dataCheck.unionByName(thirdPartyData);
         		   }
                   }
               }               
               
               
                
    	        for(String col : columnHeaders) {    	        	
    	        	dataCheck = dataCheck.withColumn(col, functions.when(col(col).equalTo(""),"N/A")
      		  		      .when(col(col).equalTo(null),"N/A").when(col(col).isNull(),"N/A")
      		  		      .otherwise(col(col)));
    	        }
    	        
    	        if(!taskListServers.isEmpty()) { //add server~ for task list call    	        	
    	        	List<String> allServers = dataCheck.select("Server Name").as(Encoders.STRING()).collectAsList();    
    	        	
    	        	taskListServers.removeAll(allServers);    	        
    	        	
    	        	if(taskListServers != null && !taskListServers.isEmpty()) {
    	        		Dataset<Row> nonOptDataset = getNonOptDatasetData(siteKey, taskListServers);
    	        		
        	        	if(nonOptDataset != null && !nonOptDataset.isEmpty()) {
        	        		dataCheck = dataCheck.unionByName(nonOptDataset);
        	        	}
    	        	}
    	        	
    	        	dataCheck = dataCheck.withColumnRenamed("End Of Life - HW", "server~End Of Life - HW");
    	        	dataCheck = dataCheck.withColumnRenamed("End Of Extended Support - HW", "server~End Of Extended Support - HW");
    	        	dataCheck = dataCheck.withColumnRenamed("End Of Life - OS", "server~End Of Life - OS");
    	        	dataCheck = dataCheck.withColumnRenamed("End Of Extended Support - OS", "server~End Of Extended Support - OS");
    	        
    	        	
    	        }
    	        
    	        logger.info("getReport Details Ends");
                
                request.setStartRow(0);
                request.setEndRow((int)dataCheck.count());
                rowGroups = request.getRowGroupCols().stream().map(ColumnVO::getField).collect(toList());
     	        groupKeys = request.getGroupKeys();
     	        valueColumns = request.getValueCols();
     	        pivotColumns = request.getPivotCols();
     	        filterModel = request.getFilterModel();
     	        sortModel = request.getSortModel();
     	        isPivotMode = request.isPivotMode();
     	        isGrouping = rowGroups.size() > groupKeys.size();   
    	    
                return paginate(dataCheck, request);
                 
             } catch (Exception ex) {
                 logger.error("Exception in getReport ", ex);
                // ex.printStackTrace();
             }
             
             dataCheck = sparkSession.emptyDataFrame();
             return paginate(dataCheck, request);
		}
		
		
		 private Dataset<Row> getThirdPartyData(List<String> columnHeaders, String siteKey, String deviceTypeHeder, List<String> sourceList) {
			 Dataset<Row> result = sparkSession.emptyDataFrame();
			try {
				 List<AwsInstanceData> thirdPartyData = queryThirdPartyData(siteKey, sourceList);
				
				 Dataset<Row> data = sparkSession.createDataFrame(thirdPartyData, AwsInstanceData.class);
				 
				 data = data.withColumnRenamed("region", "AWS Region");
				 data = data.withColumnRenamed("instancetype", "AWS Instance Type");
				 data = data.withColumnRenamed("memoryinfo", "Memory");
				 data = data.withColumnRenamed("vcpuinfo", "Number of Processors");
				 data = data.withColumnRenamed("platformdetails", "OS Name");
				 data = data.withColumnRenamed("description", "Server Name");
				 data = data.withColumnRenamed("instanceid", "instanceid");
				 data = data.withColumnRenamed("updated_date", "updated_date");
				 data.createOrReplaceGlobalTempView("awsInstanceDF");	
				
				// getAwsPricingForThirdParty();
				 getAWSPricingForThirdParty();
				 getAzurePricingForAWS();
				 getGooglePricingForAWS();		
				 
				
				 
				 try {
					
					// Dataset<Row> dataCheck1 = sparkSession.sql("select * from (select g.`Google Instance Type`, g.`Google On Demand Price`, g.`Google 1 Year Price`, g.`Google 3 Year Price`, ai.`AWS Region`, ai.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`Server Name`, ai.`OS Name`, round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`, round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`, round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`, azure.`Azure Instance Type`, azure.`Azure Specs`,  ROW_NUMBER () over (partition BY ai.`instanceid` ORDER BY a.`PricePerUnit` ASC) AS my_rank, concat_ws(',', concat('Processor: ',a.`Physical Processor`),concat('vCPU: ',a.vCPU),concat('Clock Speed: ',a.`Clock Speed`),concat('Processor Architecture: ',a.`Processor Architecture`) ,concat('Memory: ',a.Memory),concat('Storage: ',a.Storage),concat('Network Performance: ',a.`Network Performance`)) as `AWS Specs`, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a  where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 3 Year Price`, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`)  and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '1yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 1 Year Price`, round((a.`PricePerUnit` * 730),2) as `AWS On Demand Price` from global_temp.awsInstanceDF ai left join global_temp.googleReportForAWSInstance g on ai.instanceid=g.instanceid  left join global_temp.azureReportForAWSInstance azure on lower(azure.`OS Name`) = lower(ai.`actualOsType`) and cast(azure.Memory as float ) = cast(ai.Memory as float)  and cast(azure.VCPUs as int ) = cast(ai.`Number of Cores` as int)  left join global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)= lower(a.`Instance Type`) and a.`License Model` = 'No License required' and a.Location = 'US East (Ohio)' and a.Tenancy <> 'Host' and a.TermType = 'OnDemand' and lower(a.`Operating System`) = lower(ai.`OS Name`) and cast(a.`PricePerUnit` as float ) > 0.0 and (  a.`Product Family` = 'Compute Instance (bare metal)'   or a.`Product Family` = 'Compute Instance'  )) AWS where AWS.my_rank = 1").toDF();
					 Dataset<Row> dataCheck1 = sparkSession.sql("select * from (select ROW_NUMBER() OVER (PARTITION BY ai.`Server Name` ORDER BY  g.`Google On Demand Price`, a.`AWS On Demand Price`  asc) as my_rank,  g.`Google Instance Type`, g.`Google On Demand Price`, g.`Google 1 Year Price`, g.`Google 3 Year Price`, ai.`AWS Region`, a.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`Server Name`, ai.`OS Name`, round( azure.`Azure On Demand Price`, 2 ) as `Azure On Demand Price`, round(azure.`Azure 3 Year Price`, 2) as `Azure 3 Year Price`, round(azure.`Azure 1 Year Price`, 2) as `Azure 1 Year Price`, azure.`Azure Instance Type`, azure.`Azure Specs`, a.`AWS On Demand Price`, a.`AWS 3 Year Price`, a.`AWS 1 Year Price`, a.`AWS Specs` from global_temp.awsInstanceDF ai left join global_temp.googleReportForAWSInstance g on ai.instanceid = g.instanceid left join global_temp.azureReportForAWSInstance azure on ai.instanceid = azure.instanceid left join global_temp.awsReportForThirdParty a on  ai.instanceid = a.instanceid) thirdParty where thirdParty.my_rank = 1").toDF();
					
					 List<String> dup = new ArrayList<>();
					 dup.addAll(Arrays.asList(dataCheck1.columns()));
					 List<String> original = new ArrayList<>();
					 original.addAll(columnHeaders);
					 original.removeAll(dup);
					 for(String col : original) {						
							 dataCheck1 = dataCheck1.withColumn(col, lit("N/A"));
						
					 }		
					
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
			}
			return result;
		}

		 

			private List<AwsInstanceData> queryThirdPartyData(String siteKey, List<String> sourceList) {
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
					if(sourceList.contains("All")) {
						isAllSource = true;
					} else {
						sources = String.join(",", sourceList
					            .stream()
					            .map(source -> ("'" + source + "'"))
					            .collect(Collectors.toList()));
					}
					
					String sql = "select source_id, data from source_data where source_id in ("+sources+") and site_key='"+siteKey+"'";
					
					String getAllSourceSql = "select source_id, fields from source where is_active='true' and (link_to='All' or site_key='"+siteKey+"') and fields like '%memory%' and fields like '%numberOfCores%' and fields like '%osType%' and fields like '%name%'";
					
					List<Map<String, Object>> allSource = favouriteDao_v2.getFavouriteList(getAllSourceSql);
					
					  
					Map<String, JSONArray> sourceMap = new HashMap<String, JSONArray>();
					if(allSource != null && !allSource.isEmpty()) {						
						for(Map<String, Object> map : allSource) {
							sourceMap.put((String) map.get("source_id"), (JSONArray)parser.parse((String) map.get("fields")));
						}
					}
					
					
				
					if(isAllSource) {		
						sources = String.join(",", sourceMap.keySet()
					            .stream()
					            .map(source -> ("'" + source + "'"))
					            .collect(Collectors.toList()));
						sql = "select source_id, data from source_data where source_id in ("+sources+") and site_key='"+siteKey+"'";
					}
					
					List<Map<String, Object>> obj = favouriteDao_v2.getFavouriteList(sql);
				
					
					if(!obj.isEmpty()) {
						for(Map<String, Object> o : obj) {
							
						  JSONObject json = (JSONObject) parser.parse((String) o.get("data"));	
						  String sourceId = (String) o.get("source_id");	
						  JSONArray fieldMapAry = sourceMap.get(sourceId);						
						  Map<String, String> mappingNames = new HashMap<String, String>();
						
						  for(int i=0; i<fieldMapAry.size(); i++) {
							  JSONObject sourceFiledMap = (JSONObject) fieldMapAry.get(i);							
							  mappingNames.put((String)sourceFiledMap.get("primaryKey"), (String)sourceFiledMap.get("displayLabel"));
						  }
						
								if(json.containsKey(mappingNames.get("memory")) && json.containsKey(mappingNames.get("numberOfCores")) && json.containsKey(mappingNames.get("osType")) && json.containsKey(mappingNames.get("name"))) {
									
									String actualOsType = "";						        	
						        		
						        		String value = (String) json.get(mappingNames.get("osType"));			
						        		if(StringUtils.containsIgnoreCase(value, "CentOS")) {
						        			value = "LINUX";
						        			actualOsType="CentOS";
						        		}  else if(StringUtils.containsIgnoreCase(value, "SUSE")) {
						        			value = "SUSE";
						        			actualOsType="SUSE";
						        		}else if(StringUtils.containsIgnoreCase(value, "Red")) {
						        			value = "RHEL";
						        			actualOsType="RHEL";
						        		} else if(StringUtils.containsIgnoreCase(value, "LINUX")) {
						        			value = "LINUX";
						        			actualOsType="UBUNTU";
						        		} else if(StringUtils.containsIgnoreCase(value, "WINDOWS")) {
						        			value = "WINDOWS";
						        			actualOsType="WINDOWS";
						        		}
									float mem = Float.parseFloat((String)json.get(mappingNames.get("memory")));
									float vcpu = Float.parseFloat((String)json.get(mappingNames.get("numberOfCores")));
									String instanceId = mem+"_"+vcpu;
									AwsInstanceData awsInstanceData = new AwsInstanceData("US East (Ohio)", "", (String)json.get(mappingNames.get("memory")), (String)json.get(mappingNames.get("numberOfCores")), value, (String)json.get(mappingNames.get("name")), instanceId, "", actualOsType);
									row.add(awsInstanceData);
								}
						}
						
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return row;
			}
			
		
		 private Dataset<Row> getNonOptDatasetData(String siteKey, List<String> taskListServers) {
			 try {
				 String serverList= "";
				 serverList = String.join(",", taskListServers
				            .stream()
				            .map(server -> ("'" + server.toLowerCase() + "'"))
				            .collect(Collectors.toList()));
				   
				 //, eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`, eol.end_of_life_cycle as `End Of Life - OS`, eol.end_of_extended_support as `End Of Extended Support - OS`  from global_temp.localDiscoveryTemp l  left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(l.`OS Version`) and lcase(eol.os_type)=lcase(l.`Server Type`)  left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(l.`Server Model`, ' ', ''))
				 Dataset<Row> data = sparkSession.sql("select l.rank as `my_rank`, l.`Server Name`, l.OS as `OS Name`, l.`Server Type`, l.`Server Model`, l.Memory, l.`Total Size`, l.`Number of Processors`, l.`Logical Processor Count`, l.`CPU GHz`, l.`Processor Name`, l.`Number of Cores`, l.`DB Service`, l.`HBA Speed`, l.`Number of Ports`, l.`Host`, 'AWS On Demand Price', 'AWS 3 Year Price', 'AWS 1 Year Price', 'AWS Instance Type', 'AWS Region', 'AWS Specs', 'Azure On Demand Price', 'Azure 3 Year Price', 'Azure 1 Year Price', 'Azure Instance Type', 'Azure Specs', 'Google Instance Type', 'Google On Demand Price', 'Google 1 Year Price', 'Google 3 Year Price', l.`OS Version`, eolHw.end_of_life_cycle as `End Of Life - HW`,eolHw.end_of_extended_support as `End Of Extended Support - HW`, eol.end_of_life_cycle as `End Of Life - OS`, eol.end_of_extended_support as `End Of Extended Support - OS`  from global_temp.localDiscoveryTemp l  left join global_temp.eolDataDF eol on lcase(eol.os_version)=lcase(l.`OS Version`) and lcase(eol.os_type)=lcase(l.`Server Type`)  left join global_temp.eolHWDataDF eolHw on lcase(REPLACE((concat(eolHw.vendor,' ',eolHw.model)), ' ', '')) = lcase(REPLACE(l.`Server Model`, ' ', '')) where lower(l.`Server Name`) in ("+serverList+")");
					
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
			}
			 Dataset<Row> df = sparkSession.emptyDataFrame();
			return df;
			
		}



		private Dataset<Row> getAwsInstanceData(List<String> columnHeaders, String siteKey, String deviceType) {
			 Dataset<Row> result = sparkSession.emptyDataFrame();
			 Connection conn = null;
			 Statement stmt = null;
			 if(deviceType.equalsIgnoreCase("All")) {
				 deviceType = " (lower(img.platformdetails) like '%linux%' or lower(img.platformdetails) like '%windows%' or lower(img.platformdetails) like '%vmware%')";
			 } else {
				 deviceType = " lower(img.platformdetails) like '%"+deviceType.toLowerCase()+"%'";
			 }
			 try {
				String query = "select i.sitekey, i.region, i.instanceid, i.instancetype, i.imageid, it.vcpuinfo, it.memoryinfo, img.platformdetails, tag.value as description, i.updated_date from ec2_instances i  left join ec2_tags tag on i.instanceid=tag.resourceid left join ec2_instancetypes it on i.instancetype=it.instancetype  join ec2_images img on i.imageid=img.imageid where i.sitekey='"+siteKey+"' and  "+ deviceType; //i.sitekey='"+siteKey+" and  // + " group by it.instancetype, it.vcpuinfo, it.memoryinfo";
				System.out.println("----------------query------------------" + query);
				conn = AwsInventoryPostgresConnection.dataSource.getConnection();
				 stmt = conn.createStatement();				 
				 ResultSet rs = stmt.executeQuery(query);		
				
				 List<AwsInstanceData> resultRows = resultSetToList(rs);
				 Dataset<Row> data = sparkSession.createDataFrame(resultRows, AwsInstanceData.class);	
				
				// Dataset<Row> data = sparkSession.read().format("csv").option("header","true").load("E:\\opt\\aws_inventory1.csv").distinct();
			
				
				 data = data.withColumnRenamed("region", "AWS Region");
				 data = data.withColumnRenamed("instancetype", "AWS Instance Type");
				 data = data.withColumnRenamed("memoryinfo", "Memory");
				 data = data.withColumnRenamed("vcpuinfo", "Number of Cores");
				 data = data.withColumnRenamed("platformdetails", "OS Name");
				 data = data.withColumnRenamed("description", "Server Name");
				 data = data.withColumnRenamed("instanceid", "instanceid");
				 data = data.withColumnRenamed("updated_date", "updated_date");
				 data.createOrReplaceGlobalTempView("awsInstanceDF");					
				 data.show();
				 
				 getAzurePricingForAWS();
				 getGooglePricingForAWS();
				 try {
					
						
						/*  Dataset<Row> dataCheck1 = sparkSession.sql(
						  " select googlePricing.InstanceType as `Google Instance Type`, round(googlePricing.pricePerUnit*730 + (case when awsData.`Operating System` like '%Windows%' then 67.16  when awsData.`Operating System` like '%Red Hat%' then 43.8 else 0  end),2) as `Google On Demand Price`,  round(googlePricing.1YrPrice*730 + (case when awsData.`Operating System` like '%Windows%' then 67.16  when awsData.`Operating System` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 1 Year Price`, round(googlePricing.3YrPrice*730 + (case when awsData.`Operating System` like '%Windows%' then 67.16  when awsData.`Operating System` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 3 Year Price`, awsData.`AWS Region`, awsData.`Memory`, awsData.`Number of Cores`, awsData.`OS Version`, awsData.`Operating System`, awsData.`AWS 3 Year Price`, awsData.`AWS On Demand Price`, round(az.demandPrice,2) as `Azure On Demand Price`, round(az.3YrPrice,2) as `Azure 3 Year Price`, round(az.1YrPrice,2) as `Azure 1 Year Price`, az.InstanceType as `Azure Instance Type` from (select  ai.`AWS Region`, ai.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`OS Version`, ai.`Operating System`, ((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where a.`Operating System` = ai.`Operating System` and a.PurchaseOption='No Upfront' and a.`Instance Type`= ai.`AWS Instance Type` and a.LeaseContractLength='3yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 3 Year Price`,"
						  + "((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where a.`Operating System` = ai.`Operating System` and a.PurchaseOption='No Upfront' and a.`Instance Type`=ai.`AWS Instance Type` and a.LeaseContractLength='1yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 1 Year Price`,"
						  +  "(a.`PricePerUnit` * 730) as `AWS On Demand Price`  from awsInstanceDF ai left join global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)=lower(a.`Instance Type`) and  cast(ai.Memory as int) = CAST(a.Memory as int) and cast(ai.`Number of Cores` as int) = CAST(a.vCPU as int) and  a.`License Model`='No License required'  and a.Location='US East (Ohio)' and a.Tenancy <> 'Host' and (a.`Product Family` = 'Compute Instance (bare metal)' or a.`Product Family` = 'Compute Instance')) awsData "
						  +  " left join global_temp.azurePricingDF az on cast(awsData.Memory as int) = CAST(az.Memory as int) and cast(awsData.`Number of Cores` as int) = CAST(az.VCPUs as int) and lcase(az.OperatingSystem) = lcase(awsData.`Operating System`)"
						  +  " left join (select cast(OnDemandPrice as float) as pricePerUnit,VCPUs,Memory,InstanceType,1YrPrice,3YrPrice from global_temp.googlePricingDF where  Region='US East' order by cast(OnDemandPrice as float) asc) googlePricing on cast(googlePricing.VCPUs as float)=cast(awsData.`Number of Cores` as float) and cast(googlePricing.Memory as float)=cast(awsData.Memory as float)") .toDF();
						*/ 
					// Dataset<Row> dataCheck1 = sparkSession.sql("select * from (select ai.`AWS Region`, ai.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`Server Name`, ai.`OS Name`, round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`, round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`, round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`, azure.`Azure Instance Type`, azure.`Azure Specs`,  ROW_NUMBER () over (partition BY ai.`instanceid` ORDER BY a.`PricePerUnit` ASC) AS my_rank, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a  where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 3 Year Price`, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`)  and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '1yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 1 Year Price`, round((a.`PricePerUnit` * 730),2) as `AWS On Demand Price` from global_temp.awsInstanceDF ai    left join global_temp.azureReportForAWSInstance azure on lower(azure.`OS Name`) = lower(ai.`actualOsType`) and cast(azure.Memory as float ) = cast(ai.Memory as float)  and cast(azure.VCPUs as int ) = cast(ai.`Number of Cores` as int)  left join global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)= lower(a.`Instance Type`) and a.`License Model` = 'No License required' and a.Location = 'US East (Ohio)' and a.Tenancy <> 'Host' and a.TermType = 'OnDemand' and lower(a.`Operating System`) = lower(ai.`OS Name`) and cast(a.`PricePerUnit` as float ) > 0.0 and (  a.`Product Family` = 'Compute Instance (bare metal)'   or a.`Product Family` = 'Compute Instance'  )) AWS where AWS.my_rank = 1").toDF();
					
					 Dataset<Row> dataCheck1 = sparkSession.sql("select * from (select g.`Google Instance Type`, g.`Google On Demand Price`, g.`Google 1 Year Price`, g.`Google 3 Year Price`, ai.`AWS Region`, ai.`AWS Instance Type`, ai.`Memory`, ai.`Number of Cores`, ai.`Server Name`, ai.`OS Name`, round(azure.`Azure On Demand Price`,2) as `Azure On Demand Price`, round(azure.`Azure 3 Year Price`,2) as `Azure 3 Year Price`, round(azure.`Azure 1 Year Price`,2) as `Azure 1 Year Price`, azure.`Azure Instance Type`, azure.`Azure Specs`,  ROW_NUMBER () over (partition BY ai.`instanceid` ORDER BY a.`PricePerUnit` ASC) AS my_rank, concat_ws(',', concat('Processor: ',a.`Physical Processor`),concat('vCPU: ',a.vCPU),concat('Clock Speed: ',a.`Clock Speed`),concat('Processor Architecture: ',a.`Processor Architecture`) ,concat('Memory: ',a.Memory),concat('Storage: ',a.Storage),concat('Network Performance: ',a.`Network Performance`)) as `AWS Specs`, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a  where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 3 Year Price`, round(((select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`)  and a.PurchaseOption = 'No Upfront' and lcase(a.`Instance Type`) = lcase(ai.`AWS Instance Type`) and a.LeaseContractLength = '1yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ),2) as `AWS 1 Year Price`, round((a.`PricePerUnit` * 730),2) as `AWS On Demand Price` from global_temp.awsInstanceDF ai left join global_temp.googleReportForAWSInstance g on ai.instanceid=g.instanceid  left join global_temp.azureReportForAWSInstance azure on lower(azure.`OS Name`) = lower(ai.`actualOsType`) and cast(azure.Memory as float ) = cast(ai.Memory as float)  and cast(azure.VCPUs as int ) = cast(ai.`Number of Cores` as int)  left join global_temp.awsPricingDF a on lower(ai.`AWS Instance Type`)= lower(a.`Instance Type`) and a.`License Model` = 'No License required' and a.Location = 'US East (Ohio)' and a.Tenancy <> 'Host' and a.TermType = 'OnDemand' and lower(a.`Operating System`) = lower(ai.`OS Name`) and cast(a.`PricePerUnit` as float ) > 0.0 and (  a.`Product Family` = 'Compute Instance (bare metal)'   or a.`Product Family` = 'Compute Instance'  )) AWS where AWS.my_rank = 1").toDF();
					
					 List<String> dup = new ArrayList<>();
					 dup.addAll(Arrays.asList(dataCheck1.columns()));
					 List<String> original = new ArrayList<>();
					 original.addAll(columnHeaders);
					 original.removeAll(dup);
					 for(String col : original) {
						 if(col.equalsIgnoreCase("Server Type")) {							
							 dataCheck1 = dataCheck1.withColumn(col, lit("EC2"));
						 } else {
							 dataCheck1 = dataCheck1.withColumn(col, lit("N/A"));
						 }
						
					 }		
					
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
			} finally {
				try {
					conn.close();
					AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
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
			    while (rs.next()){
			        Map<String, String> row = new HashMap<String, String>(columns);
			        
			        for(int i = 1; i <= columns; ++i){
			        	String colName = md.getColumnName(i);
			        	String value = rs.getString(i);			        
			        	if(md.getColumnName(i).equals("vcpuinfo")) {
			        		value = getValueFromJson("DefaultVCpus", rs.getString(i));			        		
			        	}
			        	if(md.getColumnName(i).equals("memoryinfo")) {
			        		value = getValueFromJson("SizeInMiB", rs.getString(i));	
			        		value = Integer.parseInt(value)/1024 + "";
			        	}
			        	
			        	if(md.getColumnName(i).equals("region")) {			        		
			        		if(rs.getString(i).equalsIgnoreCase("us-east-2")) {
			        			value = "US East (Ohio)";
			        		}
			        	}
			        	if(md.getColumnName(i).equals("platformdetails")) {
			        		String actualOsType="";
			        		value = rs.getString(i);			
			        		if(StringUtils.containsIgnoreCase(value, "CentOS")) {
			        			value = "LINUX";
			        			actualOsType="CentOS";
			        		}  else if(StringUtils.containsIgnoreCase(value, "SUSE")) {
			        			value = "SUSE";
			        			actualOsType="SUSE";
			        		}else if(StringUtils.containsIgnoreCase(value, "Red")) {
			        			value = "RHEL";
			        			actualOsType="RHEL";
			        		} else if(StringUtils.containsIgnoreCase(value, "LINUX")) {
			        			value = "LINUX";
			        			actualOsType="UBUNTU";
			        		} else if(StringUtils.containsIgnoreCase(value, "WINDOWS")) {
			        			value = "WINDOWS";
			        			actualOsType="WINDOWS";
			        		}
			        		 row.put("actualOsType", actualOsType);
			        	}
			            row.put(colName, value);
			           
			        }
			        AwsInstanceData awsInstanceData = new AwsInstanceData(row.get("region"), row.get("instancetype"),row.get("memoryinfo"),row.get("vcpuinfo"),row.get("platformdetails"),row.get("description"), row.get("instanceid"), row.get("updated_date"), row.get("actualOsType"));
			    	//System.out.println("----json----------" +awsInstanceData.toString() );
			        rows.add(awsInstanceData);
			    }
			    return rows;
			}

		private String getValueFromJson(String key, String jsonString) {
			try {
				JSONParser jSONParser = new JSONParser();
				JSONObject json = (JSONObject) jSONParser.parse(jsonString);
			
				if(json.get(key)  instanceof String) {
					return (String) json.get(key);
				} else if(json.get(key)  instanceof Long) {
					Long  rs = (Long) json.get(key);
					return rs.toString();
				}
				return "";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
					   		         dataset.cache();	
					   		         
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
		            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
		                    " select report.`vCPU`, report.`Server Name`, report.`OS Name`," +
		                    " report.`Memory` as `Memory`, report.`Number of Processors`, " +
		                    "  (report.`PricePerUnit` * 730) as `AWS On Demand Price`," +
		                    " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='3yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 3 Year Price`, " +
		                    " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='1yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 1 Year Price`, " +
		                    " report.`AWS Instance Type`,report.`AWS Region`,report.`AWS Specs`," +
		                    " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`PricePerUnit` as float) asc) as my_rank" +
		                    " from (SELECT localDiscoveryDF.`Server Name`," +
		                    " localDiscoveryDF.`OS Name`, " +
		                    " cast(localDiscoveryDF.`Number of Processors` as int) as `Number of Processors`," +
		                    " cast(localDiscoveryDF.`Memory` as int) as `Memory`, " +
		                    " awsPricing2.`Instance Type` as `AWS Instance Type`, awsPricing2.Location as `AWS Region`" +
		                    " ,concat_ws(',', concat('Processor: ',awsPricing2.`Physical Processor`),concat('vCPU: ',awsPricing2.vCPU)" +
		                    " ,concat('Clock Speed: ',awsPricing2.`Clock Speed`),concat('Processor Architecture: ',awsPricing2.`Processor Architecture`)" +
		                    " ,concat('Memory: ',awsPricing2.Memory),concat('Storage: ',awsPricing2.Storage),concat('Network Performance: ',awsPricing2.`Network Performance`)) as `AWS Specs`" +
		                    " , cast(localDiscoveryDF.`Number of Processors` as int) as `vCPU`, (case when localDiscoveryDF.`Memory` is null then 0 else cast(localDiscoveryDF.`Memory` as int) end) as `MemorySize`, awsPricing2.PricePerUnit as `PricePerUnit`,awsPricing2.`Operating System` as `OperatingSystem` " +
		                    " FROM global_temp.awsInstanceDF localDiscoveryDF" +
		                    " join (Select localDiscoveryDF1.`Server Name` " +
		                    " from global_temp.awsInstanceDF localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`) localDiscoveryTemp2 ON " +
		                    " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name`" +
		                    " left join (select `Operating System`,Memory,min(PricePerUnit) as pricePerUnit, vCPU,TermType from global_temp.awsPricingDF where `License Model`='No License required'" +
		                    " and Location='US East (Ohio)' and Tenancy <> 'Host' and (`Product Family` = 'Compute Instance (bare metal)' or `Product Family` = 'Compute Instance') and cast(PricePerUnit as float) > 0 group by `Operating System`,Memory,vCPU,TermType) awsPricing on" +
		                    " lcase(awsPricing.`Operating System`) = lcase((case when localDiscoveryDF.`OS Name` like '%Red Hat%' then 'RHEL'" +
		                    " when localDiscoveryDF.`OS Name` like '%SUSE%' then 'SUSE' when localDiscoveryDF.`OS Name` like '%Linux%' OR localDiscoveryDF.`OS Name` like '%CentOS%' then 'Linux'" +
		                    " when localDiscoveryDF.`OS Name` like '%Windows%' then 'Windows')) and" +
		                    " awsPricing.Memory >= (case when localDiscoveryDF.Memory is null then 0 else cast(localDiscoveryDF.Memory as int) end)" +
		                    " and awsPricing.vCPU >= (cast(localDiscoveryDF.`Number of Processors` as int))" +
		                    " left join global_temp.awsPricingDF awsPricing2 on awsPricing2.`Operating System` = awsPricing.`Operating System` and awsPricing2.PricePerUnit = awsPricing.pricePerUnit and awsPricing.Memory = " +
		                    " awsPricing2.Memory and awsPricing.vCPU = awsPricing2.vCPU and awsPricing2.TermType='OnDemand' where cast(awsPricing2.PricePerUnit as float) > 0) report) reportData" +
		                    " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
		            dataCheck.createOrReplaceGlobalTempView("awsReport");				            
		            dataCheck.cache();
		            dataCheck.printSchema();
		            dataCheck.show();
		            
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
				                    " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='3yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 3 Year Price`, " +
				                    " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='1yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 1 Year Price`, " +
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
				                    " , (case when localDiscoveryDF.`Logical Processor Count` is null  and localDiscoveryDF.`Number of Processors` is not null then " +
				                    " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Logical Processor Count` is not null then " +
				                    " localDiscoveryDF.`Number of Processors` else 0 end) as `vCPU`, (case when localDiscoveryDF.`Memory` is null then 0 else cast(localDiscoveryDF.`Memory` as int) end) as `MemorySize`, awsPricing2.PricePerUnit as `PricePerUnit`,awsPricing2.`Operating System` as `OperatingSystem`, localDiscoveryDF.Host" +
				                    " FROM global_temp.localDiscoveryTemp localDiscoveryDF" +
				                    " join (Select localDiscoveryDF1.site_key, localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate " +
				                    " from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and " +
				                    " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key" +
				                    " left join (select `Operating System`,Memory,min(PricePerUnit) as pricePerUnit, vCPU,TermType from global_temp.awsPricingDF where `License Model`='No License required'" +
				                    " and Location='US East (Ohio)' and Tenancy <> 'Host' and (`Product Family` = 'Compute Instance (bare metal)' or `Product Family` = 'Compute Instance') and cast(PricePerUnit as float) > 0 group by `Operating System`,Memory,vCPU,TermType) awsPricing on" +
				                    " lcase(awsPricing.`Operating System`) = lcase((case when localDiscoveryDF.OS like '%Red Hat%' then 'RHEL'" +
				                    " when localDiscoveryDF.OS like '%SUSE%' then 'SUSE' when localDiscoveryDF.OS like '%Linux%' OR localDiscoveryDF.OS like '%CentOS%' then 'Linux'" +
				                    " when localDiscoveryDF.OS like '%Windows%' then 'Windows' else localDiscoveryDF.`Server Type` end)) and" +
				                    " awsPricing.Memory >= (case when localDiscoveryDF.Memory is null then 0 else cast(localDiscoveryDF.Memory as int) end)" +
				                    " and awsPricing.vCPU >= (case when localDiscoveryDF.`Logical Processor Count` is null  and localDiscoveryDF.`Number of Processors` is not null then " +
				                    " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Logical Processor Count` is not null then " +
				                    " localDiscoveryDF.`Logical Processor Count` else 0 end)" +
				                    " left join global_temp.awsPricingDF awsPricing2 on awsPricing2.`Operating System` = awsPricing.`Operating System` and awsPricing2.PricePerUnit = awsPricing.pricePerUnit and awsPricing.Memory = " +
				                    " awsPricing2.Memory and awsPricing.vCPU = awsPricing2.vCPU and awsPricing2.TermType='OnDemand' where cast(awsPricing2.PricePerUnit as float) > 0) report) reportData" +
				                    " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
				            dataCheck.createOrReplaceGlobalTempView("awsReport");				            
				            dataCheck.cache();
				        } catch (Exception ex) {
				            ex.printStackTrace();
				        }
				    }

		 public void getAzurePricing() {
				        try {
				            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
				                    " select report.log_date,report.`vCPU`, report.site_key, " +
				                    " report.`Server Name`, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`," +
				                    " report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`," +
				                    " report.`Number of Processors`, report.`Logical Processor Count`, " +
				                    " report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`," +
				                    " report.`Number of Ports`,report.`Azure On Demand Price`,report.`Azure 3 Year Price`, report.`Azure 1 Year Price`, report.`Azure Instance Type`, report.`Azure Specs`, " +
				                    " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`Azure On Demand Price` as float) asc) as my_rank" +
				                    " from (SELECT localDiscoveryDF.log_date,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Type`," +
				                    " localDiscoveryDF.OS as `OS Name`,localDiscoveryDF.`OS Version`,localDiscoveryDF.`Server Model`," +
				                    " localDiscoveryDF.`Logical Processor Count`,localDiscoveryDF.`Number of Processors`," +
				                    " cast(localDiscoveryDF.`Memory` as int) as `Memory`, round(localDiscoveryDF.`Total Size`,2) as `Total Size`, " +
				                    " cast(localDiscoveryDF.`CPU GHz` as int) as `CPU GHz`, localDiscoveryDF.`Processor Name`, " +
				                    " cast(localDiscoveryDF.`Number of Cores` as int) as `Number of Cores`," +
				                    " localDiscoveryDF.`DB Service`, localDiscoveryDF.`HBA Speed`, cast(localDiscoveryDF.`Number of Ports` as int) as `Number of Ports`," +
				                    " round(azurePricingDF.demandPrice,2) as `Azure On Demand Price`," +
				                    " round(azurePricingDF.3YrPrice,2) as `Azure 3 Year Price`," +
				                    " round(azurePricingDF.1YrPrice,2) as `Azure 1 Year Price`," +
				                    " azurePricingDF.InstanceType as `Azure Instance Type`,azurePricingDF.`Azure Specs`," +
				                    " (case when localDiscoveryDF.`Logical Processor Count` is null  and localDiscoveryDF.`Number of Processors` is not null then " +
				                    " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Logical Processor Count` is not null then " +
				                    " localDiscoveryDF.`Logical Processor Count` else 0 end) as `vCPU`" +
				                    " FROM global_temp.localDiscoveryTemp localDiscoveryDF" +
				                    " left join global_temp.azurePricingDF azurePricingDF on azurePricingDF.VCPUs >= (case when localDiscoveryDF.`Logical Processor Count` is null  and localDiscoveryDF.`Number of Processors` is not null then" +
				                    " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Logical Processor Count` is not null then" +
				                    " localDiscoveryDF.`Logical Processor Count` else 0 end)" +
				                    " and azurePricingDF.Memory >= (case when localDiscoveryDF.Memory is null then 0 else cast(localDiscoveryDF.Memory as int) end) and" +
				                    " lcase(azurePricingDF.OperatingSystem) = lcase((case when localDiscoveryDF.OS like '%Red Hat%' then 'RHEL' " +
				                    " when localDiscoveryDF.OS like '%SUSE%' then 'SUSE' when localDiscoveryDF.OS like '%Linux%' OR localDiscoveryDF.OS like '%CentOS%' then 'Linux'" +
				                    " when localDiscoveryDF.OS like '%Windows%' then 'Windows' else localDiscoveryDF.`Server Type` end))) report ) reportData " +
				                    " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
				            dataCheck.createOrReplaceGlobalTempView("azureReport");		
				            dataCheck.cache();
				            
				        } catch (Exception ex) {
				            ex.printStackTrace();
				        }
				    }
		 
		 
		 public void getAzurePricingForAWS() {
		        try {
		            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
		                    " select report.`VCPUs`, " +
		                    " report.`instanceid`, report.`OS Name`, " +
		                    " report.`Memory` as `Memory`, " +	
		                    " report.`Azure On Demand Price`,report.`Azure 3 Year Price`, report.`Azure 1 Year Price`, report.`Azure Instance Type`, report.`Azure Specs`, " +
		                    " ROW_NUMBER() OVER (PARTITION BY report.`instanceid` ORDER BY cast(report.`Azure On Demand Price` as float) asc) as my_rank" +
		                    " from (SELECT ai.`instanceid`, " +
		                    " ai.`Number of Cores`, ai.`actualOsType` as `OS Name`, " +		                 
		                    " round(azurePricingDF.demandPrice,2) as `Azure On Demand Price`," +
		                    " round(azurePricingDF.3YrPrice,2) as `Azure 3 Year Price`," +
		                    " round(azurePricingDF.1YrPrice,2) as `Azure 1 Year Price`," +
		                    " azurePricingDF.VCPUs," +
		                    " azurePricingDF.Memory, azurePricingDF.`InstanceType` as `Azure Instance Type`, azurePricingDF.`Azure Specs` "+
		                    " FROM global_temp.awsInstanceDF ai" +
		                    " left join global_temp.azurePricingDF azurePricingDF on cast(azurePricingDF.VCPUs as int) >= cast(ai.`Number of Cores` as int) " +
		                    " and cast(azurePricingDF.Memory as float) >= cast(ai.Memory as float) " +
		                    " and lower(azurePricingDF.`OperatingSystem`) = lower(ai.`actualOsType`)" +
		                    " ) report ) reportData " +
		                    " where reportData.my_rank= 1 order by reportData.`instanceid` asc").toDF();
		            dataCheck.createOrReplaceGlobalTempView("azureReportForAWSInstance");	
		            dataCheck.show();
		            
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		    }
		 
		 public void getAwsPricingForThirdParty() {
		        try {
		        //cast(a.vCPU as int) =  cast(ai.`Number of Cores` as int) and  cast(a.Memory as int) = cast (ai.Memory as int) and
		            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
		                    " select report.`vCPU`,  report.`Server Name`, " +
		                    " report.`AWS Instance Type`, report.`OS Name`, report.instanceid, " +
		                    " report.`Memory` as `Memory`, " +	
		                    " report.`AWS On Demand Price`,report.`AWS 3 Year Price`, report.`AWS 1 Year Price`, report.`AWS Instance Type`, report.`AWS Specs`, " +
		                    " ROW_NUMBER() OVER (PARTITION BY report.`Server Name`  ORDER BY cast(report.`PricePerUnit` as float) asc) as my_rank" +
		                    " from (SELECT ai.`instanceid`, " +
		                    " ai.`Number of Cores`, ai.`actualOsType` as `OS Name`, " +		                 
		                    " round( ( ( select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and  a.LeaseContractLength = '3yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ), 2 ) as `AWS 3 Year Price`, "+
		                    " round( ( ( select min(a.`PricePerUnit`) from global_temp.awsPricingDF a where lcase(a.`Operating System`) = lcase(ai.`OS Name`) and a.PurchaseOption = 'No Upfront' and  a.LeaseContractLength = '1yr' and cast(a.`PricePerUnit` as float) > 0 ) * 730 ), 2 ) as `AWS 1 Year Price`, "+
		                    " round( (a.`PricePerUnit` * 730), 2 ) as `AWS On Demand Price`, a.`PricePerUnit`, "+
		                    " a.vCPU, ai.`Server Name`, " +
		                    " a.Memory, a.`Instance Type` as `AWS Instance Type`, concat_ws(',', concat('Processor: ',a.`Physical Processor`),concat('vCPU: ',a.vCPU),concat('Clock Speed: ',a.`Clock Speed`),concat('Processor Architecture: ',a.`Processor Architecture`) ,concat('Memory: ',a.Memory),concat('Storage: ',a.Storage),concat('Network Performance: ',a.`Network Performance`)) as `AWS Specs` "+
		                    " FROM global_temp.awsInstanceDF ai" +
		                    " left join global_temp.awsPricingDF a on cast(a.vCPU as int) >=  cast(ai.`Number of Cores` as int) and  cast(a.Memory as int) >= cast (ai.Memory as int) "+
		                    " and a.`TermType`='OnDemand' and Location='US East (Ohio)' and Tenancy <> 'Host'  and (`Product Family` = 'Compute Instance (bare metal)' or `Product Family` = 'Compute Instance') and lower(a.`Operating System`) = lower(ai.`actualOsType`) and cast(a.`PricePerUnit` as float ) > 0.0" +
		                    " ) report ) reportData " +
		                    " where reportData.my_rank= 1 order by reportData.`instanceid` asc").toDF();
		            dataCheck.createOrReplaceGlobalTempView("awsReportForThirdParty");	
		          
		            
		            dataCheck.show();
		            
		           
		            
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		    }
		 
		 public void getGooglePricingForAWS() {
		        try {
		            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
		                    " select report.`VCPUs`, report.`instanceid`, " +
		                    " report.`OS Name`," +
		                    " report.`Memory`, " +		                  
		                    " report.`Number of Cores`, " +		                   
		                    " report.`Google Instance Type`,report.`Google On Demand Price`,report.`Google 1 Year Price`,report.`Google 3 Year Price`," +
		                    " ROW_NUMBER() OVER (PARTITION BY report.`instanceid` ORDER BY cast(report.`Google On Demand Price` as float) asc) as my_rank" +
		                    " from (SELECT ai.`instanceid`, " +
		                    " ai.`Number of Cores`, ai.`actualOsType` as `OS Name`," +		                    
		                    " ai.Memory, ai.instanceid," +		                  
		                    " ai.`Number of Cores`, " +		                    
		                    " googlePricing.InstanceType as `Google Instance Type`, googlePricing.`VCPUs`," +
		                    " round(googlePricing.pricePerUnit * 730 + " +		                  
		                    " (case when ai.`actualOsType` like '%Windows%' then 67.16  when ai.`actualOsType` like '%Red Hat%' then 43.8 else 0  end),2) as `Google On Demand Price`," +
		                    " round(googlePricing.1YrPrice * 730 + " +		                 
		                    " (case when ai.`actualOsType` like '%Windows%' then 67.16  when ai.`actualOsType` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 1 Year Price`," +
		                    " round(googlePricing.3YrPrice * 730 + " +		                  
		                    " (case when ai.`actualOsType` like '%Windows%' then 67.16  when ai.`actualOsType` like '%Red Hat%' then 43.8 else 0  end),2) as `Google 3 Year Price`" +
		                    " FROM global_temp.awsInstanceDF ai " +
		                    " left join (select cast(OnDemandPrice as float) as pricePerUnit,VCPUs,Memory,InstanceType,1YrPrice,3YrPrice from global_temp.googlePricingDF where " +
		                    " Region='US East' order by cast(OnDemandPrice as float) asc) googlePricing on cast(googlePricing.VCPUs as float) >=  cast(ai.`Number of Cores` as float) and " +
		                    " cast(googlePricing.Memory as float) >= cast (ai.Memory as float)) report ) reportData " +
		                    " where reportData.my_rank= 1 order by reportData.`instanceid` asc").toDF();
		            dataCheck.createOrReplaceGlobalTempView("googleReportForAWSInstance");				           
		            dataCheck.cache();
		           // dataCheck.printSchema();
		            dataCheck.show();
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		    }

		 public void getGooglePricing() {
				        try {
				            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
				                    " select report.log_date,report.`vCPU`, report.site_key, " +
				                    " report.`Server Name`, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`," +
				                    " report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`," +
				                    " report.`Number of Processors`, report.`Logical Processor Count`, " +
				                    " report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`," +
				                    " report.`Number of Ports`," +
				                    " report.`Google Instance Type`,report.`Google On Demand Price`,report.`Google 1 Year Price`,report.`Google 3 Year Price`," +
				                    " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`Google On Demand Price` as float) asc) as my_rank" +
				                    " from (SELECT localDiscoveryDF.log_date,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Type`," +
				                    " localDiscoveryDF.OS as `OS Name`,localDiscoveryDF.`OS Version`,localDiscoveryDF.`Server Model`," +
				                    " cast(localDiscoveryDF.`Logical Processor Count` as int) as `Logical Processor Count`,cast(localDiscoveryDF.`Number of Processors` as int) as `Number of Processors`," +
				                    " cast(localDiscoveryDF.Memory as int) as `Memory`, round(localDiscoveryDF.`Total Size`,2) as `Total Size`, " +
				                    " cast(localDiscoveryDF.`CPU GHz` as int) as `CPU GHz`, localDiscoveryDF.`Processor Name` as `Processor Name`, " +
				                    " cast(localDiscoveryDF.`Number of Cores` as int) as `Number of Cores`," +
				                    " localDiscoveryDF.`DB Service` as `DB Service`, localDiscoveryDF.`HBA Speed` as `HBA Speed`, cast(localDiscoveryDF.`Number of Ports` as int) as `Number of Ports`," +
				                    " localDiscoveryDF.`Logical Processor Count` as `vCPU`," +
				                    " googlePricing.InstanceType as `Google Instance Type`," +
				                    " round(googlePricing.pricePerUnit*730 + " +
				                    " ((case when localDiscoveryDF.`Total Size` is null then 0 else localDiscoveryDF.`Total Size` end) * 0.08) + " +
				                    " (case when localDiscoveryDF.OS like '%Windows%' then 67.16  when localDiscoveryDF.OS like '%Red Hat%' then 43.8 else 0  end),2) as `Google On Demand Price`," +
				                    " round(googlePricing.1YrPrice*730 + " +
				                    " ((case when localDiscoveryDF.`Total Size` is null then 0 else localDiscoveryDF.`Total Size` end) * 0.05) + " +
				                    " (case when localDiscoveryDF.OS like '%Windows%' then 67.16  when localDiscoveryDF.OS like '%Red Hat%' then 43.8 else 0  end),2) as `Google 1 Year Price`," +
				                    " round(googlePricing.3YrPrice*730 + " +
				                    " ((case when localDiscoveryDF.`Total Size` is null then 0 else localDiscoveryDF.`Total Size` end) * 0.04) + " +
				                    " (case when localDiscoveryDF.OS like '%Windows%' then 67.16  when localDiscoveryDF.OS like '%Red Hat%' then 43.8 else 0  end),2) as `Google 3 Year Price`" +
				                    " FROM global_temp.localDiscoveryTemp localDiscoveryDF " +
				                    " join (Select localDiscoveryDF1.site_key,localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate " +
				                    " from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and " +
				                    " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key" +
				                    " left join (select cast(OnDemandPrice as float) as pricePerUnit,VCPUs,Memory,InstanceType,1YrPrice,3YrPrice from global_temp.googlePricingDF where " +
				                    " Region='US East' order by cast(OnDemandPrice as float) asc) googlePricing on cast(googlePricing.VCPUs as float) >= " +
				                    " (case when localDiscoveryDF.`Number of Processors` is not null then" +
				                    " cast(localDiscoveryDF.`Number of Processors` as int)  when localDiscoveryDF.`Number of Processors` is not null then" +
				                    " localDiscoveryDF.`Logical Processor Count` else 0 end) and " +
				                    " cast(googlePricing.Memory as float) >= (case when localDiscoveryDF.Memory is null then 0 else " +
				                    " cast(localDiscoveryDF.Memory as int) end)) report ) reportData " +
				                    " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
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
				                String sql = " select report.source_id, report.site_key, report.`Server Name`,report.`Server Type`,report.`OS Name`,report.`OS Version`, " +
				                        " eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`" +
				                        " from (select localDiscoveryDF.source_id,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`, localDiscoveryDF.`Server Type`," +
				                        " localDiscoveryDF.OS as `OS Name`,localDiscoveryDF.`OS Version`" +
				                        " FROM global_temp.localDiscoveryTemp localDiscoveryDF" +
				                        " join (Select localDiscoveryDF1.site_key,localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate " +
				                        " from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and " +
				                        " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key) report" +
				                        " left join global_temp.eolDataDF eol on eol.os_type=report.`Server Type` and replace(trim(report.`OS Name`),'(R)' ' ') like concat('%',trim(eol.os_type),'%')" +
				                        //" and trim(report.`OS Version`) like concat('%',trim(eol.osversion),'%') " +
				                        " and trim(report.`OS Version`) = trim(eol.os_version) " +
				                        " where report.site_key='" + siteKey + "'";

				                Dataset<Row> dataCheck = sparkSession.sql(sql).toDF();

				                dataCount = Integer.parseInt(String.valueOf(dataCheck.count()));
				                if (dataCount > 0) {
				                    dataCheck.createOrReplaceGlobalTempView("eoleosDataDF");
				                    dataCheck.cache();
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
				                String sql = "select report.source_id, report.site_key, report.`Server Name`,report.`Server Model`, " +
				                        " eol.end_of_life_cycle as `End Of Life - HW`,eol.end_of_extended_support as `End Of Extended Support - HW`" +
				                        " from (select localDiscoveryDF.source_id,localDiscoveryDF.site_key, localDiscoveryDF.`Server Name`," +
				                        " localDiscoveryDF.`Server Model`" +
				                        " FROM global_temp.localDiscoveryTemp localDiscoveryDF" +
				                        " join (Select localDiscoveryDF1.site_key,localDiscoveryDF1.`Server Name`,max(localDiscoveryDF1.log_date) MaxLogDate " +
				                        " from global_temp.localDiscoveryTemp localDiscoveryDF1 group by localDiscoveryDF1.`Server Name`,localDiscoveryDF1.site_key) localDiscoveryTemp2 ON localDiscoveryDF.log_date = localDiscoveryTemp2.MaxLogDate and " +
				                        " localDiscoveryTemp2.`Server Name` = localDiscoveryDF.`Server Name` and localDiscoveryDF.site_key = localDiscoveryTemp2.site_key) report" +
				                        " left join global_temp.eolHWDataDF eol on lcase(concat(eol.vendor,' ',eol.model)) = lcase(report.`Server Model`)" +
				                        " where report.site_key='" + siteKey + "'";

				                Dataset<Row> dataCheck = sparkSession.sql(sql).toDF();
				                
				              //  dataCheck.printSchema();
				              

				                dataCount = Integer.parseInt(String.valueOf(dataCheck.count()));
				                if (dataCount > 0) {
				                    dataCheck.createOrReplaceGlobalTempView("eolHWData");
				                    dataCheck.cache();
				                }
				            }
				        } catch (Exception ex) {
				            logger.error("Exception in generating dataframe for EOL/EOS - HW data", ex);
				        }
				        logger.info("Construct EOL/EOS - HW Dataframe Ends");
				        return dataCount;
				    }
							
					
	    
}
