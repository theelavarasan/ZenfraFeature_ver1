package com.zenfra.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.model.ChartModel_v2;
import com.zenfra.queries.ChartQueries;
import com.zenfra.utils.CommonFunctions;

@Component
public class ChartDAO extends CommonEntityManager{

	@Autowired
	CommonFunctions functions;
	
	@Autowired
	ChartQueries chartQuery;
	
	
	public int SaveChart(Map<String,Object> params) {
		int responce=0;
		try {			
			responce=updateQuery(chartQuery.getSave());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responce;
	}




	public List<Object> getChartByUserId(String userId) {
		 List<Object> chart=new ArrayList<Object>();
		try {
			
			String query=chartQuery.getGetChartsByUserId().replace(":user_id", userId);
			chart=getEntityListByColumn(query, ChartModel_v2.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return chart;
	}
	
}
