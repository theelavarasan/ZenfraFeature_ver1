package com.zenfra.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.response.DataResult;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.dataframe.util.DataframeUtil;
import com.zenfra.dataframe.util.ZenfraConstants;
import com.zenfra.service.FavouriteApiService_v2;
import com.zenfra.service.ReportService;
import com.zenfra.utils.DBUtils;


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
			  if(request.getAnalyticstype() != null && (request.getAnalyticstype().equalsIgnoreCase("Discovery") || request.getAnalyticstype().equalsIgnoreCase("Migration Method"))) {
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
	      			 return new ResponseEntity<>(ZenfraConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 
	      			}
	      		
			} catch (Exception e) {
				System.out.println("Not able to save local discovery in dataframe {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZenfraConstants.ERROR, HttpStatus.OK);
	    }
	 
	  @PostMapping("saveDefaultFavView")
	    public ResponseEntity<String> testfav(@RequestParam("siteKey") String siteKey, @RequestParam("sourceType") String sourceType, @RequestParam("userId") String userId) { 	     
		  System.out.println("---------------api to add default fav view-----------------------" + sourceType + " : " + siteKey + " : "+userId);
		 
		  try {	
			/*
			 * if(sourceType != null && (sourceType.equalsIgnoreCase("LINUX") ||
			 * sourceType.equalsIgnoreCase("WINDOWS") ||
			 * sourceType.equalsIgnoreCase("VMWARE"))) {
			 * reportService.refreshCloudCostViews(); }
			 */
			  		
			        dataframeService.recreateLocalDiscovery(siteKey, sourceType);	
	      			favouriteApiService_v2.checkAndUpdateDefaultFavView(siteKey, sourceType, userId);
	      			
	      			return new ResponseEntity<>("", HttpStatus.OK);
	      		
	      		
			} catch (Exception e) {
				System.out.println("Not able to save local discovery in dataframe {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZenfraConstants.ERROR, HttpStatus.OK);
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
	      			 return new ResponseEntity<>(ZenfraConstants.PARAMETER_MISSING, HttpStatus.OK);	      		
	      	    }
	      		 
	      		
			} catch (Exception e) {
				
				System.out.println("Not able to get report headers {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZenfraConstants.ERROR, HttpStatus.OK);
	    }
	 
	 
	 @PostMapping("getChartLayout")
	 public ResponseEntity<?> getChartLayout(@RequestParam("userId") String userId, @RequestParam("siteKey") String siteKey, @RequestParam("reportName") String reportName) { 	     
		  
		  try {	      		 
	      		 if(userId != null && !userId.isEmpty() && siteKey != null && !siteKey.isEmpty() && reportName != null && !reportName.isEmpty()) {
	      			//List<String> response = reportService.getChartLayout(userId, siteKey, reportName);
	      			JSONObject response = reportService.getReportUserCustomData(userId, siteKey, reportName);
	      			return new ResponseEntity<>(response, HttpStatus.OK);
	      		 } else {
	      			 return new ResponseEntity<>(ZenfraConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 }
	      		
			} catch (Exception e) {
				System.out.println("Not able to get getChartLayout {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZenfraConstants.ERROR, HttpStatus.OK);
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
	
	 
}
