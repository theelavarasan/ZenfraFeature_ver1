package com.zenfra.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	 
	 private ObjectMapper mapper = new ObjectMapper();
	 private JSONParser parser = new  JSONParser();

	
	public  Map<String, List<Object>> getDiscoveryReportValues(String siteKey, String reportBy, String columnName, String category,
			String deviceType, String reportList) {
		System.out.println("-----22222h " );
		
		Dataset<Row> dataset = sparkSession.emptyDataFrame();
		Map<String, List<Object>> resutData = new HashMap<>(); 
		
		if(reportBy != null && reportBy.trim().equalsIgnoreCase("Server")) {
			try {
				 String viewName = siteKey+"_"+deviceType.toLowerCase();					
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
			
			System.out.println("-----dataframePath "  + dataframePath);
			File dir = new File(dataframePath);			
			
			System.out.println("-----dataframePath------12--------- "  +  dir.listFiles());
			
			for(File file : dir.listFiles()) {
				
			    if(file.isDirectory() && file.getName().equalsIgnoreCase(deviceType)) {
			    	actualDfFolderPath = file.getAbsolutePath();
			    	break;
			    }
		    }
			
			System.out.println("----actualDfFolderPath------- "  + actualDfFolderPath);
			
			if(actualDfFolderPath != null) {
				File d = new File(actualDfFolderPath);
				System.out.println("----d.listFiles()------- "  + d.listFiles());
				for(File file : d.listFiles()) {
					
					System.out.println("---- file.getName()----- "  +  file.getName());
					
				    if(file.isFile() && file.getName().toLowerCase().contains(reportList.toLowerCase()) &&  file.getName().toLowerCase().contains(reportBy.toLowerCase()+".json")) { // && file.getName().toLowerCase().contains(category.toLowerCase())
				    	actualDfFilePath = file.getAbsolutePath();
				    	break;
				    }
			    }
				
				System.out.println("---actualDfFilePath---- "  +  actualDfFilePath);
				
				if(actualDfFilePath != null) {
					File f = new File(actualDfFilePath);
						String viewName = f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
						System.out.println("---viewName---- "  +  viewName);
						try {
							dataset = sparkSession.sql("select * from global_temp." + viewName);	
						} catch (Exception e) {
							dataframeService.createDataframeForJsonData(f.getAbsolutePath());
						}
						
						System.out.println("---viewName---- "  +  viewName);
						
						dataset = sparkSession.sql("select data from global_temp." + viewName);
						System.out.println("---viewName-121--- "  +  dataset.count());
				}
			}
			
			System.out.println("---dataset>>>>---- "  +  dataset.count());
			dataset.printSchema();
			String dataArray = dataset.toJSON().collectAsList().toString();		
			
			try {
				JSONArray dataObj = (JSONArray) parser.parse(dataArray);	
				
				System.out.println("---dataObj>>>>---- "  +  dataObj.size());
				
				for(int i=0; i<dataObj.size(); i++) {
					JSONObject jsonObject = (JSONObject) dataObj.get(i);					
					JSONArray dataAry = (JSONArray) jsonObject.get("data");	
					
					System.out.println("---dataAry>>>>---- "  +  dataAry.size());
					
					for(int j=0; j<dataAry.size(); j++) {
						  JSONObject data = (JSONObject) dataAry.get(j);							
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

}
