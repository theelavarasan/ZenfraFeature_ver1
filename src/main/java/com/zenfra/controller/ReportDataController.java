package com.zenfra.controller;

import java.io.File;

import java.io.IOException;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;


import javax.servlet.http.HttpServletRequest;

import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.protobuf.TextFormat.ParseException;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.response.DataResult;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.dataframe.util.DataframeUtil;
import com.zenfra.model.ZKConstants;
import com.zenfra.service.FavouriteApiService_v2;
import com.zenfra.service.ReportService;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/df/")
public class ReportDataController {
	
	@Autowired
	DataframeService dataframeService;
	
	@Autowired
	ReportService reportService;
	
	@Autowired
	FavouriteApiService_v2 favouriteApiService_v2;
	
	@Autowired
	SparkSession sparkSession;
	

	@GetMapping("createLocalDiscoveryDF")
	    public ResponseEntity<String> createDataframe(@RequestParam("tableName") String tableName) {	     
		   String result = dataframeService.createDataframeForLocalDiscovery(tableName);		   
	        return new ResponseEntity<>(result, HttpStatus.OK);
	    }
	 
	/* @RequestMapping(method = RequestMethod.GET, value = "createReportHeaderDF")
	    public ResponseEntity<String> createReportHeaderDF(@RequestParam("tableName") String tableName) {	     
		   String result = dataframeService.createDataframeForReportHeader(tableName);		   
	        return new ResponseEntity<>(result, HttpStatus.OK);
	    }
	 
	 @RequestMapping(method = RequestMethod.GET, value = "createReportHeaderDF")
	    public ResponseEntity<String> createDataframe() {	
		   String result = dataframeService.createReportHeaderDF();		   
	        return new ResponseEntity<>(result, HttpStatus.OK);
	    }
	 */
	 
