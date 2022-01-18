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
import com.parse.util.Utilities;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;





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
		
		List<String> serverList = new ArrayList<String>(Arrays.asList(ZKModel.getProperty(ZKConstants.SERVER_LIST).split(",")));
		List<String> storageList =  new ArrayList<String>(Arrays.asList(ZKModel.getProperty(ZKConstants.STORAGE_LIST).split(",")));
		List<String> switchList =  new ArrayList<String>(Arrays.asList(ZKModel.getProperty(ZKConstants.SWITCH_LIST).split(",")));
	       
	   if(serverList.contains(deviceType.toLowerCase())) {
		   category = "Server";
	   }else if(storageList.contains(deviceType.toLowerCase())) {
		   category = "Storage";
	   } else if(switchList.contains(deviceType.toLowerCase())) {
		   category = "Switch";
	   }
		
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
				    if(file.isFile() && file.getName().toLowerCase().contains(category.toLowerCase()) &&  file.getName().toLowerCase().contains(reportBy.toLowerCase()+".json") && file.getName().toLowerCase().contains(reportList.toLowerCase())) { // && file.getName().toLowerCase().contains(category.toLowerCase())
				    	actualDfFilePath = file.getAbsolutePath();
				    	break;
				    }
			    }
				System.out.println("-------actualDfFilePath------------ " + actualDfFilePath);
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
		
		JSONArray serverArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SERVER_LIST));
		JSONArray storageArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.STORAGE_LIST));
		JSONArray switchArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SWITCH_LIST));
		
		if(serverArray.contains(deviceType.toLowerCase())) {
			isServer = true;
		}
		if(storageArray.contains(deviceType.toLowerCase())) {
			isStorage = true;
		}
		if(switchArray.contains(deviceType.toLowerCase())) {
			isSwitch = true;
		}
		
		if(deviceType != null && !deviceType.isEmpty() && deviceType.equalsIgnoreCase("vmware")) {
			deviceType = "vmware-host";
		}
		try {
			String query = "select column_names, column_values::jsonb || '[\"Data Not Available\"]' as column_values from ( \r\n" +
					"select column_names, json_agg(column_values) as column_values from ( \r\n" + 
					"select distinct column_names, column_values from ( \r\n" + 
					"select keys as column_names, data ->> keys as column_values from ( \r\n" + 
					"select data, json_object_keys(data) as keys from ( \r\n" + 
					"select json_array_elements(pidata::json) as data from comp_data where sitekey = '" + siteKey + "' and \r\n" + 
					"pidata is not null and pidata <> '[]' and lower(sourcetype) = lower('" + deviceType + "') \r\n" + 
					") a \r\n" + 
					") b where keys = '" + columnName + "'\r\n" + 
					") c where column_values <> 'null' and column_values <> '' and column_values <> 'N/A'\r\n" + 
					"order by column_names, column_values \r\n" + 
					") d group by column_names )e";
			
			if(isServer) {
				query = "select column_names, column_values::jsonb || '[\"Data Not Available\"]' as column_values from ( \r\n" + 
			            "select column_names, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct column_names, column_values from ( \r\n" + 
						"select keys as column_names, data ->> keys as column_values from ( \r\n" + 
						"select data, json_object_keys(data) as keys from ( \r\n" + 
						"select json_array_elements(pidata::json) as data from comp_data where sitekey = '" + siteKey + "' and \r\n" + 
						"pidata is not null and pidata <> '[]' and lower(sourcetype) = lower('" + deviceType + "') \r\n" + 
						") a \r\n" + 
						") b where keys = '" + columnName + "'\r\n" + 
						") c where column_values <> 'null' and column_values <> '' and column_values <> 'N/A'\r\n" + 
						"order by column_names, column_values \r\n" + 
						") d group by column_names ) e";
			}
			
			if(isStorage) {
				query = "select column_names, column_values::jsonb || '[\"Data Not Available\"]' as column_values from ( \r\n " +
						"select column_names, json_agg(column_values) as column_values from ( \r\n" + 
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
						") d group by column_names ) e";
			}
			
			if(isSwitch) {
				query = "select column_names, column_values::jsonb || '[\"Data Not Available\"]' as column_values from ( \r\n " +
						"select column_names, json_agg(column_values) as column_values from ( \r\n" + 
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
						") d group by column_names )e";
			}
			
			List<Map<String,Object>> valueArray = getObjectFromQuery(query); 
			JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
	}
	
	public  JSONArray getVR_MigrationMethod(String siteKey, String columnName, String category,
			String deviceType) throws ParseException {
		
		System.out.println("!!!!! siteKey: " + siteKey);
		System.out.println("!!!!! columnName: " + columnName);
		System.out.println("!!!!! category: " + category);
		System.out.println("!!!!! deviceType: " + deviceType);
		JSONParser parser = new JSONParser();
		
		boolean isServer = false;
		boolean isStorage = false;
		boolean isSwitch = false;
		
		JSONArray resultArray = new JSONArray();
		
		JSONArray serverArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SERVER_LIST));
		JSONArray storageArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.STORAGE_LIST));
		JSONArray switchArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SWITCH_LIST));
		
		if(serverArray.contains(deviceType.toLowerCase())) {
			isServer = true;
		}
		if(storageArray.contains(deviceType.toLowerCase())) {
			isStorage = true;
		}
		if(switchArray.contains(deviceType.toLowerCase())) {
			isSwitch = true;
		}
		
		try {
			String query = "select methods, json_agg(data_value) as data_value from (\r\n" + 
					"select methods, (case when data_value is null or data_value = '' or data_value = 'null' then 'Not Applicable' else data_value end) as data_value from (\r\n" + 
					"select distinct methods, dt.data_value from (\r\n" + 
					"select methods from migration_method where lower(device) = lower('" + deviceType + "') \r\n" + 
					"union all\r\n" + 
					"select 'Server Name' as methods \r\n" + 
					"union all \r\n" + 
					"select 'Source' as methods \r\n" + 
					"union all \r\n" + 
					"select 'OS Version' as methods\r\n" + 
					") a\r\n" + 
					"LEFT JOIN (select data, keys, data ->> keys as data_value from (\r\n" + 
					"select  data, json_object_keys(data::json) as keys from (\r\n" + 
					"select data from (\r\n" + 
					"select data, row_number() over(partition by data) as row_num from ( \r\n" + 
					"select server_name::jsonb || data::jsonb as data from (\r\n" + 
					"select json_build_object('Server Name', source_id, 'OS Version', os_version, 'Source', lower(source)) as server_name, \r\n" + 
					"json_array_elements(data::json) as data from migration_data \r\n" + 
					"where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "') \r\n" + 
					") b \r\n" + 
					") c\r\n" + 
					") d where row_num = 1\r\n" + 
					") a \r\n" + 
					") b ) dt on lower(dt.keys) = lower(a.methods) \r\n" + 
					") b \r\n" + 
					") d group by methods order by methods";
			
			if(isServer) {
				
				query = "select methods, json_agg(data_value) as data_value from (\r\n" + 
						"select methods, (case when data_value is null or data_value = '' or data_value = 'null' then 'Not Applicable' else data_value end) as data_value from (\r\n" + 
						"select distinct methods, dt.data_value from (\r\n" + 
						"select methods from migration_method where lower(device) = lower('" + deviceType + "') \r\n" + 
						"union all\r\n" + 
						"select 'Server Name' as methods \r\n" + 
						"union all \r\n" + 
						"select 'Source' as methods \r\n" + 
						"union all \r\n" + 
						"select 'OS Version' as methods\r\n" + 
						") a\r\n" + 
						"LEFT JOIN (select data, keys, data ->> keys as data_value from (\r\n" + 
						"select  data, json_object_keys(data::json) as keys from (\r\n" + 
						"select data from (\r\n" + 
						"select data, row_number() over(partition by data) as row_num from ( \r\n" + 
						"select server_name::jsonb || data::jsonb as data from (\r\n" + 
						"select json_build_object('Server Name', source_id, 'OS Version', os_version, 'Source', lower(source)) as server_name, \r\n" + 
						"json_array_elements(data::json) as data from migration_data \r\n" + 
						"where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "') \r\n" + 
						") b \r\n" + 
						") c\r\n" + 
						") d where row_num = 1\r\n" + 
						") a \r\n" + 
						") b ) dt on lower(dt.keys) = lower(a.methods) \r\n" + 
						") b \r\n" + 
						") d group by methods order by methods";
			}
			
			if(isStorage) {
				query = "select methods, json_agg(data_value) as data_value from (\r\n" + 
						"select methods, (case when data_value is null or data_value = '' or data_value = 'null' then 'Not Applicable' else data_value end) as data_value from (\r\n" + 
						"select distinct methods, dt.data_value from (\r\n" + 
						"select methods from migration_method where lower(device) = lower('" + deviceType + "') \r\n" + 
						"union all\r\n" + 
						"select 'Server Name' as methods \r\n" + 
						"union all \r\n" + 
						"select 'Source' as methods \r\n" + 
						"union all \r\n" + 
						"select 'OS Version' as methods\r\n" + 
						") a\r\n" + 
						"LEFT JOIN (select data, keys, data ->> keys as data_value from (\r\n" + 
						"select  data, json_object_keys(data::json) as keys from (\r\n" + 
						"select data from (\r\n" + 
						"select data, row_number() over(partition by data) as row_num from ( \r\n" + 
						"select server_name::jsonb || data::jsonb as data from (\r\n" + 
						"select json_build_object('Server Name', source_id, 'OS Version', os_version, 'Source', lower(source)) as server_name, \r\n" + 
						"json_array_elements(data::json) as data from migration_data \r\n" + 
						"where site_key = '" + siteKey + "' and lower(source_id) in (select distinct source_id from storage_discovery where site_key = '" + siteKey + "'  \r\n" + 
						"and lower(source_type) = lower('" + deviceType + "')) \r\n" + 
						") b \r\n" + 
						") c\r\n" + 
						") d where row_num = 1\r\n" + 
						") a \r\n" + 
						") b ) dt on lower(dt.keys) = lower(a.methods) \r\n" + 
						") b \r\n" + 
						") d group by methods order by methods";
			}
			
			if(isSwitch) {
				query = "select methods, json_agg(data_value) as data_value from (\r\n" + 
						"select methods, (case when data_value is null or data_value = '' or data_value = 'null' then 'Not Applicable' else data_value end) as data_value from (\r\n" + 
						"select distinct methods, dt.data_value from (\r\n" + 
						"select methods from migration_method where lower(device) = lower('" + deviceType + "') \r\n" + 
						"union all\r\n" + 
						"select 'Server Name' as methods \r\n" + 
						"union all \r\n" + 
						"select 'Source' as methods \r\n" + 
						"union all \r\n" + 
						"select 'OS Version' as methods\r\n" + 
						") a\r\n" + 
						"LEFT JOIN (select data, keys, data ->> keys as data_value from (\r\n" + 
						"select  data, json_object_keys(data::json) as keys from (\r\n" + 
						"select data from (\r\n" + 
						"select data, row_number() over(partition by data) as row_num from ( \r\n" + 
						"select server_name::jsonb || data::jsonb as data from (\r\n" + 
						"select json_build_object('Server Name', source_id, 'OS Version', os_version, 'Source', lower(source)) as server_name, \r\n" + 
						"json_array_elements(data::json) as data from migration_data \r\n" + 
						"where site_key = '" + siteKey + "' and lower(source_id) in (select distinct source_id from switch_discovery where site_key = '" + siteKey + "'  \r\n" + 
						"and lower(source_type) = lower('" + deviceType + "')) \r\n" + 
						") b \r\n" + 
						") c\r\n" + 
						") d where row_num = 1\r\n" + 
						") a \r\n" + 
						") b ) dt on lower(dt.keys) = lower(a.methods) \r\n" + 
						") b \r\n" + 
						") d group by methods order by methods";
			}
			
			System.out.println("!!!!! query: " + query);
			List<Map<String,Object>> valueArray = getObjectFromQuery(query); 
			//JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("data_value").toString());
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
