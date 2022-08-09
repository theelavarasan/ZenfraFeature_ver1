package com.zenfra.dao;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.queries.ReportQueries;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class ReportDao {

	@Autowired
	NamedParameterJdbcTemplate namedJdbc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ReportQueries reportQueries;

	@Autowired
	CommonFunctions commonFunctions;

	public JSONArray getReportHeader(String reportName, String deviceType, String reportBy, String siteKey, String userId) {
		JSONArray reportHeaders = new JSONArray();
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("report_name", reportName.toLowerCase());
			params.put("device_type", deviceType.toLowerCase());
			params.put("report_by", reportBy.toLowerCase());
			System.out.println("------params--------- " + params);
			System.out.println("------columns query--------- " + reportQueries.getHeader());
			List<Map<String, Object>> result; 
			System.out.println("!!!!! Header reportBy: " + reportBy);
			if(reportBy.equalsIgnoreCase("securityAddSource")) {
				result = namedJdbc.queryForList(reportQueries.getSecurityAddSourceHeader(), params);
			} else {
				System.out.println("!!!!! not Tanium Header: ");
				result = namedJdbc.queryForList(reportQueries.getHeader(), params);
			}
			
			System.out.println("!!!!! result: " + result);
			reportHeaders = parseResultSetForHeaderInfo(result);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return reportHeaders;
	} 
	
	public JSONArray getPrivillegeReportHeader(String reportName, String deviceType, String reportBy, String siteKey, String userId) {
		JSONArray reportHeaders = new JSONArray();
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("site_key", siteKey);
			params.put("user_id", userId);
			System.out.println("------params--------- " + params);
			System.out.println("------tanium header query--------- " + reportQueries.getTaniumHeader());
			
			List<Map<String, Object>> result; 
			String headerQuery = "";
			if(reportBy.equalsIgnoreCase("Privileged Access")) {
				headerQuery = reportQueries.getTaniumHeader();
			} else if(reportBy.equalsIgnoreCase("User")) {
				headerQuery = reportQueries.getUserSummaryHeader();
			} else if(reportBy.equalsIgnoreCase("Server")) {
				headerQuery = reportQueries.getServerSummaryHeader();
			} else if(reportBy.equalsIgnoreCase("Sudoers User")) {
				headerQuery = reportQueries.getSudoersSummaryHeader();
			}
			
			headerQuery = headerQuery.replace(":site_key", siteKey).replace(":user_id", userId);
			System.out.println("!!!!! headerQuery: " + headerQuery);
			result = jdbc.queryForList(headerQuery);
			
			System.out.println("!!!!! result: " + result);
			reportHeaders = parseResultSetForHeaderInfo(result);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return reportHeaders;
	}
	
	public JSONArray getUserReportHeader(String reportName, String deviceType, String reportBy, String siteKey, String userId) {
		JSONArray reportHeaders = new JSONArray();
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("site_key", siteKey);
			params.put("user_id", userId);
			System.out.println("------params--------- " + params);
			System.out.println("------tanium header query--------- " + reportQueries.getTaniumHeader());
			
			List<Map<String, Object>> result; 
			String headerQuery = reportQueries.getTaniumHeader();
			headerQuery = headerQuery.replace(":site_key", siteKey).replace(":user_id", userId);
			System.out.println("!!!!! headerQuery: " + headerQuery);
			result = jdbc.queryForList(headerQuery);
			
			System.out.println("!!!!! result: " + result);
			reportHeaders = parseResultSetForHeaderInfo(result);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return reportHeaders;
	}
	
	public JSONObject getReportGroup(String reportName, String deviceType, String reportBy, String siteKey, String userId) {
		JSONObject reportGroup = new JSONObject();
		try {
			
			System.out.println("!!!!! reportName: " + reportName);
			System.out.println("!!!!! deviceType: " + deviceType);
			System.out.println("!!!!! reportBy: " + reportBy);
			System.out.println("!!!!! siteKey: " + siteKey);
			System.out.println("!!!!! userId: " + userId);
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("report_name", reportName.toLowerCase());
			params.put("device_type", deviceType.toLowerCase());
			params.put("report_by", reportBy.toLowerCase());
			
			List<Map<String, Object>> result; 
			
			params = new HashMap<String, Object>();
			params.put("site_key", siteKey);
			params.put("user_id", userId);
			String query = reportQueries.getTaniumGroup();
			query = query.replace(":site_key", siteKey).replace(":user_id", userId).replace(":device_type", deviceType).replace(":report_name", reportName).replace(":report_by", reportBy);
			
			System.out.println("!!!!! tanium group query: " + query);
			if(reportBy.equalsIgnoreCase("User")) {
				query = reportQueries.getTaniumUserSummaryGroup();
				query = query.replace(":site_key", siteKey).replace(":user_id", userId).replace(":device_type", deviceType).replace(":report_name", reportName).replace(":report_by", reportBy);
				System.out.println("!!!!! tanium users summary group query: " + query);
			} else if(reportBy.equalsIgnoreCase("Server")) {
				query = reportQueries.getTaniumServerSummaryGroup();
				query = query.replace(":site_key", siteKey).replace(":user_id", userId).replace(":device_type", deviceType).replace(":report_name", reportName).replace(":report_by", reportBy);
				System.out.println("!!!!! tanium users summary group query: " + query);
			} else if(reportBy.equalsIgnoreCase("Sudoers User")) {
				query = reportQueries.getTaniumSudoersSummaryGroup();
				query = query.replace(":site_key", siteKey).replace(":user_id", userId).replace(":device_type", deviceType).replace(":report_name", reportName).replace(":report_by", reportBy);
				System.out.println("!!!!! tanium sudoers summary group query: " + query);
			}
			result = jdbc.queryForList(query);
			
			
			System.out.println("!!!!! result: " + result);
			reportGroup = parseResultSetForHeaderGroup(result);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return reportGroup;
	}
	

	private JSONArray parseResultSetForHeaderInfo(List<Map<String, Object>> resultList) {
		resultList = resultList.stream().distinct().collect(Collectors.toList());
		JSONArray reportHeaders = new JSONArray();
		try {

			int rowCount = 1;
			for (Map<String, Object> rowData : resultList) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.putAll(rowData);
				if ((boolean) rowData.get("isPinned")) {
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return reportHeaders;
	}
	
	private JSONObject parseResultSetForHeaderGroup(List<Map<String, Object>> resultList) {
		resultList = resultList.stream().distinct().collect(Collectors.toList());
		JSONObject jsonObj = new JSONObject();
		try {
			
			for (Map<String, Object> rowData : resultList) {
				jsonObj.put(rowData.get("category"), rowData.get("grouped_columns"));
				
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return jsonObj;
	}

	public List<String> getReportNumericalHeaders(String reportName, String deviceType, String reportBy,
			String siteKey) {
		List<String> result = new ArrayList<String>();
		try {
			if (deviceType.toLowerCase().contains("vmware")) {
				deviceType = "vmware";
			}

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("report_name", reportName.toLowerCase());
			params.put("device_type", deviceType.toLowerCase());
			params.put("report_by", reportBy.toLowerCase());
			params.put("report_by", reportBy.toLowerCase());
			params.put("data_type", "integer");
			result = namedJdbc.queryForList(reportQueries.getNumbericalHeader(), params, String.class);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return result;
	}

	public List<String> getReportHeaderForFilter(String reportName, String deviceType, String reportBy) {
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("report_name", reportName.toLowerCase());
			params.put("device_type", deviceType.toLowerCase());
			params.put("report_by", reportBy.toLowerCase());
			List<String> result = namedJdbc.queryForList(reportQueries.getHeaderFilter(), params, String.class);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return null;
	}

	public JSONArray getChartLayout(String userId, String siteKey, String reportName) {
		JSONArray result = new JSONArray();
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("user_id", userId);
			params.put("site_key", siteKey);
			params.put("report_name", reportName.toLowerCase());
			List<String> resultObj = namedJdbc.queryForList(reportQueries.getChartLayout(), params, String.class);
			if (resultObj != null && resultObj.size() > 0) {

			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return result;
	}

	public JSONObject getReportUserCustomData(String userId, String siteKey, String reportName) {
		JSONObject result = new JSONObject();
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("user_id", userId.trim());
			params.put("site_key", siteKey.trim());
			params.put("report_name", reportName.trim().toLowerCase());

			List<Map<String, Object>> rs = namedJdbc.queryForList(reportQueries.getReportUserCustomData(), params);

			if (rs != null && rs.size() > 0) {
				result.put("groupedColumns",
						commonFunctions.convertObjectToJsonArray(rs.get(0).get("grouped_columns")));
				result.put("columnOrder", commonFunctions.convertObjectToJsonArray(rs.get(0).get("columns_visible")));
				result.put("chartLayout", commonFunctions.formatJsonArrayr(rs.get(0).get("chart_layout")));
				ObjectMapper mapper = new ObjectMapper();
				JSONObject health_check = mapper.readValue(rs.get(0).get("health_check").toString(), JSONObject.class);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		// column reorder for compatability report
		if (reportName != null && reportName.contains("Compatibility")) {
			String deviceType = "project";
			if (reportName.toLowerCase().contains("aix")) {
				deviceType = "aix";
			} else if (reportName.toLowerCase().contains("hpux")) {
				deviceType = "hpux";
			} else if (reportName.toLowerCase().contains("linux")) {
				deviceType = "linux";
			} else if (reportName.toLowerCase().contains("solaris")) {
				deviceType = "solaris";
			} else if (reportName.toLowerCase().contains("vmware") && !reportName.toLowerCase().contains("host")) {
				deviceType = "vmware";
			} else if (reportName.toLowerCase().contains("vmware") && reportName.toLowerCase().contains("host")) {
				deviceType = "vmware-host";
			} else if (reportName.toLowerCase().contains("windows") && reportName.toLowerCase().contains("windows")) {
				deviceType = "windows";
			}

			List<String> compatabilityOrder = getReportHeaderForCompatibility("Compatibility", deviceType);

			if (!compatabilityOrder.isEmpty()) {
				List<String> existingVisibleColumns = (List<String>) result.get("columnOrder");
				if (existingVisibleColumns != null && !existingVisibleColumns.isEmpty()) {
					result.put("columnOrder", existingVisibleColumns);
				} else {
					result.put("columnOrder", compatabilityOrder);
				}
			}

		}
		return result;
	}

	private List<String> getReportHeaderForCompatibility(String reportName, String deviceType) {
		List<String> result = new ArrayList<String>();
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("report_name", reportName.toLowerCase());
			params.put("device_type", deviceType.toLowerCase());
			result = namedJdbc.queryForList(reportQueries.getHeaderForCompatibility(), params, String.class);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return result;
	}

	public JSONObject getReportUserCustomDataBySiteKey(String siteKey, String userId) {
		JSONObject result = new JSONObject();
		try {
			try {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("site_key", siteKey);
				params.put("user_id", userId);
				List<Map<String, Object>> rs = namedJdbc.queryForList(reportQueries.getReportUserCustomDataBySiteKey(),
						params);
				if (rs != null && rs.size() > 0) {
					result.put("groupedColumns",
							commonFunctions.convertObjectToJsonArray(rs.get(0).get("grouped_columns")));
					result.put("columnOrder",
							commonFunctions.convertObjectToJsonArray(rs.get(0).get("columns_visible")));
					result.put("chartLayout", commonFunctions.formatJsonArrayr(rs.get(0).get("chart_layout")));
					ObjectMapper mapper = new ObjectMapper();
					JSONObject health_check = mapper.readValue(rs.get(0).get("health_check").toString(),
							JSONObject.class);
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
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return result;
	}
	
	public void executeNativeQuery(String query) {
		try {
			jdbc.execute(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public List<Map<String, Object>> getReportCombinationByLogType(String sourceType) {
		List<Map<String, Object>> reportCombination = new ArrayList<Map<String, Object>>();
		try {
			reportCombination = jdbc.queryForList("SELECT report_type as \"reportList\", report_by as \"reportBy\", category, device FROM public.device_discovery_report_config where lower(name)='"+sourceType.toLowerCase()+"' and enabled ='1'");
	
			 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		 
		return reportCombination;
	}
	
	public List<Map<String, Object>> getListOfMapByQuery(String query) {
		List<Map<String, Object>> reportCombination = new ArrayList<>();
		try {
			reportCombination = jdbc.queryForList(query);
	
			 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	 
		return reportCombination;
	}
}
