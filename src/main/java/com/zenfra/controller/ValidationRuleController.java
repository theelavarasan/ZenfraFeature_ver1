package com.zenfra.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
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
	public ResponseEntity<?> getFieldValues(@RequestBody ValidationModel model) throws ParseException {
		
		Map<String, List<Object>> resultData = new HashMap<String, List<Object>>();
		JSONArray resultArray = new JSONArray();
		if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && !model.getReportBy().equalsIgnoreCase("Privileged Access")) {
			resultData = validationRuleService.getDiscoveryReportValues(model.getSiteKey(), model.getReportBy(),
				   	model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportList());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Compatibility")) {
			resultArray = validationRuleService.getVR_Compatibility(model.getSiteKey(), model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportBy());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Migration Method")) {
			resultArray = validationRuleService.getVR_MigrationMethod(model.getSiteKey(), model.getColumnName(), model.getCategory(), model.getDeviceType());
		} else if (model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("Privileged Access")) {
			resultArray = validationRuleService.getVR_PrivilledgeData(model.getSiteKey(), model.getColumnName());
		} else if(model.getAnalyticsType().equalsIgnoreCase("cloud-cost")) {
			resultArray = validationRuleService.getCloudCostReportValues(model.getSiteKey(), model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportBy());
		} 
			

		String colName = model.getColumnName();
		if(colName.contains("_")) {
			colName = colName.split("_")[1];
		}	
		
		if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && !model.getReportBy().equalsIgnoreCase("Privileged Access")) {
			if(resultData.containsKey(colName)) {		
				return ResponseEntity.ok(resultData.get(colName));
			} else if(resultData.containsKey(model.getColumnName())) {		
				return ResponseEntity.ok(resultData.get(model.getColumnName()));
			} 
			
		}
		
		if(model.getAnalyticsType().equalsIgnoreCase("Compatibility") || model.getAnalyticsType().equalsIgnoreCase("Migration Method") || model.getAnalyticsType().equalsIgnoreCase("cloud-cost")) {
			return ResponseEntity.ok(resultArray);
		} else if((model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("Privileged Access"))) {
			System.out.println("!!!!! Privileged Access access result: " + resultArray);
			return ResponseEntity.ok(resultArray);
		} else if(model.getAnalyticsType().equalsIgnoreCase("Project")) {
			resultArray = validationRuleService.getUniqueValues(model.getSiteKey(), model.getProjectId(), model.getColumnName());
			return ResponseEntity.ok(resultArray);
		}
		
		return ResponseEntity.ok(new JSONArray());

	}

}
