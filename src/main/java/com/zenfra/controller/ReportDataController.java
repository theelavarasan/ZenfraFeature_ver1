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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.json.stream.JsonParser;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.spark.sql.SparkSession;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.client.RestTemplate;

import com.clickhouse.jdbc.ClickHouseDataSource;
import com.zenfra.dao.PrivillegeAccessReportDAO;
import com.zenfra.dao.TaniumGroupReportDAO;
import com.zenfra.dao.TaniumUserNameReportDao;
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
	
	@Autowired
	RestTemplate restTemplate;
	
	private PrivillegeAccessReportDAO privillegeAccessReportDAO;
	
	private TaniumGroupReportDAO taniumGroupReportDAO;
	
	private TaniumUserNameReportDao taniumUserNameReportDao;
	
	@Autowired
    public ReportDataController(@Qualifier("privillegeAccessReportDAO") PrivillegeAccessReportDAO privillegeAccessReportDAO, @Qualifier("taniumGroupReportDAO") TaniumGroupReportDAO taniumGroupReportDAO) {
        this.privillegeAccessReportDAO = privillegeAccessReportDAO;
        this.taniumGroupReportDAO = taniumGroupReportDAO;
    }
	
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
				if(request.getReportType().equalsIgnoreCase("discovery") && request.getCategory().equalsIgnoreCase("user") && request.getOstype().equalsIgnoreCase("tanium") && 
						request.getReportBy().equalsIgnoreCase("Privileged Access")) {
					
					return new ResponseEntity<>(privillegeAccessReportDAO.getData(request), HttpStatus.OK);
					
				} else if(request.getReportType().equalsIgnoreCase("discovery") && request.getCategory().equalsIgnoreCase("user") && request.getOstype().equalsIgnoreCase("tanium") && 
						request.getReportBy().equalsIgnoreCase("Group")) {
					return new ResponseEntity<>(taniumGroupReportDAO.getData(request), HttpStatus.OK);
				} else if(request.getReportType().equalsIgnoreCase("discovery") && request.getCategory().equalsIgnoreCase("user") && request.getOstype().equalsIgnoreCase("tanium") && 
						request.getReportBy().equalsIgnoreCase("Group By Username")) {
					return new ResponseEntity<>(taniumUserNameReportDao.getData(request), HttpStatus.OK);
				} else {
					DataResult data = dataframeService.getReportDataFromDF(request, false);
					if (data != null) {
						return new ResponseEntity<>(DataframeUtil.asJsonResponse(data), HttpStatus.OK);
					}
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
					e.printStackTrace();
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
				 //dataframeService.recreateTaniumReportForDataframe(siteKey, sourceType, userId);			 
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
			} /*else if(request.getOstype() != null && request.getOstype().equalsIgnoreCase("Tanium") && request.getReportBy().equalsIgnoreCase("Privileged Access")) {			
				
				JSONObject columnHeaders = dataframeService.getReportHeaderForLinuxTanium(request);						
				return new ResponseEntity<>(columnHeaders, HttpStatus.OK); 
			}*/ else if ((request.getCategory().equalsIgnoreCase("Server")) &&
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
						reportList, request.getCategory(), request.getDeviceType(), request.getCategoryOpt(), request.getAnalyticstype(), request.getUserId(), true);
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
					
		//			String str="SELECT ifnull(sid,'') as `SID`, ifnull(wwn,'') as `Host WWN`, ifnull(psn,'') as `Possible Server Name(VMAX)`, ifnull(initiatorName,'') as `Initiator Name`, ifnull(igName2,'') as `Initiator Group`, ifnull(portGroup,'') as `Port Group`, ifnull(faPortWWN,'') as `FA Port WWN`, ifnull(faidentifier,'') as `FA Port Number`, ifnull(mskName,'') as `Masking View`, ifnull(sgname,'') as `Storage Group`, ifnull(devCnt,'') as `Device Count`, ifnull(round(megabytesTotal,3),0) as `Device Total Capacity`, ifnull(device_thinList,'') as `Thin Device Count`, ifnull(round(thinCapacity,3),0) as `Device Thin Capacity`, ifnull(device_srdfList,'') as `Replication Device Count`, ifnull(round(replicationCapacity,3),0)  as `Device Replication Capacity`, ifnull(device_cloneList,'') as  `Clone Device Count`, ifnull(round(cloneCapacity,3),0) as `Device Clone Capacity`, ifnull(device_bcvList,'') as `BCV Device Count`, ifnull(round(bcvCapacity,3),0) as `Device BCV Capacity` FROM ( SELECT sid, wwn, groupUniqArrayArray(initName) AS initiatorName, groupUniqArrayArray(aliasName) AS aliasName, groupUniqArrayArray(igname) AS igname, groupUniqArrayArray(igName2) AS igName2, groupUniqArrayArray(psn) AS psn, groupUniqArrayArray(mskName) AS mskName, groupUniqArrayArray(faPortWWN) AS faPortWWN, groupUniqArrayArray(portGroup) AS portGroup, groupUniqArrayArray(faidentifier) AS faidentifier, groupArrayDistinct(stogrpName) AS sgname, sum(devcnt) AS devCnt, sum(megabytes_total/1024) AS megabytesTotal, sum(device_thin_list) AS device_thinList, sum(thin_capacity/1024) AS thinCapacity, sum(device_srdf_list) AS device_srdfList, sum(replication_capacity/1024) AS replicationCapacity, sum(device_clone_list) AS device_cloneList, sum(clone_capacity/1024) AS cloneCapacity, sum(device_bcv_list) AS device_bcvList, sum(bcv_capacity/1024) AS bcvCapacity FROM ( SELECT vinitTab.sid AS sid, vinitTab.wwn as wwn, any(vinitTab.logDate) as logDates, groupArrayDistinct(vinitTab.initiatorname) AS initName, groupArrayDistinct(vinitTab.alias) AS aliasName, groupArrayDistinct(vinitTab.initiator_group_name) AS igname, groupArrayDistinct(vinitTab.possible_servername) AS psn, groupArrayDistinct(initgrpTab.igName) AS igName2, groupArrayDistinct(if(LENGTH(maskViewTab.masking_view_name) > 0, maskViewTab.masking_view_name, NULL)) AS mskName, groupUniqArrayArray(maskViewTab.port_group) AS faPortWWN, groupArrayDistinct(if(LENGTH(maskViewTab.port_group_name) > 0, maskViewTab.port_group_name, NULL)) AS portGroup, groupArrayDistinct(if(LENGTH(faPortTab.identifier) > 0, faPortTab.identifier, NULL)) AS faidentifier, stogrpTab.name AS stogrpName, any(devTab.dev_cnt) AS devcnt, any(devTab.megabytes_total) AS megabytes_total, any(devTab.device_thin_list) AS device_thin_list, any(devTab.thin_capacity) AS thin_capacity, any(devTab.device_srdf_list) AS device_srdf_list, any(devTab.replication_capacity) AS replication_capacity, any(devTab.device_clone_list) AS device_clone_list, any(devTab.clone_capacity) AS clone_capacity, any(devTab.device_bcv_list) AS device_bcv_list, any(devTab.bcv_capacity) AS bcv_capacity FROM vmax_initiator AS vinitTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvini ON (maxvini.sid = vinitTab.sid) AND (maxvini.maxDate = vinitTab.logDate) LEFT JOIN ( SELECT * FROM ( SELECT initiator_group_name AS igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vinigrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvinigrp ON (maxvinigrp.sid = vinigrpTab.sid) AND (maxvinigrp.maxDate = vinigrpTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vinigrpTab.siteKey = '[%s]') UNION ALL SELECT igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vigTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvig ON (maxvig.sid = vigTab.sid) AND (maxvig.maxDate = vigTab.logDate) LEFT ARRAY JOIN ig AS igName WHERE (vigTab.siteKey = '[%s]') AND (length(vigTab.ig) > 0) ) AS inigT ) AS initgrpTab ON (initgrpTab.sid = vinitTab.sid) AND (lower(initgrpTab.igName) = lower(vinitTab.initiator_group_name)) LEFT JOIN ( SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name, pg FROM ( SELECT sid, masking_view_name, initiator_group_name AS igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_masking_view WHERE siteKey = '[%s]' GROUP BY sid ) AS maxmskv ON (maxmskv.sid = vmaskviewTab.sid) AND (maxmskv.maxDate = vmaskviewTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vmaskviewTab.siteKey = '[%s]') UNION DISTINCT SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab2 INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_masking_view WHERE siteKey = '[%s]' GROUP BY sid ) AS maxmskv2 ON (maxmskv2.sid = vmaskviewTab2.sid) AND (maxmskv2.maxDate = vmaskviewTab2.logDate) LEFT ARRAY JOIN ig AS igName WHERE (vmaskviewTab2.siteKey = '[%s]') AND (length(vmaskviewTab2.ig) > 0) ) AS MaskView LEFT ARRAY JOIN port_group AS pg ) AS maskViewTab ON (initgrpTab.sid = maskViewTab.sid) AND (lower(initgrpTab.igName) = lower(maskViewTab.igName)) LEFT JOIN ( SELECT sid, wwn, identifier FROM vmax_faport AS vfaportTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_faport WHERE siteKey = '[%s]' GROUP BY sid ) AS maxfapo ON (maxfapo.sid = vfaportTab.sid) AND (maxfapo.maxDate = vfaportTab.logDate) WHERE vfaportTab.siteKey = '[%s]' ) AS faPortTab ON (faPortTab.sid = maskViewTab.sid) AND (lower(faPortTab.wwn) = lower(maskViewTab.pg)) LEFT JOIN ( SELECT sid, name, device FROM vmax_storage_group AS vstogrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_storage_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxstogrp ON (maxstogrp.sid = vstogrpTab.sid) AND (maxstogrp.maxDate = vstogrpTab.logDate) WHERE vstogrpTab.siteKey = '[%s]' ) AS stogrpTab ON (maskViewTab.sid = stogrpTab.sid) AND (lower(maskViewTab.storage_group_name) = lower(stogrpTab.name)) LEFT JOIN ( SELECT vdev.sid AS sid, stogrp.name AS sgName, countDistinct(vdev.deviceid) AS dev_cnt, sum(vdev.megabytes) AS megabytes_total, countIf(vdev.deviceid, vdev.device_configuration LIKE '%TDEV%') AS device_thin_list, sumIf(vdev.megabytes, vdev.device_configuration LIKE '%TDEV%') AS thin_capacity, countIf(vdev.deviceid, vdev.rdf_nfo_rdf_type LIKE '%R%') AS device_srdf_list, sumIf(vdev.megabytes, vdev.rdf_nfo_rdf_type LIKE '%R%') AS replication_capacity, countIf(vdev.deviceid, (vdev.deviceid = vdev.clo_dev_info_source_src_device_symmetrix_name) AND (vdev.clo_dev_info_target_tgt_device_symmetrix_name != '')) AS device_clone_list, sumIf(vdev.megabytes, (vdev.deviceid = vdev.clo_dev_info_source_src_device_symmetrix_name) AND (vdev.clo_dev_info_target_tgt_device_symmetrix_name != '')) AS clone_capacity, countIf(vdev.deviceid, (vdev.deviceid = vdev.bcv_pair_informationbcv_device_symmetrix_name) AND (vdev.bcv_pair_informationstate_of_pair_std_bcv_ != 'NeverEstab')) AS device_bcv_list, sumIf(vdev.megabytes, (vdev.deviceid = vdev.bcv_pair_informationbcv_device_symmetrix_name) AND (vdev.bcv_pair_informationstate_of_pair_std_bcv_ != 'NeverEstab')) AS bcv_capacity FROM vmax_devices AS vdev INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_devices WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvdev ON (maxvdev.sid = vdev.sid) AND (maxvdev.maxDate = vdev.logDate) INNER JOIN ( SELECT sid, name, device, devId FROM vmax_storage_group AS vstogrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_storage_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxstogrp ON (maxstogrp.sid = vstogrpTab.sid) AND (maxstogrp.maxDate = vstogrpTab.logDate) LEFT ARRAY JOIN device AS devId WHERE vstogrpTab.siteKey = '[%s]' ) AS stogrp ON (stogrp.sid = vdev.sid) AND (stogrp.devId = vdev.deviceid) WHERE vdev.siteKey = '[%s]' GROUP BY vdev.sid, stogrp.name ) AS devTab ON (stogrpTab.sid = devTab.sid) AND (lower(stogrpTab.name) = lower(devTab.sgName)) WHERE (vinitTab.siteKey = '[%s]' AND length(vinitTab.wwn) > 0) GROUP BY vinitTab.sid, vinitTab.wwn, stogrpTab.name )GROUP BY sid, wwn ) as hostwwn";
					//str = str.replace("[%s]", siteKey);
		//			String str = "SELECT linServerHome.ServerName AS ServerName, any(etcOSReleaseInfo.os_name) AS osName, any(CONCAT(etcOSReleaseInfo.os_name,' ', etcOSReleaseInfo.os_version)) AS osVersion, any(linServerHome.kernel_version) AS kernelVersion, any(linServerHome.processor_type) AS processorType, any(collectionDateInfo.collectionDate) AS collectionDate, any(processorInfo.modelName) AS modelName, any(dmiSysInfo.serverModeld) AS serverModeld, any(processorInfo.processorCount) AS processorCount, any(processorInfo.corePerProcessor) AS corePerProcessor, any(processorInfo.noOfCores) AS noOfCores, any(linMemoryInfo.memory) AS memory, any(linNetworkInfo.ipAddress) as ipAddress, any(linNfsInfo.nassize) AS nasSize, any(linNfsInfo.nasusedsize) AS nasUsed, any(linNfsInfo.nasunusedsize) AS nasUnused, any(diskInfo.diskCnt) as diskCount, any(diskInfo.diskSize) as diskSize, any(diskInfo.stoArrByCnt) as storageArrayCnt, any(diskInfo.stoArrBySize) as storageArraySize, any(PVInfo.pv_count) as pvCount, any(PVInfo.PVTotalgb) as pvTotalSize, any(PVInfo.PVAllocgb) as pvAllocSize, any(PVInfo.PVFreegb) as pvFreeSize, any(VGInfo.vg_count) as vgCount, any(VGInfo.VGTotalgb) as vgTotalSize, any(VGInfo.VGAllocgb) as vgAllocSize, any(VGInfo.VGFreegb) as vgFreeSize, any(LVInfo.lv_count) as lvCount, any(LVInfo.LVTotalgb) as lvTotalSize, any(VxDGInfo.VxDiskGroupCount) as VxDGCnt, any(VxDGInfo.VxDiskGroupSize) as VxDGSize, any(VxVolInfo.vxVolCnt) as VxVolCnt, any(VxVolInfo.vxVolSize) as VxVolSize, any(FSInfo.FSSize) as fstSize, any(FSInfo.FSUsedSize) as fsUsedSize, any(FSInfo.FSUnusedSize) as fsUnusedSize, any(if(ASMDiskInfo.asmDisksKer != 0, ASMDiskInfo.asmDisksKer, ASMRulesInfo.asmRuleDisks)) as asmDiskCount, any(clusterInfo.clusterType) as clusterType, any(clusterInfo.clusterMembers) as clusterMembers, any(clusterInfo.clusterName) as clusterName, any(mpDiskCntTab.mpdiskCnt) as mpdiskCnt, any(vxDiskCntTab.vxdiskCnt) as vxdiskCnt, any(ppDiskCntTab.ppdiskCnt) as ppdiskCnt, any(netstatInfo.dependencyServer) as dependencyServer, any(netstatInfo.establishedPort) as establishedPort FROM linux_server_details AS linServerHome INNER JOIN ( SELECT linux_server_details.ServerName as ServerName, max(linux_server_details.logDate) AS maxDate FROM linux_server_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_server_details.ServerName ) AS linTabMax ON (linTabMax.ServerName = linServerHome.ServerName) AND (linTabMax.maxDate = linServerHome.logDate) LEFT JOIN ( SELECT linProcessorDet.ServerName, groupUniqArray(linProcessorDet.model_name) AS modelName, groupUniqArray(linProcessorDet.cpu_mhz) AS cpumhz, count(linProcessorDet.processor) AS processorCount, groupUniqArray(linProcessorDet.cpu_cores) AS corePerProcessor, sum(linProcessorDet.cpu_cores) AS noOfCores FROM linux_processor_details AS linProcessorDet INNER JOIN ( SELECT linux_processor_details.ServerName, max(linux_processor_details.logDate) AS maxDate FROM linux_processor_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_processor_details.ServerName ) AS linProMax ON (linProMax.ServerName = linProcessorDet.ServerName) AND (linProMax.maxDate = linProcessorDet.logDate) WHERE linProcessorDet.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linProcessorDet.ServerName ) AS processorInfo ON (processorInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linCollectDate.ServerName, any(linCollectDate.collection_date_with_timezone) AS collectionDate FROM linux_collection_date AS linCollectDate INNER JOIN ( SELECT linux_collection_date.ServerName, max(linux_collection_date.logDate) AS maxDate FROM linux_collection_date WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_collection_date.ServerName ) AS linColMax ON (linColMax.ServerName = linCollectDate.ServerName) AND (linColMax.maxDate = linCollectDate.logDate) WHERE linCollectDate.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linCollectDate.ServerName ) AS collectionDateInfo ON (collectionDateInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linEtcOS.ServerName, any(linEtcOS.os_name) AS os_name, any(linEtcOS.os_version) AS os_version FROM linux_etc_osrelease AS linEtcOS INNER JOIN ( SELECT linux_etc_osrelease.ServerName, max(logDate) AS maxDate FROM linux_etc_osrelease WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_etc_osrelease.ServerName ) AS linetcOSMax ON (linetcOSMax.ServerName = linEtcOS.ServerName) AND (linetcOSMax.maxDate = linEtcOS.logDate) WHERE linEtcOS.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linEtcOS.ServerName ) AS etcOSReleaseInfo ON (etcOSReleaseInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT lindmidecodeSysinfo.ServerName, any(if(length(lindmidecodeSysinfo.product_name) > 0, CONCAT(lindmidecodeSysinfo.manufacturer, ' ',lindmidecodeSysinfo.product_name),'')) AS serverModeld FROM linux_dmidecode_sysinfo AS lindmidecodeSysinfo INNER JOIN ( SELECT linux_dmidecode_sysinfo.ServerName, max(linux_dmidecode_sysinfo.logDate) AS maxDate FROM linux_dmidecode_sysinfo WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS dmidecodeMax ON (dmidecodeMax.ServerName = lindmidecodeSysinfo.ServerName) AND (dmidecodeMax.maxDate = lindmidecodeSysinfo.logDate) WHERE lindmidecodeSysinfo.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY lindmidecodeSysinfo.ServerName ) AS dmiSysInfo ON (dmiSysInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linMemInfo.ServerName, any(linMemInfo.size_data) as sizeData, any(linMemInfo.size_metric) as sizeMetric, any(multiIf(lower(linMemInfo.size_metric)='kb',toFloat32(linMemInfo.size_data/(1024*1024)), lower(linMemInfo.size_metric)='mb',toFloat32(linMemInfo.size_data/(1024)),lower(linMemInfo.size_metric)='gb',toFloat32(linMemInfo.size_data),toFloat32(linMemInfo.size_data))) as memory FROM linux_memory_info AS linMemInfo INNER JOIN ( SELECT linux_memory_info.ServerName, max(linux_memory_info.logDate) AS maxDate FROM linux_memory_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_memory_info.ServerName ) AS memMax ON (memMax.ServerName = linMemInfo.ServerName) AND (memMax.maxDate = linMemInfo.logDate) WHERE linMemInfo.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' and lower(linMemInfo.property) = 'memtotal' GROUP BY linMemInfo.ServerName ) AS linMemoryInfo ON (linMemoryInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, round(sum(sizegb),3) as nassize, round(sum(usedgb),3) as nasusedsize, round(sum(availableSizemb),3) as nasunusedsize FROM ( SELECT ServerName, file_system, mounted_on, any(toFloat32(size/(1024*1024))) as sizegb, any(toFloat32(used/(1024*1024))) as usedgb, any(toFloat32(available_size/(1024*1024))) as availableSizemb FROM linux_file_system_kt AS linFsNfs INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_file_system_kt WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS memMax ON (memMax.ServerName = linFsNfs.ServerName) AND (memMax.maxDate = linFsNfs.logDate) WHERE linFsNfs.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' and lower(linFsNfs.file_system_type) LIKE '%nfs%' GROUP BY linFsNfs.ServerName,linFsNfs.file_system,linFsNfs.mounted_on )GROUP BY ServerName ) AS linNfsInfo ON (linNfsInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linNetwork.ServerName, groupArray(if(length(linNetwork.inet_addr) > 0,linNetwork.inet_addr,NULL)) AS ipAddress FROM linux_network_ifconfig_a AS linNetwork INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_network_ifconfig_a WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS networkMax ON (networkMax.ServerName = linNetwork.ServerName) AND (networkMax.maxDate = linNetwork.logDate) WHERE linNetwork.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linNetwork.ServerName ) AS linNetworkInfo ON (linNetworkInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(pv_name) as pv_count, round(sum(PVSizeCalc),3) as PVTotalgb, round(sum(PVAllocCalc),3) as PVAllocgb, round(sum(PVFreeCalc),3) as PVFreegb FROM ( SELECT ServerName, pv_name, any(multiIf((pv_size_metric ILIKE '%kbyte%'), pv_size/(1024*1024),(pv_size_metric ILIKE '%mib%') OR (pv_size_metric ILIKE '%mb%'), pv_size / 1024, (pv_size_metric ILIKE '%gib%') OR (pv_size_metric ILIKE '%gb%'), pv_size, (pv_size_metric ILIKE '%tib%') OR (pv_size_metric ILIKE '%tb%'), pv_size*1024, 0)) as PVSizeCalc, any(multiIf((pe_size_metric ILIKE '%kbyte%'), total_pe*pe_size/(1024*1024), (pe_size_metric ILIKE '%mib%'), total_pe*pe_size/(1024),0)) as PVTotalCalc, any(multiIf((pe_size_metric ILIKE '%kbyte%'), allocated_pe*pe_size/(1024*1024), (pe_size_metric ILIKE '%mib%'), allocated_pe*pe_size/(1024),0)) as PVAllocCalc, any(multiIf((pe_size_metric ILIKE '%kbyte%'), free_pe*pe_size/(1024*1024), (pe_size_metric ILIKE '%mib%'), free_pe*pe_size/(1024),0)) as PVFreeCalc FROM linux_physical_volume linPhyVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_physical_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linPVMax ON (linPVMax.ServerName = linPhyVol.ServerName) AND (linPVMax.maxDate = linPhyVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,pv_name )group by ServerName )AS PVInfo ON (PVInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(vg_name) AS vg_count, round(sum(VGSizeCalc), 3) AS VGTotalgb, round(sum(VGAllocCalc), 3) AS VGAllocgb, round(sum(VGFreeCalc), 3) AS VGFreegb FROM ( SELECT ServerName, vg_name, any(multiIf((vg_size_metric ILIKE '%kbyte%'), vg_size/(1024*1024),(vg_size_metric ILIKE '%mib%') OR (vg_size_metric ILIKE '%mb%'), vg_size / 1024, (vg_size_metric ILIKE '%gib%') OR (vg_size_metric ILIKE '%gb%'), vg_size, (vg_size_metric ILIKE '%tib%') OR (vg_size_metric ILIKE '%tb%'), vg_size*1024, 0)) as VGSizeCalc, any(multiIf(pe_size_metric ILIKE '%kbyte%', (total_pe * pe_size) / (1024 * 1024), (pe_size_metric ILIKE '%mib%') OR (pe_size_metric ILIKE '%mb%'), (total_pe * pe_size) / 1024, 0)) AS VGTotalCalc, any(multiIf(pe_size_metric ILIKE '%kbyte%', (alloc_pe * pe_size) / (1024 * 1024), (pe_size_metric ILIKE '%mib%') OR (pe_size_metric ILIKE '%mb%'), (alloc_pe * pe_size) / 1024, 0)) AS VGAllocCalc, any(multiIf(pe_size_metric ILIKE '%kbyte%', (free_pe * pe_size) / (1024 * 1024), (pe_size_metric ILIKE '%mib%') OR (pe_size_metric ILIKE '%mb%'), (free_pe * pe_size) / 1024, 0)) AS VGFreeCalc FROM linux_volume_group AS linVolGrp INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_volume_group WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVGMax ON (linVGMax.ServerName = linVolGrp.ServerName) AND (linVGMax.maxDate = linVolGrp.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName, vg_name )GROUP BY ServerName )AS VGInfo ON (VGInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(lv_uuid) as lv_count, round(sum(LVSizeCalc),3) as LVTotalgb FROM ( SELECT ServerName, lv_uuid, any(multiIf((lv_size_metric ILIKE '%kbyte%'), lv_size/(1024*1024),(lv_size_metric ILIKE '%mib%') OR (lv_size_metric ILIKE '%mb%'), lv_size / 1024, (lv_size_metric ILIKE '%gib%') OR (lv_size_metric ILIKE '%gb%'), lv_size, (lv_size_metric ILIKE '%tib%') OR (lv_size_metric ILIKE '%tb%'), lv_size*1024, 0)) as LVSizeCalc FROM linux_logical_volume linLogVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_logical_volume GROUP BY ServerName ) AS linLVMax ON (linLVMax.ServerName = linLogVol.ServerName) AND (linLVMax.maxDate = linLogVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,lv_uuid )group by ServerName )AS LVInfo ON (LVInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, any(vxDgDisks) as vxDgDisks, any(VxDiskGroupCount) as VxDiskGroupCount, any((TotalDskPublength+TotalDskPvtlength)/(1024*1024)) as VxDiskGroupSize FROM ( SELECT ServerName, countDistinct(device) as vxDgDisks, length(groupUniqArrayArray(diskGroup)) as VxDiskGroupCount, sum(DskPublength) as TotalDskPublength, sum(DskPvtlength) as TotalDskPvtlength FROM ( SELECT ServerName, device, groupArrayDistinct(disk_group) as diskGroup, any(publen/2) as DskPublength, any(privlen/2) as DskPvtlength FROM linux_vx_diskgroup_list linVxDG INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_diskgroup_list WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVxDGMax ON (linVxDGMax.ServerName = linVxDG.ServerName) AND (linVxDGMax.maxDate = linVxDG.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,device )group by ServerName )group by ServerName )AS VxDGInfo ON (VxDGInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(vxVolume) as vxVolCnt, sum(volSize/(1024*1024)) as vxVolSize FROM ( SELECT ServerName, v_name, disk_group, any(CONCAT(v_name,disk_group)) as vxVolume, any(length/2) as volSize FROM linux_vx_volume linVxVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVxVolMax ON (linVxVolMax.ServerName = linVxVol.ServerName) AND (linVxVolMax.maxDate = linVxVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,v_name,disk_group )group by ServerName )AS VxVolInfo ON (VxVolInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, sum(fsSize) as FSSize, sum(fsUsed) as FSUsedSize, sum(fsAvailSize) as FSUnusedSize FROM ( SELECT lindf.ServerName as ServerName, lindf.file_system as file_system, lindf.mounted_on as mounted_on, any(lindf.volume_name) as volume_name, any(lindf.disk_group) as disk_group, any(lindf.lv_name) as lv_name, any(lindf.file_system_type) as fileSystemType, any(lindf.size/(1024*1024)) as fsSize, any(lindf.used/(1024*1024)) as fsUsed, any(lindf.available_size/(1024*1024)) as fsAvailSize FROM linux_file_system_kt lindf INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_file_system_kt WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linDFMax ON (linDFMax.ServerName = lindf.ServerName) AND (linDFMax.maxDate = lindf.logDate) LEFT JOIN ( SELECT fdiskpartifs.ServerName as ServerName, fdiskpartifs.partition_name as partition_name FROM linux_disk_fdisk_partition_info fdiskpartifs INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartifsMax ON (diskpartifsMax.ServerName = fdiskpartifs.ServerName) AND (diskpartifsMax.maxDate = fdiskpartifs.logDate) WHERE fdiskpartifs.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY fdiskpartifs.ServerName,fdiskpartifs.partition_name ) AS linStdParti ON (linStdParti.ServerName = lindf.ServerName) AND (linStdParti.partition_name = lindf.file_system) LEFT JOIN ( SELECT ServerName, v_name, disk_group, any(CONCAT(v_name,disk_group)) as vxvol FROM linux_vx_volume linVxVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVxVolMax ON (linVxVolMax.ServerName = linVxVol.ServerName) AND (linVxVolMax.maxDate = linVxVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,v_name,disk_group ) AS linVxFsRel ON (linVxFsRel.ServerName = lindf.ServerName) AND (linVxFsRel.v_name = lindf.volume_name) AND (linVxFsRel.disk_group = lindf.disk_group) LEFT JOIN ( SELECT ServerName, lv_uuid, lv_name FROM linux_logical_volume linLogVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_logical_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linLVMax ON (linLVMax.ServerName = linLogVol.ServerName) AND (linLVMax.maxDate = linLogVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,lv_uuid,lv_name ) AS linLVFsRel ON (linLVFsRel.ServerName = lindf.ServerName) AND (linLVFsRel.lv_name = lindf.lv_name) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' and file_system_type not ilike '%tmpfs%' and file_system_type not ilike '%nfs%' and (length(linStdParti.partition_name) > 0 or length(linVxFsRel.vxvol) > 0 or length(linLVFsRel.lv_name) > 0) group by ServerName,file_system,mounted_on )group by ServerName )AS FSInfo ON (FSInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(kernel) as asmDisksKer, countDistinct(name) as asmDisksName FROM ( SELECT linOraRul.ServerName as ServerName, linOraRul.kernel as kernel, linOraRul.name as name FROM linux_oracle_rules linOraRul INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_oracle_rules WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linOraRulMax ON (linOraRulMax.ServerName = linOraRul.ServerName) AND (linOraRulMax.maxDate = linOraRul.logDate) INNER JOIN ( SELECT fdiskpartior.ServerName as ServerName, fdiskpartior.partition_name as partition_name, fdiskpartior.partition_disk_name as partition_disk_name FROM linux_disk_fdisk_partition_info fdiskpartior INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartiMax ON (diskpartiMax.ServerName = fdiskpartior.ServerName) AND (diskpartiMax.maxDate = fdiskpartior.logDate) ) AS linOraRulInfo ON (linOraRulInfo.ServerName = linOraRul.ServerName) AND (linOraRulInfo.partition_disk_name = linOraRul.kernel) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,kernel,name )group by ServerName )AS ASMDiskInfo ON (ASMDiskInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, length(groupArrayDistinct(if(startsWith(devic_name_alias,asmCondition),devic_name_alias,NULL))) as asmRuleDisks FROM ( SELECT asmRule.ServerName as ServerName, if(position(asmRule.env_dm_name, '*') > 0, substring(asmRule.env_dm_name, 1, position(asmRule.env_dm_name, '*') - 1), asmRule.env_dm_name) AS asmCondition, asmRule.env_dm_name as env_dm_name, MpASMJn.device_name as device_name, MpASMJn.devic_name_alias as devic_name_alias FROM linux_asm_rules asmRule INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_asm_rules WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS asmMax ON (asmMax.ServerName = asmRule.ServerName) AND (asmMax.maxDate = asmRule.logDate) LEFT JOIN ( SELECT ServerName, device_name, devic_name_alias FROM linux_multipath_info mpathasm INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpasmMax ON (mpasmMax.ServerName = mpathasm.ServerName) AND (mpasmMax.maxDate = mpathasm.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,device_name,devic_name_alias ) AS MpASMJn ON (MpASMJn.ServerName = asmRule.ServerName) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' )group by ServerName )AS ASMRulesInfo ON (ASMRulesInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, 'Oracle RAC' as clusterType, groupArrayDistinct(if(length(host) > 0, host, NULL)) AS clusterMembers, '' as clusterName FROM linux_oracle_crs_stst oraClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_oracle_crs_stst WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS oraMax ON (oraMax.ServerName = oraClus.ServerName) AND (oraMax.maxDate = oraClus.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName UNION DISTINCT SELECT ServerName, 'VCS' as clusterType, groupArrayDistinct(system) as clusterMembers, '' as clusterName FROM linux_vcs_hastatus_system vcsClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vcs_hastatus_system WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS vcsMax ON (vcsMax.ServerName = vcsClus.ServerName) AND (vcsMax.maxDate = vcsClus.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName UNION DISTINCT SELECT gpfsClus.ServerName as ServerName, any(if(length(gpfsClus.tiebreaker_disks) > 0, 'GPFS-Tiebreaker', 'GPFS')) as clusterType, any(gpfsNode.clusterMem) as clusterMembers, any(gpfsClus.clustername) as clusterName FROM linux_gpfs_mmlsconfig gpfsClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_gpfs_mmlsconfig WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS gpfsMax ON (gpfsMax.ServerName = gpfsClus.ServerName) AND (gpfsMax.maxDate = gpfsClus.logDate) LEFT JOIN ( SELECT ServerName, groupArrayDistinct(node_name) as clusterMem FROM linux_gpfs_mmgetstat_las gpfstClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_gpfs_mmgetstat_las WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS gpfstMax ON (gpfstMax.ServerName = gpfstClus.ServerName) AND (gpfstMax.maxDate = gpfstClus.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName ) AS gpfsNode ON (gpfsNode.ServerName = gpfsClus.ServerName) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName )AS clusterInfo ON (clusterInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, groupArrayDistinct(concat(storageType,toString(storageArrSize))) as stoArrBySize, groupArrayDistinct(concat(storageType,'(',toString(LENGTH(disksStoType)),')')) as stoArrByCnt, SUM(diskSizeStoType) as diskSize, length(groupUniqArrayArray(disksStoType)) as diskCnt FROM ( SELECT ServerName, storageType, SUM(diskSizes) as diskSizeStoType, groupArrayDistinct(concat(toString(rderivedSize),'(',toString(length(disks)),')')) as storageArrSize, groupUniqArrayArray(disks) as disksStoType FROM ( SELECT ServerName, if(length(derivedProduct) > 0,concat(trim(derivedVendor),'-',trim(derivedProduct)),derivedVendor) as storageType, round(derivedSize,3) as rderivedSize, SUM(round(derivedSize,3)) as diskSizes, groupArrayDistinct(derivedDisk) as disks FROM ( SELECT ServerName, multiIf(length(mp1alias) > 0,mp1alias,length(mp2alias) > 0,mp2alias,length(mp3alias) > 0,mp3alias, length(pppseudo_name) > 0,pppseudo_name,length(pp2pseudo_name) > 0,pp2pseudo_name,length(vxpsedoname) > 0,vxpsedoname,length(inqwwn) > 0,inqwwn,length(qwwuln) > 0,qwwuln,length(dppvname) > 0,disk_name,length(dpfsname) > 0,disk_name,NULL) as derivedDisk, any(multiIf(diskSizeInq != 0,diskSizeInq,fdiskSize != 0,fdiskSize,qDiskSize != 0,qDiskSize,mpdsize != 0,mpdsize,mp2dsize != 0,mp2dsize, mp3dsize != 0,mp3dsize,0)) as derivedSize, any(multiIf(length(vendorInq) > 0,vendorInq,length(qvendor) > 0,qvendor, length(mpdvendor1) > 0,if(position(mpdvendor1, ',') > 0, substring(mpdvendor1, 1, position(mpdvendor1, ',') - 1), mpdvendor1), length(mp2vendor) > 0,if(position(mp2vendor, ',') > 0, substring(mp2vendor, 1, position(mp2vendor, ',') - 1), mp2vendor), length(mp3vendor) > 0,if(position(mp3vendor, ',') > 0, substring(mp3vendor, 1, position(mp3vendor, ',') - 1), mp3vendor),NULL)) as derivedVendor, any(multiIf(length(productInq) > 0,productInq,length(qproductID) > 0,qproductID, length(mpdvendor1) > 0,if(position(mpdvendor1, ',') > 0, substring(mpdvendor1, position(mpdvendor1, ',') + 1), mpdvendor1), length(mp2vendor) > 0,if(position(mp2vendor, ',') > 0, substring(mp2vendor, position(mp2vendor, ',') + 1), mp2vendor), length(mp3vendor) > 0,if(position(mp3vendor, ',') > 0, substring(mp3vendor, position(mp3vendor, ',') + 1), mp3vendor),NULL)) as derivedProduct FROM ( SELECT linfDisk.ServerName as ServerName, linfDisk.disk_name as disk_name, linfDisk.device_name as device_name, any(linInqDiskWWN.wwn) as inqwwn, any(linQlogicDisk.wwuln) as qwwuln, any(linMPathDiskInfo1.devic_name_alias) as mp1alias, any(linMPathDiskInfo2.devic_name_alias) as mp2alias, any(linMPathDiskInfo3.devic_name_alias) as mp3alias, any(linPPDiskInfo.pseudo_name) as pppseudo_name, any(linPPDiskInfo2.pseudo_name) as pp2pseudo_name, any(linVxDmpInfo.name) as vxpsedoname, any(diskPartitionPVlk.partition_name) as dppvname, any(diskPartitionFSlk.partition_name) as dpfsname, any(linInqNoDots.diskSizeInq) as diskSizeInq, any(toFloat32(linfDisk.size_in_bytes/(1024*1024*1024))) as fdiskSize, any(linQlogicDisk.qDiskSize) as qDiskSize, any(linMPathDiskInfo1.mpDiskSize1) as mpdsize, any(linMPathDiskInfo2.mpDiskSize2) as mp2dsize, any(linMPathDiskInfo3.mpDiskSize3) as mp3dsize, any(linInqNoDots.vendor) as vendorInq, any(linInqNoDots.product) as productInq, any(linQlogicDisk.productVendor) as qvendor, any(linQlogicDisk.productID) as qproductID, any(linMPathDiskInfo1.vendor1) as mpdvendor1, any(linMPathDiskInfo2.vendor2) as mp2vendor, any(linMPathDiskInfo3.vendor3) as mp3vendor FROM linux_disk_fdisk_info linfDisk INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS fdiskMax ON (fdiskMax.ServerName = linfDisk.ServerName) AND (fdiskMax.maxDate = linfDisk.logDate) LEFT JOIN ( SELECT ServerName, disk_name, any(device_name) as device_name, any(vendor) as vendor, any(product) as product, any(toFloat32OrZero(cap_in_kb)/(1024*1024)) as diskSizeInq FROM linux_inq_no_dots_btl InqNodots INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_inq_no_dots_btl WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS inqnodotsMax ON (inqnodotsMax.ServerName = InqNodots.ServerName) AND (inqnodotsMax.maxDate = InqNodots.logDate) WHERE InqNodots.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY InqNodots.ServerName,InqNodots.disk_name ) AS linInqNoDots ON (linInqNoDots.ServerName = linfDisk.ServerName) AND (linInqNoDots.disk_name = linfDisk.disk_name) LEFT JOIN ( SELECT ServerName, device, any(wwn) as wwn FROM linux_inq_wwn_all linInqwwn INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_inq_wwn_all WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS inqWwnMax ON (inqWwnMax.ServerName = linInqwwn.ServerName) AND (inqWwnMax.maxDate = linInqwwn.logDate) WHERE linInqwwn.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linInqwwn.ServerName,linInqwwn.device ) AS linInqDiskWWN ON (linInqDiskWWN.ServerName = linfDisk.ServerName) AND (linInqDiskWWN.device = linfDisk.disk_name) LEFT JOIN ( SELECT ServerName, diskName, any(wwuln) as wwuln, any(product_vendor) as productVendor, any(product_id) as productID, any(lun) as lunId, any(multiIf(lower(qLogicLun.metric)='kb',toFloat32(qLogicLun.size/(1024*1024)), lower(qLogicLun.metric)='mb',toFloat32(qLogicLun.size/(1024)),lower(qLogicLun.metric)='gb',toFloat32(qLogicLun.size),lower(qLogicLun.metric)='tb',toFloat32(qLogicLun.size*1024),toFloat32(qLogicLun.size))) as qDiskSize FROM linux_qlogic_device_lun_info qLogicLun INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_qlogic_device_lun_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS qLogicDeviceMax ON (qLogicDeviceMax.ServerName = qLogicLun.ServerName) AND (qLogicDeviceMax.maxDate = qLogicLun.logDate) LEFT ARRAY JOIN disk_name AS diskName WHERE qLogicLun.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY qLogicLun.ServerName,diskName ) AS linQlogicDisk ON (linQlogicDisk.ServerName = linfDisk.ServerName) AND (linQlogicDisk.diskName = linfDisk.disk_name) LEFT JOIN ( SELECT ServerName, host_io_path, pseudo_name FROM linux_powerpath_subpaths linPPdiskPath INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS ppdiskMax ON (ppdiskMax.ServerName = linPPdiskPath.ServerName) AND (ppdiskMax.maxDate = linPPdiskPath.logDate) WHERE linPPdiskPath.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linPPdiskPath.ServerName,linPPdiskPath.host_io_path,linPPdiskPath.pseudo_name ) AS linPPDiskInfo ON (linPPDiskInfo.ServerName = linfDisk.ServerName) AND (linPPDiskInfo.host_io_path = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, pseudo_name FROM linux_powerpath_subpaths linPPdiskPseudo INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS ppdiskMax ON (ppdiskMax.ServerName = linPPdiskPseudo.ServerName) AND (ppdiskMax.maxDate = linPPdiskPseudo.logDate) WHERE linPPdiskPseudo.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linPPdiskPseudo.ServerName,linPPdiskPseudo.pseudo_name ) AS linPPDiskInfo2 ON (linPPDiskInfo2.ServerName = linfDisk.ServerName) AND (linPPDiskInfo2.pseudo_name = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, device_name, any(devic_name_alias) as devic_name_alias, any(device_wwid) as deviceWWid1, any(vendor) as vendor1, any(multiIf(lower(mpathDevName.size_of_dm_metrics) like '%k%',toFloat32(mpathDevName.size_of_dm/(1024*1024)), lower(mpathDevName.size_of_dm_metrics) like '%m%',toFloat32(mpathDevName.size_of_dm/(1024)),lower(mpathDevName.size_of_dm_metrics) like '%g%',toFloat32(mpathDevName.size_of_dm),lower(mpathDevName.size_of_dm_metrics) like '%t%',toFloat32(mpathDevName.size_of_dm*1024),toFloat32(mpathDevName.size_of_dm))) as mpDiskSize1 FROM linux_multipath_info mpathDevName INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpOneMax ON (mpOneMax.ServerName = mpathDevName.ServerName) AND (mpOneMax.maxDate = mpathDevName.logDate) WHERE mpathDevName.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY mpathDevName.ServerName,mpathDevName.device_name ) AS linMPathDiskInfo1 ON (linMPathDiskInfo1.ServerName = linfDisk.ServerName) AND (linMPathDiskInfo1.device_name = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, devic_name_alias, any(device_wwid) as deviceWWid2, any(vendor) as vendor2, any(multiIf(lower(mpathDevAlias.size_of_dm_metrics) like '%k%',toFloat32(mpathDevAlias.size_of_dm/(1024*1024)), lower(mpathDevAlias.size_of_dm_metrics) like '%m%',toFloat32(mpathDevAlias.size_of_dm/(1024)),lower(mpathDevAlias.size_of_dm_metrics) like '%g%',toFloat32(mpathDevAlias.size_of_dm),lower(mpathDevAlias.size_of_dm_metrics) like '%t%',toFloat32(mpathDevAlias.size_of_dm*1024),toFloat32(mpathDevAlias.size_of_dm))) as mpDiskSize2 FROM linux_multipath_info mpathDevAlias INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpTwoMax ON (mpTwoMax.ServerName = mpathDevAlias.ServerName) AND (mpTwoMax.maxDate = mpathDevAlias.logDate) WHERE mpathDevAlias.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY mpathDevAlias.ServerName,mpathDevAlias.devic_name_alias ) AS linMPathDiskInfo2 ON (linMPathDiskInfo2.ServerName = linfDisk.ServerName) AND (linMPathDiskInfo2.devic_name_alias = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, sysfs_name, any(devic_name_alias) as devic_name_alias, any(device_wwid) as deviceWWid3, any(vendor) as vendor3, any(multiIf(lower(mpathSysfsName.size_of_dm_metrics) like '%k%',toFloat32(mpathSysfsName.size_of_dm/(1024*1024)), lower(mpathSysfsName.size_of_dm_metrics) like '%m%',toFloat32(mpathSysfsName.size_of_dm/(1024)),lower(mpathSysfsName.size_of_dm_metrics) like '%g%',toFloat32(mpathSysfsName.size_of_dm),lower(mpathSysfsName.size_of_dm_metrics) like '%t%',toFloat32(mpathSysfsName.size_of_dm*1024),toFloat32(mpathSysfsName.size_of_dm))) as mpDiskSize3 FROM linux_multipath_info mpathSysfsName INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpthreeMax ON (mpthreeMax.ServerName = mpathSysfsName.ServerName) AND (mpthreeMax.maxDate = mpathSysfsName.logDate) WHERE mpathSysfsName.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY mpathSysfsName.ServerName,mpathSysfsName.sysfs_name ) AS linMPathDiskInfo3 ON (linMPathDiskInfo3.ServerName = linfDisk.ServerName) AND (linMPathDiskInfo3.sysfs_name = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, subpath, any(name) as name FROM linux_vx_dmp vxdmp INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_dmp WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS vxdmpMax ON (vxdmpMax.ServerName = vxdmp.ServerName) AND (vxdmpMax.maxDate = vxdmp.logDate) WHERE vxdmp.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY vxdmp.ServerName,vxdmp.subpath ) AS linVxDmpInfo ON (linVxDmpInfo.ServerName = linfDisk.ServerName) AND (linVxDmpInfo.subpath = linfDisk.device_name) LEFT JOIN ( SELECT fdiskparti.ServerName as ServerName, fdiskparti.partition_name as partition_name, any(fdiskparti.disk_name) as disk_name, any(fdiskparti.partition_disk_name) as partition_disk_name FROM linux_disk_fdisk_partition_info fdiskparti INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartiMax ON (diskpartiMax.ServerName = fdiskparti.ServerName) AND (diskpartiMax.maxDate = fdiskparti.logDate) INNER JOIN ( SELECT ServerName, pv_name FROM linux_physical_volume linhdppv INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_physical_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS pvMax ON (pvMax.ServerName = linhdppv.ServerName) AND (pvMax.maxDate = linhdppv.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName,pv_name ) AS linPVdisklk ON (linPVdisklk.ServerName = fdiskparti.ServerName) AND (linPVdisklk.pv_name = fdiskparti.partition_name) WHERE fdiskparti.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY fdiskparti.ServerName,fdiskparti.partition_name ) AS diskPartitionPVlk ON (diskPartitionPVlk.ServerName = linfDisk.ServerName) AND (diskPartitionPVlk.disk_name = linfDisk.disk_name) LEFT JOIN ( SELECT fdiskparti2.ServerName as ServerName, fdiskparti2.partition_name as partition_name, any(fdiskparti2.disk_name) as disk_name, any(fdiskparti2.partition_disk_name) as partition_disk_name FROM linux_disk_fdisk_partition_info fdiskparti2 INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartiMax2 ON (diskpartiMax2.ServerName = fdiskparti2.ServerName) AND (diskpartiMax2.maxDate = fdiskparti2.logDate) INNER JOIN ( SELECT ServerName, file_system, mounted_on FROM linux_file_system_kt linfspartition INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_file_system_kt WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS fsMax ON (fsMax.ServerName = linfspartition.ServerName) AND (fsMax.maxDate = linfspartition.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName,file_system,mounted_on ) AS linPVdisklk ON (linPVdisklk.ServerName = fdiskparti2.ServerName) AND (linPVdisklk.file_system = fdiskparti2.partition_name) WHERE fdiskparti2.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY fdiskparti2.ServerName,fdiskparti2.partition_name ) AS diskPartitionFSlk ON (diskPartitionFSlk.ServerName = linfDisk.ServerName) AND (diskPartitionFSlk.disk_name = linfDisk.disk_name) WHERE linfDisk.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linfDisk.ServerName,linfDisk.disk_name,linfDisk.device_name )GROUP BY ServerName,derivedDisk )GROUP BY ServerName,storageType,derivedSize )GROUP BY ServerName,storageType )GROUP BY ServerName )AS diskInfo ON (diskInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(devic_name_alias) as mpdiskCnt FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName )GROUP BY ServerName )AS mpDiskCntTab ON (mpDiskCntTab.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(name) as vxdiskCnt FROM linux_vx_dmp WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_vx_dmp WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName )GROUP BY ServerName )AS vxDiskCntTab ON (vxDiskCntTab.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(pseudo_name) as ppdiskCnt FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName )GROUP BY ServerName )AS ppDiskCntTab ON (ppDiskCntTab.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, groupUniqArrayArray(locAddrPort) as establishedPort, groupArrayDistinct(concat(foreign_address,'(',arrayStringConcat(forAddrPort,','),')')) as dependencyServer FROM ( SELECT ServerName, foreign_address, groupArrayDistinct(local_address_port) as locAddrPort, groupArrayDistinct(foreign_address_port) as forAddrPort FROM linux_netstat_a WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_netstat_a WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AND is_required=1 AND upper(state) = 'ESTABLISHED' AND local_address != foreign_address GROUP BY ServerName,foreign_address )GROUP BY ServerName )AS netstatInfo ON (netstatInfo.ServerName = linServerHome.ServerName) GROUP BY siteKey,ServerName";
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
					
		//			String str="SELECT ifnull(sid,'') as `SID`, ifnull(wwn,'') as `Host WWN`, ifnull(psn,'') as `Possible Server Name(VMAX)`, ifnull(initiatorName,'') as `Initiator Name`, ifnull(igName2,'') as `Initiator Group`, ifnull(portGroup,'') as `Port Group`, ifnull(faPortWWN,'') as `FA Port WWN`, ifnull(faidentifier,'') as `FA Port Number`, ifnull(mskName,'') as `Masking View`, ifnull(sgname,'') as `Storage Group`, ifnull(devCnt,'') as `Device Count`, ifnull(round(megabytesTotal,3),0) as `Device Total Capacity`, ifnull(device_thinList,'') as `Thin Device Count`, ifnull(round(thinCapacity,3),0) as `Device Thin Capacity`, ifnull(device_srdfList,'') as `Replication Device Count`, ifnull(round(replicationCapacity,3),0)  as `Device Replication Capacity`, ifnull(device_cloneList,'') as  `Clone Device Count`, ifnull(round(cloneCapacity,3),0) as `Device Clone Capacity`, ifnull(device_bcvList,'') as `BCV Device Count`, ifnull(round(bcvCapacity,3),0) as `Device BCV Capacity` FROM ( SELECT sid, wwn, groupUniqArrayArray(initName) AS initiatorName, groupUniqArrayArray(aliasName) AS aliasName, groupUniqArrayArray(igname) AS igname, groupUniqArrayArray(igName2) AS igName2, groupUniqArrayArray(psn) AS psn, groupUniqArrayArray(mskName) AS mskName, groupUniqArrayArray(faPortWWN) AS faPortWWN, groupUniqArrayArray(portGroup) AS portGroup, groupUniqArrayArray(faidentifier) AS faidentifier, groupArrayDistinct(stogrpName) AS sgname, sum(devcnt) AS devCnt, sum(megabytes_total/1024) AS megabytesTotal, sum(device_thin_list) AS device_thinList, sum(thin_capacity/1024) AS thinCapacity, sum(device_srdf_list) AS device_srdfList, sum(replication_capacity/1024) AS replicationCapacity, sum(device_clone_list) AS device_cloneList, sum(clone_capacity/1024) AS cloneCapacity, sum(device_bcv_list) AS device_bcvList, sum(bcv_capacity/1024) AS bcvCapacity FROM ( SELECT vinitTab.sid AS sid, vinitTab.wwn as wwn, any(vinitTab.logDate) as logDates, groupArrayDistinct(vinitTab.initiatorname) AS initName, groupArrayDistinct(vinitTab.alias) AS aliasName, groupArrayDistinct(vinitTab.initiator_group_name) AS igname, groupArrayDistinct(vinitTab.possible_servername) AS psn, groupArrayDistinct(initgrpTab.igName) AS igName2, groupArrayDistinct(if(LENGTH(maskViewTab.masking_view_name) > 0, maskViewTab.masking_view_name, NULL)) AS mskName, groupUniqArrayArray(maskViewTab.port_group) AS faPortWWN, groupArrayDistinct(if(LENGTH(maskViewTab.port_group_name) > 0, maskViewTab.port_group_name, NULL)) AS portGroup, groupArrayDistinct(if(LENGTH(faPortTab.identifier) > 0, faPortTab.identifier, NULL)) AS faidentifier, stogrpTab.name AS stogrpName, any(devTab.dev_cnt) AS devcnt, any(devTab.megabytes_total) AS megabytes_total, any(devTab.device_thin_list) AS device_thin_list, any(devTab.thin_capacity) AS thin_capacity, any(devTab.device_srdf_list) AS device_srdf_list, any(devTab.replication_capacity) AS replication_capacity, any(devTab.device_clone_list) AS device_clone_list, any(devTab.clone_capacity) AS clone_capacity, any(devTab.device_bcv_list) AS device_bcv_list, any(devTab.bcv_capacity) AS bcv_capacity FROM vmax_initiator AS vinitTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvini ON (maxvini.sid = vinitTab.sid) AND (maxvini.maxDate = vinitTab.logDate) LEFT JOIN ( SELECT * FROM ( SELECT initiator_group_name AS igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vinigrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvinigrp ON (maxvinigrp.sid = vinigrpTab.sid) AND (maxvinigrp.maxDate = vinigrpTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vinigrpTab.siteKey = '[%s]') UNION ALL SELECT igName, ig, wwn, sid, initiator_group_name FROM vmax_initiator_group AS vigTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_initiator_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvig ON (maxvig.sid = vigTab.sid) AND (maxvig.maxDate = vigTab.logDate) LEFT ARRAY JOIN ig AS igName WHERE (vigTab.siteKey = '[%s]') AND (length(vigTab.ig) > 0) ) AS inigT ) AS initgrpTab ON (initgrpTab.sid = vinitTab.sid) AND (lower(initgrpTab.igName) = lower(vinitTab.initiator_group_name)) LEFT JOIN ( SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name, pg FROM ( SELECT sid, masking_view_name, initiator_group_name AS igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_masking_view WHERE siteKey = '[%s]' GROUP BY sid ) AS maxmskv ON (maxmskv.sid = vmaskviewTab.sid) AND (maxmskv.maxDate = vmaskviewTab.logDate) LEFT ARRAY JOIN ig AS child_ig WHERE (vmaskviewTab.siteKey = '[%s]') UNION DISTINCT SELECT sid, masking_view_name, igName, initiator_group_name, ig, storage_group_name, port_group, port_group_name FROM vmax_masking_view AS vmaskviewTab2 INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_masking_view WHERE siteKey = '[%s]' GROUP BY sid ) AS maxmskv2 ON (maxmskv2.sid = vmaskviewTab2.sid) AND (maxmskv2.maxDate = vmaskviewTab2.logDate) LEFT ARRAY JOIN ig AS igName WHERE (vmaskviewTab2.siteKey = '[%s]') AND (length(vmaskviewTab2.ig) > 0) ) AS MaskView LEFT ARRAY JOIN port_group AS pg ) AS maskViewTab ON (initgrpTab.sid = maskViewTab.sid) AND (lower(initgrpTab.igName) = lower(maskViewTab.igName)) LEFT JOIN ( SELECT sid, wwn, identifier FROM vmax_faport AS vfaportTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_faport WHERE siteKey = '[%s]' GROUP BY sid ) AS maxfapo ON (maxfapo.sid = vfaportTab.sid) AND (maxfapo.maxDate = vfaportTab.logDate) WHERE vfaportTab.siteKey = '[%s]' ) AS faPortTab ON (faPortTab.sid = maskViewTab.sid) AND (lower(faPortTab.wwn) = lower(maskViewTab.pg)) LEFT JOIN ( SELECT sid, name, device FROM vmax_storage_group AS vstogrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_storage_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxstogrp ON (maxstogrp.sid = vstogrpTab.sid) AND (maxstogrp.maxDate = vstogrpTab.logDate) WHERE vstogrpTab.siteKey = '[%s]' ) AS stogrpTab ON (maskViewTab.sid = stogrpTab.sid) AND (lower(maskViewTab.storage_group_name) = lower(stogrpTab.name)) LEFT JOIN ( SELECT vdev.sid AS sid, stogrp.name AS sgName, countDistinct(vdev.deviceid) AS dev_cnt, sum(vdev.megabytes) AS megabytes_total, countIf(vdev.deviceid, vdev.device_configuration LIKE '%TDEV%') AS device_thin_list, sumIf(vdev.megabytes, vdev.device_configuration LIKE '%TDEV%') AS thin_capacity, countIf(vdev.deviceid, vdev.rdf_nfo_rdf_type LIKE '%R%') AS device_srdf_list, sumIf(vdev.megabytes, vdev.rdf_nfo_rdf_type LIKE '%R%') AS replication_capacity, countIf(vdev.deviceid, (vdev.deviceid = vdev.clo_dev_info_source_src_device_symmetrix_name) AND (vdev.clo_dev_info_target_tgt_device_symmetrix_name != '')) AS device_clone_list, sumIf(vdev.megabytes, (vdev.deviceid = vdev.clo_dev_info_source_src_device_symmetrix_name) AND (vdev.clo_dev_info_target_tgt_device_symmetrix_name != '')) AS clone_capacity, countIf(vdev.deviceid, (vdev.deviceid = vdev.bcv_pair_informationbcv_device_symmetrix_name) AND (vdev.bcv_pair_informationstate_of_pair_std_bcv_ != 'NeverEstab')) AS device_bcv_list, sumIf(vdev.megabytes, (vdev.deviceid = vdev.bcv_pair_informationbcv_device_symmetrix_name) AND (vdev.bcv_pair_informationstate_of_pair_std_bcv_ != 'NeverEstab')) AS bcv_capacity FROM vmax_devices AS vdev INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_devices WHERE siteKey = '[%s]' GROUP BY sid ) AS maxvdev ON (maxvdev.sid = vdev.sid) AND (maxvdev.maxDate = vdev.logDate) INNER JOIN ( SELECT sid, name, device, devId FROM vmax_storage_group AS vstogrpTab INNER JOIN ( SELECT sid, max(logDate) AS maxDate FROM vmax_storage_group WHERE siteKey = '[%s]' GROUP BY sid ) AS maxstogrp ON (maxstogrp.sid = vstogrpTab.sid) AND (maxstogrp.maxDate = vstogrpTab.logDate) LEFT ARRAY JOIN device AS devId WHERE vstogrpTab.siteKey = '[%s]' ) AS stogrp ON (stogrp.sid = vdev.sid) AND (stogrp.devId = vdev.deviceid) WHERE vdev.siteKey = '[%s]' GROUP BY vdev.sid, stogrp.name ) AS devTab ON (stogrpTab.sid = devTab.sid) AND (lower(stogrpTab.name) = lower(devTab.sgName)) WHERE (vinitTab.siteKey = '[%s]' AND length(vinitTab.wwn) > 0) GROUP BY vinitTab.sid, vinitTab.wwn, stogrpTab.name )GROUP BY sid, wwn ) as hostwwn";
					//str = str.replace("[%s]", siteKey);
		//			String str = "SELECT linServerHome.ServerName AS ServerName, any(etcOSReleaseInfo.os_name) AS osName, any(CONCAT(etcOSReleaseInfo.os_name,' ', etcOSReleaseInfo.os_version)) AS osVersion, any(linServerHome.kernel_version) AS kernelVersion, any(linServerHome.processor_type) AS processorType, any(collectionDateInfo.collectionDate) AS collectionDate, any(processorInfo.modelName) AS modelName, any(dmiSysInfo.serverModeld) AS serverModeld, any(processorInfo.processorCount) AS processorCount, any(processorInfo.corePerProcessor) AS corePerProcessor, any(processorInfo.noOfCores) AS noOfCores, any(linMemoryInfo.memory) AS memory, any(linNetworkInfo.ipAddress) as ipAddress, any(linNfsInfo.nassize) AS nasSize, any(linNfsInfo.nasusedsize) AS nasUsed, any(linNfsInfo.nasunusedsize) AS nasUnused, any(diskInfo.diskCnt) as diskCount, any(diskInfo.diskSize) as diskSize, any(diskInfo.stoArrByCnt) as storageArrayCnt, any(diskInfo.stoArrBySize) as storageArraySize, any(PVInfo.pv_count) as pvCount, any(PVInfo.PVTotalgb) as pvTotalSize, any(PVInfo.PVAllocgb) as pvAllocSize, any(PVInfo.PVFreegb) as pvFreeSize, any(VGInfo.vg_count) as vgCount, any(VGInfo.VGTotalgb) as vgTotalSize, any(VGInfo.VGAllocgb) as vgAllocSize, any(VGInfo.VGFreegb) as vgFreeSize, any(LVInfo.lv_count) as lvCount, any(LVInfo.LVTotalgb) as lvTotalSize, any(VxDGInfo.VxDiskGroupCount) as VxDGCnt, any(VxDGInfo.VxDiskGroupSize) as VxDGSize, any(VxVolInfo.vxVolCnt) as VxVolCnt, any(VxVolInfo.vxVolSize) as VxVolSize, any(FSInfo.FSSize) as fstSize, any(FSInfo.FSUsedSize) as fsUsedSize, any(FSInfo.FSUnusedSize) as fsUnusedSize, any(if(ASMDiskInfo.asmDisksKer != 0, ASMDiskInfo.asmDisksKer, ASMRulesInfo.asmRuleDisks)) as asmDiskCount, any(clusterInfo.clusterType) as clusterType, any(clusterInfo.clusterMembers) as clusterMembers, any(clusterInfo.clusterName) as clusterName, any(mpDiskCntTab.mpdiskCnt) as mpdiskCnt, any(vxDiskCntTab.vxdiskCnt) as vxdiskCnt, any(ppDiskCntTab.ppdiskCnt) as ppdiskCnt, any(netstatInfo.dependencyServer) as dependencyServer, any(netstatInfo.establishedPort) as establishedPort FROM linux_server_details AS linServerHome INNER JOIN ( SELECT linux_server_details.ServerName as ServerName, max(linux_server_details.logDate) AS maxDate FROM linux_server_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_server_details.ServerName ) AS linTabMax ON (linTabMax.ServerName = linServerHome.ServerName) AND (linTabMax.maxDate = linServerHome.logDate) LEFT JOIN ( SELECT linProcessorDet.ServerName, groupUniqArray(linProcessorDet.model_name) AS modelName, groupUniqArray(linProcessorDet.cpu_mhz) AS cpumhz, count(linProcessorDet.processor) AS processorCount, groupUniqArray(linProcessorDet.cpu_cores) AS corePerProcessor, sum(linProcessorDet.cpu_cores) AS noOfCores FROM linux_processor_details AS linProcessorDet INNER JOIN ( SELECT linux_processor_details.ServerName, max(linux_processor_details.logDate) AS maxDate FROM linux_processor_details WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_processor_details.ServerName ) AS linProMax ON (linProMax.ServerName = linProcessorDet.ServerName) AND (linProMax.maxDate = linProcessorDet.logDate) WHERE linProcessorDet.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linProcessorDet.ServerName ) AS processorInfo ON (processorInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linCollectDate.ServerName, any(linCollectDate.collection_date_with_timezone) AS collectionDate FROM linux_collection_date AS linCollectDate INNER JOIN ( SELECT linux_collection_date.ServerName, max(linux_collection_date.logDate) AS maxDate FROM linux_collection_date WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_collection_date.ServerName ) AS linColMax ON (linColMax.ServerName = linCollectDate.ServerName) AND (linColMax.maxDate = linCollectDate.logDate) WHERE linCollectDate.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linCollectDate.ServerName ) AS collectionDateInfo ON (collectionDateInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linEtcOS.ServerName, any(linEtcOS.os_name) AS os_name, any(linEtcOS.os_version) AS os_version FROM linux_etc_osrelease AS linEtcOS INNER JOIN ( SELECT linux_etc_osrelease.ServerName, max(logDate) AS maxDate FROM linux_etc_osrelease WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_etc_osrelease.ServerName ) AS linetcOSMax ON (linetcOSMax.ServerName = linEtcOS.ServerName) AND (linetcOSMax.maxDate = linEtcOS.logDate) WHERE linEtcOS.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linEtcOS.ServerName ) AS etcOSReleaseInfo ON (etcOSReleaseInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT lindmidecodeSysinfo.ServerName, any(if(length(lindmidecodeSysinfo.product_name) > 0, CONCAT(lindmidecodeSysinfo.manufacturer, ' ',lindmidecodeSysinfo.product_name),'')) AS serverModeld FROM linux_dmidecode_sysinfo AS lindmidecodeSysinfo INNER JOIN ( SELECT linux_dmidecode_sysinfo.ServerName, max(linux_dmidecode_sysinfo.logDate) AS maxDate FROM linux_dmidecode_sysinfo WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS dmidecodeMax ON (dmidecodeMax.ServerName = lindmidecodeSysinfo.ServerName) AND (dmidecodeMax.maxDate = lindmidecodeSysinfo.logDate) WHERE lindmidecodeSysinfo.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY lindmidecodeSysinfo.ServerName ) AS dmiSysInfo ON (dmiSysInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linMemInfo.ServerName, any(linMemInfo.size_data) as sizeData, any(linMemInfo.size_metric) as sizeMetric, any(multiIf(lower(linMemInfo.size_metric)='kb',toFloat32(linMemInfo.size_data/(1024*1024)), lower(linMemInfo.size_metric)='mb',toFloat32(linMemInfo.size_data/(1024)),lower(linMemInfo.size_metric)='gb',toFloat32(linMemInfo.size_data),toFloat32(linMemInfo.size_data))) as memory FROM linux_memory_info AS linMemInfo INNER JOIN ( SELECT linux_memory_info.ServerName, max(linux_memory_info.logDate) AS maxDate FROM linux_memory_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linux_memory_info.ServerName ) AS memMax ON (memMax.ServerName = linMemInfo.ServerName) AND (memMax.maxDate = linMemInfo.logDate) WHERE linMemInfo.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' and lower(linMemInfo.property) = 'memtotal' GROUP BY linMemInfo.ServerName ) AS linMemoryInfo ON (linMemoryInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, round(sum(sizegb),3) as nassize, round(sum(usedgb),3) as nasusedsize, round(sum(availableSizemb),3) as nasunusedsize FROM ( SELECT ServerName, file_system, mounted_on, any(toFloat32(size/(1024*1024))) as sizegb, any(toFloat32(used/(1024*1024))) as usedgb, any(toFloat32(available_size/(1024*1024))) as availableSizemb FROM linux_file_system_kt AS linFsNfs INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_file_system_kt WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS memMax ON (memMax.ServerName = linFsNfs.ServerName) AND (memMax.maxDate = linFsNfs.logDate) WHERE linFsNfs.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' and lower(linFsNfs.file_system_type) LIKE '%nfs%' GROUP BY linFsNfs.ServerName,linFsNfs.file_system,linFsNfs.mounted_on )GROUP BY ServerName ) AS linNfsInfo ON (linNfsInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT linNetwork.ServerName, groupArray(if(length(linNetwork.inet_addr) > 0,linNetwork.inet_addr,NULL)) AS ipAddress FROM linux_network_ifconfig_a AS linNetwork INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_network_ifconfig_a WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS networkMax ON (networkMax.ServerName = linNetwork.ServerName) AND (networkMax.maxDate = linNetwork.logDate) WHERE linNetwork.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linNetwork.ServerName ) AS linNetworkInfo ON (linNetworkInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(pv_name) as pv_count, round(sum(PVSizeCalc),3) as PVTotalgb, round(sum(PVAllocCalc),3) as PVAllocgb, round(sum(PVFreeCalc),3) as PVFreegb FROM ( SELECT ServerName, pv_name, any(multiIf((pv_size_metric ILIKE '%kbyte%'), pv_size/(1024*1024),(pv_size_metric ILIKE '%mib%') OR (pv_size_metric ILIKE '%mb%'), pv_size / 1024, (pv_size_metric ILIKE '%gib%') OR (pv_size_metric ILIKE '%gb%'), pv_size, (pv_size_metric ILIKE '%tib%') OR (pv_size_metric ILIKE '%tb%'), pv_size*1024, 0)) as PVSizeCalc, any(multiIf((pe_size_metric ILIKE '%kbyte%'), total_pe*pe_size/(1024*1024), (pe_size_metric ILIKE '%mib%'), total_pe*pe_size/(1024),0)) as PVTotalCalc, any(multiIf((pe_size_metric ILIKE '%kbyte%'), allocated_pe*pe_size/(1024*1024), (pe_size_metric ILIKE '%mib%'), allocated_pe*pe_size/(1024),0)) as PVAllocCalc, any(multiIf((pe_size_metric ILIKE '%kbyte%'), free_pe*pe_size/(1024*1024), (pe_size_metric ILIKE '%mib%'), free_pe*pe_size/(1024),0)) as PVFreeCalc FROM linux_physical_volume linPhyVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_physical_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linPVMax ON (linPVMax.ServerName = linPhyVol.ServerName) AND (linPVMax.maxDate = linPhyVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,pv_name )group by ServerName )AS PVInfo ON (PVInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(vg_name) AS vg_count, round(sum(VGSizeCalc), 3) AS VGTotalgb, round(sum(VGAllocCalc), 3) AS VGAllocgb, round(sum(VGFreeCalc), 3) AS VGFreegb FROM ( SELECT ServerName, vg_name, any(multiIf((vg_size_metric ILIKE '%kbyte%'), vg_size/(1024*1024),(vg_size_metric ILIKE '%mib%') OR (vg_size_metric ILIKE '%mb%'), vg_size / 1024, (vg_size_metric ILIKE '%gib%') OR (vg_size_metric ILIKE '%gb%'), vg_size, (vg_size_metric ILIKE '%tib%') OR (vg_size_metric ILIKE '%tb%'), vg_size*1024, 0)) as VGSizeCalc, any(multiIf(pe_size_metric ILIKE '%kbyte%', (total_pe * pe_size) / (1024 * 1024), (pe_size_metric ILIKE '%mib%') OR (pe_size_metric ILIKE '%mb%'), (total_pe * pe_size) / 1024, 0)) AS VGTotalCalc, any(multiIf(pe_size_metric ILIKE '%kbyte%', (alloc_pe * pe_size) / (1024 * 1024), (pe_size_metric ILIKE '%mib%') OR (pe_size_metric ILIKE '%mb%'), (alloc_pe * pe_size) / 1024, 0)) AS VGAllocCalc, any(multiIf(pe_size_metric ILIKE '%kbyte%', (free_pe * pe_size) / (1024 * 1024), (pe_size_metric ILIKE '%mib%') OR (pe_size_metric ILIKE '%mb%'), (free_pe * pe_size) / 1024, 0)) AS VGFreeCalc FROM linux_volume_group AS linVolGrp INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_volume_group WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVGMax ON (linVGMax.ServerName = linVolGrp.ServerName) AND (linVGMax.maxDate = linVolGrp.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName, vg_name )GROUP BY ServerName )AS VGInfo ON (VGInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(lv_uuid) as lv_count, round(sum(LVSizeCalc),3) as LVTotalgb FROM ( SELECT ServerName, lv_uuid, any(multiIf((lv_size_metric ILIKE '%kbyte%'), lv_size/(1024*1024),(lv_size_metric ILIKE '%mib%') OR (lv_size_metric ILIKE '%mb%'), lv_size / 1024, (lv_size_metric ILIKE '%gib%') OR (lv_size_metric ILIKE '%gb%'), lv_size, (lv_size_metric ILIKE '%tib%') OR (lv_size_metric ILIKE '%tb%'), lv_size*1024, 0)) as LVSizeCalc FROM linux_logical_volume linLogVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_logical_volume GROUP BY ServerName ) AS linLVMax ON (linLVMax.ServerName = linLogVol.ServerName) AND (linLVMax.maxDate = linLogVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,lv_uuid )group by ServerName )AS LVInfo ON (LVInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, any(vxDgDisks) as vxDgDisks, any(VxDiskGroupCount) as VxDiskGroupCount, any((TotalDskPublength+TotalDskPvtlength)/(1024*1024)) as VxDiskGroupSize FROM ( SELECT ServerName, countDistinct(device) as vxDgDisks, length(groupUniqArrayArray(diskGroup)) as VxDiskGroupCount, sum(DskPublength) as TotalDskPublength, sum(DskPvtlength) as TotalDskPvtlength FROM ( SELECT ServerName, device, groupArrayDistinct(disk_group) as diskGroup, any(publen/2) as DskPublength, any(privlen/2) as DskPvtlength FROM linux_vx_diskgroup_list linVxDG INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_diskgroup_list WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVxDGMax ON (linVxDGMax.ServerName = linVxDG.ServerName) AND (linVxDGMax.maxDate = linVxDG.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,device )group by ServerName )group by ServerName )AS VxDGInfo ON (VxDGInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(vxVolume) as vxVolCnt, sum(volSize/(1024*1024)) as vxVolSize FROM ( SELECT ServerName, v_name, disk_group, any(CONCAT(v_name,disk_group)) as vxVolume, any(length/2) as volSize FROM linux_vx_volume linVxVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVxVolMax ON (linVxVolMax.ServerName = linVxVol.ServerName) AND (linVxVolMax.maxDate = linVxVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,v_name,disk_group )group by ServerName )AS VxVolInfo ON (VxVolInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, sum(fsSize) as FSSize, sum(fsUsed) as FSUsedSize, sum(fsAvailSize) as FSUnusedSize FROM ( SELECT lindf.ServerName as ServerName, lindf.file_system as file_system, lindf.mounted_on as mounted_on, any(lindf.volume_name) as volume_name, any(lindf.disk_group) as disk_group, any(lindf.lv_name) as lv_name, any(lindf.file_system_type) as fileSystemType, any(lindf.size/(1024*1024)) as fsSize, any(lindf.used/(1024*1024)) as fsUsed, any(lindf.available_size/(1024*1024)) as fsAvailSize FROM linux_file_system_kt lindf INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_file_system_kt WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linDFMax ON (linDFMax.ServerName = lindf.ServerName) AND (linDFMax.maxDate = lindf.logDate) LEFT JOIN ( SELECT fdiskpartifs.ServerName as ServerName, fdiskpartifs.partition_name as partition_name FROM linux_disk_fdisk_partition_info fdiskpartifs INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartifsMax ON (diskpartifsMax.ServerName = fdiskpartifs.ServerName) AND (diskpartifsMax.maxDate = fdiskpartifs.logDate) WHERE fdiskpartifs.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY fdiskpartifs.ServerName,fdiskpartifs.partition_name ) AS linStdParti ON (linStdParti.ServerName = lindf.ServerName) AND (linStdParti.partition_name = lindf.file_system) LEFT JOIN ( SELECT ServerName, v_name, disk_group, any(CONCAT(v_name,disk_group)) as vxvol FROM linux_vx_volume linVxVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linVxVolMax ON (linVxVolMax.ServerName = linVxVol.ServerName) AND (linVxVolMax.maxDate = linVxVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,v_name,disk_group ) AS linVxFsRel ON (linVxFsRel.ServerName = lindf.ServerName) AND (linVxFsRel.v_name = lindf.volume_name) AND (linVxFsRel.disk_group = lindf.disk_group) LEFT JOIN ( SELECT ServerName, lv_uuid, lv_name FROM linux_logical_volume linLogVol INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_logical_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linLVMax ON (linLVMax.ServerName = linLogVol.ServerName) AND (linLVMax.maxDate = linLogVol.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,lv_uuid,lv_name ) AS linLVFsRel ON (linLVFsRel.ServerName = lindf.ServerName) AND (linLVFsRel.lv_name = lindf.lv_name) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' and file_system_type not ilike '%tmpfs%' and file_system_type not ilike '%nfs%' and (length(linStdParti.partition_name) > 0 or length(linVxFsRel.vxvol) > 0 or length(linLVFsRel.lv_name) > 0) group by ServerName,file_system,mounted_on )group by ServerName )AS FSInfo ON (FSInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(kernel) as asmDisksKer, countDistinct(name) as asmDisksName FROM ( SELECT linOraRul.ServerName as ServerName, linOraRul.kernel as kernel, linOraRul.name as name FROM linux_oracle_rules linOraRul INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_oracle_rules WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS linOraRulMax ON (linOraRulMax.ServerName = linOraRul.ServerName) AND (linOraRulMax.maxDate = linOraRul.logDate) INNER JOIN ( SELECT fdiskpartior.ServerName as ServerName, fdiskpartior.partition_name as partition_name, fdiskpartior.partition_disk_name as partition_disk_name FROM linux_disk_fdisk_partition_info fdiskpartior INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartiMax ON (diskpartiMax.ServerName = fdiskpartior.ServerName) AND (diskpartiMax.maxDate = fdiskpartior.logDate) ) AS linOraRulInfo ON (linOraRulInfo.ServerName = linOraRul.ServerName) AND (linOraRulInfo.partition_disk_name = linOraRul.kernel) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,kernel,name )group by ServerName )AS ASMDiskInfo ON (ASMDiskInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, length(groupArrayDistinct(if(startsWith(devic_name_alias,asmCondition),devic_name_alias,NULL))) as asmRuleDisks FROM ( SELECT asmRule.ServerName as ServerName, if(position(asmRule.env_dm_name, '*') > 0, substring(asmRule.env_dm_name, 1, position(asmRule.env_dm_name, '*') - 1), asmRule.env_dm_name) AS asmCondition, asmRule.env_dm_name as env_dm_name, MpASMJn.device_name as device_name, MpASMJn.devic_name_alias as devic_name_alias FROM linux_asm_rules asmRule INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_asm_rules WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS asmMax ON (asmMax.ServerName = asmRule.ServerName) AND (asmMax.maxDate = asmRule.logDate) LEFT JOIN ( SELECT ServerName, device_name, devic_name_alias FROM linux_multipath_info mpathasm INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpasmMax ON (mpasmMax.ServerName = mpathasm.ServerName) AND (mpasmMax.maxDate = mpathasm.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName,device_name,devic_name_alias ) AS MpASMJn ON (MpASMJn.ServerName = asmRule.ServerName) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' )group by ServerName )AS ASMRulesInfo ON (ASMRulesInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, 'Oracle RAC' as clusterType, groupArrayDistinct(if(length(host) > 0, host, NULL)) AS clusterMembers, '' as clusterName FROM linux_oracle_crs_stst oraClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_oracle_crs_stst WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS oraMax ON (oraMax.ServerName = oraClus.ServerName) AND (oraMax.maxDate = oraClus.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName UNION DISTINCT SELECT ServerName, 'VCS' as clusterType, groupArrayDistinct(system) as clusterMembers, '' as clusterName FROM linux_vcs_hastatus_system vcsClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vcs_hastatus_system WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS vcsMax ON (vcsMax.ServerName = vcsClus.ServerName) AND (vcsMax.maxDate = vcsClus.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName UNION DISTINCT SELECT gpfsClus.ServerName as ServerName, any(if(length(gpfsClus.tiebreaker_disks) > 0, 'GPFS-Tiebreaker', 'GPFS')) as clusterType, any(gpfsNode.clusterMem) as clusterMembers, any(gpfsClus.clustername) as clusterName FROM linux_gpfs_mmlsconfig gpfsClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_gpfs_mmlsconfig WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS gpfsMax ON (gpfsMax.ServerName = gpfsClus.ServerName) AND (gpfsMax.maxDate = gpfsClus.logDate) LEFT JOIN ( SELECT ServerName, groupArrayDistinct(node_name) as clusterMem FROM linux_gpfs_mmgetstat_las gpfstClus INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_gpfs_mmgetstat_las WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS gpfstMax ON (gpfstMax.ServerName = gpfstClus.ServerName) AND (gpfstMax.maxDate = gpfstClus.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName ) AS gpfsNode ON (gpfsNode.ServerName = gpfsClus.ServerName) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' group by ServerName )AS clusterInfo ON (clusterInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, groupArrayDistinct(concat(storageType,toString(storageArrSize))) as stoArrBySize, groupArrayDistinct(concat(storageType,'(',toString(LENGTH(disksStoType)),')')) as stoArrByCnt, SUM(diskSizeStoType) as diskSize, length(groupUniqArrayArray(disksStoType)) as diskCnt FROM ( SELECT ServerName, storageType, SUM(diskSizes) as diskSizeStoType, groupArrayDistinct(concat(toString(rderivedSize),'(',toString(length(disks)),')')) as storageArrSize, groupUniqArrayArray(disks) as disksStoType FROM ( SELECT ServerName, if(length(derivedProduct) > 0,concat(trim(derivedVendor),'-',trim(derivedProduct)),derivedVendor) as storageType, round(derivedSize,3) as rderivedSize, SUM(round(derivedSize,3)) as diskSizes, groupArrayDistinct(derivedDisk) as disks FROM ( SELECT ServerName, multiIf(length(mp1alias) > 0,mp1alias,length(mp2alias) > 0,mp2alias,length(mp3alias) > 0,mp3alias, length(pppseudo_name) > 0,pppseudo_name,length(pp2pseudo_name) > 0,pp2pseudo_name,length(vxpsedoname) > 0,vxpsedoname,length(inqwwn) > 0,inqwwn,length(qwwuln) > 0,qwwuln,length(dppvname) > 0,disk_name,length(dpfsname) > 0,disk_name,NULL) as derivedDisk, any(multiIf(diskSizeInq != 0,diskSizeInq,fdiskSize != 0,fdiskSize,qDiskSize != 0,qDiskSize,mpdsize != 0,mpdsize,mp2dsize != 0,mp2dsize, mp3dsize != 0,mp3dsize,0)) as derivedSize, any(multiIf(length(vendorInq) > 0,vendorInq,length(qvendor) > 0,qvendor, length(mpdvendor1) > 0,if(position(mpdvendor1, ',') > 0, substring(mpdvendor1, 1, position(mpdvendor1, ',') - 1), mpdvendor1), length(mp2vendor) > 0,if(position(mp2vendor, ',') > 0, substring(mp2vendor, 1, position(mp2vendor, ',') - 1), mp2vendor), length(mp3vendor) > 0,if(position(mp3vendor, ',') > 0, substring(mp3vendor, 1, position(mp3vendor, ',') - 1), mp3vendor),NULL)) as derivedVendor, any(multiIf(length(productInq) > 0,productInq,length(qproductID) > 0,qproductID, length(mpdvendor1) > 0,if(position(mpdvendor1, ',') > 0, substring(mpdvendor1, position(mpdvendor1, ',') + 1), mpdvendor1), length(mp2vendor) > 0,if(position(mp2vendor, ',') > 0, substring(mp2vendor, position(mp2vendor, ',') + 1), mp2vendor), length(mp3vendor) > 0,if(position(mp3vendor, ',') > 0, substring(mp3vendor, position(mp3vendor, ',') + 1), mp3vendor),NULL)) as derivedProduct FROM ( SELECT linfDisk.ServerName as ServerName, linfDisk.disk_name as disk_name, linfDisk.device_name as device_name, any(linInqDiskWWN.wwn) as inqwwn, any(linQlogicDisk.wwuln) as qwwuln, any(linMPathDiskInfo1.devic_name_alias) as mp1alias, any(linMPathDiskInfo2.devic_name_alias) as mp2alias, any(linMPathDiskInfo3.devic_name_alias) as mp3alias, any(linPPDiskInfo.pseudo_name) as pppseudo_name, any(linPPDiskInfo2.pseudo_name) as pp2pseudo_name, any(linVxDmpInfo.name) as vxpsedoname, any(diskPartitionPVlk.partition_name) as dppvname, any(diskPartitionFSlk.partition_name) as dpfsname, any(linInqNoDots.diskSizeInq) as diskSizeInq, any(toFloat32(linfDisk.size_in_bytes/(1024*1024*1024))) as fdiskSize, any(linQlogicDisk.qDiskSize) as qDiskSize, any(linMPathDiskInfo1.mpDiskSize1) as mpdsize, any(linMPathDiskInfo2.mpDiskSize2) as mp2dsize, any(linMPathDiskInfo3.mpDiskSize3) as mp3dsize, any(linInqNoDots.vendor) as vendorInq, any(linInqNoDots.product) as productInq, any(linQlogicDisk.productVendor) as qvendor, any(linQlogicDisk.productID) as qproductID, any(linMPathDiskInfo1.vendor1) as mpdvendor1, any(linMPathDiskInfo2.vendor2) as mp2vendor, any(linMPathDiskInfo3.vendor3) as mp3vendor FROM linux_disk_fdisk_info linfDisk INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS fdiskMax ON (fdiskMax.ServerName = linfDisk.ServerName) AND (fdiskMax.maxDate = linfDisk.logDate) LEFT JOIN ( SELECT ServerName, disk_name, any(device_name) as device_name, any(vendor) as vendor, any(product) as product, any(toFloat32OrZero(cap_in_kb)/(1024*1024)) as diskSizeInq FROM linux_inq_no_dots_btl InqNodots INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_inq_no_dots_btl WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS inqnodotsMax ON (inqnodotsMax.ServerName = InqNodots.ServerName) AND (inqnodotsMax.maxDate = InqNodots.logDate) WHERE InqNodots.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY InqNodots.ServerName,InqNodots.disk_name ) AS linInqNoDots ON (linInqNoDots.ServerName = linfDisk.ServerName) AND (linInqNoDots.disk_name = linfDisk.disk_name) LEFT JOIN ( SELECT ServerName, device, any(wwn) as wwn FROM linux_inq_wwn_all linInqwwn INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_inq_wwn_all WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS inqWwnMax ON (inqWwnMax.ServerName = linInqwwn.ServerName) AND (inqWwnMax.maxDate = linInqwwn.logDate) WHERE linInqwwn.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linInqwwn.ServerName,linInqwwn.device ) AS linInqDiskWWN ON (linInqDiskWWN.ServerName = linfDisk.ServerName) AND (linInqDiskWWN.device = linfDisk.disk_name) LEFT JOIN ( SELECT ServerName, diskName, any(wwuln) as wwuln, any(product_vendor) as productVendor, any(product_id) as productID, any(lun) as lunId, any(multiIf(lower(qLogicLun.metric)='kb',toFloat32(qLogicLun.size/(1024*1024)), lower(qLogicLun.metric)='mb',toFloat32(qLogicLun.size/(1024)),lower(qLogicLun.metric)='gb',toFloat32(qLogicLun.size),lower(qLogicLun.metric)='tb',toFloat32(qLogicLun.size*1024),toFloat32(qLogicLun.size))) as qDiskSize FROM linux_qlogic_device_lun_info qLogicLun INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_qlogic_device_lun_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS qLogicDeviceMax ON (qLogicDeviceMax.ServerName = qLogicLun.ServerName) AND (qLogicDeviceMax.maxDate = qLogicLun.logDate) LEFT ARRAY JOIN disk_name AS diskName WHERE qLogicLun.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY qLogicLun.ServerName,diskName ) AS linQlogicDisk ON (linQlogicDisk.ServerName = linfDisk.ServerName) AND (linQlogicDisk.diskName = linfDisk.disk_name) LEFT JOIN ( SELECT ServerName, host_io_path, pseudo_name FROM linux_powerpath_subpaths linPPdiskPath INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS ppdiskMax ON (ppdiskMax.ServerName = linPPdiskPath.ServerName) AND (ppdiskMax.maxDate = linPPdiskPath.logDate) WHERE linPPdiskPath.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linPPdiskPath.ServerName,linPPdiskPath.host_io_path,linPPdiskPath.pseudo_name ) AS linPPDiskInfo ON (linPPDiskInfo.ServerName = linfDisk.ServerName) AND (linPPDiskInfo.host_io_path = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, pseudo_name FROM linux_powerpath_subpaths linPPdiskPseudo INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS ppdiskMax ON (ppdiskMax.ServerName = linPPdiskPseudo.ServerName) AND (ppdiskMax.maxDate = linPPdiskPseudo.logDate) WHERE linPPdiskPseudo.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linPPdiskPseudo.ServerName,linPPdiskPseudo.pseudo_name ) AS linPPDiskInfo2 ON (linPPDiskInfo2.ServerName = linfDisk.ServerName) AND (linPPDiskInfo2.pseudo_name = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, device_name, any(devic_name_alias) as devic_name_alias, any(device_wwid) as deviceWWid1, any(vendor) as vendor1, any(multiIf(lower(mpathDevName.size_of_dm_metrics) like '%k%',toFloat32(mpathDevName.size_of_dm/(1024*1024)), lower(mpathDevName.size_of_dm_metrics) like '%m%',toFloat32(mpathDevName.size_of_dm/(1024)),lower(mpathDevName.size_of_dm_metrics) like '%g%',toFloat32(mpathDevName.size_of_dm),lower(mpathDevName.size_of_dm_metrics) like '%t%',toFloat32(mpathDevName.size_of_dm*1024),toFloat32(mpathDevName.size_of_dm))) as mpDiskSize1 FROM linux_multipath_info mpathDevName INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpOneMax ON (mpOneMax.ServerName = mpathDevName.ServerName) AND (mpOneMax.maxDate = mpathDevName.logDate) WHERE mpathDevName.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY mpathDevName.ServerName,mpathDevName.device_name ) AS linMPathDiskInfo1 ON (linMPathDiskInfo1.ServerName = linfDisk.ServerName) AND (linMPathDiskInfo1.device_name = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, devic_name_alias, any(device_wwid) as deviceWWid2, any(vendor) as vendor2, any(multiIf(lower(mpathDevAlias.size_of_dm_metrics) like '%k%',toFloat32(mpathDevAlias.size_of_dm/(1024*1024)), lower(mpathDevAlias.size_of_dm_metrics) like '%m%',toFloat32(mpathDevAlias.size_of_dm/(1024)),lower(mpathDevAlias.size_of_dm_metrics) like '%g%',toFloat32(mpathDevAlias.size_of_dm),lower(mpathDevAlias.size_of_dm_metrics) like '%t%',toFloat32(mpathDevAlias.size_of_dm*1024),toFloat32(mpathDevAlias.size_of_dm))) as mpDiskSize2 FROM linux_multipath_info mpathDevAlias INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpTwoMax ON (mpTwoMax.ServerName = mpathDevAlias.ServerName) AND (mpTwoMax.maxDate = mpathDevAlias.logDate) WHERE mpathDevAlias.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY mpathDevAlias.ServerName,mpathDevAlias.devic_name_alias ) AS linMPathDiskInfo2 ON (linMPathDiskInfo2.ServerName = linfDisk.ServerName) AND (linMPathDiskInfo2.devic_name_alias = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, sysfs_name, any(devic_name_alias) as devic_name_alias, any(device_wwid) as deviceWWid3, any(vendor) as vendor3, any(multiIf(lower(mpathSysfsName.size_of_dm_metrics) like '%k%',toFloat32(mpathSysfsName.size_of_dm/(1024*1024)), lower(mpathSysfsName.size_of_dm_metrics) like '%m%',toFloat32(mpathSysfsName.size_of_dm/(1024)),lower(mpathSysfsName.size_of_dm_metrics) like '%g%',toFloat32(mpathSysfsName.size_of_dm),lower(mpathSysfsName.size_of_dm_metrics) like '%t%',toFloat32(mpathSysfsName.size_of_dm*1024),toFloat32(mpathSysfsName.size_of_dm))) as mpDiskSize3 FROM linux_multipath_info mpathSysfsName INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS mpthreeMax ON (mpthreeMax.ServerName = mpathSysfsName.ServerName) AND (mpthreeMax.maxDate = mpathSysfsName.logDate) WHERE mpathSysfsName.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY mpathSysfsName.ServerName,mpathSysfsName.sysfs_name ) AS linMPathDiskInfo3 ON (linMPathDiskInfo3.ServerName = linfDisk.ServerName) AND (linMPathDiskInfo3.sysfs_name = linfDisk.device_name) LEFT JOIN ( SELECT ServerName, subpath, any(name) as name FROM linux_vx_dmp vxdmp INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_vx_dmp WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS vxdmpMax ON (vxdmpMax.ServerName = vxdmp.ServerName) AND (vxdmpMax.maxDate = vxdmp.logDate) WHERE vxdmp.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY vxdmp.ServerName,vxdmp.subpath ) AS linVxDmpInfo ON (linVxDmpInfo.ServerName = linfDisk.ServerName) AND (linVxDmpInfo.subpath = linfDisk.device_name) LEFT JOIN ( SELECT fdiskparti.ServerName as ServerName, fdiskparti.partition_name as partition_name, any(fdiskparti.disk_name) as disk_name, any(fdiskparti.partition_disk_name) as partition_disk_name FROM linux_disk_fdisk_partition_info fdiskparti INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartiMax ON (diskpartiMax.ServerName = fdiskparti.ServerName) AND (diskpartiMax.maxDate = fdiskparti.logDate) INNER JOIN ( SELECT ServerName, pv_name FROM linux_physical_volume linhdppv INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_physical_volume WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS pvMax ON (pvMax.ServerName = linhdppv.ServerName) AND (pvMax.maxDate = linhdppv.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName,pv_name ) AS linPVdisklk ON (linPVdisklk.ServerName = fdiskparti.ServerName) AND (linPVdisklk.pv_name = fdiskparti.partition_name) WHERE fdiskparti.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY fdiskparti.ServerName,fdiskparti.partition_name ) AS diskPartitionPVlk ON (diskPartitionPVlk.ServerName = linfDisk.ServerName) AND (diskPartitionPVlk.disk_name = linfDisk.disk_name) LEFT JOIN ( SELECT fdiskparti2.ServerName as ServerName, fdiskparti2.partition_name as partition_name, any(fdiskparti2.disk_name) as disk_name, any(fdiskparti2.partition_disk_name) as partition_disk_name FROM linux_disk_fdisk_partition_info fdiskparti2 INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_disk_fdisk_partition_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS diskpartiMax2 ON (diskpartiMax2.ServerName = fdiskparti2.ServerName) AND (diskpartiMax2.maxDate = fdiskparti2.logDate) INNER JOIN ( SELECT ServerName, file_system, mounted_on FROM linux_file_system_kt linfspartition INNER JOIN ( SELECT ServerName, max(logDate) AS maxDate FROM linux_file_system_kt WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AS fsMax ON (fsMax.ServerName = linfspartition.ServerName) AND (fsMax.maxDate = linfspartition.logDate) WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName,file_system,mounted_on ) AS linPVdisklk ON (linPVdisklk.ServerName = fdiskparti2.ServerName) AND (linPVdisklk.file_system = fdiskparti2.partition_name) WHERE fdiskparti2.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY fdiskparti2.ServerName,fdiskparti2.partition_name ) AS diskPartitionFSlk ON (diskPartitionFSlk.ServerName = linfDisk.ServerName) AND (diskPartitionFSlk.disk_name = linfDisk.disk_name) WHERE linfDisk.siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY linfDisk.ServerName,linfDisk.disk_name,linfDisk.device_name )GROUP BY ServerName,derivedDisk )GROUP BY ServerName,storageType,derivedSize )GROUP BY ServerName,storageType )GROUP BY ServerName )AS diskInfo ON (diskInfo.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(devic_name_alias) as mpdiskCnt FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_multipath_info WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName )GROUP BY ServerName )AS mpDiskCntTab ON (mpDiskCntTab.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(name) as vxdiskCnt FROM linux_vx_dmp WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_vx_dmp WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName )GROUP BY ServerName )AS vxDiskCntTab ON (vxDiskCntTab.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, countDistinct(pseudo_name) as ppdiskCnt FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_powerpath_subpaths WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName )GROUP BY ServerName )AS ppDiskCntTab ON (ppDiskCntTab.ServerName = linServerHome.ServerName) LEFT JOIN ( SELECT ServerName, groupUniqArrayArray(locAddrPort) as establishedPort, groupArrayDistinct(concat(foreign_address,'(',arrayStringConcat(forAddrPort,','),')')) as dependencyServer FROM ( SELECT ServerName, foreign_address, groupArrayDistinct(local_address_port) as locAddrPort, groupArrayDistinct(foreign_address_port) as forAddrPort FROM linux_netstat_a WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' AND (ServerName, logDate) IN ( SELECT ServerName, max(logDate) AS logDate FROM linux_netstat_a WHERE siteKey = 'b8f6a026-0a0b-4aca-a24a-767a0fd25316' GROUP BY ServerName ) AND is_required=1 AND upper(state) = 'ESTABLISHED' AND local_address != foreign_address GROUP BY ServerName,foreign_address )GROUP BY ServerName )AS netstatInfo ON (netstatInfo.ServerName = linServerHome.ServerName) GROUP BY siteKey,ServerName";
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
			@RequestBody String chartParams,
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ParseException {

		JSONParser jsonParser = new JSONParser();

		JSONObject Object = (JSONObject) jsonParser.parse(chartParams.toString());
		System.out.println("log 1 : " + Object);

		String reportLabel = Object.get("reportLabel") != null && Object.get("reportLabel").toString().isEmpty() ? "" : Object.get("reportLabel").toString();
		
		JSONObject jsonObject = new JSONObject();
		if(reportLabel.contains("Privileged")) {
			jsonObject =  dataframeService.prepareChartForTanium(Object);
		}else {
			jsonObject = dataframeService.prepareChart(Object);
		}
		
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
					|| reportCategory.trim().equalsIgnoreCase("Compatibility")) {

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
