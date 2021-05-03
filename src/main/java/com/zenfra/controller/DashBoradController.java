package com.zenfra.controller;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.dataframe.service.DashBoardService;
import com.zenfra.model.ResponseModel_v2;

@RestController
@RequestMapping("/dashboard")
public class DashBoradController {

	
	@Autowired
	DashBoardService dashService;
	
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
}
