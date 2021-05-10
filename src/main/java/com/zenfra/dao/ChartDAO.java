package com.zenfra.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	
}
