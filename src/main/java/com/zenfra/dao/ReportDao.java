package com.zenfra.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.queries.ReportQueries;

@Component
public class ReportDao {

	@Autowired
	NamedParameterJdbcTemplate namedJdbc;

	@Autowired
	ReportQueries reportQueries;
	
	public JSONArray getReportHeader(String reportName, String deviceType, String reportBy) {
		JSONArray reportHeaders = new JSONArray();
		try {
			Map<String,Object> params=new HashMap<String, Object>();
			params.put("report_name", reportName.toLowerCase());
			params.put("device_type", deviceType.toLowerCase());
			params.put("report_by", reportBy.toLowerCase());			
			List<Map<String, Object>> result = namedJdbc.queryForList(reportQueries.getHeader(), params);	
			reportHeaders = parseResultSetForHeaderInfo(result); 	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reportHeaders;
	}
	
	private JSONArray parseResultSetForHeaderInfo(List<Map<String, Object>> resultList) {	
		resultList = resultList.stream()
		 .distinct()
		 .collect(Collectors.toList());
		JSONArray reportHeaders = new JSONArray();
		try {
			
			int rowCount = 1;
			for (Map<String, Object> rowData : resultList) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.putAll(rowData);
				if (rowCount == 1) {
					jsonObj.put("lockPinned", true);
					jsonObj.put("lockPosition", true);
					jsonObj.put("pinned", "left");
				} else {
					jsonObj.put("lockPinned", false);
					jsonObj.put("lockPosition", false);
					jsonObj.put("pinned", "");
				}
				reportHeaders.add(jsonObj);
				rowCount++;
			}
			
		} catch (Exception e) {			
			e.printStackTrace();
		}
		return reportHeaders;
	}

	public List<String> getReportNumericalHeaders(String reportName, String deviceType, String reportBy,
			String siteKey) {
		List<String> result = new ArrayList<String>();
		try {
			Map<String,Object> params=new HashMap<String, Object>();
			params.put("report_name", reportName.toLowerCase());
			params.put("device_type", deviceType.toLowerCase());
			params.put("report_by", reportBy.toLowerCase());	
			params.put("report_by", reportBy.toLowerCase());	
			params.put("data_type", "integer");	
			result = namedJdbc.queryForList(reportQueries.getNumbericalHeader(), params, String.class);	
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<String> getReportHeaderForFilter(String reportName, String deviceType, String reportBy) {
		try {
				Map<String,Object> params=new HashMap<String, Object>();
				params.put("report_name", reportName.toLowerCase());
				params.put("device_type", deviceType.toLowerCase());
				params.put("report_by", reportBy.toLowerCase());			
				List<String> result = namedJdbc.queryForList(reportQueries.getHeaderFilter(), params, String.class);	
				return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<String> getChartLayout(String userId, String siteKey, String reportName) {
		List<String> result = new ArrayList<>();
		try {
			Map<String,Object> params=new HashMap<String, Object>();
			params.put("user_id", userId);
			params.put("site_key", siteKey);
			params.put("report_name", reportName.toLowerCase());			
			result = namedJdbc.queryForList(reportQueries.getChartLayout(), params, String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

	public JSONObject getReportUserCustomData(String userId, String siteKey, String reportName) {
		JSONObject result = new JSONObject();
		try {
			Map<String,Object> params=new HashMap<String, Object>();
			params.put("user_id", userId);
			params.put("site_key", siteKey);
			params.put("report_name", reportName.toLowerCase());			
			List<Map<String, Object>> rs = namedJdbc.queryForList(reportQueries.getReportUserCustomData(), params);			
			if(rs != null && rs.size() > 0) {
				result.put("groupedColumns", rs.get(0).get("grouped_columns"));
				result.put("columnOrder", rs.get(0).get("columns_visible"));
				result.put("chartLayout", rs.get(0).get("chart_layout"));
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return result;
	}
}
