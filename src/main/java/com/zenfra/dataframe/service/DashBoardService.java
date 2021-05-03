package com.zenfra.dataframe.service;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.DashBoardDao;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.model.DashboardUserCustomization;
import com.zenfra.utils.CommonFunctions;

import scala.reflect.ManifestFactory.ObjectManifest;

@Service
public class DashBoardService {

	@Autowired
	DashBoardDao dashDao;

	@Autowired
	CommonFunctions functions;

	@Autowired
	CommonQueriesData queries;

	public JSONObject getDasboardLayout(String tenantId, String siteKey, String userId) {
		JSONObject obj = new JSONObject();
		try {
			
		String getDashboardLayoutChart = queries.dashboardQueries().getGetDashboardLayoutChart()
					.replace(":user_id_value", userId).replace(":site_key_value", siteKey);
			DashboardUserCustomization layout = (DashboardUserCustomization) dashDao
					.getEntityByColumn(getDashboardLayoutChart, DashboardUserCustomization.class);

			obj.put("chartLayout", functions.convertStringToJsonArray(layout.getLayout()));
			
			String getDashboardLayoutChartLayout=queries.dashboardQueries().getGetDashboardLayoutChartLayout()
					.replace(":user_id_value", userId).replace(":site_key_value", siteKey);

			List<Map<String,Object>> chartDetails=dashDao.getListMapObjectById(getDashboardLayoutChartLayout);
				JSONArray chartObj=new JSONArray();
				ObjectMapper mapper = new ObjectMapper();
				for(Map<String,Object> list:chartDetails) {	
					
					JSONObject tempBreak = mapper.convertValue(list.get("filterProperty"), JSONObject.class);
						
					JSONObject obtemp=mapper.convertValue(list, JSONObject.class);
					if(tempBreak!=null && tempBreak.containsKey("value") && tempBreak.get("value")!=null) {
						JSONObject tempFilter=mapper.readValue(tempBreak.get("value").toString(), JSONObject.class);
						obtemp.put("filterProperty", tempFilter);						
					}
					chartObj.add(obtemp);		
				}
			obj.put("chartDetails", chartObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

}
