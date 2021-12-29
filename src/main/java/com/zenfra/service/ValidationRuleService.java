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
		
        String actualDfFolderPath = null;
        String actualDfFilePath = null;
		String dataframePath = commonPath + "Dataframe" + File.separator + "migrationReport" + File.separator + siteKey + File.separator;
		
        Map<String, List<Object>> resutData = new HashMap<>();
		
		File dir = new File(dataframePath);
		for(File file : dir.listFiles()) {
			
		    if(file.isDirectory() && file.getName().equalsIgnoreCase(deviceType)) {
		    	actualDfFolderPath = file.getAbsolutePath();
		    	break;
		    }
	    }
		
		System.out.println("------actualDfFolderPath " +  actualDfFolderPath);
		
		if(actualDfFolderPath != null) {
			File d = new File(actualDfFolderPath);
			for(File file : d.listFiles()) {
				
			    if(file.isFile() && file.getName().toLowerCase().contains(reportList.toLowerCase())) { // && file.getName().toLowerCase().contains(category.toLowerCase())
			    	actualDfFilePath = file.getAbsolutePath();
			    	break;
			    }
		    }
			
			
			if(actualDfFilePath != null) {
				File f = new File(actualDfFilePath);			
				
				Dataset<Row> dataset = sparkSession.emptyDataFrame();
				boolean isPostgresDataframe = false;
				try {
					 String viewName = siteKey+"_"+deviceType.toLowerCase();
					 viewName = viewName.replaceAll("-", "").replaceAll("\\s+","");	
					dataset = sparkSession.sql("select * from global_temp." + viewName);
					isPostgresDataframe = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(!isPostgresDataframe) {
					String viewName = f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
					
					try {
						dataset = sparkSession.sql("select * from global_temp." + viewName);	
					} catch (Exception e) {
						dataframeService.createDataframeForJsonData(f.getAbsolutePath());
					}
					dataset = sparkSession.sql("select data from global_temp." + viewName);
				}
				
				dataset.printSchema();
				String dataArray = dataset.toJSON().collectAsList().toString();		
				
				try {
					JSONArray dataObj = (JSONArray) parser.parse(dataArray);	
					
					for(int i=0; i<dataObj.size(); i++) {
						JSONObject jsonObject = (JSONObject) dataObj.get(i);					
						JSONArray dataAry = (JSONArray) jsonObject.get("data");	
						
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
		}
		 
		return resutData;
	}

}
