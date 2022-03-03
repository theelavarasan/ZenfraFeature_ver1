package com.zenfra.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

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
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.PolicyModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.PolicyService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;
import com.zenfra.utils.NullAwareBeanUtilsBean;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/policy")
@Api(value = "Policy", description = "Policy details table Operations")
@Validated
public class PolicyController {

	@Autowired
	PolicyService service;

	@Autowired
	CommonFunctions functions;

	@PostMapping
	@ApiOperation(value = "Policy site Details ")
	@ApiResponse(code = 201, message = "Successfully created")
	public ResponseEntity<ResponseModel_v2> saveSiteDetails(@Valid @RequestBody PolicyModel policy) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			policy.setPolicyDataId(functions.generateRandomId());
			policy.setCreatedDateTime(functions.getCurrentDateWithTime());
			policy.setUpdatedDateTime(functions.getCurrentDateWithTime());
			policy.setCreateBy(policy.getUserId());
			policy.setUpdateBy(policy.getUserId());
			policy.setResourcesString(policy.getResources().toJSONString());
			response.setjData(service.save(policy));
			response.setResponseCode(HttpStatus.CREATED);
			response.setStatusCode(HttpStatus.CREATED.value());
			response.setResponseDescription("Successfully created");
			response.setResponseMessage("Successfully created");
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.CREATED);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.EXPECTATION_FAILED);
		}
	}

	@GetMapping
	@ApiOperation(value = "Get all policy details")
	@ApiResponse(code = 200, message = "Successfully retrieved")
	public ResponseEntity<ResponseModel_v2> getAllSiteDetails() {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			response.setjData(service.findAll());
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setResponseDescription("Successfully retrieved");
			response.setResponseMessage("Successfully retrieved");
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.EXPECTATION_FAILED);
		}
	}

	@GetMapping("/{policyId}")
	@ApiOperation(value = "Get policy details by id")
	@ApiResponse(code = 201, message = "Successfully retrieved")
	public ResponseEntity<ResponseModel_v2> getSiteById(
			@NotEmpty(message = "policyId must not be empty") @PathVariable String policyId) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			response.setjData(service.findOne(policyId));
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setResponseDescription("Successfully retrieved");
			response.setResponseMessage("Successfully retrieved");
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.EXPECTATION_FAILED);
		}
	}

	@PutMapping
	@ApiOperation(value = "Update policy Details by site id")
	@ApiResponse(code = 201, message = "Successfully updated")
	public ResponseEntity<ResponseModel_v2> updateLogFileDetailsByLogId(@RequestBody PolicyModel policy) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {

			PolicyModel policyExist = service.findOne(policy.getPolicyDataId());

			if (policyExist == null) {
				response.setResponseDescription("Policy details not exist");
				response.setResponseMessage("Please sent valid params");
				response.setResponseCode(HttpStatus.NOT_FOUND);
				return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.OK);

			}
			BeanUtils.copyProperties(policy, policyExist, NullAwareBeanUtilsBean.getNullPropertyNames(policy));
			policyExist.setResourcesString(policyExist.getResources().toJSONString());
			response.setjData(service.update(policyExist));
			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			response.setResponseDescription("Successfully retrieved");
			response.setResponseMessage("Successfully retrieved");
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.EXPECTATION_FAILED);
		}
	}

	@DeleteMapping("/{policyId}")
	@ApiOperation(value = "Delete policy Details by log id")
	@ApiResponse(code = 201, message = "Successfully created")
	public ResponseEntity<ResponseModel_v2> deleteLogFileDetailsByLogId(@PathVariable String policyId) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {

			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			service.deleteById(policyId);
			response.setResponseDescription("Successfully deleted");
			response.setResponseMessage("Successfully deleted");
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.EXPECTATION_FAILED);
		}
	}

}
