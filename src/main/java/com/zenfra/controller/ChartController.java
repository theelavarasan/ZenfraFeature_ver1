package com.zenfra.controller;

import java.text.ParseException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zenfra.model.ChartModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.utils.CommonFunctions;

@RestController
@RequestMapping("/chart")
public class ChartController {

	
	@Autowired
	CommonFunctions functions;
	
	@PostMapping
	public ResponseEntity<?> createChartConfig(@RequestAttribute(name = "authUserId", required = false) String userId, @RequestBody ChartModel chartModel) throws JsonProcessingException, ParseException {
		
		chartModel.setUserId(userId);
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		
		
		try 
		{
			chartModel.setCreatedTime(functions.getCurrentDateWithTime());		
			chartModel.setUpdateTime(functions.getCurrentDateWithTime());		
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return ResponseEntity.ok(responseModel);
		
	}
}
