package com.zenfra.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.zenfra.dao.ToolApiConfigRepository;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.ToolApiConfigModel;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.DBUtils;

@Service
public class ToolApiConfigService {

	ResponseModel_v2 responseModel = new ResponseModel_v2();
	
	@Autowired
	CommonFunctions commonFunctions;
	
	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ToolApiConfigRepository toolApiConfigRepository;

	public ResponseEntity<?> createApiConfig(ToolApiConfigModel toolApiConfigModel) {

		try {
			
//			Map<String, Object> userNameData = jdbc.queryForMap("SELECT first_name, last_name FROM USER_TEMP WHERE user_id= '"+ toolApiConfigModel.getUserId() +"'");
			toolApiConfigModel.setActive(true);
			toolApiConfigModel.setApiConfigId(UUID.randomUUID().toString());
			toolApiConfigModel.setCreatedTime(commonFunctions.getCurrentDateWithTime());
			toolApiConfigModel.setUpdatedTime(commonFunctions.getCurrentDateWithTime());
			toolApiConfigModel.setCreatedBy(toolApiConfigModel.getUserId());
			toolApiConfigModel.setUpdatedBy(toolApiConfigModel.getUserId());
//			Optional<ToolApiConfigModel> userName = toolApiConfigRepository
//					.findById(toolApiConfigModel.getUserId());			
//			String userNmae = userNameData.get("first_name").toString()+" "+userNameData.get("last_name").toString();		
				
			toolApiConfigRepository.save(toolApiConfigModel);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(toolApiConfigModel.getApiConfigId());
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}

	}

	public ResponseEntity<?> getToolApiData(String toolApiConfigId) {
		try {
			Optional<ToolApiConfigModel> toolApiConfigData = toolApiConfigRepository.findById(toolApiConfigId);
			
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(toolApiConfigData);
			return ResponseEntity.ok(responseModel);
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}

	}

	public ResponseEntity<?> getListToolApiData(String siteKey) {

		Map<String, String> data = new HashMap<>();
		data = DBUtils.getPostgres();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {

			String selectQuery = "select trim(concat(trim(ut.first_name), ' ', coalesce(trim(ut.last_name), ''))) as created_by,\r\n"
					+ "to_char(to_timestamp(tac.created_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as created_time, tac.site_key,\r\n"
					+ "trim(concat(trim(ut1.first_name), ' ', coalesce(trim(ut1.last_name), ''))) as updated_by,\r\n"
					+ "to_char(to_timestamp(tac.updated_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as updated_time, tac.tenant_id, device_type,\r\n"
					+ "api_key, api_secret_key, config_name\r\n"
					+ "from tool_api_config tac\r\n"
					+ "LEFT JOIN user_temp ut on ut.user_id = tac.created_by\r\n"
					+ "LEFT JOIN user_temp ut1 on ut1.user_id = tac.updated_by\r\n"
					+ "where tac.is_active = true and site_key = '"+ siteKey +"'";

			List<Map<String, Object>> res = jdbc.queryForList(selectQuery);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(res);
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}

	}

	public ResponseEntity<?> updateListToolApiData(ToolApiConfigModel toolApiConfigModel) {
		try {
			Optional<ToolApiConfigModel> existingToolConfigData = toolApiConfigRepository.findById(toolApiConfigModel.getApiConfigId()== null ? "" : toolApiConfigModel.getApiConfigId());
			if(existingToolConfigData.isPresent()) {
				ToolApiConfigModel getExistingToolConfigData = existingToolConfigData.get();
//				Optional<ToolApiConfigModel> usernameData = toolApiConfigRepository.findByUserId(toolApiConfigModel.getUserId());
//				Map<String, Object> userNameData = jdbc.queryForMap("SELECT first_name, last_name FROM USER_TEMP WHERE user_id= '"+ toolApiConfigModel.getUserId() +"'");
//				String userName = userNameData.get("first_name").toString()+" "+userNameData.get("last_name").toString();
				
				getExistingToolConfigData.setActive(true);
				getExistingToolConfigData.setApiConfigId(toolApiConfigModel.getApiConfigId() == null ? "" : toolApiConfigModel.getApiConfigId());   
				getExistingToolConfigData.setUpdatedBy(toolApiConfigModel.getUserId() == null ? "" : toolApiConfigModel.getUserId());
				getExistingToolConfigData.setUpdatedTime(commonFunctions.getCurrentDateWithTime());
				getExistingToolConfigData.setSiteKey(toolApiConfigModel.getSiteKey()  == null ? "" : toolApiConfigModel.getSiteKey());
				getExistingToolConfigData.setTenantId(toolApiConfigModel.getTenantId()  == null ? "" : toolApiConfigModel.getTenantId());
				getExistingToolConfigData.setApiKey(toolApiConfigModel.getApiKey()  == null ? "" : toolApiConfigModel.getApiKey());
				getExistingToolConfigData.setApiSecretKey(toolApiConfigModel.getApiSecretKey()  == null ? "" : toolApiConfigModel.getApiSecretKey());
				getExistingToolConfigData.setDeviceType(toolApiConfigModel.getDeviceType()  == null ? "" : toolApiConfigModel.getDeviceType());
				getExistingToolConfigData.setConfigName(toolApiConfigModel.getConfigName()  == null ? "" : toolApiConfigModel.getConfigName());
				
				toolApiConfigRepository.save(getExistingToolConfigData);
		}
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData("Updated successfully");		
			return ResponseEntity.ok(responseModel);
		}catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}		
	}

	public ResponseEntity<?> deleteToolApiData(String toolApiConfigId) {
		try {
			Optional<ToolApiConfigModel> existingToolConfigData = toolApiConfigRepository
					.findById(toolApiConfigId == null ? "" : toolApiConfigId);
			if (existingToolConfigData.isPresent()) {
				ToolApiConfigModel getExistingToolConfigData = existingToolConfigData.get();
				getExistingToolConfigData.setActive(false);

				toolApiConfigRepository.save(getExistingToolConfigData);
			}
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData("Deleted successfully");
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
	}

}
