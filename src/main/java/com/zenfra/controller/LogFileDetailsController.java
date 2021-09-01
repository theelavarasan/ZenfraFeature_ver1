
package com.zenfra.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.zenfra.model.LogFileDetails;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.LogFileDetailsService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Contants;
import com.zenfra.utils.NullAwareBeanUtilsBean;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/api/log-file")
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
	public ResponseEntity<ResponseModel_v2> saveLogFileDetails(@RequestParam String authUserId,@Valid @RequestBody LogFileDetails logFileDetails){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			logFileDetails.setLogFileId(functions.generateRandomId());
			logFileDetails.setUploadedBy(authUserId);
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
	public ResponseEntity<ResponseModel_v2> getALlLogFileDetails(@RequestParam String siteKey){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			response.setjData(service.getLogFileDetailsBySiteKey(siteKey));
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
	public ResponseEntity<ResponseModel_v2> updateLogFileDetailsByLogId(
			@RequestParam String authUserId,
			@RequestBody LogFileDetails logFileDetails){
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
			logFileDetails.setUploadedBy(authUserId);
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
	

	
	@GetMapping("/check-parsing-status")
	@ApiOperation(value="Get Log File processing status by array of log ids")
	@ApiResponse(code = 200, message = "Successfully retrived")
	public ResponseEntity<ResponseModel_v2> getLogFileProcessingStatus(
			@NotEmpty(message = "LogId's must be not empty") @RequestParam List<String> logIds){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {			
			
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setjData(service.getLogFileDetailsByLogids(logIds));
			response.setResponseDescription("Successfully deleted");
			response.setResponseMessage("Successfully deleted");	
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	
	
	@PostMapping("/upload-log-file")
    @ApiOperation(value = "Make a POST request to upload the file",
            produces = "application/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The POST call is Successful"),
            @ApiResponse(code = 500, message = "The POST call is Failed"),
            @ApiResponse(code = 404, message = "The API could not be found")
    })
    public ResponseEntity<?> uploadFile(
    		@RequestAttribute(name = "authUserId", required = false) String userId, MultipartHttpServletRequest request
            ) {
	  
	  ResponseModel_v2 responseModel_v2=new ResponseModel_v2();
			  try {
					MultipartFile file = request.getFile("parseFile");
					String type = request.getParameter("logType");
					String siteKey = request.getParameter("siteKey");
					String tenantId = request.getParameter("tenantId");
					String siteName = request.getParameter("siteName");
					String logId = request.getParameter("logId");
					String description = request.getParameter("description");
					String uploadAndProcess = request.getParameter("uploadAndProcess");
					String folderPath = request.getParameter("parseFilePath");
					String filePath = request.getParameter("parseFileName");
					
					File convFile=service.getFilePath(siteKey, type, file);
					
					LogFileDetails logFile=new LogFileDetails();
						logFile.setActive(true);
						logFile.setLogType(type);
						logFile.setSiteKey(siteKey);
						logFile.setTenantId(tenantId);
						logFile.setDescription(description);
						logFile.setUploadedBy(userId);
						logFile.setCreatedDateTime(functions.getCurrentDateWithTime());
						logFile.setUpdatedDateTime(functions.getCurrentDateWithTime());
						
					JSONObject filePaths=new JSONObject();
						filePaths.put("folderPath", folderPath);
						filePaths.put("filePath", filePath);
					
					
						List<LogFileDetails> logFileIdList=new ArrayList<LogFileDetails>();
					
					
					if(type.equalsIgnoreCase("auto")){
						String responsePath   = service.unZipFiles(convFile.getAbsolutePath(), siteKey);
						JSONObject responseJsonobject = service.predictModel(responsePath, siteKey);
						if(responseJsonobject != null && responseJsonobject.size() > 0 ){
							logFileIdList=service.parseExce(responseJsonobject, filePaths, uploadAndProcess, logFile, convFile);
						}else {
							responseModel_v2.setResponseMessage("Failed");
							responseModel_v2.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
							responseModel_v2.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
							responseModel_v2.setjData("There is no files found");
						}							
					}else {							
						LogFileDetails logFileTemp=service.saveLogFileDetails(filePaths, logFile, convFile, uploadAndProcess, Contants.LOG_FILE_STATUS_DRAFT, Contants.LOG_FILE_STATUS_DRAFT, "File in draft");
						logFileIdList.add(logFileTemp);
					}
					responseModel_v2.setResponseMessage("Success");
					responseModel_v2.setResponseCode(HttpStatus.OK);
					responseModel_v2.setStatusCode(HttpStatus.OK.value());
					JSONObject response=new JSONObject();
						response.put("logFileDetails", logFileIdList);
					responseModel_v2.setjData(response);
			
			return new ResponseEntity<ResponseModel_v2>(responseModel_v2,HttpStatus.OK);
			} catch (Exception e) {
				e.printStackTrace();
				
				
				return new ResponseEntity<ResponseModel_v2>(responseModel_v2,HttpStatus.EXPECTATION_FAILED);
			}
           
    }
  
  
	@PostMapping("/save-logtype-description")
	@ApiOperation(value="Saved Log File description ")
	@ApiResponse(code = 201, message = "Successfully created")	
	public ResponseEntity<ResponseModel_v2> saveLogtypeAndDescription(
			@NotEmpty(message = "logtype must be not empty") 
			@RequestParam(required = true) String logtype, 
			@NotEmpty(message = "description must be not empty") 
			@RequestParam(required = true) String description,
			@NotEmpty(message = "logFileIds must be not empty") 
			@RequestParam(required = true) List<String> logFileIds){
		ResponseModel_v2 response=new ResponseModel_v2();
		try {		
			
			if(!service.saveLogtypeAndDescription(logFileIds,description,logtype)) {
				
				response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
				response.setStatusCode(HttpStatus.EXPECTATION_FAILED.value());
				response.setResponseDescription("Something went wrong");
				response.setResponseMessage("Something went wrong");	
				return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
			}
			
			
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setResponseDescription("Successfully created");
			response.setResponseMessage("Successfully created");	
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.CREATED);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<ResponseModel_v2>(response,HttpStatus.EXPECTATION_FAILED);
		}
	}
	

}
	
	


