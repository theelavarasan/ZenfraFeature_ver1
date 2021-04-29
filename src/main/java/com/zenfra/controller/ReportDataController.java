package com.zenfra.controller;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.response.DataResult;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.dataframe.util.DataframeUtil;
import com.zenfra.dataframe.util.ZenfraConstants;
import com.zenfra.service.ReportService;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/df/")
public class ReportDataController {
	
	@Autowired
	DataframeService dataframeService;
	
	@Autowired
	ReportService reportService;
	

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
	    public ResponseEntity<String> getRows(@RequestBody ServerSideGetRowsRequest request) { 		
		  
		  try {
	      		 DataResult data = dataframeService.getReportData(request);
	      		 if(data != null) {
	      			return new ResponseEntity<>(DataframeUtil.asJsonResponse(data), HttpStatus.OK);
	      		 }
	 	        
			} catch (Exception e) {
				System.out.println("Not able to fecth report {}"+ e);
			}   	
	    	JSONArray emptyArray = new JSONArray();
	      	 return new ResponseEntity<>(emptyArray.toJSONString(), HttpStatus.OK);
	    }
	 
	 @PostMapping("saveLocalDiscoveryDF")
	    public ResponseEntity<String> saveLocalDiscoveryDF(@RequestParam("siteKey") String siteKey, @RequestParam("sourceType") String sourceType, @RequestBody JSONObject localDiscoveryData) { 	     
		  System.out.println("--------------------------------------");
		  System.out.println(siteKey);
		  System.out.println(sourceType);
		  System.out.println(localDiscoveryData);
		  System.out.println("--------------------------------------");
		  
		  
		  try {	      		 
	      		 if(localDiscoveryData != null && !localDiscoveryData.isEmpty() && siteKey != null && !siteKey.isEmpty() && sourceType != null && !sourceType.isEmpty()) {
	      			 String result = "";
	      			 if(sourceType.equalsIgnoreCase("Solaris")) {
	      				dataframeService.recreateLocalDiscovery(siteKey, sourceType);
	      			} else {
	      				//for(int i = 0; i <= data.size(); i++) {
	      					//JSONObject localDiscoveryData = (JSONObject) data.get(i);
	      					result = dataframeService.appendLocalDiscovery(siteKey, sourceType, localDiscoveryData);
	      				//}
	      				
	      			}
	      			
	      			return new ResponseEntity<>(result, HttpStatus.OK);
	      		 } else {
	      			 return new ResponseEntity<>(ZenfraConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 }
	      		
			} catch (Exception e) {
				System.out.println("Not able to save local discovery in dataframe {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZenfraConstants.ERROR, HttpStatus.OK);
	    }
	 
	 /*RequestMapping(method = RequestMethod.POST, value = "getReportHeader")
	    public ResponseEntity<String> getReportHeader(@RequestParam("reportType") String reportType, @RequestParam("deviceType") String deviceType, @RequestParam("reportBy") String reportBy) { 	     
		  
		  try {	      		 
	      		 if(reportType != null && !reportType.isEmpty() && deviceType != null && !deviceType.isEmpty() && reportBy != null && !reportBy.isEmpty()) {
	      			DataResult columnHeaders = dataframeService.getReportHeader(reportType, deviceType, reportBy);
	      			return new ResponseEntity<>(DataframeUtil.asJsonResponse(columnHeaders), HttpStatus.OK);
	      		 } else {
	      			 return new ResponseEntity<>(ZenfraConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 }
	      		
			} catch (Exception e) {
				System.out.println("Not able to get report headers {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZenfraConstants.ERROR, HttpStatus.OK);
	    } */
	 
	 
	 @PostMapping("getReportHeader")
	    public ResponseEntity<String> getReportHeader(@RequestParam("reportType") String reportName, @RequestParam("ostype") String deviceType, @RequestParam("reportBy") String reportBy, @RequestParam("siteKey") String siteKey, @RequestParam("reportList") String reportList) { 	     
		  
		  try {	      		 
	      		 if(reportName != null && !reportName.isEmpty() && deviceType != null && !deviceType.isEmpty() && reportBy != null && !reportBy.isEmpty()) {
	      			String columnHeaders = reportService.getReportHeader(reportName, deviceType, reportBy, siteKey, reportList);
	      			return new ResponseEntity<>(columnHeaders, HttpStatus.OK);
	      		 } else {
	      			 return new ResponseEntity<>(ZenfraConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 }
	      		
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
	 
	 
	/* @RequestMapping(method = RequestMethod.POST, value = "getSublinkList")
	    public ResponseEntity<String> getReportHeader(@RequestParam("deviceType") String deviceType, @RequestParam("reportType") String reportType) { 	     
		  
		  try {	      		 
	      		 if(reportType != null && !reportType.isEmpty() && deviceType != null && !deviceType.isEmpty()) {
	      			//JSONObject sublinkData = dataframeService.getSubReportList(reportType, deviceType);
	      			return new ResponseEntity<>("", HttpStatus.OK);
	      		 } else {
	      			 return new ResponseEntity<>(ZenfraConstants.PARAMETER_MISSING, HttpStatus.OK);	      		 }
	      		
			} catch (Exception e) {
				System.out.println("Not able to get sublink data {}"+ e);
			}   	
	    	
	      	 return new ResponseEntity<>(ZenfraConstants.ERROR, HttpStatus.OK);
	    }
	  */
	 
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
