package com.zenfra.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dataframe.service.DataframeService;





@Component
public class ValidationRuleService {

	@Autowired
	SparkSession sparkSession;
	
	 @Value("${zenfra.path}")
	 private String commonPath;
	 
	 @Autowired
     DataframeService dataframeService;	
	 
	 @Autowired
	 JdbcTemplate jdbc;
	 
	 private ObjectMapper mapper = new ObjectMapper();
	 private JSONParser parser = new  JSONParser();

	
	public  Map<String, List<Object>> getDiscoveryReportValues(String siteKey, String reportBy, String columnName, String category,
			String deviceType, String reportList) {
	
		
		Dataset<Row> dataset = sparkSession.emptyDataFrame();
		Map<String, List<Object>> resutData = new HashMap<>(); 
		
		 
	       
	      
		
		if(reportBy != null && ((reportBy.trim().equalsIgnoreCase("Server") && category.equalsIgnoreCase("Server")) || 
				((reportBy.trim().equalsIgnoreCase("VM") || reportBy.trim().equalsIgnoreCase("Host")) && deviceType.equalsIgnoreCase("Nutanix")) || 
				((reportBy.trim().equalsIgnoreCase("VM") || reportBy.trim().equalsIgnoreCase("Host")) && deviceType.equalsIgnoreCase("Hyper-V")) ||
				((reportBy.trim().equalsIgnoreCase("VM") || reportBy.trim().equalsIgnoreCase("Host")) && deviceType.equalsIgnoreCase("vmware")))) {
			try {
				
				deviceType = deviceType.toLowerCase();
				if(deviceType != null && !deviceType.trim().isEmpty() && deviceType.contains("hyper")) {
					deviceType = deviceType + "-" + reportBy.toLowerCase();
				} else if(deviceType != null && !deviceType.trim().isEmpty() && (deviceType.contains("vmware") && reportBy.toLowerCase().contains("host"))) {
					deviceType = deviceType + "-" + reportBy.toLowerCase();
				} else if(deviceType != null && !deviceType.trim().isEmpty() && (deviceType.contains("nutanix") && reportBy.toLowerCase().contains("host"))) {
					deviceType = deviceType + "-" + reportBy.toLowerCase();
				} else if(deviceType != null && !deviceType.trim().isEmpty() && (deviceType.contains("nutanix") && reportBy.toLowerCase().equalsIgnoreCase("vm"))) {
					deviceType = deviceType + "-" + "guest";
				} 
				
				
				 String viewName = siteKey+"_"+deviceType;					
				 viewName = viewName.replaceAll("-", "").replaceAll("\\s+","");	
				dataset = sparkSession.sql("select * from global_temp." + viewName);	
				
				String dataArray = dataset.toJSON().collectAsList().toString();
				
				try {
					JSONArray dataObj = (JSONArray) parser.parse(dataArray);	
					
					for(int i=0; i<dataObj.size(); i++) {
						JSONObject data = (JSONObject) dataObj.get(i);												
						 Set<String> keys =  data.keySet();								 
							for(String key : keys) { 								
								  if(resutData.containsKey(key.trim())) {									
									  List<Object> values = resutData.get(key.trim());
									  if(!values.contains(data.get(key))) {
										  values.add(data.get(key));
										  values.removeAll(Arrays.asList("", null));
										  resutData.put(key, values);
									  }
								  } else {
									
									  List<Object> values = new ArrayList<>();									 
										  values.add(data.get(key));
										  values.removeAll(Arrays.asList("", null));
										  resutData.put(key, values);
								  }
							  }
							
						
					}
				} catch (Exception e) {					
					e.printStackTrace();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			String actualDfFolderPath = null;
	        String actualDfFilePath = null;
			String dataframePath = commonPath + "Dataframe" + File.separator + "migrationReport" + File.separator + siteKey + File.separator;
			
			File dir = new File(dataframePath);				
			
			for(File file : dir.listFiles()) {
				
			    if(file.isDirectory() && file.getName().equalsIgnoreCase(deviceType)) {
			    	actualDfFolderPath = file.getAbsolutePath();
			    	break;
			    }
		    }
					
			
			if(actualDfFolderPath != null) {
				File d = new File(actualDfFolderPath);			
				for(File file : d.listFiles()) {					
				    if(file.isFile() && file.getName().toLowerCase().contains(category.toLowerCase()) &&  file.getName().toLowerCase().contains(reportBy.toLowerCase()+".json")) { // && file.getName().toLowerCase().contains(category.toLowerCase())
				    	actualDfFilePath = file.getAbsolutePath();
				    	break;
				    }
			    }
				
				if(actualDfFilePath != null) {
					File f = new File(actualDfFilePath);
						String viewName = f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");					
						try {
							dataset = sparkSession.sql("select * from global_temp." + viewName);	
						} catch (Exception e) {
							dataframeService.createDataframeForJsonData(f.getAbsolutePath());
						}				
						
						dataset = sparkSession.sql("select data from global_temp." + viewName);
						
				}
			}
		
			dataset.printSchema();			
			String dataArray = dataset.toJSON().collectAsList().toString();		
			
			try {				
				JSONArray dataObj = mapper.readValue(dataArray, JSONArray.class);
				
				for(int i=0; i<dataObj.size(); i++) {
					LinkedHashMap<String, Object> jsonObject = (LinkedHashMap) dataObj.get(i);					
					List<Object> dataAry = (List<Object>) jsonObject.get("data");	
					
					for(int j=0; j<dataAry.size(); j++) {
						LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) dataAry.get(j);							
						  Set<String> keys =  data.keySet();								 
						  for(String key : keys) { 
							
							  if(resutData.containsKey(key.trim())) {
								
								  List<Object> values = resutData.get(key.trim());
								  if(!values.contains(data.get(key))) {
									  values.add(data.get(key));
									  values.removeAll(Arrays.asList("", null));
									  resutData.put(key, values);
								  }
							  } else {
								
								  List<Object> values = new ArrayList<>();									 
									  values.add(data.get(key));
									  values.removeAll(Arrays.asList("", null));
									  resutData.put(key, values);
							  }
						  }
						
					}
				}
			} catch (Exception e) {					
				e.printStackTrace();
			}
			
		}
		
		
        
		
		//dirPath+siteKey+"_"+reportType+"_"+category+"_"+providers+"_"+reportList+"_"+reportBy+".json";	
		
		 
		return resutData;
	}
	
	public  JSONArray getVR_Compatibility(String siteKey, String columnName, String category,
			String deviceType, String model) throws ParseException {
		
		System.out.println("!!!!! siteKey: " + siteKey);
		System.out.println("!!!!! columnName: " + columnName);
		System.out.println("!!!!! category: " + category);
		System.out.println("!!!!! deviceType: " + deviceType);
		System.out.println("!!!!! model: " + model);
		boolean isServer = false;
		boolean isStorage = false;
		boolean isSwitch = false;
		
		JSONArray resultArray = new JSONArray();
		
		try {
			String query = "select column_names, json_agg(column_values) as column_values from ( \r\n" + 
					"select distinct column_names, column_values from ( \r\n" + 
					"select keys as column_names, data ->> keys as column_values from ( \r\n" + 
					"select data, json_object_keys(data) as keys from ( \r\n" + 
					"select json_array_elements(pidata::json) as data from comp_data where sitekey = 'f0beccad-4a21-41bb-9dec-25d3e632a065' and \r\n" + 
					"pidata is not null and pidata <> '[]' and lower(sourcetype) = lower('AIX') \r\n" + 
					") a \r\n" + 
					") b where keys = 'Cluster_Native Cluster Exp Version'\r\n" + 
					") c where column_values <> 'null' and column_values <> '' and column_values <> 'N/A'\r\n" + 
					"order by column_names, column_values \r\n" + 
					") d group by column_names";
			
			if(isServer) {
				query = "select column_names, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct column_names, column_values from ( \r\n" + 
						"select keys as column_names, data ->> keys as column_values from ( \r\n" + 
						"select data, json_object_keys(data) as keys from ( \r\n" + 
						"select json_array_elements(pidata::json) as data from comp_data where sitekey = '" + siteKey + "' and \r\n" + 
						"pidata is not null and pidata <> '[]' and lower(sourcetype) = lower('" + deviceType + "') \r\n" + 
						") a \r\n" + 
						") b where keys = '" + columnName + "'\r\n" + 
						") c where column_values <> 'null' and column_values <> '' and column_values <> 'N/A'\r\n" + 
						"order by column_names, column_values \r\n" + 
						") d group by column_names";
			}
			
			if(isStorage) {
				query = "select column_names, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct column_names, column_values from ( \r\n" + 
						"select keys as column_names, data ->> keys as column_values from ( \r\n" + 
						"select data, json_object_keys(data) as keys from ( \r\n" + 
						"select json_array_elements(pidata::json) as data from comp_data where sitekey = '" + siteKey + "' and \r\n" + 
						"pidata is not null and pidata <> '[]' and \r\n" + 
						"lower(sourceid) in (select distinct source_id from storage_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "'))\r\n" + 
						") a \r\n" + 
						") b where keys = '" + columnName + "'\r\n" + 
						") c where column_values <> 'null' and column_values <> '' and column_values <> 'N/A'\r\n" + 
						"order by column_names, column_values \r\n" + 
						") d group by column_names";
			}
			
			if(isSwitch) {
				query = "select column_names, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct column_names, column_values from ( \r\n" + 
						"select keys as column_names, data ->> keys as column_values from ( \r\n" + 
						"select data, json_object_keys(data) as keys from ( \r\n" + 
						"select json_array_elements(pidata::json) as data from comp_data where sitekey = '" + siteKey + "' and \r\n" + 
						"pidata is not null and pidata <> '[]' and \r\n" + 
						"lower(sourceid) in (select distinct source_id from switch_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "'))\r\n" + 
						") a \r\n" + 
						") b where keys = '" + columnName + "'\r\n" + 
						") c where column_values <> 'null' and column_values <> '' and column_values <> 'N/A'\r\n" + 
						"order by column_names, column_values \r\n" + 
						") d group by column_names";
			}
			
			List<Map<String,Object>> valueArray = getObjectFromQuery(query); 
			JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("columnValues").toString());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
	}
	
	public List<Map<String, Object>> getObjectFromQuery(String query) {
		List<Map<String, Object>> obj = new ArrayList<>();
		try {

			obj = jdbc.queryForList(query);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

}
