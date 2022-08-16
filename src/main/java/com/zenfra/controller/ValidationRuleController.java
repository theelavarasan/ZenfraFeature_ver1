package com.zenfra.controller;

import java.util.Collection;
import java.util.Collections;
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
		if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && !model.getReportBy().equalsIgnoreCase("Privileged Access") && !model.getReportBy().equalsIgnoreCase("Group Info")
				&& !model.getReportBy().equalsIgnoreCase("Group") && !model.getReportBy().equalsIgnoreCase("User") && !model.getReportBy().equalsIgnoreCase("Sudoers")
				&& !(model.getDeviceType().equalsIgnoreCase("Tanium") && model.getReportBy().equalsIgnoreCase("Server"))) {
			resultData = validationRuleService.getDiscoveryReportValues(model.getSiteKey(), model.getReportBy(),
				   	model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportList(), model.getAnalyticsType());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Compatibility")) {
			resultArray = validationRuleService.getVR_Compatibility(model.getSiteKey(), model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportBy());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Migration Method")) {
			resultArray = validationRuleService.getVR_MigrationMethod(model.getSiteKey(), model.getColumnName(), model.getCategory(), model.getDeviceType());
		} else if (model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("Privileged Access")) {
			resultArray = validationRuleService.getVR_PrivilledgeData(model.getSiteKey(), model.getColumnName());
		} else if(model.getAnalyticsType().equalsIgnoreCase("cloud-cost")) {
			//resultArray = validationRuleService.getCloudCostReportValues(model.getSiteKey(), model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportBy());
			resultArray = validationRuleService.getCloudCostReportValuesPostgres(model.getSiteKey(), model.getColumnName(), model.getCategory(), model.getDeviceType(), model.getReportBy());
		} else if (model.getAnalyticsType().equalsIgnoreCase("onpremises-cost") && !model.getReportBy().equalsIgnoreCase("Privileged Access") 
				&& !model.getReportBy().equalsIgnoreCase("Group Info") && !model.getReportBy().equalsIgnoreCase("Group") && !model.getReportBy().equalsIgnoreCase("User")) {
			resultArray = validationRuleService.getOnpremisesCostFieldType(model.getSiteKey(), model.getColumnName(), model.getOsType());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("Group Info")) {
			resultArray = validationRuleService.getVR_VanguardGroupInfo(model.getSiteKey(), model.getColumnName());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("Group")) {
			resultArray = validationRuleService.getVR_TaniumGroup(model.getSiteKey(), model.getColumnName());
		}else if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("User") && model.getDeviceType().equalsIgnoreCase("zoom")) {
			resultArray = validationRuleService.getVR_ZoomUsers(model.getSiteKey(), model.getColumnName());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("User") && model.getDeviceType().equalsIgnoreCase("tanium")) {
			resultArray = validationRuleService.getVR_TaniumUsers(model.getSiteKey(), model.getColumnName());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("server") && model.getDeviceType().equalsIgnoreCase("tanium")) {
			resultArray = validationRuleService.getVR_TaniumServer(model.getSiteKey(), model.getColumnName());
		} else if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && model.getReportBy().equalsIgnoreCase("sudoers") && model.getDeviceType().equalsIgnoreCase("tanium")) {
			resultArray = validationRuleService.getVR_TaniumSudoers(model.getSiteKey(), model.getColumnName());
		}
			

		String colName = model.getColumnName();
		if(colName.contains("_")) {
			colName = colName.split("_")[1];
		}	
		
		if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && !model.getReportBy().equalsIgnoreCase("Privileged Access") 
				&& !model.getReportBy().equalsIgnoreCase("Group Info") && !model.getReportBy().equalsIgnoreCase("Group") && !model.getReportBy().equalsIgnoreCase("User") 
				&& !model.getReportBy().equalsIgnoreCase("Sudoers")
		&& !(model.getDeviceType().equalsIgnoreCase("Tanium") && model.getReportBy().equalsIgnoreCase("Server"))) {
					
			if(resultData.containsKey(colName)) {	
				List<Object> datas = resultData.get(colName);
				return ResponseEntity.ok(datas.stream().sorted());
			} else if(resultData.containsKey(model.getColumnName())) {
				List<Object> datas = resultData.get(model.getColumnName());
				return ResponseEntity.ok(datas.stream().sorted());
			} 
			
		}
		
		if(model.getAnalyticsType().equalsIgnoreCase("Compatibility") || model.getAnalyticsType().equalsIgnoreCase("Migration Method") 
				|| model.getAnalyticsType().equalsIgnoreCase("cloud-cost") || model.getAnalyticsType().equalsIgnoreCase("onpremises-cost")) {
			return ResponseEntity.ok(resultArray);
		} else if(model.getAnalyticsType().equalsIgnoreCase("Discovery") && (model.getReportBy().equalsIgnoreCase("Privileged Access") || model.getReportBy().equalsIgnoreCase("Group Info")
				|| model.getReportBy().equalsIgnoreCase("Group") || model.getReportBy().equalsIgnoreCase("User") || model.getReportBy().equalsIgnoreCase("Sudoers")
				|| (model.getDeviceType().equalsIgnoreCase("Tanium") && model.getReportBy().equalsIgnoreCase("Server")))) {
			System.out.println("!!!!! Privileged Access access result: " + resultArray);
			return ResponseEntity.ok(resultArray);
		} else if(model.getAnalyticsType().equalsIgnoreCase("Project")) {
			resultArray = validationRuleService.getUniqueValues(model.getSiteKey(), model.getReportBy(), model.getColumnName());
			return ResponseEntity.ok(resultArray);
		}
		
		return ResponseEntity.ok(new JSONArray());

	}

}
