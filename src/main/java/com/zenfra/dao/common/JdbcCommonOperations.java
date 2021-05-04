package com.zenfra.dao.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component
public abstract class JdbcCommonOperations {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	NamedParameterJdbcTemplate namedJdbc;

	public Integer updateQuery(SqlParameterSource parameter, String query) {

		int responce = 0;
		try {
			KeyHolder holder = new GeneratedKeyHolder();
			responce = namedJdbc.update(query, parameter);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return responce;

	}

	public Integer updateQuery(Map<String, Object> parameter, String query) {

		int responce = 0;
		try {
			responce = namedJdbc.update(query, parameter);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return responce;

	}

	public Integer getCount(String query) {

		try {

			return jdbc.queryForObject(query, Integer.class);
		} catch (Exception e) {
			e.printStackTrace();

		}
		return 0;
	}

	public int updateQuery(String query) {
		int responce = 0;
		try {

			responce = jdbc.update(query);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return responce;
	}

	public int queryWithParams(Map<String, Object> params, String query) {

		int responce = 0;
		try {

			return namedJdbc.update(query, params);
		} catch (Exception e) {
			e.printStackTrace();

		}

		return responce;
	}

	public List<Map<String, Object>> getListMapObjectById(String query) {
		List<Map<String, Object>> obj = new ArrayList<>();
		try {

			obj = jdbc.queryForList(query);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	public List<Map<String, Object>> getObjectFromQuery(String query) {
		List<Map<String, Object>> obj = new ArrayList<>();
		try {

			obj = jdbc.queryForList(query);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}
}
