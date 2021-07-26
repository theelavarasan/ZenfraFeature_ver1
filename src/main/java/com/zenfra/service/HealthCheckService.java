package com.zenfra.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zenfra.dao.HealthCheckDao;
import com.zenfra.model.HealthCheck;
import com.zenfra.model.HealthCheckModel;
import com.zenfra.model.ZKConstants;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Constants;

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

	public HealthCheck convertToEntity(HealthCheckModel healthCheckModel, String type) {
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
		healthCheck.setUserId(healthCheckModel.getAuthUserId());
		if(type.equalsIgnoreCase("update")) {
			healthCheck.setCreateBy(healthCheckModel.getAuthUserId());
			healthCheck.setCreatedDate(new Date());
		}		
		healthCheck.setUpdateBy(healthCheckModel.getAuthUserId());
		healthCheck.setUpdateDate(new Date());
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
			List<Object> resultList = healthCheckDao.getEntityListByColumn("select * from health_check where site_key='"+siteKey+"'", HealthCheck.class);
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


	public JSONArray getHealthCheckNames(String siteKey) {
		JSONArray resultArray = new JSONArray();
		try {
			List<Object> resultList = healthCheckDao.getEntityListByColumn("select * from health_check where site_key='"+siteKey+"'", HealthCheck.class);
			if(resultList != null && !resultList.isEmpty()) {
				for(Object obj : resultList) {
					if(obj instanceof HealthCheck) {						
						resultArray.add(((HealthCheck) obj).getHealthCheckName());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return resultArray;
	}
	
	public JSONArray getHeaderListFromV2(String siteKey, String userId, String token) {
		JSONArray  healthCheckHeader = new JSONArray();
		try {
			String protocol = com.zenfra.model.ZKModel.getProperty(ZKConstants.APP_SERVER_PROTOCOL);
			String host_name = com.zenfra.model.ZKModel.getProperty(ZKConstants.APP_SERVER_IP);
			String port = com.zenfra.model.ZKModel.getProperty(ZKConstants.APP_SERVER_PORT);			
           
			
			JSONObject requestBody = new JSONObject();
			requestBody.put("siteKey", siteKey);
			requestBody.put("authUserId", userId);		
			
			

			System.out.println("-----------------healthcheck----------------- "+token+ " : "  +  protocol + "://" + host_name + ":" + port + "/ZenfraV2/rest/reports/health-check/headrInfo");
			
			
			org.jsoup.Connection.Response execute = Jsoup.connect(protocol + "://" + host_name + ":" + port + "/ZenfraV2/rest/reports/health-check/headrInfo")
					  .header("Content-Type", "application/json")
				        .header("Accept", "application/json")
				        .header("Authorization", token)
				        .followRedirects(true)
				        .ignoreHttpErrors(true)
				        .ignoreContentType(true)
				        .userAgent("Mozilla/5.0 AppleWebKit/537.36 (KHTML," +
				                " like Gecko) Chrome/45.0.2454.4 Safari/537.36")
				        .method(org.jsoup.Connection.Method.POST)
				        .data("siteKey", siteKey)
				        .data("authUserId", userId)
				        .requestBody( requestBody.toString())
				        .maxBodySize(1_000_000 * 30) // 30 mb ~
				        .timeout(0) // infinite timeout
				        .execute();		

					Document doc =  execute.parse();
					Element body = doc.body();
					JSONParser parser = new JSONParser();
					
					System.out.println("-----discovery data length--------" + body.text());
					JSONObject headrInfoData = (JSONObject)parser.parse(body.text());				
					healthCheckHeader = (JSONArray) parser.parse(headrInfoData.toJSONString());						
					System.out.println("---datacount-----" + healthCheckHeader.size());
					

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return healthCheckHeader;
	}

}
