package com.zenfra.service;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.HealthCheckDao;
import com.zenfra.model.HealthCheck;
import com.zenfra.model.HealthCheckModel;
import com.zenfra.utils.CommonFunctions;

@Service
public class HealthCheckService {
	
	@Autowired
	HealthCheckDao healthCheckDao;

	@Autowired
	CommonFunctions commonFunctions;
	
	public HealthCheck saveHealthCheck(HealthCheck healthCheck) {
		healthCheck.setHealthCheckId(commonFunctions.generateRandomId());
		healthCheckDao.saveEntity(HealthCheck.class, healthCheck);
		HealthCheck savedObj = (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
		healthCheck.setHealthCheckId(savedObj.getHealthCheckId());
		return healthCheck;
	}


	public JSONObject getHealthCheck(String healthCheckId) {
		HealthCheck healthCheck= new HealthCheck();
		healthCheck.setHealthCheckId(healthCheckId);
		HealthCheck savedObj = (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
		JSONObject healthCheckModel = convertEntityToModel(savedObj);
		System.out.println("healthCheckModel::"+healthCheckModel);
		return healthCheckModel;
	}

	public JSONObject updateHealthCheck(HealthCheck healthCheck) {
		healthCheckDao.updateEntity(HealthCheck.class, healthCheck);
		HealthCheck savedObj = (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
		JSONObject healthCheckModel = convertEntityToModel(savedObj);
		return healthCheckModel;
	}

	public boolean deleteHealthCheck(HealthCheck healthCheck) {
		// TODO Auto-generated method stub
		healthCheck = (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
		return healthCheckDao.deleteByEntity(healthCheck);
	}

	public HealthCheck convertToEntity(HealthCheckModel healthCheckModel) {
		HealthCheck healthCheck = new HealthCheck();
		healthCheck.setHealthCheckId(healthCheckModel.getHealthCheckId());
		healthCheck.setSiteKey(healthCheckModel.getSiteKey());
		healthCheck.setComponentType(healthCheckModel.getComponentType());
		healthCheck.setHealthCheckName(healthCheckModel.getHealthCheckName());
		healthCheck.setReportName(healthCheckModel.getReportName());
		healthCheck.setReportBy(healthCheckModel.getReportBy());
		healthCheck.setReportName(healthCheckModel.getReportName());
		healthCheck.setSiteAccessList(String.join(",", healthCheckModel.getSiteAccessList()));
		healthCheck.setUserAccessList(String.join(",", healthCheckModel.getUserAccessList()));
		healthCheck.setReportCondition(healthCheckModel.getReportCondition().toJSONString()); //().replaceAll("\\s", "").replaceAll("\n", "").replaceAll("\r", "")
		
		return healthCheck;
	}
	

	private JSONObject convertEntityToModel(HealthCheck healthCheck) {
		JSONObject response=new JSONObject();
		response.put("siteKey", healthCheck.getHealthCheckId());
		response.put("healthCheckName", healthCheck.getHealthCheckName());
		response.put("componentType", healthCheck.getComponentType());
		response.put("reportName", healthCheck.getReportName());
		response.put("reportBy", healthCheck.getReportBy());
		try {
			String s =  healthCheck.getReportCondition();
			ObjectMapper mapper = new ObjectMapper();
			JSONArray actualObj = mapper.readValue(s, JSONArray.class);
			response.put("reportCondition",  actualObj);			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		response.put("siteAccessList",Arrays.asList(healthCheck.getSiteAccessList()));
		response.put("userAccessList",Arrays.asList(healthCheck.getUserAccessList()));		
		return response;
	}


	public JSONArray getAllHealthCheck(String siteKey) {
		JSONArray resultArray = new JSONArray();
		try {
			List<Object> resultList = healthCheckDao.getEntityListByColumn("select * from health_check where site_key="+siteKey, HealthCheck.class);
			System.out.println("--------resultList---------- " + resultList.size());
			if(resultList != null && !resultList.isEmpty()) {
				for(Object obj : resultList) {
					if(obj instanceof HealthCheck) {
						JSONObject response = convertEntityToModel((HealthCheck) obj);
						resultArray.add(response);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
	}

}
