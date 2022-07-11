package com.zenfra.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.zenfra.model.DeviceTypeModel;
import com.zenfra.model.DeviceTypeValueModel;
import com.zenfra.model.FavouriteModel;
import com.zenfra.model.FavouriteOrder;
import com.zenfra.model.GridDataFormat;
import com.zenfra.model.HealthCheck;
import com.zenfra.model.HealthCheckModel;
import com.zenfra.model.ResponseModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.Users;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.service.CategoryMappingService;
import com.zenfra.service.FavouriteApiService_v2;
import com.zenfra.service.HealthCheckService;
import com.zenfra.service.UserCreateService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.CommonUtils;
import com.zenfra.utils.ExceptionHandlerMail;
import com.zenfra.utils.NullAwareBeanUtilsBean;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/reports")
public class FavouriteController_v2 {

	@Autowired
	FavouriteApiService_v2 service;

	@Autowired
	CommonFunctions functions;

	@Autowired
	UserCreateService userCreateService;

	@Autowired
	HealthCheckService healthCheckService;

	@Autowired
	CategoryMappingService catService;

	private ObjectMapper objectMapper = new ObjectMapper();

	@PostMapping("/get-all-favourite")
	public ResponseEntity<?> getFavouriteView(@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam(name = "siteKey") String siteKey,
			@RequestParam(name = "reportName", required = false) String reportName,
			@RequestParam(name = "projectId", required = false) String projectId) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			responseModel.setResponseMessage("Success");
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setResponseDescription("FavouriteView Successfully Retrieved");
			responseModel.setjData(service.getFavView(userId, siteKey, reportName, projectId));
		} catch (Exception e) {
			e.printStackTrace();
			/*StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
			*/

			return ResponseEntity.ok(responseModel);
		}

		return ResponseEntity.ok(responseModel);
	}

	@PostMapping("/save-filter-view")
	public ResponseEntity<?> saveFavouriteViewData(@RequestBody FavouriteModel favouriteModel) throws IOException,
			URISyntaxException, org.json.simple.parser.ParseException, ParseException, SQLException {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			Users user = userCreateService.getUserByUserId(favouriteModel.getAuthUserId());
			if (user == null) {
				responseModel.setResponseDescription("User id is invalid");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				return ResponseEntity.ok(responseModel);
			}

			/*
			 * for(int i=0;0<favouriteModel.getFilterProperty().size();i++) { JSONObject
			 * obj=(JSONObject) favouriteModel.getFilterProperty().get(i); String
			 * label=obj.get("label").toString(); String label
			 * 
			 * 
			 * 
			 * } if((category.equalsIgnoreCase("Server") ||
			 * category.equalsIgnoreCase("Project") ||
			 * category.equalsIgnoreCase("Third Party Data")) &&
			 * reportList.equalsIgnoreCase("Local")) { label = "Server"; }
			 * if(category.equalsIgnoreCase("Storage") &&
			 * reportList.equalsIgnoreCase("Local")) { label = "Storage"; }
			 * if(category.equalsIgnoreCase("Switch") &&
			 * reportList.equalsIgnoreCase("Local")) { label = "Switch"; }
			 * if((category.equalsIgnoreCase("Server") ||
			 * category.equalsIgnoreCase("Project") ||
			 * category.equalsIgnoreCase("Third Party Data")) &&
			 * reportList.equalsIgnoreCase("End-To-End-Basic")) { label =
			 * "Server - Switch - Storage Summary"; }
			 * if(category.equalsIgnoreCase("Storage") &&
			 * reportList.equalsIgnoreCase("End-To-End-Basic")) { label =
			 * "Server - Switch - Storage Summary"; } if(category.equalsIgnoreCase("Switch")
			 * && reportList.equalsIgnoreCase("End-To-End-Basic")) { label =
			 * "Server - Switch - Storage Summary"; }
			 * 
			 * if((category.equalsIgnoreCase("Server") ||
			 * category.equalsIgnoreCase("Project") ||
			 * category.equalsIgnoreCase("Third Party Data")) &&
			 * reportList.equalsIgnoreCase("End-To-End-Detail")) { label =
			 * "Server - Switch - Storage Detailed"; }
			 * if(category.equalsIgnoreCase("Storage") &&
			 * reportList.equalsIgnoreCase("End-To-End-Detail")) { label =
			 * "Server - Switch - Storage Detailed"; }
			 * if(category.equalsIgnoreCase("Switch") &&
			 * reportList.equalsIgnoreCase("End-To-End-Detail")) { label =
			 * "Server - Switch - Storage Detailed"; }
			 */

			favouriteModel.setFavouriteId(functions.generateRandomId());
			if (favouriteModel.getReportName().equalsIgnoreCase("project-summary")) {
				favouriteModel.setReportLabel(favouriteModel.getFavouriteId());
			}
			favouriteModel.setIsActive(true);
			favouriteModel.setCreatedTime(functions.getCurrentDateWithTime());
			favouriteModel.setUpdatedBy(favouriteModel.getCreatedBy());
			favouriteModel.setUpdatedTime(functions.getCurrentDateWithTime());
						

			if (service.saveFavouriteView(favouriteModel) == 1) {
				catService.saveMap(favouriteModel.getCategoryList(), favouriteModel.getFavouriteId());
				favouriteModel.setCategoryColumns(catService.getCategoryLabelById(favouriteModel.getFavouriteId()));
				favouriteModel.setCreatedBy((user.getFirst_name() + " " + user.getLast_name()));
				responseModel.setjData(functions.convertEntityToJsonObject(favouriteModel));
				responseModel.setResponseDescription("FavouriteView Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Favourite not inserted ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			responseModel.setResponseMessage("Success!");

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}

	@PutMapping("/update-filter-view")
	public ResponseEntity<?> updateFavouriteViewData(@RequestBody FavouriteModel favouriteModel) throws IOException,
			URISyntaxException, org.json.simple.parser.ParseException, ParseException, SQLException {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			favouriteModel.setUpdatedBy(favouriteModel.getAuthUserId());
			favouriteModel.setIsActive(true);
			favouriteModel.setUpdatedTime(functions.getCurrentDateWithTime());

			if (service.updateFavouriteView(favouriteModel.getAuthUserId(), favouriteModel) == 1) {
				catService.deleteCategoryMappingFavouriteIdOrChartId(favouriteModel.getFavouriteId());
				catService.saveMap(favouriteModel.getCategoryList(), favouriteModel.getFavouriteId());
				favouriteModel.setCategoryColumns(catService.getCategoryLabelById(favouriteModel.getFavouriteId()));
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData(functions.convertEntityToJsonObject(favouriteModel));
				responseModel.setResponseDescription("FavouriteView Successfully updated");

			} else {
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription("Favourite Id not found ");
			}

			try {

				if (favouriteModel.getReportName().equalsIgnoreCase("healthcheck")) {
					JSONArray filterProperty = favouriteModel.getFilterProperty();
					for (int i = 0; i < filterProperty.size(); i++) {
						LinkedHashMap<String, String> prop = (LinkedHashMap<String, String>) filterProperty.get(i);

						if (prop.containsKey("name") && prop.get("name").toString().equalsIgnoreCase("healthCheck")) {
							String healthCheckId = prop.get("selection").toString();
							HealthCheck healthCheck = healthCheckService.getHealthCheckObject(healthCheckId);

							if (healthCheck != null) {
								List<String> favSites = favouriteModel.getSiteAccessList();
								List<String> favUsers = favouriteModel.getUserAccessList();

								List<String> healthCheckSites = new ArrayList<>();
								List<String> healthCheckUsers = new ArrayList<>();
								if (healthCheck.getSiteAccessList() != null
										&& !healthCheck.getSiteAccessList().trim().isEmpty()) {
									healthCheckSites.addAll(
											Arrays.asList(healthCheck.getSiteAccessList().toString().split(",")));
								}
								if (healthCheck.getUserAccessList() != null
										&& !healthCheck.getUserAccessList().trim().isEmpty()) {
									healthCheckUsers.addAll(
											Arrays.asList(healthCheck.getUserAccessList().toString().split(",")));
								}

								healthCheckSites.remove(favSites);
								healthCheckSites.addAll(favSites);

								healthCheckUsers.remove(favUsers);
								healthCheckUsers.addAll(favUsers);

								healthCheck.setSiteAccessList(StringUtils.join(favSites, ','));
								healthCheck.setUserAccessList(StringUtils.join(favUsers, ','));
								healthCheckService.updateHealthCheck(healthCheck);
								break;
							}
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

			responseModel.setResponseMessage("Success!");

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

		// return ResponseEntity.ok(body);
	}

	@DeleteMapping("/delete-favourite-filter-view")
	public ResponseEntity<?> deleteFavouriteViewData(@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam(name = "favouriteID") String FavouriteId,
			@RequestParam(name = "siteKey") String siteKey) throws IOException, URISyntaxException {

		ResponseModel responseModel = new ResponseModel();
		try {

			if (service.deleteFavouriteViewData(userId, FavouriteId, siteKey) == 1) {
				responseModel.setResponseDescription("FavouriteView Successfully deleted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Favourite Id not found ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			responseModel.setResponseMessage("Success!");

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
			return ResponseEntity.ok(responseModel);
		}

		return ResponseEntity.ok(responseModel);
	}

	@PostMapping("/save-favourite-order")
	public ResponseEntity<?> saveFavouriteOrder(@RequestBody FavouriteOrder favouriteModel) throws IOException,
			URISyntaxException, org.json.simple.parser.ParseException, ParseException, SQLException {
		ResponseModel responseModel = new ResponseModel();

		try {

			favouriteModel.setIsActive(true);
			favouriteModel.setCreatedTime(functions.getCurrentDateWithTime());
			favouriteModel.setOrderId(favouriteModel.getCreatedBy() + "_" + favouriteModel.getReportName());

			if (service.saveFavouriteOrder(favouriteModel) == 1) {
				responseModel.setResponseDescription("FavouriteOrder Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Try again");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			responseModel.setResponseMessage("Success!");

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

		// return ResponseEntity.ok(body);
	}

	@PostMapping("/check-favourite-name")
	public ResponseEntity<?> checkfavouriteName(@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam(name = "siteKey") String siteKey,
			@RequestParam(name = "reportName", required = false) String reportName,
			@RequestParam(name = "favouriteName", required = false) String favouriteName) throws IOException,
			URISyntaxException, org.json.simple.parser.ParseException, ParseException, SQLException {

		ResponseModel responseModel = new ResponseModel();

		try {
			responseModel.setValidation(service.checkfavouriteName(userId, siteKey, favouriteName, reportName));
			responseModel.setResponseMessage("Success");
			responseModel.setResponseCode(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
			responseModel.setData(new JSONArray());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

		// return ResponseEntity.ok(body);
	}

	@PostMapping("/saveHealthCheck")
	public ResponseEntity<?> saveHealthCheck(@RequestBody HealthCheckModel healthCheckModel, HttpServletRequest request,
			HttpServletResponse respone) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			HealthCheck healthCheck = healthCheckService.convertToEntity(healthCheckModel, "create");
			HealthCheck healthCheckObj = healthCheckService.saveHealthCheck(healthCheck);

			healthCheckModel.setHealthCheckId(healthCheckObj.getHealthCheckId());
			updateTasklistActions(healthCheckModel.getReportBy());
			if (healthCheckObj != null) {
				responseModel.setResponseMessage("Success");
				responseModel.setjData(healthCheckModel);
				responseModel.setResponseDescription("HealthCheck Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseMessage("Error");
				responseModel.setResponseDescription("HealthCheck not saved ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}

	@PostMapping("/getHealthCheck")
	public ResponseEntity<?> getHealthCheck(@RequestParam("healthCheckId") String healthCheckId,
			@RequestParam("authUserId") String authUserId, HttpServletRequest request, HttpServletResponse respone) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			JSONObject healthCheckObj = healthCheckService.getHealthCheck(healthCheckId, authUserId);

			if (healthCheckObj != null) {
				responseModel.setjData(healthCheckObj);
				responseModel.setResponseDescription("HealthCheck Successfully Retrieved ");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setjData(new JSONObject());
				responseModel.setResponseDescription("No data found");
				responseModel.setResponseCode(HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}

	@PostMapping("/updateHealthCheck")
	public ResponseEntity<?> updateHealthCheck(@RequestBody HealthCheckModel healthCheckModel,
			HttpServletRequest request, HttpServletResponse respone) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			HealthCheck healthCheck = healthCheckService.convertToEntity(healthCheckModel, "update");
			HealthCheck existingHealthCheck = healthCheckService
					.getHealthCheckObject(healthCheckModel.getHealthCheckId());
			BeanUtils.copyProperties(healthCheck, existingHealthCheck,
					NullAwareBeanUtilsBean.getNullPropertyNames(healthCheck));

			JSONObject healthCheckObj = healthCheckService.updateHealthCheck(healthCheck);
			
			updateTasklistActions(healthCheckModel.getReportBy());
			if (healthCheckObj != null) {
				responseModel.setjData(healthCheckObj);
				responseModel.setResponseDescription("HealthCheck Successfully Updated ");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("HealthCheck not Updated ");
				responseModel.setjData(new JSONObject());
				responseModel.setResponseCode(HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}

	@PostMapping("/deleteHealthCheck")
	public ResponseEntity<?> deleteHealthCheck(@RequestParam("healthCheckId") String healthCheckId,
			HttpServletRequest request, HttpServletResponse respone) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			HealthCheck healthCheck = new HealthCheck();
			healthCheck.setHealthCheckId(healthCheckId);
			boolean healthCheckObj = healthCheckService.deleteHealthCheck(healthCheck);

			if (healthCheckObj) {
				responseModel.setjData(healthCheckObj);
				responseModel.setResponseDescription("HealthCheck Successfully Deleted ");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("HealthCheck not Deleted ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}
	
	
	@PostMapping("/activateHealthCheck")
	public String activateHealthCheck(@RequestParam("healthCheckId") String healthCheckId, @RequestParam("isActive")boolean isActive,
			HttpServletRequest request, HttpServletResponse respone) {
		return healthCheckService.activateHealthCheck(healthCheckId, isActive);
	}

	@PostMapping("/getHealthCheckList")
	public ResponseEntity<?> getHealthCheckList(@RequestParam("siteKey") String siteKey,
			@RequestParam(name = "authUserId", required = false) String userId, HttpServletRequest request,
			HttpServletResponse respone, @RequestParam(name = "projectId", required = false) String projectId) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			String token = request.getHeader("Authorization");

			// JSONArray healthcheckList = healthCheckService.getAllHealthCheck(siteKey);
			GridDataFormat gridDataFormat = healthCheckService.getHealthCheckData(siteKey, userId, projectId);

			// if
			// (com.zenfra.model.ZKModel.getZkData().containsKey(ZKConstants.HEALTHCHECK_COLUMN_ORDER))
			// {
			String siteOrder = ZKConstants.HEALTHCHECK_COLUMN_ORDER;
			responseModel.setColumnOrder(Arrays.asList(siteOrder.split(",")));
			// }

			if (com.zenfra.model.ZKModel.getZkData().containsKey(ZKConstants.HEADER_LABEL)) {
				responseModel.setHeaderInfo(healthCheckService.getHeaderListFromV2(siteKey, userId, token));
			}

			if (!gridDataFormat.getData().isEmpty()) {
				//com.zenfra.model.GridDataFormat gridData = healthCheckService.getHealthCheckData(siteKey, userId, projectId);
				responseModel.setjData(gridDataFormat.getData());
				responseModel.setHeaderInfo(gridDataFormat.getHeaderInfo());
				responseModel.setResponseDescription("HealthCheck Successfully retrieved by sitekey ");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("No data found ");
				List<com.zenfra.model.GridHeader> gridHeaderList = new ArrayList<>();
				responseModel.setHeaderInfo(gridHeaderList);
				responseModel.setResponseCode(HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}

	@PostMapping("/healthCheckValidate")
	public JSONArray getHealthCheckNames(@RequestParam("siteKey") String siteKey) {

		JSONArray responseArray = new JSONArray();
		try {
			responseArray = healthCheckService.getHealthCheckNames(siteKey);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}
		return responseArray;

	}

	@PostMapping("/getHealthCheckProperties")
	public ResponseEntity<?> getDiscoveryReportProperties(@RequestParam(name = "reportCategory") String reportCategory,
			@RequestParam(name = "categoryType") String categoryType,
			@RequestParam(name = "projectID", required = false) String projectID,
			@RequestParam(name = "projectSource", required = false) String projectSource,
			@RequestParam(name = "siteKey", required = false) String siteKey,
			@RequestParam(name = "baseFilter", required = false) String baseFilter,
			@RequestParam(name = "groupBy", required = false) String groupBy,
			@RequestParam(name = "authUserId", required = false) String userId) throws Exception {

		JSONArray resultArray = new JSONArray();
		if (reportCategory.equalsIgnoreCase("healthcheck") && (categoryType.equalsIgnoreCase("healthcheck"))) {

			JSONArray healthCheckArray = healthCheckService.getAllHealthCheck(siteKey, false, userId, projectID);

			DeviceTypeModel deviceTypeModel1 = new DeviceTypeModel();
			deviceTypeModel1.setName("healthCheck");
			deviceTypeModel1.setLabel("Health Check Report Name");
			deviceTypeModel1.setType("select");
			List<DeviceTypeValueModel> deviceTypeValueModelList = new ArrayList<DeviceTypeValueModel>();

			for (int i = 0; i < healthCheckArray.size(); i++) {

				JSONObject jsonObject = (JSONObject) healthCheckArray.get(i);
				DeviceTypeValueModel deviceTypeValueModel = new DeviceTypeValueModel();
				if (jsonObject.containsKey("healthCheckId")) {
					deviceTypeValueModel.setValue(jsonObject.get("healthCheckId").toString());
				}
				if (jsonObject.containsKey("healthCheckName")) {
					deviceTypeValueModel.setLabel(jsonObject.get("healthCheckName").toString());
				}
				if (i == 0) {
					deviceTypeValueModel.setIsDefault(1);
				} else {
					deviceTypeValueModel.setIsDefault(0);
				}
				deviceTypeValueModelList.add(deviceTypeValueModel);
			}

			deviceTypeModel1.setValue(deviceTypeValueModelList);

			resultArray.add(deviceTypeModel1); // Server Type

			JSONArray healthCheckDisplayArray = healthCheckService.getHealthCheckDisplay();

			for (int i = 0; i < healthCheckDisplayArray.size(); i++) {

				JSONObject jsonObject = (JSONObject) healthCheckDisplayArray.get(i);

				DeviceTypeModel deviceTypeModel_display = new DeviceTypeModel();
				deviceTypeModel_display.setName(jsonObject.get("name").toString());
				deviceTypeModel_display.setLabel(jsonObject.get("label").toString());
				deviceTypeModel_display.setType(jsonObject.get("type").toString());
				deviceTypeModel_display.setMandatory(false);
				List<DeviceTypeValueModel> deviceTypeValueModelDisplay = new ArrayList<DeviceTypeValueModel>();
				JSONArray displayValue = objectMapper.readValue(jsonObject.get("value").toString(), JSONArray.class);

				if (!displayValue.isEmpty()) {
					for (int j = 0; j < displayValue.size(); j++) {
						Map<String, Object> displayObj = (LinkedHashMap<String, Object>) displayValue.get(j);
						DeviceTypeValueModel deviceTypeValueModel = new DeviceTypeValueModel();
						if (displayObj.containsKey("value")) {
							deviceTypeValueModel.setValue(displayObj.get("value").toString());
						}
						if (displayObj.containsKey("label")) {
							deviceTypeValueModel.setLabel(displayObj.get("label").toString());
						}
						int isDefault = 0;
						if (displayObj.get("isDefault").toString().equals("1")) {
							isDefault = 1;
						}
						deviceTypeValueModel.setIsDefault(isDefault);
						deviceTypeValueModelDisplay.add(deviceTypeValueModel);
					}
				}
				deviceTypeModel_display.setValue(deviceTypeValueModelDisplay);
				resultArray.add(deviceTypeModel_display);
			}
		}

		System.out.println("----resultArray--------- " + resultArray);

		return ResponseEntity.ok(resultArray);
	}
	
	private void updateTasklistActions(String projectId) {
		
		try {
			String protocol = ZKModel.getProperty(ZKConstants.APP_SERVER_PROTOCOL);
			String appServerIp = ZKModel.getProperty(ZKConstants.APP_SERVER_IP);
			String port = ZKModel.getProperty(ZKConstants.APP_SERVER_PORT);
			String uri = protocol + "://" + appServerIp + ":" + port
					+ "/UserManagement/rest/project/update-tasklist-validation-actions?projectId=" + projectId;
			System.out.println("URL: " + uri);
			RestTemplate restTemplate = new RestTemplate();
			uri =  CommonUtils.checkPortNumberForWildCardCertificate(uri);
			String result = restTemplate.getForObject(uri, String.class);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@GetMapping("/getHealthCheckByFilters")
	public ResponseEntity<?> getHealthCheckByFilters(@RequestParam("siteKey") String siteKey,
			@RequestParam(name = "authUserId", required = false) String userId, HttpServletRequest request,
			HttpServletResponse respone, @RequestParam(name="reportBy", required = false) String reportBy, 
			@RequestParam(name="componentType", required = false) String componentType,
			@RequestParam(name="analyticsType", required = false) String analyticsType,
			@RequestParam(name="reportName", required = false) String reportName) {

		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			String token = request.getHeader("Authorization");

			GridDataFormat gridDataFormat = healthCheckService.getHealthCheckDataByFilters(siteKey, userId, reportBy, componentType, analyticsType, reportName);

			String siteOrder = ZKConstants.HEALTHCHECK_COLUMN_ORDER;
			responseModel.setColumnOrder(Arrays.asList(siteOrder.split(",")));

			if (com.zenfra.model.ZKModel.getZkData().containsKey(ZKConstants.HEADER_LABEL)) {
				responseModel.setHeaderInfo(healthCheckService.getHeaderListFromV2(siteKey, userId, token));
			}

			if (!gridDataFormat.getData().isEmpty()) {
				responseModel.setjData(gridDataFormat.getData());
				responseModel.setHeaderInfo(gridDataFormat.getHeaderInfo());
				responseModel.setResponseDescription("HealthCheck Successfully retrieved by the filters ");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("No data found ");
				List<com.zenfra.model.GridHeader> gridHeaderList = new ArrayList<>();
				responseModel.setHeaderInfo(gridHeaderList);
				responseModel.setResponseCode(HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}

}
