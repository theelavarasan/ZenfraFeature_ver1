package com.zenfra.controller;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ValidationModel;
import com.zenfra.service.ValidationRuleService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/reports")
public class ValidationRuleController {

	@Autowired
	ValidationRuleService validationRuleService;

	@RequestMapping(value = "/health-check/get-field-values", method = RequestMethod.POST)
	public ResponseEntity<?> getFieldValues(@RequestBody ValidationModel model) {

		System.out.println("----------Inside Controller");

		JSONArray resultArray = new JSONArray();
		Map<String, List<String>> resultData = validationRuleService.getDiscoveryReportValues(model.getSiteKey(), model.getReportBy(),
			   	model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportList());

		System.out.println("----------resultData=------- " + resultData);

		return ResponseEntity.ok(resultArray);

	}

}
