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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

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
import com.zenfra.dao.ReportDao;
import com.zenfra.dataframe.filter.ColumnFilter;
import com.zenfra.dataframe.filter.NumberColumnFilter;
import com.zenfra.dataframe.filter.SetColumnFilter;
import com.zenfra.dataframe.filter.TextColumnFilter;
import com.zenfra.dataframe.request.ColumnVO;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.request.SortModel;
import com.zenfra.dataframe.response.DataResult;
import com.zenfra.dataframe.util.DataframeUtil;
import com.zenfra.dataframe.util.ZenfraConstants;
import com.zenfra.service.ReportService;
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
			
			return ZenfraConstants.SUCCESS;
		} catch (Exception exp) {
			logger.error("Not able to create dataframe {}",  exp.getMessage(), exp);
		}
		
		return ZenfraConstants.ERROR;
	}
		
	
	
	 public DataResult getReportData(ServerSideGetRowsRequest request) {	    
		 
		 
		 String siteKey = request.getSiteKey();
         String source_type = request.getSourceType().toLowerCase();
        
       
 		if(source_type != null && !source_type.trim().isEmpty() && (source_type.contains("hyper") || source_type.contains("nutanix"))) {
 			source_type = source_type + "-" + request.getReportBy().toLowerCase();
 		} else if(source_type != null && !source_type.trim().isEmpty() && (source_type.contains("vmware") && request.getReportBy().toLowerCase().contains("host"))) {
 			source_type = source_type + "-" + request.getReportBy().toLowerCase();
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
	        	 
	        // int osCount = 0;	
	        /// int hwCount = 0;

	                String hwJoin = "";
	                String hwdata = "";
	                String osJoin = "";
	                String osdata = "";
	        	
	        	if(osCount > 0) {
	        		 Dataset<Row> eolos = sparkSession.sql("select * from global_temp.eolDataDF where lower(os_type)='"+source_type+"'");  // where lower(`Server Name`)="+source_type
	        		 eolos.createOrReplaceTempView("eolos");
	        		 if(eolos.count() > 0) { 
	        			 
	        			 osJoin = " left join eolos eol on lcase(eol.os_type)=lcase(ldView.actual_os_type) where lcase(eol.os_version)=lcase(ldView.`OS Version`)";
	                     osdata = ",eol.end_of_life_cycle as `End Of Life - OS`,eol.end_of_extended_support as `End Of Extended Support - OS`";
		        		 
	 	        		/*String eosQuery = "Select * from ( Select ldView.* ,eol.end_of_life_cycle as `End Of Life - OS` ,eol.end_of_extended_support as `End Of Extended Support - OS`  from global_temp."+viewName+" ldView left join eolos eol on lcase(eol.os_type)=lcase(ldView.actual_os_type) where lcase(eol.os_version)=lcase(ldView.`OS Version`) )";
	 	        		Dataset<Row> datasetTmp =  sparkSession.sql(eosQuery);
	 	        		 System.out.println("----------->>>>>>>>>>>>>>>>>>>>>>--------------" + datasetTmp.count());
	 	        		datasetTmp.show(); */
			        	
			         }
	        	}
	        	
	        	 if(hwCount > 0) {
	        		 if(Arrays.stream(dataset.columns()).anyMatch("Server Model"::equals) && dataset.first().fieldIndex("Server Model") != -1) {
	        			 
	        			 hwJoin = " left join global_temp.eolHWDataDF eolHw on (concat(eolHw.vendor,' ',eolHw.model))= ldView.`Server Model`";
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
	        	 
	        
	        // dataset.printSchema();
	         
	         //------------------------------------------------------//
	         
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
	        
	        List<String> numericalHeaders = getReportNumericalHeaders("Discovery", source_type, "Discovery", siteKey);

            for(String column : numericalHeaders) {
            	if(results.columns().toString().contains(column)) {
            		results.withColumn(column, results.col(column).cast("integer"));
            	}
            	
            }
	        
	        
	       
	       /* List<String> headers = reportDao.getReportHeaderForFilter("discovery", source_type.toLowerCase(), request.getReportBy().toLowerCase());	  
	        List<String> actualHeadets = new ArrayList<>();
	        actualHeadets.addAll(Arrays.asList(results.columns()));	      
	        actualHeadets.removeAll(headers);	       
	        results =  results.drop(actualHeadets.stream().toArray(String[]::new)); */
	        return paginate(results, request);
		} catch (Exception e) {
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



public DataResult getOptimizationReportData(ServerSideGetRowsRequest request) {	    
		 
		 
		 String siteKey = request.getSiteKey();
         String source_type = request.getSourceType().toLowerCase();
         String sourceTypeFinal = request.getSourceType().toLowerCase();
       
 		if(source_type != null && !source_type.trim().isEmpty() && source_type.contains("hyper")) {
 			source_type = source_type + "-" + request.getReportBy().toLowerCase();
 		} 	
         
		 boolean isDiscoveryDataInView = false;
		 Dataset<Row> dataset = null;
		 String viewName = siteKey+"_"+source_type.toLowerCase();
		 viewName = viewName.replaceAll("-", "");
		 try {
		    eolService.getAWSReport(viewName);
	 		eolService.getAzureReport(viewName);
	 		eolService.getGoogleReport(viewName);
	 		
		
	         String sql = "select * from (" +
	                    " select " +
	                    " ROW_NUMBER() OVER (PARTITION BY aws.`Server Name` ORDER BY aws.`logDate` desc) as my_rank," +
	                    " aws.`Server Name`, aws.`OS Name`, aws.`Server Type`, aws.`Server Model`," +
	                    " aws.`Memory`, aws.`Total Size`, aws.`Number of Processors`, aws.`Logical Processor Count`, " +
	                    " round(aws.`CPU GHz`,2) as `CPU GHz`, aws.`Processor Name`,aws.`Number of Cores`,aws.`DB Service`, " +
	                    " aws.`HBA Speed`,aws.`Number of Ports`," +
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
	                    " aws.`OS Version` " +	                  
	                    " from global_temp.awsReport aws " +	                   
	                    " left join global_temp.azureReport azure on azure.`Server Name` = aws.`Server Name`" +
	                    " left join global_temp.googleReport google on google.`Server Name` = aws.`Server Name` " +	                 
	                    " where aws.siteKey='" + siteKey + "' and " + source_type + " order by aws.`Server Name` asc)ld where ld.my_rank = 1";
	         
	         dataset = sparkSession.sql(sql).toDF();
	           
	         //---------------------EOL EOS---------------------------//	     
	         if(dataset.count() > 0) {
	        	 int osCount = eolService.getEOLEOSData();
	        	 int hwCount = eolService.getEOLEOSHW();
	        	String hwModel =  dataset.first().getAs("Server Model");
	        	if(osCount > 0) {
	        		 Dataset<Row> eolos = sparkSession.sql("select endoflifecycle as `End Of Life - OS`, endofextendedsupport as `End Of Extended Support - OS` from global_temp.eolDataDF where lower(ostype)='"+source_type+"'");  // where lower(`Server Name`)="+source_type
		        	 if(eolos.count() > 0) {		        	
			        	 dataset = dataset.join(eolos);
			         }
	        	}
	        	
	        	 if(hwCount > 0) {
	        		 Dataset<Row> eolhw = sparkSession.sql("select endoflifecycle as `End Of Life - HW`, endofextendedsupport as `End Of Extended Support - HW` from global_temp.eolHWDataDF where lower(concat(vendor,' ',model))='"+hwModel.toLowerCase()+"'");  // where lower(`Server Name`)="+source_type
		        	 if(eolhw.count() > 0) {		        	
			        	 dataset = dataset.join(eolhw);
			         } 
	        	 }
	        	
	         }
	         dataset.printSchema();
	         
	         //------------------------------------------------------//
	         
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
	        
	       /* List<String> numericalHeaders = reportService.getReportNumericalHeaders("Discovery", source_type, "Discovery", siteKey);
	        if(!numericalHeaders.isEmpty()) {
	        	numericalHeaders.stream().forEach((c) -> System.out.println(c));
	        } */
	       
	        /*List<String> headers = reportDao.getReportHeaderForFilter("discovery", source_type.toLowerCase(), request.getReportBy().toLowerCase());	  
	        List<String> actualHeadets = new ArrayList<>();
	        actualHeadets.addAll(Arrays.asList(results.columns()));	      
	        actualHeadets.removeAll(headers);	       
	        results =  results.drop(actualHeadets.stream().toArray(String[]::new));*/
	        return paginate(results, request);
		} catch (Exception e) {
			logger.error("Exception occured while fetching local discoverydata from DF{}", e.getMessage(), e);
		}
	         
		
		 return null;
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
				                 result = ZenfraConstants.SUCCESS;				                
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
							 result = ZenfraConstants.SUCCESS;
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
				return ZenfraConstants.SUCCESS;
				 
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ZenfraConstants.ERROR;
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
				
				return ZenfraConstants.SUCCESS;
			} catch (Exception exp) {
				logger.error("Not able to create dataframe {}",  exp.getMessage(), exp);
			}
			
			return ZenfraConstants.ERROR;
			
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
	    
}
