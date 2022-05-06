package com.zenfra.service;

import java.util.List;
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
			toolApiConfigModel.setCreatedBy(toolApiConfigModel.getUserId());
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

	public ResponseEntity<?> getListToolApiData() {
		try {
			List<ToolApiConfigModel> toolApiConfigData = toolApiConfigRepository.findByIsActive(true);

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
