package com.zenfra.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.UserCreateService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@RestController
@RequestMapping("/rest/user")
public class UserController {

	@Autowired
	UserCreateService service;

	@Autowired
	CommonFunctions functions;

	@GetMapping
	public ResponseEntity<?> getUser(@RequestParam String userId) {
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			Object obj = service.getUserByUserId(userId);
			responseModel.setResponseMessage("Success");
			if (obj != null) {
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData(functions.convertEntityToJsonObject(obj));
			} else {
				responseModel.setResponseCode(HttpStatus.NOT_FOUND);
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			responseModel.setResponseDescription(e.getMessage());

		}
		return ResponseEntity.ok(responseModel);
	}

}
