package com.zenfra.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ResponseModel_v2;

@RestController
@RequestMapping("/report")
public class ReportsController {

	
	@GetMapping("/chart-migrationreport")
	public ResponseEntity<?> getMigarationReport(@RequestParam String siteKey,
			String userId,String reportName){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			responseModel.setResponseMessage("Success");
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}
		
	}
	
	
	
	
}
