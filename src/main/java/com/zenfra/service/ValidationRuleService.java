package com.zenfra.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.CommonFunctions;





@Component
public class ValidationRuleService {

	@Autowired
	SparkSession sparkSession;
	
	private String commonPath;
	 @PostConstruct
	 public void init() {
		 commonPath = ZKModel.getProperty(ZKConstants.DATAFRAME_PATH);		
	  }
	 
	 @Autowired
     DataframeService dataframeService;	
	 
	 @Autowired
	 JdbcTemplate jdbc;
	 
	 @Autowired
	 CommonFunctions commonFunctions;
	 
	 private ObjectMapper mapper = new ObjectMapper();
	 private JSONParser parser = new  JSONParser();

	
	public  Map<String, List<Object>> getDiscoveryReportValues(String siteKey, String reportBy, String columnName, String category,
			String deviceType, String reportList) {
	
		

		  
		Dataset<Row> dataset = sparkSession.emptyDataFrame();
		Map<String, List<Object>> resutData = new HashMap<>(); 
		
		List<String> serverList = commonFunctions.convertToArrayList(ZKModel.getProperty(ZKConstants.SERVER_LIST), ",");
		List<String> storageList =  commonFunctions.convertToArrayList(ZKModel.getProperty(ZKConstants.STORAGE_LIST), ",");
		List<String> switchList = commonFunctions.convertToArrayList(ZKModel.getProperty(ZKConstants.SWITCH_LIST), ","); 
	       	
	   if(serverList.contains(deviceType.toLowerCase())) {
		   category = "Server";
	   }else if(storageList.contains(deviceType.toLowerCase())) {
		   category = "Storage";
	   } else if(switchList.contains(deviceType.toLowerCase())) {
		   category = "Switch";
	   }
		
	   System.out.println("------category---------" + category);	
	   
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
		
		if(deviceType != null && !deviceType.isEmpty() && deviceType.equalsIgnoreCase("7-mode")) {
			deviceType = "7-mode server";
		}
		if(deviceType != null && !deviceType.isEmpty() && deviceType.equalsIgnoreCase("c-mode")) {
			deviceType = "c-mode server";
		}
		try {
			JSONArray defaultArray = getDefaultPIData(deviceType, model);
			String query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
					"select distinct keys, column_values from ( \r\n" + 
					"select keys, data ->> keys as column_values from (\r\n" + 
					"select data, json_object_keys(data) as keys from (\r\n" + 
					"select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( select site_key, source_type, source_id, source_id as server_name, coalesce(metricsdate, 'Data Not Available') as metrics_date, \r\n" + 
					"coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(pidata, '" + defaultArray + "') as pidata, coalesce(vm_name, '') as vm_name, \r\n" + 
					"coalesce(vcenter, '') as vcenter,  \r\n" + 
					"coalesce(os_version, 'Data Not Available') as os_version, row_number() over(partition by source_id) as row_num  from (  \r\n" + 
					"select site_key, source_type, coalesce(server_name, source_id) as source_id, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter, \r\n" + 
					"(case when lower(source_type) = 'vmware' then esx_version else os_version end) as os_version from (  \r\n" + 
					"select site_key, source_type, source_id, json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,  \r\n" + 
					"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name, \r\n" + 
					"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter, \r\n" + 
					"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version, \r\n" + 
					"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version, \r\n" + 
					"row_number() over(partition by site_key, server_name order by log_date desc)  as row_num  \r\n" + 
					"from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "')  \r\n" + 
					") a where row_num = 1  \r\n" + 
					") ld  \r\n" + 
					"LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data  \r\n" + 
					"where sitekey = '" + siteKey + "' and lower(sourcetype) = lower('" + deviceType + "') and lower(destinationtype) = lower('" + model + "') \r\n" + 
					"and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('" + model + "') and lower(device) = lower('" + deviceType + "'))) cd on  \r\n" + 
					"cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id) order by source_id ) b where row_num = 1 \r\n" + 
					") e ) f where keys = '" + columnName + "' order by keys ) g ) h group by keys";
			
			if(isServer) {
				//JSONArray defaultArray = getDefaultPIData(deviceType, model);
				query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct keys, column_values from ( \r\n" + 
						"select keys, data ->> keys as column_values from (\r\n" + 
						"select data, json_object_keys(data) as keys from (\r\n" + 
						"select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( select site_key, source_type, source_id, source_id as server_name, coalesce(metricsdate, 'Data Not Available') as metrics_date, \r\n" + 
						"coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(pidata, '" + defaultArray + "') as pidata, coalesce(vm_name, '') as vm_name, \r\n" + 
						"coalesce(vcenter, '') as vcenter,  \r\n" + 
						"coalesce(os_version, 'Data Not Available') as os_version, row_number() over(partition by source_id) as row_num  from (  \r\n" + 
						"select site_key, source_type, coalesce(server_name, source_id) as source_id, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter, \r\n" + 
						"(case when lower(source_type) = 'vmware' then esx_version else os_version end) as os_version from (  \r\n" + 
						"select site_key, source_type, source_id, json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,  \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name, \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter, \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version, \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version, \r\n" + 
						"row_number() over(partition by site_key, server_name order by log_date desc)  as row_num  \r\n" + 
						"from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "')  \r\n" + 
						") a where row_num = 1  \r\n" + 
						") ld  \r\n" + 
						"LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data  \r\n" + 
						"where sitekey = '" + siteKey + "' and lower(sourcetype) = lower('" + deviceType + "') and lower(destinationtype) = lower('" + model + "') \r\n" + 
						"and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('" + model + "') and lower(device) = lower('" + deviceType + "'))) cd on  \r\n" + 
						"cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id) order by source_id ) b where row_num = 1 \r\n" + 
						") e ) f where keys = '" + columnName + "' order by keys ) g ) h group by keys";
			}
			
			if(isStorage) {
				
				defaultArray = getDefaultPIData("project", model);
				query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct keys, column_values from ( \r\n" + 
						"select keys, data ->> keys as column_values from ( \r\n" + 
						"select data, json_object_keys(data) as keys from ( \r\n" + 
						"select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( \r\n" +
						"select site_key, coalesce(source_type, 'Not Discovered') as source_type, source_id, coalesce(metricsdate, 'Data Not Available') as metricsdate,\r\n" + 
						"coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter,\r\n" + 
						"coalesce(pidata, '" + defaultArray + "') as pidata, coalesce(os_version, 'Data Not Available') as os_version  from (\r\n" + 
						"select site_key, source_type, source_id, vm_name, vcenter, os_version from (\r\n" + 
						"select site_key, ld.source_type, sd.source_id, vm_name, vcenter, (case when lower(ld.source_type) = 'vmware' then esx_version else os_version end) as os_version, \r\n" + 
						"row_number() over(partition by site_key, sd.source_id order by log_date desc)  as row_num\r\n" + 
						"from storage_discovery sd \r\n" + 
						"LEFT JOIN (select source_id, source_type, coalesce(server_name, source_id) as server_name, coalesce(vm_name, '') as vm_name,\r\n" + 
						"coalesce(vcenter, '') as vcenter, os_version, esx_version from (select source_id, lower(source_type) as source_type, \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,\r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name, \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter, \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version, \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version, \r\n" + 
						"row_number() over(partition by source_id order by log_date desc) as row_num from local_discovery \r\n" + 
						"where site_key = '" + siteKey + "') a ) ld on lower(ld.source_id) = lower(sd.source_id)\r\n" + 
						"where site_key = '" + siteKey + "' \r\n" + 
						"and lower(sd.source_type) = lower('" + deviceType + "')\r\n" + 
						") a where row_num = 1\r\n" + 
						") ld\r\n" + 
						"LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data\r\n" + 
						"where sitekey = '" + siteKey + "' and lower(destinationtype) = lower('" + model + "') \r\n" +
						"and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('" + model + "'))) cd on\r\n" + 
						"cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id)\r\n" + 
						"order by source_id ) e ) f ) g where keys = '" + columnName + "' order by keys) h ) k group by keys";
			}
			
			if(isSwitch) {
				
				defaultArray = getDefaultPIData("project", model);
				query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct keys, column_values from ( \r\n" + 
						"select keys, data ->> keys as column_values from (\r\n" + 
						"select data, json_object_keys(data) as keys from (\r\n" + 
						"select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( \r\n" + 
						"select site_key, coalesce(source_type, 'Not Discovered') as source_type, source_id, coalesce(metricsdate, 'Data Not Available') as metricsdate,  \r\n" + 
						"coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter,  \r\n" + 
						"coalesce(pidata, '" + defaultArray + "') as pidata, coalesce(os_version, 'Data Not Available') as os_version  from (  \r\n" + 
						"select site_key, source_type, source_id, vm_name, vcenter, os_version from (  \r\n" + 
						"select site_key, ld.source_type, sd.source_id, vm_name, vcenter, (case when (ld.source_type) = 'vmware' then esx_version else os_version end) as os_version, \r\n" + 
						"row_number() over(partition by site_key, sd.source_id order by log_date desc)  as row_num  \r\n" + 
						"from switch_discovery sd  \r\n" + 
						"LEFT JOIN (select source_id, source_type, coalesce(server_name, source_id) as server_name, coalesce(vm_name, '') as vm_name,  \r\n" + 
						"coalesce(vcenter, '') as vcenter, os_version, esx_version from (select source_id, lower(source_type) as source_type,  \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,  \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name,  \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter,  \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version,  \r\n" + 
						"json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version,  \r\n" + 
						"row_number() over(partition by source_id order by log_date desc) as row_num from local_discovery  \r\n" + 
						"where site_key = '" + siteKey + "') a ) ld on lower(ld.source_id) = lower(sd.source_id)  \r\n" + 
						"where site_key = '" + siteKey + "'  \r\n" + 
						"and lower(sd.source_type) = lower('" + deviceType + "')  \r\n" + 
						") a where row_num = 1  \r\n" + 
						") ld  \r\n" + 
						"LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data  \r\n" + 
						"where sitekey = '" + siteKey + "' and  \r\n" + 
						" lower(destinationtype) = lower('" + model + "') \r\n" +
						"and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('" + model + "'))) cd on  \r\n" + 
						"cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id)  \r\n" + 
						"where source_id != ''  \r\n" + 
						"order by source_id\r\n" + 
						") e ) f ) g where keys = '" + columnName + "' order by keys) h ) k group by keys";
			}
			
			System.out.println("!!!! compatibility validation query: " + query);
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
			
			String vmware_os_column = "OS Version";
			if(deviceType.equalsIgnoreCase("vmware")) {
				vmware_os_column = "ESX Version";
			} else {
				vmware_os_column = "OS Version";
			}
			
			String query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
					"select distinct keys, column_values from (  \r\n" + 
					"select keys, data::json ->> keys as column_values from ( \r\n" + 
					"select data, json_object_keys(data::json) as keys from ( \r\n" + 
					"select replace(data, '}{', ',') as data from (\r\n" + 
					"select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from ( \r\n" + 
					"select source_id, source_type, os_version, json_array_elements(data::json) as data from ( \r\n" + 
					"select source_id, source_type, os_version, replace(replace(data, '}, {', ','), '},{', ',') as data from ( \r\n" + 
					"select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n" + 
					"coalesce(source, 'Data Not Available') as source, json_agg(data)::text as data, vcenter from (  \r\n" + 
					"select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data, vcenter from (  \r\n" + 
					"select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values, vcenter from (  \r\n" + 
					"select device, methods, source_id, site_key, os_version, source_type, source,  \r\n" + 
					"json_array_elements(data::json) ->> methods as column_values, vcenter from (  \r\n" + 
					"select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, \r\n" + 
					"coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data, vcenter  from migration_method mm  \r\n" + 
					"LEFT JOIN (select mld.source_id, mld.source, mld.source as source_type, mld.os_version, mld.site_key, mld.vcenter, md.data from (  \r\n" + 
					"select site_key, source_id, source, os_version, coalesce(vcenter, '') as vcenter from ( \r\n" + 
					"select site_key, source_id, lower(source_type) as source, json_array_elements(data_temp::json) ->> '" + vmware_os_column + "' as os_version,  \r\n" + 
					"json_array_elements(data_temp::json) ->> 'vCenter' as vcenter, row_number() over(partition by source_id order by log_date desc) as row_num \r\n" + 
					"from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "') \r\n" + 
					") a where row_num = 1 \r\n" + 
					") mld \r\n" + 
					"LEFT JOIN (select source_id, site_key, os_version, source_type, source, data from migration_data \r\n" + 
					"where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "')) md on md.source_id = mld.source_id)  \r\n" + 
					"dt on lower(dt.source_type) = lower(mm.device)  \r\n" + 
					"where lower(mm.device) = lower('" + deviceType + "') and lower(method_name) in (select lower(method_name) from migration_method where lower(device) = lower('" + deviceType + "')) and \r\n" + 
					"lower(mode_name) in (select lower(mode_name) from migration_method where lower(device) = lower('" + deviceType + "'))  \r\n" + 
					") a  \r\n" + 
					") b  \r\n" + 
					") c  \r\n" + 
					") d where source_id != '' group by source_id, site_key, os_version, source_type, source, vcenter  \r\n" + 
					"order by source_id \r\n" + 
					") e ) f ) d ) g ) h ) i where keys = '" + columnName + "' ) j order by keys ) k group by keys";
			
			if(isServer) {
				
				query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct keys, column_values from (  \r\n" + 
						"select keys, data::json ->> keys as column_values from ( \r\n" + 
						"select data, json_object_keys(data::json) as keys from ( \r\n" + 
						"select replace(data, '}{', ',') as data from (\r\n" + 
						"select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from ( \r\n" + 
						"select source_id, source_type, os_version, json_array_elements(data::json) as data from ( \r\n" + 
						"select source_id, source_type, os_version, replace(replace(data, '}, {', ','), '},{', ',') as data from ( \r\n" + 
						"select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n" + 
						"coalesce(source, 'Data Not Available') as source, json_agg(data)::text as data, vcenter from (  \r\n" + 
						"select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data, vcenter from (  \r\n" + 
						"select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values, vcenter from (  \r\n" + 
						"select device, methods, source_id, site_key, os_version, source_type, source,  \r\n" + 
						"json_array_elements(data::json) ->> methods as column_values, vcenter from (  \r\n" + 
						"select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, \r\n" + 
						"coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data, vcenter  from migration_method mm  \r\n" + 
						"LEFT JOIN (select mld.source_id, mld.source, mld.source as source_type, mld.os_version, mld.site_key, mld.vcenter, md.data from (  \r\n" + 
						"select site_key, source_id, source, os_version, coalesce(vcenter, '') as vcenter from ( \r\n" + 
						"select site_key, source_id, lower(source_type) as source, json_array_elements(data_temp::json) ->> '" + vmware_os_column + "' as os_version,  \r\n" + 
						"json_array_elements(data_temp::json) ->> 'vCenter' as vcenter, row_number() over(partition by source_id order by log_date desc) as row_num \r\n" + 
						"from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "') \r\n" + 
						") a where row_num = 1 \r\n" + 
						") mld \r\n" + 
						"LEFT JOIN (select source_id, site_key, os_version, source_type, source, data from migration_data \r\n" + 
						"where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType + "')) md on md.source_id = mld.source_id)  \r\n" + 
						"dt on lower(dt.source_type) = lower(mm.device)  \r\n" + 
						"where lower(mm.device) = lower('" + deviceType + "') and lower(method_name) in (select lower(method_name) from migration_method where lower(device) = lower('" + deviceType + "')) and \r\n" + 
						"lower(mode_name) in (select lower(mode_name) from migration_method where lower(device) = lower('" + deviceType + "'))  \r\n" + 
						") a  \r\n" + 
						") b  \r\n" + 
						") c  \r\n" + 
						") d where source_id != '' group by source_id, site_key, os_version, source_type, source, vcenter  \r\n" + 
						"order by source_id \r\n" + 
						") e ) f ) d ) g ) h ) i where keys = '" + columnName + "') j order by keys ) k group by keys";
			}
			
			if(isStorage) {
				
				query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
						"select distinct keys, column_values from (  \r\n" + 
						"select keys, data::json ->> keys as column_values from ( \r\n" + 
						"select data, json_object_keys(data::json) as keys from ( \r\n" + 
						"select replace(data, '}{', ',') as data from (\r\n" + 
						"select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from ( \r\n" + 
						"select source_id, source_type, os_version, json_array_elements(data::json) as data from ( \r\n" + 
						"select source_id, source_type, os_version, replace(replace(data::text, '}, {', ','), '},{', ',') as data from (\r\n" + 
						"select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n" + 
						"coalesce(source, 'Data Not Available') as source, json_agg(data) as data from ( \r\n" + 
						"select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data from ( \r\n" + 
						"select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values from ( \r\n" + 
						"select device, methods, source_id, site_key, os_version, source_type, source, \r\n" + 
						"json_array_elements(data::json) ->> methods as column_values from ( \r\n" + 
						"select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data  from migration_method mm \r\n" + 
						"LEFT JOIN (select b.site_key, b.source_id, b.os_version, b.source_type, md.source, md.data from ( \r\n" + 
						"select site_key, source_id, os_version, source_type, vcenter from (select sd.source_id, sd.site_key, data, ld.os_version, ld.source_type, ld.vcenter, \r\n" + 
						"row_number() over(partition by sd.source_id order by log_date desc) as row_num \r\n" + 
						"from storage_discovery sd \r\n" + 
						"LEFT JOIN (select site_key, source_id, source_type, coalesce(os_version, 'Data Not Available') as os_version,  \r\n" + 
						"coalesce(vcenter, 'Data Not Available') as vcenter from ( \r\n" + 
						"select site_key,source_id, lower(source_type) as source_type, json_array_elements(data_temp::json) ->> 'OS Version' as os_version, \r\n" + 
						"json_array_elements(data_temp::json) ->> 'vCenter' as vcenter,  \r\n" + 
						"row_number() over(partition by source_id order by log_date desc) as row_num \r\n" + 
						"from local_discovery where site_key = '" + siteKey + "' \r\n" + 
						") a1 where row_num = 1) ld on lower(ld.source_id) = lower(sd.source_id) and ld.site_key = sd.site_key \r\n" + 
						"where sd.site_key = '" + siteKey + "' and \r\n" + 
						"lower(sd.source_type) = lower('" + deviceType + "')) a  where row_num = 1 \r\n" + 
						") b \r\n" + 
						"LEFT JOIN(select source_id, site_key, os_version, source_type, source, data from migration_data \r\n" + 
						"where site_key = '" + siteKey + "') md on lower(md.source_id) = lower(b.source_id)) \r\n" + 
						"dt on 1 = 1 \r\n" + 
						" and lower(method_name) in (select lower(method_name) from migration_method) and mode_name in (select lower(mode_name) from migration_method)) a \r\n" + 
						") b \r\n" + 
						") c \r\n" + 
						") d where source_id != '' group by source_id, site_key, os_version, source_type, source \r\n" + 
						"order by source_id \r\n" + 
						") e ) f ) d ) g ) h ) i where keys = '" + columnName + "' ) j order by keys ) k group by keys ";
			}
			
			if(isSwitch) {
				query = "select keys, json_agg(column_values) as column_values from (  \r\n" + 
						"select distinct keys, column_values from (  \r\n" + 
						"select keys, data::json ->> keys as column_values from (  \r\n" + 
						"select data, json_object_keys(data::json) as keys from ( \r\n" + 
						"select replace(data, '}{', ',') as data from (  \r\n" + 
						"select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from (  \r\n" + 
						"select source_id, source_type, os_version, json_array_elements(data::json) as data from (  \r\n" + 
						"select source_id, source_type, os_version, replace(replace(data::text, '}, {', ','), '},{', ',') as data from (\r\n" + 
						"select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n" + 
						"coalesce(source, 'Data Not Available') as source, json_agg(data) as data from ( \r\n" + 
						"select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data from ( \r\n" + 
						"select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values from ( \r\n" + 
						"select device, methods, source_id, site_key, os_version, source_type, source, \r\n" + 
						"json_array_elements(data::json) ->> methods as column_values from ( \r\n" + 
						"select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data  from migration_method mm \r\n" + 
						"LEFT JOIN (select b.site_key, b.source_id, b.os_version, b.source_type, md.source, md.data from ( \r\n" + 
						"select site_key, source_id, os_version, source_type, vcenter from (select sd.source_id, sd.site_key, data, ld.os_version, ld.source_type, ld.vcenter, \r\n" + 
						"row_number() over(partition by sd.source_id order by log_date desc) as row_num \r\n" + 
						"from switch_discovery sd \r\n" + 
						"LEFT JOIN (select site_key, source_id, source_type, coalesce(os_version, 'Data Not Available') as os_version,  \r\n" + 
						"coalesce(vcenter, 'Data Not Available') as vcenter from ( \r\n" + 
						"select site_key,source_id, lower(source_type) as source_type, json_array_elements(data_temp::json) ->> 'OS Version' as os_version, \r\n" + 
						"json_array_elements(data_temp::json) ->> 'vCenter' as vcenter,  \r\n" + 
						"row_number() over(partition by source_id order by log_date desc) as row_num \r\n" + 
						"from local_discovery where site_key = '" + siteKey + "' \r\n" + 
						") a1 where row_num = 1) ld on lower(ld.source_id) = lower(sd.source_id) and ld.site_key = sd.site_key \r\n" + 
						"where sd.site_key = '" + siteKey + "' and \r\n" + 
						"lower(sd.source_type) = '" + deviceType.toLowerCase() + "') a  where row_num = 1 \r\n" + 
						") b \r\n" + 
						"LEFT JOIN(select source_id, site_key, os_version, source_type, source, data from migration_data \r\n" + 
						"where site_key = '" + siteKey + "') md on lower(md.source_id) = lower(b.source_id)) \r\n" + 
						"dt on 1 = 1 \r\n" + 
						" and lower(method_name) in (select lower(method_name) from migration_method) and lower(mode_name) in (select lower(mode_name) from migration_method)) a \r\n" + 
						") b \r\n" + 
						") c \r\n" + 
						") d where source_id != '' group by source_id, site_key, os_version, source_type, source \r\n" + 
						"order by source_id \r\n" + 
						") e ) f ) d ) g ) h ) i where keys = '" + columnName + "') j order by keys ) k group by keys";
			}
			
			System.out.println("!!!!! query: " + query);
			List<Map<String,Object>> valueArray = getObjectFromQuery(query); 
			//JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
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
	
	@SuppressWarnings("unchecked")
	private JSONArray getDefaultPIData(String device, String destinationType) {
		
		JSONParser parser = new JSONParser();
		JSONArray resultArray = new JSONArray();
		
		try {
			String query = "select json_agg(column_name) as column_name from ( \r\n" +
					"select json_object_agg(column_name,'Data Not Available') as column_name from (\r\n" + 
					"select distinct(column_name), seq::integer from report_columns where report_name = 'Compatibility' and lower(device_type) = '" + device.toLowerCase() + "' \r\n" + 
					"order by seq::integer\r\n" + 
					") a ) b";
			
			System.out.println("!!!!! query: " + query);
			List<Map<String,Object>> valueArray = getObjectFromQuery(query); 
			//JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_name").toString());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
	} 
	
/*public JSONArray getVR_PrivilledgeData(String siteKey, String columnName) {
		
		JSONArray resultArray = new JSONArray();
		
		try {
			
			String query = "select keys, json_agg(column_values) as column_values from (\r\n" + 
					"select distinct keys, column_values from (\r\n" + 
					"select concat('Server Data~', keys) as keys, json_array_elements(data::json) ->> keys as column_values from ( \r\n" + 
					"select data, json_object_keys(data_object) as keys from (\r\n" + 
					"select data, json_array_elements(data::json) as data_object from privillege_data  \r\n" + 
					"where site_key = '" + siteKey + "'  \r\n" + 
					") a \r\n" + 
					") b\r\n" + 
					"union all \r\n" + 
					"select concat(source_name, '~', keys) as keys, data::json ->> keys as column_values from ( \r\n" + 
					"select source_name, data, json_object_keys(data::json) as keys from (  \r\n" + 
					"select sd.source_id, s.source_name, primary_key_value, replace(data, '.0\",', '\",') as data, row_number() over(partition by sd.source_id, primary_key_value order by update_time desc) as row_num \r\n" +
					"from source_data sd  \r\n" + 
					"LEFT JOIN source s on s.source_id = sd.source_id and s.site_key = '" + siteKey + "'  \r\n" + 
					"where sd.site_key = '" + siteKey + "' and sd.source_id in ( \r\n" + 
					"select source_id from (  \r\n" + 
					"select source_id, json_array_elements(fields::json) ->> 'primary' as is_primary,  \r\n" + 
					"json_array_elements(fields::json) ->> 'primaryKey' as primary_key from source  \r\n" + 
					"where site_key = '" + siteKey + "'  \r\n" + 
					") a where is_primary::boolean = true and primary_key = 'userName' ) \r\n" + 
					") b  \r\n" + 
					"where row_num = 1  \r\n" + 
					") c \r\n" + 
					") d where keys <> 'sourceId' and keys <> 'siteKey'\r\n" + 
					") e where keys = '" + columnName + "' \r\n" + 
					"group by keys ";
			
			System.out.println("!!!!! query: " + query);
			List<Map<String,Object>> valueArray = getObjectFromQuery(query); 
			//JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
		
	}
*/	
	public JSONArray getVR_PrivilledgeData(String siteKey, String columnName) {
		
		JSONArray resultArray = new JSONArray();
		
		try {
			
			String query = "select keys, json_agg(column_values) as column_values from (\r\n" + 
					"select distinct keys, column_values from (\r\n" + 
					"select concat('Server Data~', keys) as keys, json_array_elements(data::json) ->> keys as column_values from ( \r\n" + 
					"select data, json_object_keys(data_object) as keys from (\r\n" + 
					"select data, json_array_elements(data::json) as data_object from privillege_data  \r\n" + 
					"where site_key = '" + siteKey + "'  \r\n" + 
					") a \r\n" + 
					") b\r\n" + 
					"union all \r\n" + 
					"select concat(source_name, '~', keys) as keys, data::json ->> keys as column_values from ( \r\n" + 
					"select source_name, data, json_object_keys(data::json) as keys from (  \r\n" + 
					"select sd.source_id, s.source_name, primary_key_value, replace(data, '.0\",', '\",') as data, row_number() over(partition by sd.source_id, primary_key_value order by update_time desc) as row_num \r\n" +
					"from source_data sd  \r\n" + 
					"LEFT JOIN source s on s.source_id = sd.source_id and s.site_key = '" + siteKey + "'  \r\n" + 
					"where sd.site_key = '" + siteKey + "' and sd.source_id in ( \r\n" + 
					"select source_id from (  \r\n" + 
					"select source_id, json_array_elements(fields::json) ->> 'primary' as is_primary,  \r\n" + 
					"json_array_elements(fields::json) ->> 'primaryKey' as primary_key from source  \r\n" + 
					"where site_key = '" + siteKey + "'  \r\n" + 
					") a where is_primary::boolean = true and primary_key = 'userName' ) \r\n" + 
					") b  \r\n" + 
					"where row_num = 1  \r\n" + 
					") c \r\n" + 
					") d where keys <> 'sourceId' and keys <> 'siteKey'\r\n" + 
					") e where keys = '" + columnName + "' \r\n" + 
					"group by keys ";
			
			System.out.println("!!!!! query: " + query);
			List<Map<String,Object>> valueArray = getObjectFromQuery(query); 
			//JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
		
	}

	public JSONArray getCloudCostReportValues(String siteKey, String columnName, String category, String deviceType, String report_by) {
		JSONArray resultData = new JSONArray();		
		try {
			String inputDeviceType = deviceType;
			Dataset<Row> dataset = sparkSession.emptyDataFrame();
			String viewName = siteKey.replaceAll("-", "").replaceAll("\\s+", "")+"_cloudcost";	
			
			if (deviceType.equalsIgnoreCase("All")) {
           	 deviceType = " lcase(`Server Type`) in ('windows','linux', 'vmware')";           	
            } else {           
           	 deviceType = "lcase(`Server Type`)='" + deviceType.toLowerCase() + "'";           	 
            }
			

          	 if(category.toLowerCase().equalsIgnoreCase("AWS Instances")) {
          		if (deviceType.equalsIgnoreCase("All")) {
                  	 deviceType = " lcase(`Server Type`)='ec2'  and lcase(`OS Name`) in ('windows','linux', 'vmware')";           	
                   } else {           
                	  deviceType = "lcase(`Server Type`)='ec2'  and lcase(`OS Name`) = '"+inputDeviceType.toLowerCase()+"'";     	 
                   }
       		  
       	     }
			
			if(report_by.equalsIgnoreCase("All")) {
				report_by = "report_by in ('Physical Servers','AWS Instances','Custom Excel Data')";
			} else {
				report_by = "report_by='"+report_by+"'";
			}
			
			try {
				dataset = sparkSession.sql("select `"+columnName+"` from global_temp." + viewName + " where "+deviceType + " and "+report_by+"").distinct();	
			} catch (Exception e) {
				ServerSideGetRowsRequest request = new ServerSideGetRowsRequest();
				request.setSiteKey(siteKey);
				request.setReportType("optimization");
				request.setDeviceType("All");
				request.setCategoryOpt("All");
				request.setSource("All");
				request.setCategory("price");				
				dataframeService.getCloudCostData(request);
				
				dataset = sparkSession.sql("select `"+columnName+"` from global_temp." + viewName + " where "+deviceType + " and "+report_by+"").distinct();
			}		
			
			List<String> data = dataset.as(Encoders.STRING()).collectAsList();
			
			boolean isPriceColumn = false;
			if(columnName.toLowerCase().contains("price")) {
				isPriceColumn = true;
			}
			if(data != null && !data.isEmpty()) {
				for(String str : data) {
					if(str != null && !str.isEmpty()) {
						if(!isPriceColumn) {
							resultData.add(str);
						} else {
							resultData.add("$"+str);
						}
					}
					
					
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultData;
	}

}