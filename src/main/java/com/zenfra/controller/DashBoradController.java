package com.zenfra.controller;

import javax.ws.rs.GET;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.dataframe.service.DashBoardService;
import com.zenfra.model.DashBoardCharts;
import com.zenfra.model.DashboardUserCustomization;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.utils.CommonFunctions;

@RestController
@RequestMapping("/dashboard")
public class DashBoradController {

	
	@Autowired
	DashBoardService dashService;
	
	@Autowired
	CommonFunctions functions;
	
	
	@GetMapping("/layout")
	public ResponseEntity<?> getDashLayout(
			@RequestParam String tenantId,@RequestParam String siteKey,
			@RequestParam String userId
			){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
	try {

		JSONObject responce=dashService.getDasboardLayout(tenantId,siteKey,userId);
		responseModel.setResponseMessage("Success");
		if (responce != null) {
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(responce);
		} else {
			responseModel.setResponseCode(HttpStatus.NOT_FOUND);
		}

	} catch (Exception e) {
		e.printStackTrace();
		responseModel.setResponseMessage("Error");
		responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
		responseModel.setResponseDescription(e.getMessage());

	}
	return ResponseEntity.ok(responseModel);
}
	
	@GetMapping("/delete-Chart")
	public ResponseEntity<?> deleteDashboardChart(
			@RequestParam String chartId,@RequestParam String siteKey,
			@RequestParam String userId
			){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
	try {

		Boolean responce=dashService.deleteDashboardChart(chartId,siteKey,userId);
		
		if (responce != null) {
			responseModel.setResponseMessage("Success!");
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(responce);
		} else {
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_FOUND);
		}

	} catch (Exception e) {
		e.printStackTrace();
		responseModel.setResponseMessage("Error");
		responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
		responseModel.setResponseDescription(e.getMessage());

	}
	return ResponseEntity.ok(responseModel);
}
	
	@PostMapping("/save-dashboard-layout")
	public ResponseEntity<?> saveDashboardLayout(
			@RequestBody DashboardUserCustomization dash
			){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
			try {
		
				dash.setData_id(functions.generateRandomId());
				dash.setActive(true);
				dash.setCreatedBy(dash.getUserId());
				dash.setUpdatedTime(functions.getCurrentDateWithTime());
				
				Boolean responce=dashService.saveDashboardLayout(dash);
				responseModel.setResponseMessage("Success");
				if (responce != null) {
					responseModel.setResponseDescription("Dashboard layout saved");
					responseModel.setResponseCode(HttpStatus.OK);
					responseModel.setjData(responce);
				} else {
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				}
		
			} catch (Exception e) {
				e.printStackTrace();
				responseModel.setResponseMessage("Error");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription(e.getMessage());
		
			}
			return ResponseEntity.ok(responseModel);
		}
	
	

	@PostMapping("/update-dashboard-layout")
	public ResponseEntity<?> updateDashboardLayout(
			@RequestBody DashboardUserCustomization dash
			){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
			try {
		
				DashboardUserCustomization dashExit=dashService.getDashboardUserCustomizationById(dash.getData_id());
						dashExit.setLayout(dash.getLayout());						
						dashExit.setActive(true);
						dashExit.setUpdatedBy(dash.getUserId());
						dashExit.setUpdatedTime(functions.getCurrentDateWithTime());
				
				Boolean responce=dashService.updateDashboardLayout(dashExit);
				responseModel.setResponseMessage("Success");
				if (responce != null) {
					responseModel.setResponseDescription("Dashboard layout updated");
					responseModel.setResponseCode(HttpStatus.OK);
					responseModel.setjData(responce);
				} else {
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				}
		
			} catch (Exception e) {
				e.printStackTrace();
				responseModel.setResponseMessage("Error");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription(e.getMessage());
		
			}
			return ResponseEntity.ok(responseModel);
		}
	
	@PostMapping("/save-dashboard-chart")
	public ResponseEntity<?> saveDashboardChart(
			@RequestBody DashBoardCharts dash
			){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
			try {
		
				dash.setData_id(functions.generateRandomId());
				dash.setActive(true);
				dash.setUpdatedBy(dash.getUserId());
				dash.setCreatedBy(dash.getUserId());
				dash.setCreatedTime(functions.getCurrentDateWithTime());
				dash.setUpdatedTime(functions.getCurrentDateWithTime());
				
				Boolean responce=dashService.saveDashboardChart(dash);
				responseModel.setResponseMessage("Success");
				if (responce != null) {
					responseModel.setResponseDescription("Dashboard charts updated");
					responseModel.setResponseCode(HttpStatus.OK);
					responseModel.setjData(responce);
				} else {
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				}
		
			} catch (Exception e) {
				e.printStackTrace();
				responseModel.setResponseMessage("Error");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription(e.getMessage());
		
			}
			return ResponseEntity.ok(responseModel);
		}
	
	
	@PostMapping("/update-dashboard-chart")
	public ResponseEntity<?> updateDashboardChart(
			@RequestBody DashBoardCharts dash
			){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
			try {
		
				dash.setActive(true);
				dash.setUpdatedBy(dash.getUserId());
				dash.setUpdatedTime(functions.getCurrentDateWithTime());
				
				Boolean responce=dashService.updateDashboardChart(dash);
				responseModel.setResponseMessage("Success");
				if (responce != null) {
					responseModel.setResponseDescription("Dashboard Chart updated");
					responseModel.setResponseCode(HttpStatus.OK);
					responseModel.setjData(responce);
				} else {
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				}
			} catch (Exception e) {
				e.printStackTrace();
				responseModel.setResponseMessage("Error");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription(e.getMessage());
		
			}
			return ResponseEntity.ok(responseModel);
		}
	
	
	@GetMapping("/get-chart-details")
	public ResponseEntity<?> getDashboardChartDetails(
			@RequestParam String chartId,
			@RequestParam String siteKey
			
			){
		

		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
			try {
		
				
				JSONObject responce=dashService.getDashboardChartDetails(chartId,siteKey);
				responseModel.setResponseMessage("Success");
				if (responce != null) {
					responseModel.setResponseDescription("Dashboard Chart retrieve");
					responseModel.setResponseCode(HttpStatus.OK);
					responseModel.setjData(responce);
				} else {
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				}
			} catch (Exception e) {
				e.printStackTrace();
				responseModel.setResponseMessage("Error");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription(e.getMessage());
		
			}
			return ResponseEntity.ok(responseModel);
	}
	
	
	@GetMapping("/get-chart-fav-menu")
	public ResponseEntity<?> getChatForFavMenu(@RequestParam String favouriteId,
			@RequestParam String userId,@RequestParam String siteKey){
		

		ResponseModel_v2 responseModel = new ResponseModel_v2();
	
			try {
		
				
				JSONObject responce=dashService.getChatForFavMenu(favouriteId,userId,siteKey);
				responseModel.setResponseMessage("Success");
				if (responce != null) {
					responseModel.setResponseDescription("Dashboard Chart retrieve");
					responseModel.setResponseCode(HttpStatus.OK);
					responseModel.setjData(responce);
				} else {
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				}
			} catch (Exception e) {
				e.printStackTrace();
				responseModel.setResponseMessage("Error");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription(e.getMessage());
		
			}
			return ResponseEntity.ok(responseModel);
	}
	
	
}
