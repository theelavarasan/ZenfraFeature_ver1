package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.UserService;
import com.zenfra.utils.CommonFunctions;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	UserService service;
	
	@Autowired
	CommonFunctions functions;
	
	@GetMapping
	public  ResponseEntity<?> getUser(@RequestParam String userId){
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			Object obj=service.getUserByUserId(userId);
			responseModel.setResponseMessage("Success");
				if(obj!=null) {
					responseModel.setResponseCode(HttpStatus.OK);					
					responseModel.setjData(functions.convertEntityToJsonObject(obj));
				}else {
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