	 @PostMapping("getReportData")
	    public ResponseEntity<String> getReportData(@RequestBody ServerSideGetRowsRequest request) { 		
		  		 
		  try {
			  if(request.getAnalyticstype() != null && request.getAnalyticstype().equalsIgnoreCase("Discovery") ) {
				  DataResult data = dataframeService.getReportData(request);
		      		 if(data != null) {
		      			return new ResponseEntity<>(DataframeUtil.asJsonResponse(data), HttpStatus.OK);
		      		 }
			  } else if (request.getReportType() != null && request.getReportType().equalsIgnoreCase("optimization")) {				
				  /*JSONArray data = reportService.getCloudCostData(request);
				  
		      		 if(data != null) {	
		      			JSONObject resultData = new JSONObject();
		      			resultData.put("data", data);
		      			resultData.put("lastRow", data.size());
		      			resultData.put("totalCount", data.size());
		      			return new ResponseEntity<>(resultData.toString(), HttpStatus.OK);
		      		 }
		      		 */
				  
				  DataResult data = dataframeService.getOptimizationReport(request);
				  if(data != null) {
		      			return new ResponseEntity<>(DataframeUtil.asJsonResponse(data), HttpStatus.OK);
		      		 }
			  }
	      		
	 	        
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Not able to fecth report {}"+ e);
			}   	
	    	JSONArray emptyArray = new JSONArray();
	      	 return new ResponseEntity<>(emptyArray.toJSONString(), HttpStatus.OK);
	    }
	 
	 
	 @PostMapping("saveLocalDiscoveryDF")
	    public ResponseEntity<String> saveLocalDiscoveryDF(@RequestParam("siteKey") String siteKey, @RequestParam("sourceType") String sourceType, @RequestBody JSONObject localDiscoveryData) { 	     
		  System.out.println("---------------api entered to add dataframe-----------------------");
		 
		  try {	      		 
	      		 if(localDiscoveryData != null && !localDiscoveryData.isEmpty() && siteKey != null && !siteKey.isEmpty() && sourceType != null && !sourceType.isEmpty()) {
	      			 String result = "Success";	      			 			
	      			//result = dataframeService.appendLocalDiscovery(siteKey, sourceType, localDiscoveryData);	
	      			result = dataframeService.recreateLocalDiscovery(siteKey, sourceType);	
	      			
	      			//verify default fav is present or not
	      			//favouriteApiService_v2.checkAndUpdateDefaultFavView(siteKey, sourceType, localDiscoveryData.get("userId").toString());
	      			
	      			return new ResponseEntity<>(result, HttpStatus.OK);
	      		 } else {
	      			 return new ResponseEntity<>(ZKConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 
	      			}
	      		
			} catch (Exception e) {
				System.out.println("Not able to save local discovery in dataframe {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	    }
	 
	  @PostMapping("saveDefaultFavView")
	    public ResponseEntity<String> saveDefaultFavView(@RequestParam("siteKey") String siteKey, @RequestParam("sourceType") String sourceType, @RequestParam("userId") String userId) { 	     
		  System.out.println("---------------api to add default fav view-----------------------" + sourceType + " : " + siteKey + " : "+userId);
		 
		  try {	
			
			  try { //remove orient db dataframe
					String dataframePath = File.separator + "opt" + File.separator + "ZENfra" + File.separator + "Dataframe" + File.separator + "migrationReport" + File.separator + siteKey + File.separator; // + sourceType + File.separator;
					File[] directories = new File(dataframePath).listFiles(File::isDirectory);
					for(File dir : directories) {					
						if(dir.getName().equalsIgnoreCase(sourceType)) {							
							FileSystemUtils.deleteRecursively(dir);
						}
					}
					
				  } catch (Exception e) {
					e.printStackTrace();
				}
			
			        dataframeService.recreateLocalDiscovery(siteKey, sourceType);	
	      			favouriteApiService_v2.checkAndUpdateDefaultFavView(siteKey, sourceType, userId);
	      			
	      			return new ResponseEntity<>("", HttpStatus.OK);
	      		
	      		
			} catch (Exception e) {
				System.out.println("Not able to save local discovery in dataframe {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	    }
	 
	 @PostMapping("getReportHeader")
	    public ResponseEntity<String> getReportHeader(@ModelAttribute ServerSideGetRowsRequest request) { 
		
		  try {	    
			  String reportName = "";
			  String deviceType = "";
			  String reportBy = "";
			  String siteKey = "";
			  String reportList = "";
			  if(request.getReportType().equalsIgnoreCase("discovery")) {
				  reportName = request.getReportType();
				  deviceType = request.getOstype();
				  reportBy = request.getReportBy();
				  siteKey = request.getSiteKey();
				  reportList = request.getReportList();
			  } else if(request.getReportType().equalsIgnoreCase("optimization")){
				   reportName = request.getReportType();
				   deviceType = "All";
				   reportBy = request.getReportType();
				   siteKey = request.getSiteKey();
				   reportList = request.getReportList();
			  }
			  
				if(reportName != null && !reportName.isEmpty() && deviceType != null && !deviceType.isEmpty() && reportBy != null && !reportBy.isEmpty()) {
		      			String columnHeaders = reportService.getReportHeader(reportName, deviceType, reportBy, siteKey, reportList, request.getCategory());
		      			return new ResponseEntity<>(columnHeaders, HttpStatus.OK);
		        }  else {
	      			 return new ResponseEntity<>(ZKConstants.PARAMETER_MISSING, HttpStatus.OK);	      		
	      	    }
	      		 
	      		
			} catch (Exception e) {
				
				System.out.println("Not able to get report headers {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	    }
	 
	 
	 @PostMapping("getChartLayout")
	 public ResponseEntity<?> getChartLayout(@RequestParam("userId") String userId, @RequestParam("siteKey") String siteKey, @RequestParam("reportName") String reportName) { 	     
		  
		  try {	      		 
	      		 if(userId != null && !userId.isEmpty() && siteKey != null && !siteKey.isEmpty() && reportName != null && !reportName.isEmpty()) {
	      			//List<String> response = reportService.getChartLayout(userId, siteKey, reportName);
	      			JSONObject response = reportService.getReportUserCustomData(userId, siteKey, reportName);
	      			return new ResponseEntity<>(response, HttpStatus.OK);
	      		 } else {
	      			 return new ResponseEntity<>(com.zenfra.model.ZKConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 }
	      		
			} catch (Exception e) {
				System.out.println("Not able to get getChartLayout {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	    }
	 
	 
	 
	 @GetMapping("/getAllSublinkData")
	    public ResponseEntity<?> getAllSublinkData() {	    	 
	    	  JSONObject resultObject = new JSONObject();
	    	try {    		
	    		resultObject.put("subLinkDetails", reportService.getSubReportList("all", "project")); 
	    		 
	    	} catch (Exception e) {
				e.printStackTrace();
			}
	        
	          return ResponseEntity.ok(resultObject);
	          
	    }
	 
	 
	 @PostMapping("getOdbReportData")
	    public ResponseEntity<?> getOdbReportData(@RequestParam("filePath") String filePath) { 		
		  		 
		  try {	  
				  JSONObject data = dataframeService.getMigrationReport(filePath);
				  if(data != null) {
		      			return new ResponseEntity<>(data, HttpStatus.OK);
		      		 }
			  }
		    catch (Exception e) {
				e.printStackTrace();
				System.out.println("Not able to fecth report {}"+ e);
			}   	
		  JSONObject emptyJSONObject = new JSONObject();
	      	 return new ResponseEntity<>(emptyJSONObject, HttpStatus.OK);
	    }
	 
	   @PostMapping("createDataframeOdbData")
	    public  ResponseEntity<?> createDataframeOdbData(@RequestParam("filePath") String filePath) { 		
		  		 
		  try {	  
				    dataframeService.createDataframeForJsonData(filePath);				  
		      		return new ResponseEntity<>("Dataframe Created Successfullty", HttpStatus.OK);
		      		
			  }catch (Exception e) {
				e.printStackTrace();				
			}   	
		     
	      	 return new ResponseEntity<>("Not able to create dataframe" , HttpStatus.OK);
	    }	
		
	
	 
	 
	 @PostMapping("getOdbReportData")
	    public ResponseEntity<?> getOdbReportData(HttpServletRequest request) { 		
		  		 
		  try {	  
			  String filePath = request.getParameter("filePath");
			  System.out.println("-------getOdbReportData------  " + filePath);
			  
				  JSONObject data = dataframeService.getMigrationReport(filePath);
				  if(data != null) {
		      			return new ResponseEntity<>(data, HttpStatus.OK);
		      		 }
			  }
		    catch (Exception e) {
				e.printStackTrace();
				System.out.println("Not able to fecth report {}"+ e);
			}   	
		  JSONObject emptyJSONObject = new JSONObject();
	      	 return new ResponseEntity<>(emptyJSONObject, HttpStatus.OK);
	    }
	 
	   @PostMapping("createDataframeOdbData")
	    public  ResponseEntity<?> createDataframeOdbData(HttpServletRequest request) { 		
		  
		   
		  try {	  
			  String filePath = request.getParameter("filePath");
			  System.out.println("-------createDataframeOdbData------  " + filePath);
			  
				    dataframeService.createDataframeForJsonData(filePath);				  
		      		return new ResponseEntity<>("Dataframe Created Successfullty", HttpStatus.OK);
		      		
			  }catch (Exception e) {
				e.printStackTrace();				
			}   	
		     
	      	 return new ResponseEntity<>("Not able to create dataframe" , HttpStatus.OK);
	    }
	 
}
