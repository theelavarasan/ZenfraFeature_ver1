package com.zenfra.dao;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DashboardChartDetailsDao {

	@Autowired
	JdbcTemplate jdbc;
	
	@Autowired
	NamedParameterJdbcTemplate namedJdbc;
	
	
public Integer updateQuery(Map<String,Object> parameter,String query) {
		
		int responce=0;
		try {			
			responce=namedJdbc.update(query,parameter);
		} catch (Exception e) {
			e.printStackTrace();			
		}
		
		return responce;
		
	}
	
	
}
