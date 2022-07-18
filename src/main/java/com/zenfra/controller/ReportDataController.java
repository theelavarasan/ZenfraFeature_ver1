package com.zenfra.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clickhouse.jdbc.ClickHouseDataSource;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.response.DataResult;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.dataframe.service.EolService;
import com.zenfra.dataframe.util.DataframeUtil;
import com.zenfra.model.ZKConstants;
import com.zenfra.service.ChartService;
import com.zenfra.service.FavouriteApiService_v2;
import com.zenfra.service.ReportService;
import com.zenfra.utils.ExceptionHandlerMail;

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

	@Autowired
	ChartService chartService;

	@Autowired
	EolService eolService;

	@GetMapping("createLocalDiscoveryDF")
	public ResponseEntity<String> createDataframe(@RequestParam("tableName") String tableName) {
		String result = dataframeService.createDataframeForLocalDiscovery(tableName);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/*
	 * @RequestMapping(method = RequestMethod.GET, value = "createReportHeaderDF")
	 * public ResponseEntity<String> createReportHeaderDF(@RequestParam("tableName")
	 * String tableName) { String result =
	 * dataframeService.createDataframeForReportHeader(tableName); return new
	 * ResponseEntity<>(result, HttpStatus.OK); }
	 * 
	 * @RequestMapping(method = RequestMethod.GET, value = "createReportHeaderDF")
	 * public ResponseEntity<String> createDataframe() { String result =
	 * dataframeService.createReportHeaderDF(); return new ResponseEntity<>(result,
	 * HttpStatus.OK); }
	 */

	@PostMapping("getReportData")
	public ResponseEntity<?> getReportData(@RequestBody ServerSideGetRowsRequest request) {

		try {			
			/*if (request.getCategory().equalsIgnoreCase("Server") && request.getAnalyticstype() != null && request.getAnalyticstype().equalsIgnoreCase("Discovery") && (request.getReportBy().equalsIgnoreCase("Server") || request.getReportBy().equalsIgnoreCase("VM") || request.getReportBy().equalsIgnoreCase("Host"))) {
				DataResult data = dataframeService.getReportData(request);
				if (data != null) {
					return new ResponseEntity<>(DataframeUtil.asJsonResponse(data), HttpStatus.OK);
				}
			} else */ if (request.getReportType() != null && request.getReportType().equalsIgnoreCase("optimization")) {
				List<Map<String, Object>> data = dataframeService.getCloudCostDataPostgresFn(request);
				JSONObject result = new JSONObject();
				result.put("data", data);
				return new ResponseEntity<>(result, HttpStatus.OK);
			} else  {  // orient db reports
				
				DataResult data = dataframeService.getReportDataFromDF(request, false);
				if (data != null) {
					return new ResponseEntity<>(DataframeUtil.asJsonResponse(data), HttpStatus.OK);
				}
			}
			

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			System.out.println("Not able to fecth report {}" + e);
		}
		JSONArray emptyArray = new JSONArray();
		return new ResponseEntity<>(emptyArray.toJSONString(), HttpStatus.OK);
	}

	@PostMapping("saveLocalDiscoveryDF")
	public ResponseEntity<String> saveLocalDiscoveryDF(@RequestParam("siteKey") String siteKey,
			@RequestParam("sourceType") String sourceType, @RequestBody JSONObject localDiscoveryData) {

		try {
			if (localDiscoveryData != null && !localDiscoveryData.isEmpty() && siteKey != null && !siteKey.isEmpty()
					&& sourceType != null && !sourceType.isEmpty()) {
				String result = "Success";
				// result = dataframeService.appendLocalDiscovery(siteKey, sourceType,
				// localDiscoveryData);
				result = dataframeService.recreateLocalDiscovery(siteKey, sourceType);

				// verify default fav is present or not
				// favouriteApiService_v2.checkAndUpdateDefaultFavView(siteKey, sourceType,
				// localDiscoveryData.get("userId").toString());

				return new ResponseEntity<>(result, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(ZKConstants.PARAMETER_MISSING, HttpStatus.OK);
			}

		} catch (Exception e) {
			System.out.println("Not able to save local discovery in dataframe {}" + e);
		}

		return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	}

	@PostMapping("saveDefaultFavView")
	public ResponseEntity<String> saveDefaultFavView(@RequestParam("siteKey") String siteKey,
			@RequestParam("sourceType") String sourceType, @RequestParam("userId") String userId) {
		System.out.println("---------------api to add default fav view-----------------------" + sourceType + " : "
				+ siteKey + " : " + userId);
		
		sourceType = sourceType.toLowerCase();

		try {			 

			try { // remove orient db dataframe
				String dataframePath = File.separator + "opt" + File.separator + "ZENfra" + File.separator + "Dataframe"
						+ File.separator + siteKey + File.separator; // +
																											// sourceType
																											// +
																											// File.separator;
				File[] directories = new File(dataframePath).listFiles(File::isDirectory);
				if(directories != null)  {
					for (File dir : directories) {
						if (dir.getName().equalsIgnoreCase(sourceType)) {
							FileSystemUtils.deleteRecursively(dir);
						}
					}
				}
				
				
				try { // delete end to end df file for all log folders
					Path  configFilePath = FileSystems.getDefault().getPath(dataframePath);

				    List<Path> fileWithName = Files.walk(configFilePath)
				            .filter(s -> s.toFile().getAbsolutePath().toLowerCase().contains("end-to-end")).collect(Collectors.toList());
				          

				    for (Path name : fileWithName) {
				    	FileSystemUtils.deleteRecursively(name);
				    }
				
				} catch (Exception e) {
					// TODO: handle exception
				} 
				

			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}
			
			sourceType = sourceType.toLowerCase();
			
			//recreate Reports after completed parsing			
			if(sourceType != null && sourceType.equalsIgnoreCase("Tanium")) {
				 dataframeService.recreateTaniumReportForDataframe(siteKey, sourceType, userId);			 
			} else {
				 dataframeService.recreateReportForDataframe(siteKey, sourceType, userId);
			}
			
			//dataframeService.prepareDsrReport(siteKey, sourceType);
			dataframeService.prepareDsrReport(siteKey, sourceType);
			
			favouriteApiService_v2.checkAndUpdateDefaultFavView(siteKey, sourceType, userId);

			return new ResponseEntity<>("", HttpStatus.OK);

		} catch (Exception e) {
			System.out.println("Not able to save local discovery in dataframe {}" + e);
		}

		return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	}

	

	@PostMapping("createCustomExcelDf")
	public void createCustomExcelDf(@RequestParam("siteKey") String siteKey, @RequestParam("userId") String userId) {
		
		 dataframeService.recreateCustomExcelReportForDataframe(siteKey, userId);
		
	}

	@PostMapping("getReportHeader")
	public ResponseEntity<?> getReportHeader(@ModelAttribute ServerSideGetRowsRequest request) {

	
		try {
			String reportName = "";
			String deviceType = "";
			String reportBy = "";
			String siteKey = "";
			String reportList = "";

			/*if (request.getReportType().equalsIgnoreCase("discovery")) {
				reportName = request.getReportType();
				deviceType = request.getOstype();
				reportBy = request.getReportBy();
				siteKey = request.getSiteKey();
				reportList = request.getReportList();
			} else*/ 
			if (request.getReportType().equalsIgnoreCase("optimization")) {
				reportName = request.getReportType();
				deviceType = "All";
				reportBy = request.getReportType();
				siteKey = request.getSiteKey();
				reportList = request.getReportList();
			} else if(request.getOstype() != null && request.getOstype().equalsIgnoreCase("Tanium") && request.getReportBy().equalsIgnoreCase("Privileged Access")) {			
				
				JSONObject columnHeaders = dataframeService.getReportHeaderForLinuxTanium(request);						
				return new ResponseEntity<>(columnHeaders, HttpStatus.OK); 
			} else if ((request.getCategory().equalsIgnoreCase("Server")) &&
					request.getAnalyticstype() != null 
					&& request.getAnalyticstype().equalsIgnoreCase("Discovery") 
					&& request.getReportList().equalsIgnoreCase("Local") ) {
				if(!request.getReportBy().contains("End-")) {  // || request.getReportBy().equalsIgnoreCase("VM") || request.getReportBy().equalsIgnoreCase("Host")
					reportName = request.getReportType();				
					reportBy = request.getReportBy();
					siteKey = request.getSiteKey();
					reportList = request.getReportList();
					deviceType = request.getOstype();					
					
				} else {
					reportName =  request.getReportType();				
					reportBy = request.getReportBy();
					siteKey = request.getSiteKey();
					reportList = request.getReportList();
					deviceType = request.getOstype();
					
				}
				
			} else {
			
				String componentName = "";
				if(request.getOstype() != null && !request.getOstype().isEmpty()) { //server
					componentName = request.getOstype();
				} else if(request.getSwitchtype() != null && !request.getSwitchtype().isEmpty()) { //switch
					componentName = request.getSwitchtype();
				} else if(request.getStorage() != null && !request.getStorage().isEmpty()) { //Storage
					componentName = request.getStorage();
				} else if(request.getThirdPartyId() != null && !request.getThirdPartyId().isEmpty()) { //Project
					componentName = request.getThirdPartyId();
				} else if(request.getProviders() != null && !request.getProviders().isEmpty()) { //Providers
					componentName = request.getProviders();
				} else if(request.getProject() != null && !request.getProject().isEmpty()) { //Project
					componentName = request.getProject();
				}
				if(request.getReportList().equalsIgnoreCase("Local")) {
					reportName = request.getReportType();
				} else {
					reportName = request.getReportList();
				}
				 
				
				deviceType = componentName;
				reportBy = request.getReportBy();
				siteKey = request.getSiteKey();
				reportList = request.getReportList();	
				
			}
			
		
			
			
			if (reportName != null && !reportName.isEmpty() && deviceType != null && !deviceType.isEmpty()
					&& reportBy != null && !reportBy.isEmpty()) {
				String columnHeaders = reportService.getReportHeader(request, reportName, deviceType, reportBy, siteKey,
						reportList, request.getCategory(), request.getDeviceType(), request.getCategoryOpt(), request.getAnalyticstype(), true);
				return new ResponseEntity<>(columnHeaders, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(ZKConstants.PARAMETER_MISSING, HttpStatus.OK);
			}

		} catch (Exception e) {

			e.printStackTrace();
			System.out.println("Not able to get report headers {}" + e);
		}

		return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	}

	@PostMapping("getChartLayout")
	public ResponseEntity<?> getChartLayout(@RequestParam("userId") String userId,
			@RequestParam("siteKey") String siteKey, @RequestParam("reportName") String reportName) {

		try {
			if (userId != null && !userId.isEmpty() && siteKey != null && !siteKey.isEmpty() && reportName != null
					&& !reportName.isEmpty()) {
				// List<String> response = reportService.getChartLayout(userId, siteKey,
				// reportName);
				JSONObject response = reportService.getReportUserCustomData(userId, siteKey, reportName);
				return new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(com.zenfra.model.ZKConstants.PARAMETER_MISSING, HttpStatus.OK);
			}

		} catch (Exception e) {
			System.out.println("Not able to get getChartLayout {}" + e);
		}

		return new ResponseEntity<>(ZKConstants.ERROR, HttpStatus.OK);
	}

	@GetMapping("/getAllSublinkData")
	public ResponseEntity<?> getAllSublinkData() {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject.put("subLinkDetails", reportService.getDSRLinks());

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return ResponseEntity.ok(resultObject);

	}

	@PostMapping("getOdbReportData")
	public ResponseEntity<?> getOdbReportData(HttpServletRequest request) {

		try {
			String filePath = request.getParameter("filePath");

			JSONObject data = dataframeService.getMigrationReport(filePath);
			if (data != null) {
				return new ResponseEntity<>(data, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			System.out.println("Not able to fecth report {}" + e);
		}
		JSONObject emptyJSONObject = new JSONObject();
		return new ResponseEntity<>(emptyJSONObject, HttpStatus.OK);
	}

	@PostMapping("createDataframeOdbData")
	public ResponseEntity<?> createDataframeOdbData(HttpServletRequest request) {

		try {
			String filePath = request.getParameter("filePath");

			dataframeService.createDataframeForJsonData(filePath);
			JSONObject data = dataframeService.getMigrationReport(filePath);
			if (data != null) {
				return new ResponseEntity<>(data, HttpStatus.OK);
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return new ResponseEntity<>("Not able to create dataframe", HttpStatus.OK);
	}
	
	@PostMapping("getVmaxSubreport")
	public ResponseEntity<?> getVmaxSubreport(HttpServletRequest request) {

		try {
		
			String filePath = request.getParameter("filePath");
			String sid = request.getParameter("sid");
			String serverName = request.getParameter("serverName");
			System.out.println("-----------getVmaxSubreport---------" );
			System.out.println("filePath :: " + filePath );
			System.out.println("sid :: " + sid );
			System.out.println("serverName :: " + serverName );
			JSONArray data = dataframeService.getVmaxSubreport(filePath, serverName, sid);
			if (data != null) {
				return new ResponseEntity<>(data, HttpStatus.OK);
			}

		} catch (Exception e) {
			e.printStackTrace();
			 
		}

		return new ResponseEntity<>(new JSONArray(), HttpStatus.OK);
	}
	
	
	
	

	@GetMapping("createEolEodDf")
	public void createEolEodDf(HttpServletRequest request) {
		eolService.recreateEolEosDataframe();
	}

	

	@GetMapping("test")
	public void test(@RequestParam("siteKey") String siteKey, HttpServletRequest request) {
		
		dataframeService.recreateTaniumReportForDataframe(siteKey, "Tanium", "5f02cb34-ab38-4321-9749-0698e37de8cd");
		dataframeService.recreateCustomExcelReportForDataframe(siteKey, "5f02cb34-ab38-4321-9749-0698e37de8cd");
		 
	}
	
	

	
	
	@PostMapping("getReportDataFromClickHouse")
	public ResponseEntity<?> getReportDataFromClickHouse(@RequestParam("siteKey") String siteKey) {
			
		JSONArray jsonArray = new JSONArray();
		System.out.println("-------------Start Time--------------" + new Date());
		//JSONArray jsonArray = getDirectReportClickHouse(siteKey);
	
		
			    String url = "jdbc:ch://164.52.218.85:8123/alpha";
				String user = "default";
				String password = "fdcDAxec";
				Connection connection = null;
				
				try {
					//siteKey="b8f6a026-0a0b-4aca-a24a-767a0fd25316";
	        
	            Properties properties = new Properties();
	            properties.setProperty("user", "default");
	            properties.setProperty("password","fdcDAxec");
					//connection = DriverManager.getConnection(url, user, password);
					try {
						  Class.forName("com.clickhouse.jdbc.ClickHouseDriver");
						  ClickHouseDataSource dataSource = new ClickHouseDataSource(url, properties);				
							connection = dataSource.getConnection();
					} catch (Exception e) {
						e.printStackTrace();
					}					
					
					
					Statement statement = connection.createStatement();
					
		//			String str="SELECT ifnull(sid,'') as `SID`, ifnull(wwn,'') as `Host WWN`, ifnull(psn,'') as `Possible Server Name(VMAX)`, ifnull(initiatorName,'') as `Initiator Name`, ifnull(igName2,'') as `Initiator Group`, ifnull(portGroup,'') as `Port Group`, ifnull(faPortWWN,'') as `FA Port WWN`, ifnull(faidentifier,'') as `FA Port Number`, ifnull(mskName,'') as `Masking View`, ifnull(sgname,'') as `Storage Group`, ifnull(devCnt,'') as `Device Count`, ifnull(round(megabytesTotal,3),0) as `Device Total Capacity`, ifnull(device_thinList,'') as `Thin Device Count`, ifnull(round(thinCapacity,3),0) as `Device Thin Capacity`, ifnull(device_srdfList,'') as `Replication Device Count`, ifnull(round(replicationCapacity,3),0)  as `Device Replication Capacity`, ifnull(device_cloneList,'') as  `Clone Device Count`, ifnull(round(cloneCapacity,3),0) as `Device Clone Capacity`, ifnull(device_bcvList,'') as `BCV Device Count`, ifnull(round(bcvCapacity,3),0) as `Device BCV Capacity` FROM ( SELECT sid, wwn, groupUniqArrayArray(initName) AS initiatorName, groupUniqArrayArray(aliasName) AS aliasName, groupUniqArrayArray(igname) AS igname, groupUniqArrayArray(igName2) AS igName2, groupUniqArrayArray(psn) AS psn, groupUniqArrayArray(mskName) AS mskName, groupUniqArrayArray(faPortWWN) AS faPortWWN, groupUniqArrayArray(portGroup) AS portGroup, groupUniqArrayArray(faidentifier) AS faidentifier, groupArrayDistinct(stogrpName) AS sgname, sum(devcnt) AS devCnt, sum(megabytes_total/1024) AS megabytesTotal, sum(device_thin_list) AS device_thinList, sum(thin_capacity/1024) AS thinCapacity, sum(device_srdf_list) AS device_srdfList, sum(replication_capacity/1024) AS replicationCapacity, sum(device_clone_list) AS device_cloneList, sum(clone_capacity/1024) AS cloneCapacity, sum(device_bcv_list) AS device_bcvList, sum(bcv_capacity/1024) AS bcvCapacity FROM ( SELECT vinitTab.sid AS sid, vinitTab.wwn as wwn, any(vinitTab.logDate) as logDates, groupArrayDistinct(vinitTab.initiatorname) AS initName, groupArrayDistinct(vinitTab.alias) AS aliasName, groupArrayDistinct(vinitTab.initiator_group_name) AS igname, groupArrayDistinct(vinitTab.possible_servername) AS psn, groupArrayDistinct(initgrpTab.igName) AS igName2, groupArrayDistinct(if(LENGTH(maskViewTab.masking_view_name) > 0, maskViewTab.masking_view_name, NULL)) AS mskName, groupUniqArrayArray(maskViewTab.port_group) AS faPortWWN, groupArrayDistinct(if(LENGTH(maskViewTab.port_group_name) > 0, maskViewTab.port_group_name, NULL)) AS portGroup, groupArrayDistinct(if(LENGTH(faPortTab.identifier) > 0, faPortTab.identifier, NULL)) AS faidentifier, stogrpTab.name AS stogrpName, any(devTab.dev_cnt) AS devcnt, any(devTab.megabytes_total) AS megabytes_total, any(devTab.device_thin_list) AS device_thin_list, any(devTab.thin_capacity) AS thin_capacity, any(devTab.device_srdf_list) AS device_srdf_list, any(devTab.replication_capacity) AS replication_capacity, any(devTab.device_clone_list) AS device_clone_list, any(devTab.clone_capacity) AS clone_capacity, any(devTab.device_bcv_list) AS device_bcv_list, any(devTab.bcv_capacity) AS bcv_capacity FROM vmax_initiator AS vinitTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvini ON (maxvini.sid = vinitTab.sid) AND (maxvini.maxDate = vinitTab.logDate) LEFT JOIN ( SELECT * FROM ( SELECT initiator_group_name AS igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vinigrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvinigrp ON (maxvinigrp.sid = vinigrpTab.sid) AND (maxvinigrp.maxDate = vinigrpTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vinigrpTab.siteKey = '[%s]') UNION ALL SELECT igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vigTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvig ON (maxvig.sid = vigTab.sid) AND (maxvig.maxDate = vigTab.logDate) LEFT ARRAY JOIN ig AS igName WHERE (vigTab.siteKey = '[%s]') AND (length(vigTab.ig) > 0) ) AS inigT ) AS initgrpTab ON (initgrpTab.sid = vinitTab.sid) AND (lower(initgrpTab.igName) = lower(vinitTab.initiator_group_name)) LEFT JOIN ( SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name, pg FROM ( SELECT sid, masking_view_name, initiator_group_name AS igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_masking_view WHERE siteKey = '[%s]' GROUP BY sid ) AS maxmskv ON (maxmskv.sid = vmaskviewTab.sid) AND (maxmskv.maxDate = vmaskviewTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vmaskviewTab.siteKey = '[%s]') UNION DISTINCT SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab2 INNER JO
					//str = str.replace("[%s]", siteKey);
		//			String str = "SELECT linServerHome.ServerName AS ServerName, any(etcOSReleaseInfo.os_name) AS osName, any(CONCAT(etcOSReleaseInfo.os_name,' ', etcOSReleaseInfo.os_version)) AS osVersion, any(linServerHome.kernel_version) AS kernelVersion, any(linServerHome.processor_type) AS processorType, any(collectionDateInfo.collectionDate) AS collectionDate, any(processorInfo.modelName) AS modelName, any(dmiSysInfo.serverModeld) AS serverModeld, any(processorInfo.processorCount) AS processorCount, any(processorInfo.corePerProcessor) AS corePerProcessor, any(processorInfo.noOfCores) AS noOfCores, any(linMemoryInfo.memory) AS memory, any(linNetworkInfo.ipAddress) as ipAddress, any(linNfsInfo.nassize) AS nasSize, any(linNfsInfo.nasusedsize) AS nasUsed, any(linNfsInfo.nasunusedsize) AS nasUnused, any(diskInfo.diskCnt) as diskCount, any(diskInfo.diskSize) as diskSize, any(diskInfo.stoArrByCnt) as storageArrayCnt, any(diskInfo.stoArrBySize) as storageArraySize, any(PVInfo.pv_count) as pvCount, any(PVInfo.PVTotalgb) as pvTotalSize, any(PVInfo.PVAllocgb) as pvAllocSize, any(PVInfo.PVFreegb) as pvFreeSize, any(VGInfo.vg_count) as vgCount, any(VGInfo.VGTotalgb) as vgTotalSize, any(VGInfo.VGAllocgb) as vgAllocSize, any(VGInfo.VGFreegb) as vgFreeSize, any(LVInfo.lv_count) as lvCount, any(LVInfo.LVTotalgb) as lvTotalSize, any(VxDGInfo.VxDiskGroupCount) as VxDGCnt, any(VxDGInfo.VxDiskGroupSize) as VxDGSize, any(VxVolInfo.vxVolCnt) as VxVolCnt, any(VxVolInfo.vxVolSize) as VxVolSize, any(FSInfo.FSSize) as fstSize, any(FSInfo.FSUsedSize) as fsUsedSize, any(FSInfo.FSUnusedSize) as fsUnusedSize, any(if(ASMDiskInfo.asmDisksKer != 0, ASMDiskInfo.asmDisksKer, ASMRulesInfo.asmRuleDisks)) as asmDiskCount, any(clusterInfo.clusterType) as clusterType, any(clusterInfo.clusterMembers) as clusterMembers, any(clusterInfo.clusterName) as clusterName, any(mpDiskCntTab.mpdiskCnt) as mpdiskCnt, any(vxDiskCntTab.vxdiskCnt) as vxdiskCnt, any(ppDiskCntTab.ppdiskCnt) as ppdiskCnt, any(netstatInfo.dependencyServer) as dependencyServer, any(netstatInfo.establishedPort) as establishedPort FROM linux_server_details AS linServerHome INNER JOIN ( SELECT linux_server_details.ServerName as ServerName, max(linux_server_details.logDate) AS maxDate FROM linux_server_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_server_details.ServerName ) AS linTabMax ON (linTabMax.ServerName = linServerHome.ServerName) AND (linTabMax.maxDate = linServerHome.logDate) LEFT JOIN ( SELECT linProcessorDet.ServerName, groupUniqArray(linProcessorDet.model_name) AS modelName, groupUniqArray(linProcessorDet.cpu_mhz) AS cpumhz, count(linProcessorDet.processor) AS processorCount, groupUniqArray(linProcessorDet.cpu_cores) AS corePerProcessor, sum(linProcessorDet.cpu_cores) AS noOfCores FROM linux_processor_details AS linProcessorDet INNER JOIN ( SELECT linux_processor_details.ServerName, max(linux_processor_details.logDate) AS maxDate FROM linux_processor_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_processor_details.ServerName ) AS linProMax ON (linProMax.ServerName = linProcessorDet.ServerName) AND (linProMax.maxDate = linProcessorDet.logDate) WHERE linProcessorDet.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linProcessorDet.ServerName ) AS processorInfo ON (processorInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linCollectDate.ServerName, any(linCollectDate.collection_date_with_timezone) AS collectionDate FROM linux_collection_date AS linCollectDate INNER JOIN ( SELECT linux_collection_date.ServerName, max(linux_collection_date.logDate) AS maxDate FROM linux_collection_date WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_collection_date.ServerName ) AS linColMax ON (linColMax.ServerName = linCollectDate.ServerName) AND (linColMax.maxDate = linCollectDate.logDate) WHERE linCollectDate.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linCollectDate.ServerName ) AS collectionDateInfo ON (collectionDateInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linEtcOS.ServerName, any(linEtcOS.os_name) AS os_name, any(linEtcOS.os_version) AS os_version FROM linux_etc_osrelease AS linEtcOS INNER JOIN ( SELECT linux_etc_osrelease.ServerName, max(logDate) AS maxDate FROM linux_etc_osrelease WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_etc_osrelease.ServerName ) AS linetcOSMax ON (linetcOSMax.ServerName = linEtcOS.ServerName) AND (linetcOSMax.maxDate = linEtcOS.logDate) WHERE linEtcOS.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linEtcOS.ServerName ) AS etcOSReleaseInfo ON (etcOSReleaseInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT lindmidecodeSysinfo.ServerName, any(if(length(lindmidecodeSysinfo.product_name) > 0, CONCAT(lindmidecodeSysinfo.manufacturer, ' ',lindmidecodeSysinfo.product_name),'')) AS serverModeld FROM linux_dmidecode_sysinfo AS lindmidecodeSysinfo INNER JOIN ( SELECT linux_dmidecode_sysinfo.Serve
					String str = "SELECT ifnull(ServerName,'') AS `Server Name`, ifnull(interface,'') AS `Interface`, ifnull(sharename,'') AS `Share Name`, ifnull(volume_name,'') AS `Volume Name`, ifnull(mount_point,'') AS `Mount Point`, ifnull(filesytem_type,'') AS `File System Type`, ifnull(permission,'') AS `Mount Info`, ifnull(proto,'') AS `Protocol`, ifnull(nfsvers,'') AS `NFS Version`, ifnull(rsize,'') AS `Read Size`, ifnull(wsize,'') AS `Write Size`, ifnull(addr,'') AS `Address`, ifnull(uuid,'') AS `UUID` FROM ( SELECT ServerName, substring(device, 1, position(device, ':') - 1) as interface, substring(device, position(device, ':') + 1) as sharename, any(volume_name) as volume_name, any(mount_point) as mount_point, any(filesytem_type) as filesytem_type, any(permission) as permission, any(proto) as proto, any(nfsvers) as nfsvers, any(rsize) as rsize, any(wsize) as wsize, any(addr) as addr, any(uuid) as uuid from linux_mount INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_mount where siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linmntmax ON (linmntmax.ServerName = linux_mount.ServerName) AND (linmntmax.maxDate = linux_mount.logDate) WHERE length(source_host_name) > 0 and siteKey ='b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName,interface,sharename )";
					ResultSet resultSet = statement.executeQuery(str);
					
					 ResultSetMetaData metaData = resultSet.getMetaData();
					          int columnCount = metaData.getColumnCount();
					          while (resultSet.next()) {
					        	  JSONObject jsonobject = new JSONObject();
					             for (int i = 1; i <= columnCount; i++) {				              
					                 jsonobject.put(metaData.getColumnName(i), resultSet.getString(i));
					             }
					             jsonArray.add(jsonobject);
					            
					         }
					          
					        
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if(connection != null) {
						try {
							connection.close();
						} catch (SQLException e) {				
							e.printStackTrace();
						}
					}
				}	
				
				System.out.println(" ------- result Size : " + jsonArray.size());
				
				System.out.println("-------------End Time ------------- "+ new Date());
				
		
				return new ResponseEntity<>(jsonArray, HttpStatus.OK);
		}
	
	
	
	//@PostMapping("getDirectReportClickHouse")
	public JSONArray getDirectReportClickHouse(String siteKey) {
			
			 String url = "jdbc:ch://164.52.218.85:8123/alpha";
				String user = "default";
				String password = "fdcDAxec";
				Connection connection = null;
				JSONArray jsonArray = new JSONArray();
				try {
					//siteKey="b8f6a026-0a0b-4aca-a24a-767a0fd25316";
	            System.out.println("------------Direct query for click house------------");
	            Properties properties = new Properties();
	            properties.setProperty("user", "default");
	            properties.setProperty("password","fdcDAxec");
					//connection = DriverManager.getConnection(url, user, password);
					try {
						  Class.forName("com.clickhouse.jdbc.ClickHouseDriver");
						  ClickHouseDataSource dataSource = new ClickHouseDataSource(url, properties);				
							connection = dataSource.getConnection();
					} catch (Exception e) {
						e.printStackTrace();
					}
	              
	            
					
					
					
					System.out.println("--------------connection------  121----------- " + connection.isClosed());
					
					Statement statement = connection.createStatement();
					
		//			String str="SELECT ifnull(sid,'') as `SID`, ifnull(wwn,'') as `Host WWN`, ifnull(psn,'') as `Possible Server Name(VMAX)`, ifnull(initiatorName,'') as `Initiator Name`, ifnull(igName2,'') as `Initiator Group`, ifnull(portGroup,'') as `Port Group`, ifnull(faPortWWN,'') as `FA Port WWN`, ifnull(faidentifier,'') as `FA Port Number`, ifnull(mskName,'') as `Masking View`, ifnull(sgname,'') as `Storage Group`, ifnull(devCnt,'') as `Device Count`, ifnull(round(megabytesTotal,3),0) as `Device Total Capacity`, ifnull(device_thinList,'') as `Thin Device Count`, ifnull(round(thinCapacity,3),0) as `Device Thin Capacity`, ifnull(device_srdfList,'') as `Replication Device Count`, ifnull(round(replicationCapacity,3),0)  as `Device Replication Capacity`, ifnull(device_cloneList,'') as  `Clone Device Count`, ifnull(round(cloneCapacity,3),0) as `Device Clone Capacity`, ifnull(device_bcvList,'') as `BCV Device Count`, ifnull(round(bcvCapacity,3),0) as `Device BCV Capacity` FROM ( SELECT sid, wwn, groupUniqArrayArray(initName) AS initiatorName, groupUniqArrayArray(aliasName) AS aliasName, groupUniqArrayArray(igname) AS igname, groupUniqArrayArray(igName2) AS igName2, groupUniqArrayArray(psn) AS psn, groupUniqArrayArray(mskName) AS mskName, groupUniqArrayArray(faPortWWN) AS faPortWWN, groupUniqArrayArray(portGroup) AS portGroup, groupUniqArrayArray(faidentifier) AS faidentifier, groupArrayDistinct(stogrpName) AS sgname, sum(devcnt) AS devCnt, sum(megabytes_total/1024) AS megabytesTotal, sum(device_thin_list) AS device_thinList, sum(thin_capacity/1024) AS thinCapacity, sum(device_srdf_list) AS device_srdfList, sum(replication_capacity/1024) AS replicationCapacity, sum(device_clone_list) AS device_cloneList, sum(clone_capacity/1024) AS cloneCapacity, sum(device_bcv_list) AS device_bcvList, sum(bcv_capacity/1024) AS bcvCapacity FROM ( SELECT vinitTab.sid AS sid, vinitTab.wwn as wwn, any(vinitTab.logDate) as logDates, groupArrayDistinct(vinitTab.initiatorname) AS initName, groupArrayDistinct(vinitTab.alias) AS aliasName, groupArrayDistinct(vinitTab.initiator_group_name) AS igname, groupArrayDistinct(vinitTab.possible_servername) AS psn, groupArrayDistinct(initgrpTab.igName) AS igName2, groupArrayDistinct(if(LENGTH(maskViewTab.masking_view_name) > 0, maskViewTab.masking_view_name, NULL)) AS mskName, groupUniqArrayArray(maskViewTab.port_group) AS faPortWWN, groupArrayDistinct(if(LENGTH(maskViewTab.port_group_name) > 0, maskViewTab.port_group_name, NULL)) AS portGroup, groupArrayDistinct(if(LENGTH(faPortTab.identifier) > 0, faPortTab.identifier, NULL)) AS faidentifier, stogrpTab.name AS stogrpName, any(devTab.dev_cnt) AS devcnt, any(devTab.megabytes_total) AS megabytes_total, any(devTab.device_thin_list) AS device_thin_list, any(devTab.thin_capacity) AS thin_capacity, any(devTab.device_srdf_list) AS device_srdf_list, any(devTab.replication_capacity) AS replication_capacity, any(devTab.device_clone_list) AS device_clone_list, any(devTab.clone_capacity) AS clone_capacity, any(devTab.device_bcv_list) AS device_bcv_list, any(devTab.bcv_capacity) AS bcv_capacity FROM vmax_initiator AS vinitTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvini ON (maxvini.sid = vinitTab.sid) AND (maxvini.maxDate = vinitTab.logDate) LEFT JOIN ( SELECT * FROM ( SELECT initiator_group_name AS igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vinigrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvinigrp ON (maxvinigrp.sid = vinigrpTab.sid) AND (maxvinigrp.maxDate = vinigrpTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vinigrpTab.siteKey = '[%s]') UNION ALL SELECT igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vigTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvig ON (maxvig.sid = vigTab.sid) AND (maxvig.maxDate = vigTab.logDate) LEFT ARRAY JOIN ig AS igName WHERE (vigTab.siteKey = '[%s]') AND (length(vigTab.ig) > 0) ) AS inigT ) AS initgrpTab ON (initgrpTab.sid = vinitTab.sid) AND (lower(initgrpTab.igName) = lower(vinitTab.initiator_group_name)) LEFT JOIN ( SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name, pg FROM ( SELECT sid, masking_view_name, initiator_group_name AS igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_masking_view WHERE siteKey = '[%s]' GROUP BY sid ) AS maxmskv ON (maxmskv.sid = vmaskviewTab.sid) AND (maxmskv.maxDate = vmaskviewTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vmaskviewTab.siteKey = '[%s]') UNION DISTINCT SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab2 INNER JO
					//str = str.replace("[%s]", siteKey);
		//			String str = "SELECT linServerHome.ServerName AS ServerName, any(etcOSReleaseInfo.os_name) AS osName, any(CONCAT(etcOSReleaseInfo.os_name,' ', etcOSReleaseInfo.os_version)) AS osVersion, any(linServerHome.kernel_version) AS kernelVersion, any(linServerHome.processor_type) AS processorType, any(collectionDateInfo.collectionDate) AS collectionDate, any(processorInfo.modelName) AS modelName, any(dmiSysInfo.serverModeld) AS serverModeld, any(processorInfo.processorCount) AS processorCount, any(processorInfo.corePerProcessor) AS corePerProcessor, any(processorInfo.noOfCores) AS noOfCores, any(linMemoryInfo.memory) AS memory, any(linNetworkInfo.ipAddress) as ipAddress, any(linNfsInfo.nassize) AS nasSize, any(linNfsInfo.nasusedsize) AS nasUsed, any(linNfsInfo.nasunusedsize) AS nasUnused, any(diskInfo.diskCnt) as diskCount, any(diskInfo.diskSize) as diskSize, any(diskInfo.stoArrByCnt) as storageArrayCnt, any(diskInfo.stoArrBySize) as storageArraySize, any(PVInfo.pv_count) as pvCount, any(PVInfo.PVTotalgb) as pvTotalSize, any(PVInfo.PVAllocgb) as pvAllocSize, any(PVInfo.PVFreegb) as pvFreeSize, any(VGInfo.vg_count) as vgCount, any(VGInfo.VGTotalgb) as vgTotalSize, any(VGInfo.VGAllocgb) as vgAllocSize, any(VGInfo.VGFreegb) as vgFreeSize, any(LVInfo.lv_count) as lvCount, any(LVInfo.LVTotalgb) as lvTotalSize, any(VxDGInfo.VxDiskGroupCount) as VxDGCnt, any(VxDGInfo.VxDiskGroupSize) as VxDGSize, any(VxVolInfo.vxVolCnt) as VxVolCnt, any(VxVolInfo.vxVolSize) as VxVolSize, any(FSInfo.FSSize) as fstSize, any(FSInfo.FSUsedSize) as fsUsedSize, any(FSInfo.FSUnusedSize) as fsUnusedSize, any(if(ASMDiskInfo.asmDisksKer != 0, ASMDiskInfo.asmDisksKer, ASMRulesInfo.asmRuleDisks)) as asmDiskCount, any(clusterInfo.clusterType) as clusterType, any(clusterInfo.clusterMembers) as clusterMembers, any(clusterInfo.clusterName) as clusterName, any(mpDiskCntTab.mpdiskCnt) as mpdiskCnt, any(vxDiskCntTab.vxdiskCnt) as vxdiskCnt, any(ppDiskCntTab.ppdiskCnt) as ppdiskCnt, any(netstatInfo.dependencyServer) as dependencyServer, any(netstatInfo.establishedPort) as establishedPort FROM linux_server_details AS linServerHome INNER JOIN ( SELECT linux_server_details.ServerName as ServerName, max(linux_server_details.logDate) AS maxDate FROM linux_server_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_server_details.ServerName ) AS linTabMax ON (linTabMax.ServerName = linServerHome.ServerName) AND (linTabMax.maxDate = linServerHome.logDate) LEFT JOIN ( SELECT linProcessorDet.ServerName, groupUniqArray(linProcessorDet.model_name) AS modelName, groupUniqArray(linProcessorDet.cpu_mhz) AS cpumhz, count(linProcessorDet.processor) AS processorCount, groupUniqArray(linProcessorDet.cpu_cores) AS corePerProcessor, sum(linProcessorDet.cpu_cores) AS noOfCores FROM linux_processor_details AS linProcessorDet INNER JOIN ( SELECT linux_processor_details.ServerName, max(linux_processor_details.logDate) AS maxDate FROM linux_processor_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_processor_details.ServerName ) AS linProMax ON (linProMax.ServerName = linProcessorDet.ServerName) AND (linProMax.maxDate = linProcessorDet.logDate) WHERE linProcessorDet.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linProcessorDet.ServerName ) AS processorInfo ON (processorInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linCollectDate.ServerName, any(linCollectDate.collection_date_with_timezone) AS collectionDate FROM linux_collection_date AS linCollectDate INNER JOIN ( SELECT linux_collection_date.ServerName, max(linux_collection_date.logDate) AS maxDate FROM linux_collection_date WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_collection_date.ServerName ) AS linColMax ON (linColMax.ServerName = linCollectDate.ServerName) AND (linColMax.maxDate = linCollectDate.logDate) WHERE linCollectDate.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linCollectDate.ServerName ) AS collectionDateInfo ON (collectionDateInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linEtcOS.ServerName, any(linEtcOS.os_name) AS os_name, any(linEtcOS.os_version) AS os_version FROM linux_etc_osrelease AS linEtcOS INNER JOIN ( SELECT linux_etc_osrelease.ServerName, max(logDate) AS maxDate FROM linux_etc_osrelease WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_etc_osrelease.ServerName ) AS linetcOSMax ON (linetcOSMax.ServerName = linEtcOS.ServerName) AND (linetcOSMax.maxDate = linEtcOS.logDate) WHERE linEtcOS.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linEtcOS.ServerName ) AS etcOSReleaseInfo ON (etcOSReleaseInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT lindmidecodeSysinfo.ServerName, any(if(length(lindmidecodeSysinfo.product_name) > 0, CONCAT(lindmidecodeSysinfo.manufacturer, ' ',lindmidecodeSysinfo.product_name),'')) AS serverModeld FROM linux_dmidecode_sysinfo AS lindmidecodeSysinfo INNER JOIN ( SELECT linux_dmidecode_sysinfo.Serve
					String str = "SELECT * from Linux_NAS_rep  where `Site Key`='"+siteKey+"' ";
					ResultSet resultSet = statement.executeQuery(str);
					System.out.println("--------------resultSet----------------- " + resultSet.getFetchSize());
					
					 ResultSetMetaData metaData = resultSet.getMetaData();
					          int columnCount = metaData.getColumnCount();
					          while (resultSet.next()) {
					        	  JSONObject jsonobject = new JSONObject();
					             for (int i = 1; i <= columnCount; i++) {				              
					                 jsonobject.put(metaData.getColumnName(i), resultSet.getString(i));
					             }
					             jsonArray.add(jsonobject);
					            
					         }
					          
					        
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if(connection != null) {
						try {
							connection.close();
						} catch (SQLException e) {				
							e.printStackTrace();
						}
					}
				}	
				
				System.out.println("--------------jsonArray----------------- " + jsonArray.size());
				return jsonArray;
		}
	
	
	
	
	@PostMapping("export")
	public void export(@RequestBody  ServerSideGetRowsRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		
		String filePath =  dataframeService.writeDfToCsv(request);
		
		File file = new File(filePath);

        String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        httpServletResponse.setContentType(mimeType);
        httpServletResponse.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));
        httpServletResponse.setContentLength((int) file.length());

        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            FileCopyUtils.copy(inputStream, httpServletResponse.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
				inputStream.close();
			} catch (IOException e) {				
				e.printStackTrace();
			}
        }
        
        try {
        	 FileUtils.delete(new File(filePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		 
	}
	
	@PostMapping("getChartDetails")
	public JSONObject prepareChart(
			@RequestParam("chartConfiguration") String chartConfiguration,
			@RequestParam("chartType") String chartType, 
			@RequestParam("reportLabel") String reportLabel,
			@RequestParam("reportName") String reportName,
			@RequestParam("analyticstype") String analyticstype,
			@RequestParam("siteKey") String siteKey,
			@RequestParam("category") String category,			
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		
		     JSONObject jsonObject = dataframeService.prepareChart(siteKey, chartConfiguration, chartType, reportLabel, reportName, analyticstype, category);
		return jsonObject;
	}
	
	
	
	@PostMapping("subreport")
	public JSONObject subreport(@RequestAttribute(name = "authUserId", required = false) String userId,
			@RequestParam(name = "reportName") String reportName, @RequestParam(name = "columnName") String columnName,
			@RequestParam(name = "selectedData") String resultObject, @RequestParam(name = "siteKey") String siteKey,
			@RequestParam(name = "logDate") String logDate,
			@RequestParam(name = "reportCategory") String reportCategory, @RequestParam(name = "userId") String userId1,
			@RequestParam(name = "deviceType") String deviceType,
			@RequestParam(name = "subReportId", required = false) String subReportId,
			@RequestParam(name = "subReportName", required = false) String subReportName,
			@RequestParam(name = "subReportList", required = false) String subReportList1, HttpServletRequest httpServletRequest)
			throws IOException, SQLException, ParseException, java.text.ParseException {
	
		deviceType = deviceType.toLowerCase().trim().replace("-", "");
		System.out.println("!!!! deviceType: " + deviceType);
		System.out.println("!!!! reportCategory: " + reportCategory);
		System.out.println("!!!! authUserId: " + userId);
		 
		JSONObject resultJSONObject = new JSONObject();
		
		try {
			JSONParser parser = new JSONParser();
			JSONObject resultJSON = (JSONObject) parser.parse(resultObject);
			JSONArray subReportList = (JSONArray) parser.parse(subReportList1);
			if (deviceType.equalsIgnoreCase("HP-UX")) {
				deviceType = "hpux";
			} else if (deviceType.equalsIgnoreCase("vmwarehost")) {
				deviceType = "vmwarehost";
			}
			
			if (reportCategory.trim().equalsIgnoreCase("discovery")
					|| reportCategory.trim().equalsIgnoreCase("compatability")
					|| reportCategory.trim().equalsIgnoreCase("project")
					|| reportCategory.trim().equalsIgnoreCase("migrationmethod")) {

				String serverName = "";
				String vCenter = "";
				String vmname = "";
				String sid = "";
				
				Map<String, String> whereClause = new HashMap<String, String>();

				if (resultJSON.containsKey(columnName) || resultJSON.containsKey("vmax_Replication Device Count")) {
					if(resultJSON.containsKey("Possible Server Name(VMAX)")) {
						serverName = resultJSON.get("Possible Server Name(VMAX)").toString();
						sid = resultJSON.get("SID").toString();
						
						if(!serverName.trim().isEmpty()) {
							whereClause.put("Possible Server Name(VMAX)", serverName);
						}
						if(!sid.trim().isEmpty()) {
							whereClause.put("SID", sid);
						}
					} else if(resultJSON.containsKey("Possible Server Name")) {
						serverName = resultJSON.get("Possible Server Name").toString();
						sid = resultJSON.get("Serial Number").toString();
						
						if(!serverName.trim().isEmpty()) {
							whereClause.put("Possible Server Name", serverName);
						}
						if(!sid.trim().isEmpty()) {
							whereClause.put("Serial Number", sid);
						}
					} else if(resultJSON.containsKey("vmax_Possible Server Name(VMAX)")) { 
						serverName = resultJSON.get("vmax_Possible Server Name(VMAX)").toString();
						sid = resultJSON.get("vmax_SID").toString();
						
						if(!serverName.trim().isEmpty()) {
							whereClause.put("vmax_Possible Server Name(VMAX)", serverName);
						}
						if(!sid.trim().isEmpty()) {
							whereClause.put("vmax_SID", sid);
						}
						
					} else {
						serverName = resultJSON.get(columnName).toString();
						
						whereClause.put(columnName, serverName);
						
					}			
					System.out.println("!!!!! deviceType: " + deviceType);
					System.out.println("!!!!! resultJSON: " + resultJSON);
					if(resultJSON.containsKey("Possible Server Name(VMAX)")) {
						serverName = resultJSON.get("Possible Server Name(VMAX)").toString();
						sid = resultJSON.get("SID").toString();
						
						if(!serverName.trim().isEmpty()) {
							whereClause.put("Possible Server Name(VMAX)", serverName);
						}
						if(!sid.trim().isEmpty()) {
							whereClause.put("SID", sid);
						}
						
					} else if(resultJSON.containsKey("Possible Server Name")) {
						serverName = resultJSON.get("Possible Server Name").toString();
						sid = resultJSON.get("Serial Number").toString();
						
						if(!serverName.trim().isEmpty()) {
							whereClause.put("Possible Server Name", serverName);
						}
						if(!sid.trim().isEmpty()) {
							whereClause.put("Serial Number", sid);
						}
						
					} else if(resultJSON.containsKey("vmax_Possible Server Name(VMAX)")) {
						serverName = resultJSON.get("vmax_Possible Server Name(VMAX)").toString();
						sid = resultJSON.get("vmax_SID").toString();
						

						if(!serverName.trim().isEmpty()) {
							whereClause.put("vmax_Possible Server Name(VMAX)", serverName);
						}
						if(!sid.trim().isEmpty()) {
							whereClause.put("vmax_SID", sid);
						}
						
						
					} else {
						if(deviceType.equalsIgnoreCase("ibmsvc")) {
							serverName = resultJSON.get("Host Name").toString();
						} else {
						serverName = resultJSON.get("Server Name").toString();
						}
						
						whereClause.put(columnName, serverName);
					}					
				}
				
				if (resultJSON.containsKey("VM")) {
					vmname = resultJSON.get("VM") == null ? "" : resultJSON.get("VM").toString();
					
					if(!vmname.trim().isEmpty()) {
						whereClause.put("VM", vmname);
					}
					
				}
				if (resultJSON.containsKey("vCenter")) {
					vCenter = resultJSON.get("vCenter") == null ? "" : resultJSON.get("vCenter").toString();
					
					if(!vCenter.trim().isEmpty()) {
						whereClause.put("vCenter", vCenter);
					}
					
				}
				System.out.println("!!!!! whereClause: " + whereClause);
				 
				

				JSONObject dsrData = dataframeService.getDsrData(subReportList.get(0).toString(), siteKey, whereClause, deviceType);
				JSONObject data = new JSONObject();
				data.putAll(dsrData);
				data.put("responseCode", "200");
				data.put("responseDescription", "successfully loaded");
				data.put("responseMessage", "success");
				
				resultJSONObject.put(subReportList.get(0).toString(), data);
				resultJSONObject.put("title", "Detailed Report for Server (" + serverName + ")");

			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		

		return resultJSONObject;
	}

}