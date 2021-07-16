package com.zenfra.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.queries.ReportQueries;
import com.zenfra.utils.CommonFunctions;

@Component
public class ReportDao {

	@Autowired
	NamedParameterJdbcTemplate namedJdbc;

	@Autowired
	ReportQueries reportQueries;
	
	@Autowired
	CommonFunctions commonFunctions;
	
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

	public JSONArray getChartLayout(String userId, String siteKey, String reportName) {
		JSONArray result = new JSONArray();
		try {
			Map<String,Object> params=new HashMap<String, Object>();
			params.put("user_id", userId);
			params.put("site_key", siteKey);
			params.put("report_name", reportName.toLowerCase());			
			List<String> resultObj = namedJdbc.queryForList(reportQueries.getChartLayout(), params, String.class);
			if(resultObj != null && resultObj.size() > 0) {
				
			}
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
				result.put("groupedColumns", commonFunctions.convertObjectToJsonArray(rs.get(0).get("grouped_columns")));
				result.put("columnOrder", commonFunctions.convertObjectToJsonArray(rs.get(0).get("columns_visible")));
				result.put("chartLayout", commonFunctions.formatJsonArrayr(rs.get(0).get("chart_layout")));
				ObjectMapper mapper = new ObjectMapper();	
				JSONArray health_check = mapper.readValue(rs.get(0).get("health_check").toString(), JSONArray.class);
				result.put("health_check", health_check);
				
				
				
			} else {
				JSONArray empty = new JSONArray();
				JSONObject jSONObject = new JSONObject();
				result.put("groupedColumns", empty);
				result.put("columnOrder", empty);
				result.put("chartLayout", empty);
				result.put("health_check", jSONObject);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return result;
	}
}
