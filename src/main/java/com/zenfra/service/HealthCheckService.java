package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.HealthCheckDao;
import com.zenfra.dao.HealthCheckDisplayDao;
import com.zenfra.model.HealthCheck;
import com.zenfra.model.HealthCheckModel;
import com.zenfra.model.SiteModel;
import com.zenfra.model.Users;
import com.zenfra.model.ZKConstants;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class HealthCheckService {

	final ObjectMapper map = new ObjectMapper();

	@Autowired
	HealthCheckDao healthCheckDao;

	@Autowired
	HealthCheckDisplayDao healthCheckDisplayDao;

	@Autowired
	CommonFunctions commonFunctions;

	@Autowired
	UserCreateService userCreateService;

	@Autowired
	SiteService siteService;

	private SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

	private ObjectMapper objectMapper = new ObjectMapper();
	private JSONParser jSONParser = new JSONParser();

	public HealthCheck saveHealthCheck(HealthCheck healthCheck) {
		healthCheck.setHealthCheckId(commonFunctions.generateRandomId());
		healthCheckDao.saveEntity(HealthCheck.class, healthCheck);
		HealthCheck savedObj = (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class,
				healthCheck.getHealthCheckId());
		healthCheck.setHealthCheckId(savedObj.getHealthCheckId());
		return healthCheck;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getHealthCheck(String healthCheckId, String authUserId) throws ParseException {
		HealthCheck healthCheck = new HealthCheck();
		healthCheck.setHealthCheckId(healthCheckId);
		JSONObject healthCheckModel = new JSONObject();
		HealthCheck savedObj = (HealthCheck) healthCheckDao.getEntityByColumn(
				"select * from health_check where health_check_id='" + healthCheckId + "' and is_active='true'",
				HealthCheck.class);
		savedObj.setAuthUserId(authUserId);
		if (savedObj != null) {
		healthCheckModel = convertEntityToModel(savedObj);

		System.out.println("healthCheckModel::" + healthCheckModel);
		}



		return healthCheckModel;
		}

	public HealthCheck getHealthCheckObject(String healthCheckId) {
		HealthCheck healthCheck = new HealthCheck();
		healthCheck.setHealthCheckId(healthCheckId);
		HealthCheck healthCheckObj = (HealthCheck) healthCheckDao.getEntityByColumn(
				"select * from health_check where health_check_id='" + healthCheckId + "' and is_active='true'",
				HealthCheck.class);
		return healthCheckObj;
	}

	public JSONObject updateHealthCheck(HealthCheck healthCheck) throws ParseException {
		healthCheckDao.updateEntity(HealthCheck.class, healthCheck);
		HealthCheck savedObj = (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class,
				healthCheck.getHealthCheckId());
		JSONObject healthCheckModel = convertEntityToModel(savedObj);
		return healthCheckModel;
	}

	public boolean deleteHealthCheck(HealthCheck healthCheck) {
		// TODO Auto-generated method stub
		healthCheck = (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
		healthCheck.setActive(false);
		healthCheck.setUpdateDate(new Date());
		healthCheck.setUpdateBy(healthCheck.getUserId());
		healthCheck.setUserId(healthCheck.getUserId());
		return healthCheckDao.updateEntity(HealthCheck.class, healthCheck);
	}

	public HealthCheck convertToEntity(HealthCheckModel healthCheckModel, String type) {
		HealthCheck healthCheck = new HealthCheck();
		if (type.equalsIgnoreCase("update")) {
			healthCheck = getHealthCheckObject(healthCheckModel.getHealthCheckId());
		}
		healthCheck.setHealthCheckId(healthCheckModel.getHealthCheckId());
		healthCheck.setSiteKey(healthCheckModel.getSiteKey());
		if (healthCheckModel.getComponentType() != null) {
			healthCheck.setComponentType(healthCheckModel.getComponentType());
		}
		if (healthCheckModel.getHealthCheckName() != null) {
			healthCheck.setHealthCheckName(healthCheckModel.getHealthCheckName());
		}
		if (healthCheckModel.getReportName() != null) {
			healthCheck.setReportName(healthCheckModel.getReportName());
		}

		if (healthCheckModel.getReportBy() != null) {
			healthCheck.setReportBy(healthCheckModel.getReportBy());
		}
		if (healthCheckModel.getReportName() != null) {
			healthCheck.setReportName(healthCheckModel.getReportName());
		}

		if (healthCheckModel.getSiteAccessList() != null) {
//			healthCheck.setSiteAccessList(String.join(",", healthCheckModel.getSiteAccessList()));
			healthCheck.setSiteAccessList(
					map.convertValue(healthCheckModel.getSiteAccessList(), JSONArray.class).toJSONString());

		}

		if (healthCheckModel.getUserAccessList() != null) {
//			healthCheck.setUserAccessList(String.join(",", healthCheckModel.getUserAccessList()));
			healthCheck.setUserAccessList(
					map.convertValue(healthCheckModel.getUserAccessList(), JSONArray.class).toJSONString());
		}

		if (healthCheckModel.getAnalyticsType() != null) {
			healthCheck.setAnalyticsType(healthCheckModel.getAnalyticsType());
		}

		if (healthCheckModel.getReportCondition() != null) {
			healthCheck.setReportCondition(healthCheckModel.getReportCondition().toJSONString());
		}
		// ().replaceAll("\\s", "").replaceAll("\n", "").replaceAll("\r", "")
		healthCheck.setActive(true);
		healthCheck.setUserId(healthCheckModel.getAuthUserId());
		if (type.equalsIgnoreCase("update")) {
			healthCheck.setUpdateBy(healthCheckModel.getAuthUserId());
			healthCheck.setUpdateDate(new Date());
		} else {
			healthCheck.setCreateBy(healthCheckModel.getAuthUserId());
			healthCheck.setCreatedDate(new Date());
			healthCheck.setUpdateBy(healthCheckModel.getAuthUserId());
			healthCheck.setUpdateDate(new Date());
		}

		return healthCheck;
	}

	private JSONObject convertEntityToModel(HealthCheck healthCheck) throws ParseException {
		JSONObject response = new JSONObject();
		JSONParser parser = new JSONParser();
		String reportBy = "";
		/*
		 * JSONArray storageList = (JSONArray)
		 * parser.parse(ZKModel.getProperty(ZKConstants.STORAGE_LIST)); JSONArray
		 * serverList = (JSONArray)
		 * parser.parse(ZKModel.getProperty(ZKConstants.SERVER_LIST)); JSONArray
		 * switchList = (JSONArray)
		 * parser.parse(ZKModel.getProperty(ZKConstants.SWITCH_LIST));
		 * 
		 * String reportBy = ""; if
		 * (serverList.contains(healthCheck.getComponentType().toLowerCase())) {
		 * reportBy = "Server"; } else if
		 * (storageList.contains(healthCheck.getComponentType().toLowerCase())) {
		 * reportBy = "Storage"; } else if
		 * (switchList.contains(healthCheck.getComponentType().toLowerCase())) {
		 * reportBy = "Switch"; } if
		 * (healthCheck.getReportBy().equalsIgnoreCase("End-To-End-Basic")) { reportBy =
		 * "Server - Switch - Storage Summary";
		 * 
		 * } if (healthCheck.getReportBy().equalsIgnoreCase("End-To-End-Detail")) {
		 * reportBy = "Server - Switch - Storage"; }
		 */

		response.put("siteKey", healthCheck.getHealthCheckId());
		response.put("healthCheckName", healthCheck.getHealthCheckName());
		response.put("componentType", healthCheck.getComponentType());
		response.put("reportName", healthCheck.getReportName());
		response.put("reportBy", healthCheck.getReportBy());
		response.put("analyticsType", healthCheck.getAnalyticsType());
		try {
			String s = healthCheck.getReportCondition();
			ObjectMapper mapper = new ObjectMapper();
			JSONArray actualObj = mapper.readValue(s, JSONArray.class);
			response.put("reportCondition", actualObj);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

//		List<String> uList = new ArrayList<String>();
//		uList.addAll(Arrays.asList(healthCheck.getUserAccessList().split(",")));

//		List<String> sList = new ArrayList<String>();
//		sList.addAll(Arrays.asList(healthCheck.getSiteAccessList().split(",")));
		
		response.put("siteAccessList", parser.parse(healthCheck.getSiteAccessList()));
		response.put("userAccessList", parser.parse(healthCheck.getUserAccessList()));
		response.put("healthCheckId", healthCheck.getHealthCheckId());
		response.put("createdById", healthCheck.getCreateBy());
		response.put("updatedById", healthCheck.getUpdateBy());

		Users user = userCreateService.getUserByUserId(healthCheck.getCreateBy());
		if (user != null) {
			response.put("createdBy", user.getFirst_name() + " " + user.getLast_name());
		} else {
			response.put("createdBy", "");
		}
		try {
			response.put("createdTime", formatter.format(healthCheck.getCreatedDate()));

		} catch (Exception e) {
			response.put("createdTime", "");
		}
		if (healthCheck.getCreateBy().equalsIgnoreCase(healthCheck.getUpdateBy())) {
			if (user != null) {
				response.put("updatedBy", user.getFirst_name() + " " + user.getLast_name());
			} else {
				response.put("updatedBy", "");
			}

		} else if (!healthCheck.getCreateBy().equalsIgnoreCase(healthCheck.getUpdateBy())) {
			Users updateUser = userCreateService.getUserByUserId(healthCheck.getUpdateBy());
			if (updateUser != null) {
				response.put("updatedBy", updateUser.getFirst_name() + " " + updateUser.getLast_name());
			} else {
				response.put("updatedBy", "");
			}
		} else {
			response.put("updatedBy", "");
		}

		try {
			response.put("updatedTime", formatter.format(healthCheck.getUpdateDate()));
		} catch (Exception e) {
			response.put("updatedTime", "");
		}

		response.put("userId", healthCheck.getUserId());

		boolean isWriteAccess = false;
		if (healthCheck.getAuthUserId() != null) {
			boolean isTenantAdmin = false;

			Users loginUser = userCreateService.getUserByUserId(healthCheck.getAuthUserId());
			if (loginUser != null && loginUser.isIs_tenant_admin()) {
				isTenantAdmin = true;
			}

			if (isTenantAdmin || healthCheck.getCreateBy().equalsIgnoreCase(healthCheck.getAuthUserId())) {
				isWriteAccess = true;
			}
		}

		response.put("isWriteAccess", isWriteAccess);

		/*
		 * if(healthCheck.getSiteAccessList() != null &&
		 * !healthCheck.getSiteAccessList().trim().isEmpty()) { List<String>
		 * siteAccessList = new ArrayList<>();
		 * siteAccessList.addAll(Arrays.asList(healthCheck.getSiteAccessList().
		 * replaceAll("\\[", "").replaceAll("\\]","").split(","))); JSONArray siteArray
		 * = new JSONArray(); if(!siteAccessList.isEmpty()) { for(String site :
		 * siteAccessList) { siteArray.add(site); } } response.put("siteAccessList",
		 * siteArray); } else { response.put("siteAccessList", new JSONArray()); }
		 * 
		 * if(healthCheck.getUserAccessList() != null &&
		 * !healthCheck.getUserAccessList().trim().isEmpty()) { List<String>
		 * userAccessList = new ArrayList<>();
		 * userAccessList.addAll(Arrays.asList(healthCheck.getUserAccessList().
		 * replaceAll("\\[", "").replaceAll("\\]","").split(","))); JSONArray userArray
		 * = new JSONArray(); if(!userAccessList.isEmpty()) { for(String userAccess :
		 * userAccessList) { userArray.add(userAccess); } }
		 * response.put("userAccessList", userArray); } else {
		 * response.put("userAccessList", new JSONArray()); }
		 */

		return response;
	}

	public JSONArray getAllHealthCheck(String siteKey, boolean isTenantAdmin, String userId, String projectId) {
		JSONArray resultArray = new JSONArray();
		String query = null;
		try {
			if (projectId != null && !projectId.isEmpty()) {
				query = "SELECT health_check_id as healthCheckId, component_type as componentType, health_check_name as healthCheckName,"
						+ "report_by as reportBy, report_condition as reportCondition, report_name as reportName, coalesce(site_access_list , '') as siteAccessList,"
						+ "site_key as siteKey, coalesce(user_access_list , '') as userAccessList,"
						+ "to_char(to_timestamp(created_date::text, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as createdTime, "
						+ "to_char(to_timestamp(update_date::text, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as updatedTime, "
						+ "user_id as userId, analytics_type as analyticsType, a.createBy, c.updateBy"
						+ "FROM health_check h "
						+ "LEFT JOIN(select concat(first_name, '', trim(coalesce(last_name,''))) as createBy, user_id as userId from user_temp)a on a.userId = h.user_id "
						+ "LEFT JOIN(select concat(first_name, '', trim(coalesce(last_name,''))) as updateBy, user_id as userId from user_temp)c on c.userId = h.user_id "
						+ "where is_active='true' and site_key='" + siteKey + "' and report_by ='" + projectId
						+ "' order by health_check_name ASC";

			} else {
				query = "SELECT health_check_id as healthCheckId, component_type as componentType, health_check_name as healthCheckName,"
						+ "report_by as reportBy, report_condition as reportCondition, report_name as reportName, coalesce(site_access_list , '') as siteAccessList,"
						+ "site_key as siteKey, coalesce(user_access_list , '') as userAccessList,"
						+ "to_char(to_timestamp(created_date::text, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as createdTime,"
						+ "to_char(to_timestamp(update_date::text, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as updatedTime, "
						+ "user_id as userId, analytics_type as analyticsType, a.createBy, c.updateBy"
						+ "FROM health_check h "
						+ "LEFT JOIN(select concat(first_name, '', trim(coalesce(last_name,''))) as createBy, user_id as userId from user_temp)a on a.userId = h.user_id "
						+ "LEFT JOIN(select concat(first_name, '', trim(coalesce(last_name,''))) as updateBy, user_id as userId from user_temp)c on c.userId = h.user_id "
						+ "where is_active='true' and site_key='" + siteKey + "' order by health_check_name ASC";
				
				if (!isTenantAdmin) {
					query = "SELECT health_check_id as healthCheckId, component_type as componentType, health_check_name as healthCheckName,"
							+ "report_by as reportBy, report_condition as reportCondition, report_name as reportName, coalesce(site_access_list , '') as siteAccessList,"
							+ "site_key as siteKey, coalesce(user_access_list , '') as userAccessList,"
							+ "to_char(to_timestamp(created_date::text, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as createdTime,"
							+ "to_char(to_timestamp(update_date::text, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as updatedTime, "
							+ "user_id as userId, analytics_type as analyticsType, a.createBy, c.updateBy "
							+ "FROM health_check h "
							+ "LEFT JOIN(select concat(first_name, '', trim(coalesce(last_name,''))) as createBy, user_id as userId from user_temp)a on a.userId = h.user_id "
							+ "LEFT JOIN(select concat(first_name, '', trim(coalesce(last_name,''))) as updateBy, user_id as userId from user_temp)c on c.userId = h.user_id"
							+ " where is_active='true' and ((create_by = '" + userId + "' " + "and site_key = '%"
							+ siteKey + "%') or " + "((site_access_list like '%" + siteKey
							+ "%' or site_access_list like '%All%') and " + "(user_access_list like '%" + userId
							+ "%' or user_access_list  like '%All%'))) order by health_check_name ASC";
				}
			}
			System.out.println("--------------query--------------" + query);
//			List<Object> resultList = healthCheckDao.getEntityListByColumn(query, HealthCheck.class);

			List<Map<String, Object>> resultList = healthCheckDao.getListMapObjectById(query);
			if (resultList != null && !resultList.isEmpty()) {
				for (Map<String, Object> mapObject : resultList) {
						JSONObject healthCheckModel = new JSONObject();
						healthCheckModel.put("healthCheckId", mapObject.get("healthcheckid"));
//						 healthCheckModel.put("healthCheckId", mapObject.get("healthcheckid"));
						healthCheckModel.put("componentType", mapObject.get("componenttype"));
						healthCheckModel.put("healthCheckName", mapObject.get("healthcheckname"));
						healthCheckModel.put("reportBy", mapObject.get("reportby"));
						healthCheckModel.put("reportCondition", mapObject.get("reportcondition"));
						healthCheckModel.put("reportName", mapObject.get("reportname"));
						healthCheckModel.put("siteAccessList", Arrays.asList( mapObject.get("siteaccesslist") != null ?  ((String) mapObject.get("siteaccesslist")).split(",") : null));
						healthCheckModel.put("siteKey", mapObject.get("sitekey"));
//						 healthCheckModel.put("siteKey", mapObject.get("sitekey"));
						healthCheckModel.put("userAccessList", Arrays.asList(mapObject.get("useraccesslist") != null ? ((String) mapObject.get("useraccesslist")).split(",") : null));
						healthCheckModel.put("createdTime", mapObject.get("createdtime"));
						healthCheckModel.put("updatedTime", mapObject.get("updatedtime"));
						healthCheckModel.put("userId", mapObject.get("userid"));
						healthCheckModel.put("createdById", mapObject.get("userid"));
						healthCheckModel.put("updatedById", mapObject.get("userid"));
						healthCheckModel.put("analyticsType", mapObject.get("analyticstype"));
						healthCheckModel.put("createdById", mapObject.get("createby"));
						healthCheckModel.put("updatedById", mapObject.get("updateby"));
						healthCheckModel.put("createdBy", mapObject.get("createby"));
						healthCheckModel.put("updatedBy", mapObject.get("updateby"));
						System.out.println("--------getAnalyticsType-----------" + mapObject.get("analyticstype"));
						System.out.println("--------------getCreateBy-----------------------" + mapObject.get("createby")); 
						System.out.println("--------------getCreatedDate-----------------------" + mapObject.get("createdtime"));
							
//						JSONObject response = convertEntityToModel((HealthCheck) obj);
						boolean isWriteAccess = false;
						if (mapObject.get("userid") != null) {
							boolean isTenantAdmin1 = false;

						Users loginUser = userCreateService.getUserByUserId(mapObject.get("userid").toString());
						if (loginUser != null && loginUser.isIs_tenant_admin()) {
							isTenantAdmin1 = true;
						}

						if (isTenantAdmin1 || mapObject.get("createby") != null) {
							isWriteAccess = true;
						}
					}

					healthCheckModel.put("isWriteAccess", isWriteAccess);

					resultArray.add(healthCheckModel);
				
//				for (Object obj : resultList) {
//					if (obj instanceof HealthCheck) {
//						JSONObject response = convertEntityToModel((HealthCheck) obj);
//						resultArray.add(response);
//					}
//				}
			}
			}
			} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return resultArray;
	}

	public JSONArray getHealthCheckNames(String siteKey) {
		JSONArray resultArray = new JSONArray();
		try {
			List<Object> resultList = healthCheckDao.getEntityListByColumn(
					"select * from health_check where site_key='" + siteKey + "'", HealthCheck.class);
			if (resultList != null && !resultList.isEmpty()) {
				for (Object obj : resultList) {
					if (obj instanceof HealthCheck) {
						resultArray.add(((HealthCheck) obj).getHealthCheckName());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return resultArray;
	}

	public JSONArray getHeaderListFromV2(String siteKey, String userId, String token) {
		JSONArray healthCheckHeader = new JSONArray();
		/*
		 * try { String protocol =
		 * com.zenfra.model.ZKModel.getProperty(ZKConstants.APP_SERVER_PROTOCOL); String
		 * host_name = com.zenfra.model.ZKModel.getProperty(ZKConstants.APP_SERVER_IP);
		 * String port =
		 * com.zenfra.model.ZKModel.getProperty(ZKConstants.APP_SERVER_PORT);
		 * 
		 * if(userId == null) { userId=""; }
		 * 
		 * JSONObject requestBody = new JSONObject(); requestBody.put("siteKey",
		 * siteKey); requestBody.put("authUserId", userId);
		 * 
		 * 
		 * 
		 * System.out.println("-----------------healthcheck----------------- "+token+
		 * " : " + protocol + "://" + host_name + ":" + port +
		 * "/ZenfraV2/rest/reports/health-check/headrInfo");
		 * 
		 * 
		 * org.jsoup.Connection.Response execute = Jsoup.connect(protocol + "://" +
		 * host_name + ":" + port + "/ZenfraV2/rest/reports/health-check/headrInfo")
		 * .header("Content-Type", "application/json") .header("Accept",
		 * "application/json") .header("Authorization", token) .followRedirects(true)
		 * .ignoreHttpErrors(true) .ignoreContentType(true)
		 * .userAgent("Mozilla/5.0 AppleWebKit/537.36 (KHTML," +
		 * " like Gecko) Chrome/45.0.2454.4 Safari/537.36")
		 * .method(org.jsoup.Connection.Method.POST) .data("siteKey", siteKey)
		 * .data("authUserId", userId) .requestBody( requestBody.toString())
		 * .maxBodySize(1_000_000 * 30) // 30 mb ~ .timeout(0) // infinite timeout
		 * .execute();
		 * 
		 * Document doc = execute.parse(); Element body = doc.body(); JSONParser parser
		 * = new JSONParser();
		 * 
		 * System.out.println("-----discovery data length--------" + body.text());
		 * JSONObject headrInfoData = (JSONObject)parser.parse(body.text());
		 * healthCheckHeader = (JSONArray) parser.parse(headrInfoData.toJSONString());
		 * System.out.println("---datacount-----" + healthCheckHeader.size());
		 * 
		 * 
		 * 
		 * e.printStackTrace(); StringWriter errors = new StringWriter();
		 * e.printStackTrace(new PrintWriter(errors)); String ex = errors.toString();
		 * ExceptionHandlerMail.errorTriggerMail(ex);
		 */

		return null;
	}

	public com.zenfra.model.GridDataFormat getHealthCheckData(String siteKey, String userId, String projectId) {
		JSONArray toRet = new JSONArray();
		com.zenfra.model.GridDataFormat gridDataFormat = new com.zenfra.model.GridDataFormat();
		gridDataFormat.setColumnOrder(new ArrayList<>());

		JSONObject headerLabelJson = new JSONObject();
		if (com.zenfra.model.ZKModel.getZkData().containsKey(ZKConstants.HEADER_LABEL)) {
			String headerLabel = com.zenfra.model.ZKModel.getProperty(ZKConstants.HEADER_LABEL);

			JSONParser parser = new JSONParser();
			try {
				headerLabelJson = (JSONObject) parser.parse(headerLabel);
			} catch (ParseException e) {
			}
		}
		if (com.zenfra.model.ZKModel.getZkData().containsKey(ZKConstants.HEALTHCHECK_COLUMN_ORDER)) {
			String siteOrder = com.zenfra.model.ZKModel.getProperty(ZKConstants.HEALTHCHECK_COLUMN_ORDER);
			gridDataFormat.setColumnOrder(Arrays.asList(siteOrder.split(",")));

		}
		List<com.zenfra.model.GridHeader> gridHeaderList = new ArrayList<>();
		gridDataFormat.setHeaderInfo(gridHeaderList);
		List<Object> gridData = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();
		Map<String, com.zenfra.model.GridHeader> headerKeys = new HashMap<>();

		try {

			Map<String, JSONObject> userMap = getUserList(new JSONArray(), true);
			boolean isTenantAdmin = false;
			if (userMap.containsKey(userId)) {
				JSONObject userObj = userMap.get(userId);
				if (userObj.containsKey("is_tenant_admin")) {
					isTenantAdmin = (boolean) userObj.get("is_tenant_admin");
				}

			}

			JSONArray siteQueryArray = getSiteList();
			HashMap<String, String> siteMap = new HashMap<>();
			for (int i = 0; i < siteQueryArray.size(); i++) {
				JSONObject siteObj = (JSONObject) siteQueryArray.get(i);
				if (siteObj.containsKey("site_key") && siteObj.containsKey("site_name")) {
					siteMap.put(siteObj.get("site_key").toString(), siteObj.get("site_name").toString());
				}
			}
			// String listQuery = "select * from healthcheck where siteKey = '"+siteKey+"'
			// and active = true";

			// if(!isTenantAdmin) {

			// listQuery = "select from HealthCheck where active = 'true' and ((createdBy =
			// '"+userId+"'and siteKey = '"+siteKey+"') or ((siteAccessList in '"+siteKey+"'
			// or siteAccessList in ['All']) and (userAccessList in '"+userId +"' or
			// userAccessList in ['All'])))";

			// }
			// System.out.println("listQuery:: "+listQuery);
			JSONArray healthCheckList = getAllHealthCheck(siteKey, isTenantAdmin, userId, projectId);
			JSONParser parser = new JSONParser();
			for (int i = 0; i < healthCheckList.size(); i++) {
				JSONObject jObj = (JSONObject) healthCheckList.get(i);
				/*
				 * JSONArray storageList = (JSONArray)
				 * parser.parse(ZKModel.getProperty(ZKConstants.STORAGE_LIST)); JSONArray
				 * serverList = (JSONArray)
				 * parser.parse(ZKModel.getProperty(ZKConstants.SERVER_LIST)); JSONArray
				 * switchList = (JSONArray)
				 * parser.parse(ZKModel.getProperty(ZKConstants.SWITCH_LIST));
				 * 
				 * 
				 * String reportBy = ""; if
				 * (serverList.contains(jObj.get("componentType").toString().toLowerCase()) &&
				 * jObj.get("reportName").toString().equalsIgnoreCase("Local")) { reportBy =
				 * "Server"; jObj.replace("reportName", reportBy); } else if
				 * (storageList.contains(jObj.get("componentType").toString().toLowerCase()) &&
				 * jObj.get("reportName").toString().equalsIgnoreCase("Local")) { reportBy =
				 * "Storage"; jObj.replace("reportName", reportBy); } else if
				 * (switchList.contains(jObj.get("componentType").toString().toLowerCase()) &&
				 * jObj.get("reportName").toString().equalsIgnoreCase("Local")) { reportBy =
				 * "Switch"; jObj.replace("reportName", reportBy); }
				 * 
				 * if (jObj.get("reportName").toString().equalsIgnoreCase("End-To-End-Basic")) {
				 * reportBy = "Server - Switch - Storage Summary"; jObj.replace("reportName",
				 * reportBy);
				 * 
				 * } if
				 * (jObj.get("reportName").toString().equalsIgnoreCase("End-To-End-Detail")) {
				 * reportBy = "Server - Switch - Storage"; jObj.replace("reportName", reportBy);
				 * }
				 */

				boolean isreadAccess = true;
				boolean isWriteAccess = false;
				Set<String> keys = jObj.keySet();
				for (String key : keys) {

					if (!headerKeys.containsKey(key)) {
						headerKeys.put(key,
								generateGridHeader(key, jObj.get(key), null, siteKey, "user", headerLabelJson));
					}
				}

				if (jObj.size() > 0) {
					// Share Access updated.
					if (isTenantAdmin || jObj.get("createdById").toString().equalsIgnoreCase(userId)) {
						isWriteAccess = true;
					}
					jObj.put("isWriteAccess", isWriteAccess);

					if (jObj.containsKey("siteAccessList")) {
						JSONArray siteAccessList = new ObjectMapper().convertValue(jObj.get("siteAccessList"),
								JSONArray.class);
						if (!siteAccessList.isEmpty()) {
							JSONArray siteList = new JSONArray();
							for (int j = 0; j < siteAccessList.size(); j++) {
								JSONObject siteObjList = new JSONObject();
								String siteKey1 = (String) siteAccessList.get(j);
								if (siteKey1.equalsIgnoreCase("all")) {
									siteObjList.put("value", "allSites");
									siteObjList.put("label", "All Sites");
								} else if (siteMap.containsKey(siteKey1)) {
									String siteName = siteMap.get(siteKey1);
									siteObjList.put("value", siteKey1);
									siteObjList.put("label", siteName);
								}
								siteList.add(siteObjList);
							}
							jObj.put("siteList", siteList);
						}
					}
					if (jObj.containsKey("userAccessList")) {
						JSONArray userAccessList = new ObjectMapper().convertValue(jObj.get("userAccessList"),
								JSONArray.class);
						if (!userAccessList.isEmpty()) {
							JSONArray userList = new JSONArray();
							for (int j = 0; j < userAccessList.size(); j++) {
								JSONObject userListObj = new JSONObject();
								String accessUserId = (String) userAccessList.get(j);
								if (accessUserId.equalsIgnoreCase("all")) {
									userListObj.put("value", "allUsers");
									userListObj.put("label", "All Users");
								} else if (userMap.containsKey(accessUserId)) {
									JSONObject userObj = userMap.get(accessUserId);
									if (userObj != null && userObj.containsKey("first_name")
											&& userObj.containsKey("last_name")) {
										userListObj.put("label",
												userObj.get("first_name") + " " + userObj.get("last_name"));
										userListObj.put("value", accessUserId);
									}
								}
								userList.add(userListObj);
							}
							jObj.put("userList", userList);
						}
					}

					gridData.add(jObj);
				}
			}
			// System.out.println("gridData : " + gridData);
			for (int i = 0; i < gridData.size(); i++) {
				JSONObject site = (JSONObject) gridData.get(i);
				if (userMap.containsKey(site.get("createdBy"))) {
					JSONObject user = userMap.get(site.get("createdBy"));
					if (user.containsKey("first_name") && user.containsKey("last_name")) {
						site.remove("createdBy");
						site.put("createdBy", user.get("first_name") + " " + user.get("last_name"));
					}
				}

				if (userMap.containsKey(site.get("updatedBy"))) {
					JSONObject user = userMap.get(site.get("updatedBy"));
					if (user.containsKey("first_name") && user.containsKey("last_name")) {
						site.remove("updatedBy");
						site.put("updatedBy", user.get("first_name") + " " + user.get("last_name"));
					}
				}

			}
			gridHeaderList.addAll(headerKeys.values());
			gridDataFormat.setData(gridData);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return gridDataFormat;
	}

	private JSONArray getSiteList() {
		JSONArray result = new JSONArray();
		List<SiteModel> sites = siteService.findAll();
		if (sites != null && !sites.isEmpty()) {
			for (SiteModel site : sites) {
				result.add(commonFunctions.convertEntityToJsonObject(site));
			}
		}
		return result;
	}

	private Map<String, JSONObject> getUserList(JSONArray jsonArray, boolean b) {
		Map<String, JSONObject> result = new HashMap<String, JSONObject>();
		List<Users> users = userCreateService.getAllUsers();
		if (users != null && !users.isEmpty()) {
			for (Users u : users) {
				result.put(u.getUser_id(), commonFunctions.convertEntityToJsonObject(u));
			}
		}
		return result;
	}

	public com.zenfra.model.GridHeader generateGridHeader(String key, Object data, String tenantId, String siteKey,
			String objectList, JSONObject headerLabelJson) {
		com.zenfra.model.GridHeader toReturnHeader = new com.zenfra.model.GridHeader();
		// propertyLabel =
		// convertJSON(ZKModel.getProperty(ZKConstants.DISPLAY_LABLE));
		try {
			toReturnHeader.setActualName(key);
			// String dataType = data.getClass().getSimpleName();
			toReturnHeader.setDataType("String");
			// String dataType = data.getClass().getSimpleName();

			String string = toReturnHeader.getActualName();
			if (string == "createdTime" || string == "updatedTime") {
				toReturnHeader.setDataType("date");
			} else {
				toReturnHeader.setDataType("String");
			}

			String displayName = commonFunctions.convertCamelCase(key);

			/*
			 * // if (propertyLabel.containsKey(key)) { displayName = (String)
			 * propertyLabel.get(key); }
			 */
			if (headerLabelJson.containsKey(displayName.toLowerCase())) {
				toReturnHeader.setDisplayName(headerLabelJson.get(displayName.toLowerCase()).toString());
			} else {
				toReturnHeader.setDisplayName(displayName);
			}

			if (key.equalsIgnoreCase("tenantId") || key.equalsIgnoreCase("siteKey")
					|| key.equalsIgnoreCase("availableSites") || key.equalsIgnoreCase("address")
					|| key.equalsIgnoreCase("description")) {

				toReturnHeader.setHide(true);
			}

			if (objectList.equalsIgnoreCase("user") && key.equalsIgnoreCase("customPolicy")
					|| key.equalsIgnoreCase("password") || key.equalsIgnoreCase("userId")
					|| key.equalsIgnoreCase("favorite_menus") || key.equalsIgnoreCase("resourceSet")
					|| key.equalsIgnoreCase("siteKey") || key.equalsIgnoreCase("healthCheckId")) {
				toReturnHeader.setHide(true);
			}
			if (objectList.equalsIgnoreCase("site") && key.equalsIgnoreCase("description")
					|| key.equalsIgnoreCase("contactNumber") || key.equalsIgnoreCase("siteAddress")) {
				toReturnHeader.setHide(true);
			}

			if (key.equalsIgnoreCase("policySet") && tenantId != null) {
				toReturnHeader.setOptional(new JSONArray());
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return toReturnHeader;
	}

	public JSONArray getHealthCheckDisplay() {
		JSONArray jSONArray = new JSONArray();

		try {
			List<Map<String, Object>> resultList = healthCheckDisplayDao
					.getListObjectsByQueryNew("select * from health_check_display");
			if (resultList != null && !resultList.isEmpty()) {
				for (Map<String, Object> map : resultList) {
					String jsonString = objectMapper.writeValueAsString(map);
					jSONArray.add(jSONParser.parse(jsonString));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return jSONArray;
	}

}
