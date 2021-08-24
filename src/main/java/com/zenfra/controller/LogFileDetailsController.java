package com.zenfra.controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.LogFileDetails;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.LogFileDetailsService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.NullAwareBeanUtilsBean;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/log-file")
@Api(value="Log file details", description="Log file details table Operations")
@Validated
public class LogFileDetailsController {

	@Autowired
	LogFileDetailsService service;
	
	
	
	@Autowired
	CommonFunctions functions;
	
	@PostMapping
	@ApiOperation(value="Saved Log File Details ")
	@ApiResponse(code = 201, message = "Successfully created")	
	public ResponseEntity<ResponseModel_v2> saveLogFileDetails(@Valid @RequestBody LogFileDetails logFileDetails){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			logFileDetails.setLogFileId(functions.generateRandomId());
			response.setjData(service.save(logFileDetails));
			response.setResponseCode(HttpStatus.CREATED);
			response.setStatusCode(HttpStatus.CREATED.value());
			response.setResponseDescription("Successfully created");
			response.setResponseMessage("Successfully created");	
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.CREATED);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	
	
	
	@GetMapping
	@ApiOperation(value="Get all log file details")
	@ApiResponse(code = 200, message = "Successfully retrieved")	
	public ResponseEntity<ResponseModel_v2> getALlLogFileDetails(){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			response.setjData(service.findAll());
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setResponseDescription("Successfully retrieved");
			response.setResponseMessage("Successfully retrieved");	
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	
	@GetMapping("/{logId}")
	@ApiOperation(value="Get log file details by id")
	@ApiResponse(code = 201, message = "Successfully retrieved")	
	public ResponseEntity<ResponseModel_v2> getLogFileDetailsByLogId(
			@NotEmpty(message ="logId must be not empty") @PathVariable String logId){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			
			LogFileDetails logFile=service.findOne(logId);
			
			if(logFile!=null && logFile.getActive()) {
				response.setjData(logFile);
				response.setResponseCode(HttpStatus.OK);
				response.setStatusCode(HttpStatus.OK.value());
				response.setResponseDescription("Successfully retrieved");
				response.setResponseMessage("Successfully retrieved");	
				return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
			}else {
				response.setResponseCode(HttpStatus.NOT_FOUND);
				response.setStatusCode(HttpStatus.NOT_FOUND.value());
				response.setResponseDescription("Successfully retrieved");
				response.setResponseMessage("Successfully retrieved");	
				return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.NOT_FOUND);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	
	
	@PutMapping
	@ApiOperation(value="Update Log File Details by log id")
	@ApiResponse(code = 201, message = "Successfully updated")	
	public ResponseEntity<ResponseModel_v2> updateLogFileDetailsByLogId(@RequestBody LogFileDetails logFileDetails){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			
			LogFileDetails logFileDetailsExist=service.findOne(logFileDetails.getLogFileId());
			
			if(logFileDetailsExist==null) {
				response.setResponseDescription("LogFileDetails details not exist");
				response.setResponseMessage("Please sent valid params");	
				response.setResponseCode(HttpStatus.NOT_FOUND);	
				return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
			
			}
			BeanUtils.copyProperties(logFileDetails, logFileDetailsExist, NullAwareBeanUtilsBean.getNullPropertyNames(logFileDetails));
			response.setjData(service.update(logFileDetailsExist));
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setResponseDescription("Successfully retrieved");
			response.setResponseMessage("Successfully retrieved");	
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	
	@DeleteMapping("/{logId}")
	@ApiOperation(value="Delete Log File Details by log id")
	@ApiResponse(code = 201, message = "Successfully deleted")	
	public ResponseEntity<ResponseModel_v2> deleteLogFileDetailsByLogId(@NotEmpty(message = "logId must be not empty") @PathVariable String logId){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			
			LogFileDetails logFileDetailsExist=service.findOne(logId);
			
			if(logFileDetailsExist==null) {
				response.setResponseDescription("LogFileDetails details not exist");
				response.setResponseMessage("Please sent valid params");	
				response.setResponseCode(HttpStatus.NOT_FOUND);	
				return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
			
			}
			logFileDetailsExist.setActive(false);
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			service.update(logFileDetailsExist);
			response.setResponseDescription("Successfully deleted");
			response.setResponseMessage("Successfully deleted");	
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	

	
	@GetMapping("/get-log-status")
	@ApiOperation(value="Get Log File processing status by array of log ids")
	@ApiResponse(code = 200, message = "Successfully retrived")
	public ResponseEntity<ResponseModel_v2> getLogFileProcessingStatus(
			@NotEmpty(message = "LogId's must be not empty") @RequestParam String logIds){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			
			JSONParser parser = new JSONParser();
			JSONArray rid = (JSONArray) parser.parse(logIds);

			Stream<String> ss = rid.stream().map (json->json.toString ());
	        List<String> logId = ss.collect (Collectors.toList ());
	        
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setjData(service.getLogFileDetailsByLogids(logId));
			response.setResponseDescription("Successfully deleted");
			response.setResponseMessage("Successfully deleted");	
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	
	
	
	
}
