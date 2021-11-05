package com.zenfra.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.queries.ChartQueries;
import com.zenfra.utils.CommonFunctions;

@Component
public class ChartDAO extends CommonEntityManager{

	@Autowired
	CommonFunctions functions;
	
	@Autowired
	CommonQueriesData quereis;	
	
	public int SaveChart(Map<String,Object> params) {
		int responce=0;
		try {			
			responce=updateQuery(quereis.chart().getSave());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responce;
	}




	public List<Object> getChartByUserId(String userId) {
		 List<Object> chart=new ArrayList<Object>();
		try {
			
			String query=quereis.chart().getGetChartsByUserId().replace(":user_id", userId);
			chart=getEntityListByColumn(query, ChartModel_v2.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return chart;
	}




	public List<Map<String, Object>> getMigarationReport(String siteKey, String userId, String reportName) {
		List<Map<String, Object>> list=new ArrayList<Map<String,Object>>();
		try{			
			String query=quereis.chart().getMigarationReport().replace(":site_key", siteKey)
					.replace(":user_id", userId).replace(":report_name", reportName);
			System.out.println("query::"+query);
			list=getListMapObjectById(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	public List<Object> getChartByCategoryId(String catgoryId) {
		 List<Object> chart=new ArrayList<Object>();
		try {			
			String query="select * from chart where category_list @> Array[':catgoryId']".replace(":catgoryId", catgoryId);
			chart=getEntityListByColumn(query, ChartModel_v2.class);			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return chart;
	}




	public List<Map<String, Object>> getChartsBySiteKeyAndLogType(String siteKey, String sourceType) {
		List<Map<String, Object>> charts = new ArrayList<>();
		try {		
			charts = getListMapObjectById("select chart_id, chart_configuration, filter_property, chart_type, site_key, chart_name from chart where site_key='"+siteKey+"' and lower(report_label) like '%"+sourceType.toLowerCase()+"%' and is_active='true'");
		 } catch (Exception e) {
			e.printStackTrace();
		}
		return charts;
	}




	public Map<String, Object> getServerDiscoveryChartAggValues(String query) {
		Map<String, Object> charts = new HashMap<>();
		try {		
			charts = getResultAsMap(query);
		 } catch (Exception e) {
			e.printStackTrace();
		}
		return charts;
	}
	
	
	
}
