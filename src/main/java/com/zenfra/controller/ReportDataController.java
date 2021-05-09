package com.zenfra.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.response.DataResult;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.dataframe.util.DataframeUtil;
import com.zenfra.dataframe.util.ZenfraConstants;
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
		  System.out.println("---------------api entered to add dataframe-----------------------");
		 
		  try {	      		 
	      		 if(localDiscoveryData != null && !localDiscoveryData.isEmpty() && siteKey != null && !siteKey.isEmpty() && sourceType != null && !sourceType.isEmpty()) {
	      			 String result = "Success";	      			 			
	      			result = dataframeService.appendLocalDiscovery(siteKey, sourceType, localDiscoveryData);	
	      			
	      			//verify default fav is present or not
	      			favouriteApiService_v2.checkAndUpdateDefaultFavView(siteKey, sourceType, localDiscoveryData.get("userId").toString());
	      			
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
	      		
	      			favouriteApiService_v2.checkAndUpdateDefaultFavView(siteKey, sourceType, userId);
	      			
	      			return new ResponseEntity<>("", HttpStatus.OK);
	      		
	      		
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
	 
	 @GetMapping("/checkodb")
	    public void checkodb() {
		 
		 try {
		
         ODatabaseSession db = getDBSession();
         System.out.println("---------db----------- "+ db.getName());
         JavaSparkContext jsc = new JavaSparkContext(sparkSession.sparkContext());
      
         OResultSet rs = db.query("select * from eoleosData");
         System.out.println("Query Executed");
         List<String> ls = resultSetToJson(rs);
         System.out.println("Json  created");
         JavaRDD<String> javaRdd = jsc.parallelize(ls);
         Dataset<org.apache.spark.sql.Row> DataSet = sparkSession.read().json(javaRdd);
         DataSet.printSchema();
         long count = DataSet.count();
         if (count > 0) {
        	 System.out.println("---------eoleosDataSet----------- "+ DataSet.count());
         }
         
        
	 } catch (Exception e) {
			e.printStackTrace();
		}
         
	 }
	 
	 public List<String> resultSetToJson(OResultSet rs) {
	        List<String> ls = new ArrayList<String>();
	        JSONParser parse = new JSONParser();
	        try {
	            while (rs.hasNext()) {
	                OResult item = rs.next();
	                try {
	                    JSONObject result = (JSONObject) parse.parse(item.toJSON());
	                    String s = result.toString();
	                    ls.add(s);
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return ls;
	    }
	 
	 public ODatabaseSession getDBSession() {
	        ODatabaseSession db = null;
	        try {	        	
	        	
	            OrientDB orient = new OrientDB(com.zenfra.model.ZKModel.getProperty(com.zenfra.model.ZKConstants.ORIENTDBIP), OrientDBConfig.defaultConfig());
	            db = orient.open(com.zenfra.model.ZKModel.getProperty(com.zenfra.model.ZKConstants.ORIENTDBNAME), com.zenfra.model.ZKModel.getProperty(com.zenfra.model.ZKConstants.ORIENTDBUSER), com.zenfra.model.ZKModel.getProperty(com.zenfra.model.ZKConstants.ORIENTDBPWD));
	            System.out.println("Connection open");
	        } catch (Exception e) {
	            System.out.println("Connection Failure");
	            e.printStackTrace();
	        }
	        return db;
	    }
	 

	  /*  @RequestMapping(value = "/getReportData/optimization", method = RequestMethod.POST)
	    public ResponseEntity<?> getReport(@RequestAttribute(name = "authUserId", required = false) String userId,
	                                       @RequestParam(name = "deviceType") String deviceType,
	                                       @RequestParam(name = "groupBy", required = false) String groupBy,
	                                       @RequestParam(name = "filterBy", required = false) String filterBy,
	                                       @RequestParam(name = "filterValue", required = false) String filterValue,
	                                       @RequestParam(name = "siteKey", required = false) String siteKey) {
	       
	        System.out.println("**** Generating the Optimization Report");
	        ObjectMapper mapping = new ObjectMapper();
	        com.zenfra.model.ReportResultModel reportResultModel = new com.zenfra.model.ReportResultModel();
	        try {
	            mapping.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	            mapping.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	            //mapping.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	        	           
	            JSONArray  reportArr = new JSONArray();
	                String deviceTypeCondition;
	                if (deviceType.equalsIgnoreCase("All")) {
	                    deviceTypeCondition = " (lcase(aws.actualOSType) in ('windows','linux', 'vmware'))";
	                } else {
	                    deviceTypeCondition = "(lcase(aws.actualOSType)='" + deviceType.toLowerCase() + "')";
	                }
	                List<String> serverNameLst = new ArrayList<>();
	                if(!StringUtils.isBlank(filterValue)) {
	                    String serverNameQuery = "select set(serverName.toLowerCase()).asString() as serverName from taskList where projectId = '"+filterValue+"'";
	                    JSONArray serverNameArr = QueryExecutor.orientDBQueryExecution(serverNameQuery);
	                    if(!serverNameArr.isEmpty()) {
	                        com.zenfra.model.ZenfraJSONObject serverNameObj = (com.zenfra.model.ZenfraJSONObject) serverNameArr.get(0);
	                        String serverName = serverNameObj.get("serverName").toString();
	                        String[] sq = serverName.split(",");
	                        for(int i=0;i<sq.length;i++) {
	                            serverNameLst.add(sq[i].trim());
	                        }
	                    }
	                }
	                reportArr = sparkUtilities.getReport(siteKey, deviceTypeCondition, serverNameLst);

	                reportResultModel.setData(reportArr);
	              
	           

	            String reportName = "optimization" + "_" + deviceType;

	            reportResultModel.setHeaderInfo(getHeaderInfo(reportArr, "optimization", ""));
	            if(StringUtils.isBlank(filterValue)) {
	                ChartController chartController = new ChartController();
	                reportResultModel.setReport_label("Cloud Cost Comparison Report");
	                reportResultModel.setReport_name(reportName);
	                reportResultModel.setColumnGroupInfo(null);
	                reportResultModel.setDetailsColumnOrder(new JSONArray());
	                reportResultModel.setDetailsData(new JSONArray());
	                reportResultModel.setDetailsHeaderInfo(new JSONArray());

	                String columnOrderQuery = "select columnName from reportColumns where reportName ='Optimization' and deviceType='All' order by seq,columnName";
	                JSONArray columnOrderArray = QueryExecutor.orientDBQueryExecution(columnOrderQuery);
	                JSONArray columnArray = new JSONArray();
	                if (!columnOrderArray.isEmpty()) {
	                    for (int i = 0; i < columnOrderArray.size(); i++) {
	                        ZenfraJSONObject columnOrderObject = (ZenfraJSONObject) columnOrderArray.get(i);
	                        if (!columnOrderObject.get("columnName").toString().equalsIgnoreCase("Server Name"))
	                            columnArray.add(columnOrderObject.get("columnName"));
	                    }
	                }

	                reportResultModel.setUnit_conv_details(null);
	                reportResultModel.setChartOnly_enabled(0);
	                reportResultModel.setChart(chartController.getChart(siteKey, userId, "optimization_" + deviceType));
	                String chartLayoutQuery = "select from reportUserCustomization where siteKey = '" + siteKey
	                        + "' and userId = '" + userId + "' and reportName = 'optimization_" + deviceType + "'";

	                JSONArray chartLayoutData = QueryExecutor.orientDBQueryExecution(chartLayoutQuery);
	                JSONArray chartLayoutArray = new JSONArray();
	                if (!chartLayoutData.isEmpty()) {
	                    for (int i = 0; i < chartLayoutData.size(); i++) {
	                        ZenfraJSONObject jsonObject = (ZenfraJSONObject) chartLayoutData.get(i);
	                        List<Object> chartLayoutList = (List<Object>) jsonObject.get("chartLayout");
	                        chartLayoutArray.addAll(chartLayoutList);
	                        List<String> columnsVisibleArray = (List<String>) jsonObject.get("columnsVisible");
	                        if (!columnsVisibleArray.isEmpty()) {
	                            columnArray = new JSONArray();
	                            columnArray.addAll(columnsVisibleArray);
	                        }
	                    }
	                }
	                reportResultModel.setColumnOrder(columnArray);
	                reportResultModel.setChartLayout(chartLayoutArray);
	            }
	            reportResultModel.setResponseCode(200);
	            reportResultModel.setResponseMessage("success");
	            reportResultModel.setResponseDescription("successfully loaded with "+reportArr.size());
	        } catch (Exception ex) {
	            logger.error("Exception in Generating the Report: " + ex);
	            ex.printStackTrace();
	            reportResultModel.setReport_label("Cloud Cost Comparison Report");
	            reportResultModel.setResponseCode(400);
	            reportResultModel.setResponseMessage("Failure");
	            reportResultModel.setResponseDescription(ex.toString());
	        }
	        return ResponseEntity.ok(reportResultModel);
	    }*/
	 
}
