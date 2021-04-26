package com.zenfra.dao;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
	
}
