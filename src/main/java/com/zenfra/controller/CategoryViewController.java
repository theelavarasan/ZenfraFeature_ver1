package com.zenfra.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ResponseModel_v2;

@RestController
@RequestMapping("/category-view")
public class CategoryViewController{

	
	
	@PostMapping
	public ResponseEntity<?> saveCategoryView(){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			responseModel.setResponseMessage("Success");
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setResponseDescription("Category saved");
			//responseModel.setjData(service.getFavView(userId, siteKey, reportName, projectId));
			
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
		}
		return ResponseEntity.ok(responseModel);
	}
	
}
