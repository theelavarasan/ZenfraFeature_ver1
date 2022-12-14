package com.zenfra.controller;

import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.ProcessService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/process-log")
@Validated
public class ProcessController {

	
	@Autowired
	ProcessService service;
	
	@PostMapping
	public ResponseModel_v2 getFTPLogByServerId(@NotEmpty(message = "Please provide valid server id")@RequestParam("server_id") String serverId) {
		ResponseModel_v2 response=new ResponseModel_v2();
		try {
			response.setResponseCode(HttpStatus.OK);
			response.setjData(service.getFTPLogByServerId(serverId));
			response.setResponseDescription("Successfully retrieved..");
		} catch (Exception e) {
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage(e.getMessage());
		}		
		return response;
	}
	
}
