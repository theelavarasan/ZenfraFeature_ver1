package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dao.HeaderInfoModalRepository;
import com.zenfra.dao.ToolApiConfigRepository;
import com.zenfra.model.HeaderInfoModel;
import com.zenfra.model.Response;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.ToolApiConfigModel;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.CommonUtils;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class ToolApiConfigService {

	// ResponseModel_v2 responseModel = new ResponseModel_v2();

	Response responseModel = new Response();

	@Autowired
	CommonFunctions commonFunctions;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ToolApiConfigRepository toolApiConfigRepository;

	@Autowired
	HeaderInfoModalRepository headerInfoModalRepository;

	public ResponseEntity<JSONObject> zoomAPICheck(String apiKey, String apiSecretKey, String siteKey) {

		RestTemplate restTemplate = new RestTemplate();
		String parsingURL = DBUtils.getParsingServerIP();

		String uri = parsingURL + "/parsing/zoom-check";
		uri = CommonUtils.checkPortNumberForWildCardCertificate(uri);

		Map<String, Object> map = new HashMap<>();
		map.put("apiKey", apiKey);
		map.put("apiSecretKey", apiSecretKey);
		map.put("siteKey", siteKey);
		Map<String, Object> body = new LinkedHashMap<>();
		body.putAll(map);

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
		builder.build(map);

		HttpEntity<Object> httpRequest = new HttpEntity<>(body);
		ResponseEntity<JSONObject> restResult = restTemplate.exchange(builder.buildAndExpand(map).toUri(),
				HttpMethod.POST, httpRequest, JSONObject.class);
		return restResult;
	}

	@SuppressWarnings("unchecked")
	public Response createApiConfig(ToolApiConfigModel toolApiConfigModel)
			throws JsonMappingException, JsonProcessingException {
		JSONArray dataArray = new JSONArray();

		ResponseEntity<JSONObject> response1 = zoomAPICheck(toolApiConfigModel.getApiKey(),
				toolApiConfigModel.getApiSecretKey(), toolApiConfigModel.getSiteKey());

		JSONObject response = response1.getBody();
		String code = response.get("code").toString();
		String message = response.get("message").toString();
		if (code.equalsIgnoreCase("200") && message.equalsIgnoreCase("Valid access token")) {
			try {
				toolApiConfigModel.setActive(true);
				toolApiConfigModel.setApiConfigId(UUID.randomUUID().toString());
				toolApiConfigModel.setCreatedTime(commonFunctions.getCurrentDateWithTime());
				toolApiConfigModel.setUpdatedTime(commonFunctions.getCurrentDateWithTime());
				toolApiConfigModel.setCreatedBy(toolApiConfigModel.getUserId());
				toolApiConfigModel.setUpdatedBy(toolApiConfigModel.getUserId());
				toolApiConfigRepository.save(toolApiConfigModel);
				dataArray.add(toolApiConfigModel);

				responseModel.setResponseMsg("Success");
				responseModel.setResponseCode(200);
				responseModel.setjData(dataArray);

			} catch (Exception e) {
				e.printStackTrace();
				responseModel.setResponseCode(500);
			}
		} else {
			responseModel.setResponseMsg("Invalid Access Token");
			responseModel.setResponseCode(400);
			responseModel.setjData("Configuration Keys Are Not Valid");
		}
		return responseModel;
	}

	public Response getToolApiData(String toolApiConfigId) {
		try {
			Optional<ToolApiConfigModel> toolApiConfigData = toolApiConfigRepository.findById(toolApiConfigId);

			responseModel.setResponseMsg("Success");
			responseModel.setResponseCode(200);
			responseModel.setjData(toolApiConfigData);
			return responseModel;
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseCode(500);
			return responseModel;
		}

	}

	@SuppressWarnings("unchecked")
	public Response getListToolApiData(String siteKey, String deviceType) {

		JSONArray dataArray = new JSONArray();
		JSONObject response = new JSONObject();

		Map<String, String> data = new HashMap<>();
		data = DBUtils.getPostgres();
		
		String selectQuery = "select tac.api_config_id, trim(concat(trim(ut.first_name), ' ', coalesce(trim(ut.last_name), ''))) as created_by,\r\n"
				+ "to_char(to_timestamp(tac.created_time, 'yyyy-mm-dd HH24:MI:SS') at \r\n"
				+ "time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as created_time, tac.site_key, \r\n"
				+ "trim(concat(trim(ut1.first_name), ' ', coalesce(trim(ut1.last_name), ''))) as updated_by,\r\n"
				+ "to_char(to_timestamp(tac.updated_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as updated_time, tac.tenant_id, device_type,\r\n"
				+ "api_key, api_secret_key, config_name\r\n" + "from tool_api_config tac\r\n"
				+ "LEFT JOIN user_temp ut on ut.user_id = tac.created_by\r\n"
				+ "LEFT JOIN user_temp ut1 on ut1.user_id = tac.updated_by\r\n"
				+ "where tac.is_active = true and site_key = '" + siteKey + "'";

		System.out.println("!!! selectQuery: " + selectQuery);

		
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(selectQuery)) {

			
			

			while (rs.next()) {
				JSONObject dataObj = new JSONObject();

				dataObj.put("apiKey", rs.getString("api_key"));
				dataObj.put("apiConfigId", rs.getString("api_config_id"));
				dataObj.put("apiSecretKey", rs.getString("api_secret_key"));
				dataObj.put("configName", rs.getString("config_name"));
				dataObj.put("createdBy", rs.getString("created_by"));
				dataObj.put("createdTime", rs.getString("created_time"));
				dataObj.put("siteKey", rs.getString("site_key"));
				dataObj.put("tenantId", rs.getString("tenant_id"));
				dataObj.put("updatedBy", rs.getString("updated_by"));
				dataObj.put("updatedTime", rs.getString("updated_time"));

				dataArray.add(dataObj);
			}

			// List<Map<String, Object>> toolData = jdbc.queryForList(selectQuery);

			String[] array = { "configName", "apiKey", "apiSecretKey", "createdBy", "updatedBy", "createdTime",
					"updatedTime" };

			JSONArray header = getToolHeaderInfo(deviceType);

			response.put("columnOrder", array);
			response.put("headerInfo", header);
			response.put("data", dataArray);

			responseModel.setResponseMsg("Success");
			responseModel.setResponseCode(200);
			responseModel.setjData(response);

			return responseModel;

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseCode(500);
			return responseModel;
		}

	}

	@SuppressWarnings("unchecked")
	public Response getListConfigName(String siteKey) {

		JSONArray dataArray = new JSONArray();
//		JSONObject response = new JSONObject();

		Map<String, String> data = new HashMap<>();
		data = DBUtils.getPostgres();
		
		String selectQuery = "select * from tool_api_config where site_key = '" + siteKey
				+ "' and is_active = true";
		System.out.println("!!! selectQuery: " + selectQuery);
		
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(selectQuery)) {

			while (rs.next()) {
				JSONObject dataObj = new JSONObject();

				dataObj.put("value", rs.getString("config_name"));
				dataObj.put("label", rs.getString("config_name"));

				dataArray.add(dataObj);
			}
			responseModel.setResponseMsg("Success");
			responseModel.setResponseCode(200);
			responseModel.setjData(dataArray);
			return responseModel;

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseCode(500);
			return responseModel;
		}

	}

	public Response updateListToolApiData(ToolApiConfigModel toolApiConfigModel) {
		JSONArray dataArray = new JSONArray();
		try {
			Optional<ToolApiConfigModel> existingToolConfigData = toolApiConfigRepository
					.findById(toolApiConfigModel.getApiConfigId() == null ? "" : toolApiConfigModel.getApiConfigId());
			if (existingToolConfigData.isPresent()) {
				ToolApiConfigModel getExistingToolConfigData = existingToolConfigData.get();
//				Optional<ToolApiConfigModel> usernameData = toolApiConfigRepository.findByUserId(toolApiConfigModel.getUserId());
//				Map<String, Object> userNameData = jdbc.queryForMap("SELECT first_name, last_name FROM USER_TEMP WHERE user_id= '"+ toolApiConfigModel.getUserId() +"'");
//				String userName = userNameData.get("first_name").toString()+" "+userNameData.get("last_name").toString();

				getExistingToolConfigData.setActive(true);
				getExistingToolConfigData.setApiConfigId(
						toolApiConfigModel.getApiConfigId() == null ? "" : toolApiConfigModel.getApiConfigId());
				getExistingToolConfigData
						.setUpdatedBy(toolApiConfigModel.getUserId() == null ? "" : toolApiConfigModel.getUserId());
				getExistingToolConfigData.setUpdatedTime(commonFunctions.getCurrentDateWithTime());
				getExistingToolConfigData
						.setSiteKey(toolApiConfigModel.getSiteKey() == null ? "" : toolApiConfigModel.getSiteKey());
				getExistingToolConfigData
						.setTenantId(toolApiConfigModel.getTenantId() == null ? "" : toolApiConfigModel.getTenantId());
				getExistingToolConfigData
						.setApiKey(toolApiConfigModel.getApiKey() == null ? "" : toolApiConfigModel.getApiKey());
				getExistingToolConfigData.setApiSecretKey(
						toolApiConfigModel.getApiSecretKey() == null ? "" : toolApiConfigModel.getApiSecretKey());
				getExistingToolConfigData.setDeviceType(
						toolApiConfigModel.getDeviceType() == null ? "" : toolApiConfigModel.getDeviceType());
				getExistingToolConfigData.setConfigName(
						toolApiConfigModel.getConfigName() == null ? "" : toolApiConfigModel.getConfigName());

				toolApiConfigRepository.save(getExistingToolConfigData);
			}
			dataArray.add(existingToolConfigData);
			responseModel.setResponseMsg("Success");
			responseModel.setResponseCode(200);
			responseModel.setjData(dataArray);
			return responseModel;
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseCode(500);
			return responseModel;
		}
	}

	public Response deleteToolApiData(String toolApiConfigId) {
		try {
			Optional<ToolApiConfigModel> existingToolConfigData = toolApiConfigRepository
					.findById(toolApiConfigId == null ? "" : toolApiConfigId);
			if (existingToolConfigData.isPresent()) {
				ToolApiConfigModel getExistingToolConfigData = existingToolConfigData.get();
				getExistingToolConfigData.setActive(false);

				toolApiConfigRepository.save(getExistingToolConfigData);
			}
			responseModel.setResponseMsg("Success");
			responseModel.setResponseCode(200);
			responseModel.setjData("Deleted successfully");
			return responseModel;

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseCode(500);
			return responseModel;
		}
	}

	@SuppressWarnings("unchecked")
	public JSONArray getToolHeaderInfo(String deviceType) throws SQLException {
		List<HeaderInfoModel> headerInfoModals = headerInfoModalRepository.getHeaderInfoModelByDeviceType(deviceType);

		JSONArray jsonArray = new JSONArray();
		try {
			if (headerInfoModals != null && !headerInfoModals.isEmpty()) {
				{
					for (HeaderInfoModel headerInfoModal : headerInfoModals) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("displayName", headerInfoModal.getAliasName());
						jsonObject.put("actualName", headerInfoModal.getColumnName());
						jsonObject.put("dataType", headerInfoModal.getDataType());
						jsonObject.put("hide", headerInfoModal.getHide());
						jsonArray.add(jsonObject);
					}
				}
			}
			System.out.println("*-*-*-*-*-*-*-*-* " + jsonArray);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return jsonArray;
	}

}