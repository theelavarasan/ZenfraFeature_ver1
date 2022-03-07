package com.zenfra.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.validation.Valid;

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

import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.SiteModel;
import com.zenfra.service.SiteService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;
import com.zenfra.utils.NullAwareBeanUtilsBean;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/site")
@Api(value = "Site", description = "Site details table Operations")
@Validated
public class SiteController {

	@Autowired
	SiteService service;

	@Autowired
	CommonFunctions functions;

	@PostMapping
	@ApiOperation(value = "Saved site Details ")
	@ApiResponse(code = 201, message = "Successfully created")
	public ResponseEntity<ResponseModel_v2> saveSiteDetails(@Valid @RequestBody SiteModel site) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			site.setSiteDataId(functions.generateRandomId());
			// site.setTenantId(functions.generateRandomId());
			site.setSiteKey(functions.generateRandomId());
			site.setCreateTime(functions.getCurrentDateWithTime());
			site.setUpdatedTime(functions.getCurrentDateWithTime());
			site.setCreateBy(site.getUserId());
			site.setUpdatedBy(site.getUserId());
			site.setColumnOrderValue(site.getColumnOrder().toString());
			response.setjData(service.save(site));
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
	@ApiOperation(value = "Get all site details")
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

	@GetMapping("/{siteId}")
	@ApiOperation(value = "Get site details by id")
	@ApiResponse(code = 201, message = "Successfully retrieved")
	public ResponseEntity<ResponseModel_v2> getSiteById(@PathVariable String siteId) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			response.setjData(service.findOne(siteId));
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
	@ApiOperation(value = "Update Site Details by site id")
	@ApiResponse(code = 201, message = "Successfully updated")
	public ResponseEntity<ResponseModel_v2> updateLogFileDetailsByLogId(@RequestBody SiteModel site) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {

			SiteModel siteExist = service.findOne(site.getSiteDataId());

			if (siteExist == null) {
				response.setResponseDescription("Site details not exist");
				response.setResponseMessage("Please sent valid params");
				response.setResponseCode(HttpStatus.NOT_FOUND);
				return new ResponseEntity<ResponseModel_v2>(response, HttpStatus.OK);

			}
			BeanUtils.copyProperties(site, siteExist, NullAwareBeanUtilsBean.getNullPropertyNames(site));
			siteExist.setColumnOrderValue(siteExist.getColumnOrder().toString());
			siteExist.setUpdatedTime(functions.getCurrentDateWithTime());
			siteExist.setUpdatedBy(site.getUserId());
			response.setjData(service.update(siteExist));
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

	@DeleteMapping("/{siteId}")
	@ApiOperation(value = "Delete Site Details by log id")
	@ApiResponse(code = 201, message = "Successfully created")
	public ResponseEntity<ResponseModel_v2> deleteLogFileDetailsByLogId(@PathVariable String siteId) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {

			response.setResponseCode(HttpStatus.OK);
			response.setStatusCode(HttpStatus.OK.value());
			service.deleteById(siteId);
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
