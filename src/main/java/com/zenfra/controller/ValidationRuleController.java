package com.zenfra.controller;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.service.ValidationRuleService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/reports")
public class ValidationRuleController {

	@Autowired
	ValidationRuleService validationRuleService;

	@RequestMapping(value = "/health-check/get-field-values", method = RequestMethod.POST)
	public ResponseEntity<?> getFieldValues(
			@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam(name = "siteKey", required = true) String siteKey,
			@RequestParam(name = "reportBy", required = false) String reportBy,
			@RequestParam(name = "columnName", required = true) String columnName,
			@RequestParam(name = "category", required = true) String category,
			@RequestParam(name = "deviceType", required = false) String deviceType,
			@RequestParam(name = "reportList", required = true) String reportList) throws Throwable {
		
		System.out.println("----------Inside Controller");

		JSONArray resultArray = new JSONArray();
		Map<String, List<String>> resultData = validationRuleService.getDiscoveryReportValues(siteKey, reportBy, columnName, category,
				deviceType, reportList);
		
		System.out.println("----------resultData=------- " + resultData);
		
		return ResponseEntity.ok(resultArray);

	}

}
