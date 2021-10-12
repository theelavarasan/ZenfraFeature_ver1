package com.zenfra.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dao.ChartDAO;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.utils.CommonFunctions;

@Service
public class ChartService {

	@Autowired
	CommonFunctions functions;
	
	
	@Autowired
	ChartDAO chartDao;
	
	private JSONParser jsonParser = new JSONParser();
	
	public boolean saveChart(ChartModel_v2 chart) {
		boolean response=false;
		try {
			
			/*Map<String, Object> params = new HashMap<String, Object>();
				params.put(":chart_id", chart.getChartId());
				params.put(":chart_configuration",chart.getChartConfiguration());
				params.put(":is_dashboard", chart.isDashboard());
				params.put(":site_key", chart.getSiteKey());
				params.put(":report_name", chart.getReportName());
				params.put(":chart_name", chart.getChartName());
				params.put(":filter_property", chart.getFilterProperty());			
				params.put(":chart_type", chart.getChartType());
				params.put(":created_time", chart.getCreatedTime());
				params.put(":update_time", chart.getUpdateTime());	
				params.put(":is_active", chart.isActive());	
				params.put(":user_id", chart.getUserId());	
			
				responce=chartDao.SaveChart(params);	*/
			
			if(chart.getChartId()!=null) {
				response=chartDao.updateEntity(ChartModel_v2.class, chart);
			}else {
				response=chartDao.saveEntity(ChartModel_v2.class, chart);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();			
		}
		return response;
	}
	
	
	public ChartModel_v2 getChartByChartId(String chartId) {
		ChartModel_v2 chart=new ChartModel_v2();
		try {
			
			System.out.println(chartDao.findEntityById(ChartModel_v2.class, chartId));
			chart=(ChartModel_v2) chartDao.findEntityById(ChartModel_v2.class, chartId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chart;
	}


	public boolean deleteChartByObject(ChartModel_v2 chart) {
		boolean response=false;
		try {
			
			response=chartDao.updateEntity(ChartModel_v2.class,chart);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return response;
	}

	
	public List<ChartModel_v2> getChartByUserId(String userId) {
		
		List<ChartModel_v2> object=new ArrayList<ChartModel_v2>();
		try {
			
			object=(List<ChartModel_v2>) (Object) chartDao.getChartByUserId(userId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return object;
	}


	public JSONArray getMigarationReport(String siteKey, String userId, String reportName) {
		
		JSONArray output=new JSONArray();
		List<Map<String, Object>> object=new ArrayList<Map<String,Object>>();
		try {
			
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			object=chartDao.getMigarationReport(siteKey,userId,reportName);
			for(Map<String,Object> s:object) {
				
				output.add(functions.convertGetMigarationReport(s));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return output;
	}
	
	
	public Boolean eveitEntity(ChartModel_v2 chart) {
		try {
			
			chartDao.eveitEntity(chart);
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return true;
	}


	public List<Object> getChartByCategoryId(String categoryId) {
		try {
			
			return chartDao.getChartByCategoryId(categoryId);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	

	public void getChartDatas(String siteKey, String sourceType) {
		try {
			List<Map<String, Object>> chatDatas = chartDao.getChartsBySiteKeyAndLogType(siteKey, sourceType);
			//Set<String> chartParameters = new HashSet<String>();
			Map<String, Set<String>> chartInputs = new HashMap<String, Set<String>>();
			if(chatDatas != null && !chatDatas.isEmpty()) {
		    	for(Map<String, Object> chart : chatDatas) {
		    		
		    		JSONObject chartDetails = (JSONObject) jsonParser.parse(chart.get("chart_configuration").toString());
		    		JSONObject filterDetails = (JSONObject) jsonParser.parse(chart.get("filter_property").toString());
		    	
		    		if(!filterDetails.isEmpty()) {
		    			String componentType = (String) filterDetails.get("category");
		    			
		    			
		    			Set<String> chartParameters = new HashSet<>();
		    			if(!chartDetails.isEmpty()) {		    			
			    			JSONArray xAxis = (JSONArray) chartDetails.get("xaxis");
			    			JSONArray yAxis = (JSONArray) chartDetails.get("yaxis");
			    			
			    			for(int i=0; i<xAxis.size(); i++) {
			    				JSONObject xAxisData = (JSONObject) xAxis.get(i);
			    				chartParameters.add((String) xAxisData.get("label"));
			    			}
			    			
			    			for(int i=0; i<yAxis.size(); i++) {
			    				JSONObject yAxisData = (JSONObject) yAxis.get(i);
			    				chartParameters.add((String) yAxisData.get("label"));
			    			}
			    		}
		    			
		    			if(chartInputs.containsKey(componentType)) {
		    				Set<String> values = chartInputs.get(componentType);
		    				values.addAll(chartParameters);
		    				chartInputs.put(componentType, values);
		    			} else {
		    				Set<String> values = new HashSet<>();
		    				values.addAll(chartParameters);
		    				chartInputs.put(componentType, values);
		    			}
		    		}
		    				    	
		    	}
		    }
			System.out.println("------------- >> " + chartInputs);
			
			if(!chartInputs.isEmpty()) {
				String componentType = chartInputs.keySet().stream().findFirst().get();
				if(componentType.equalsIgnoreCase("Server")) { //local discovery table
					Set<String> chartParams = chartInputs.values().stream().findFirst().get();
					String query = formAggrigationQuery(siteKey, componentType, sourceType, chartParams);
					List<Map<String, Object>> serverChartAggValues = chartDao.getServerDiscoveryChartAggValues(query);
					System.out.println("----------serverChartAggValues----------" + serverChartAggValues);
					JSONArray result = new JSONArray();
					for(Map<String, Object> map : serverChartAggValues) {
						JSONObject json = new JSONObject();
					    json.putAll( map );
					    result.add(json);
					}
					
					System.out.println("----------result----------" + result);
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


	private String formAggrigationQuery(String siteKey, String componentType, String sourceType, Set<String> chartParams) {
		String query = "";
		if(componentType.equalsIgnoreCase("Server")) {
			String selectQuery = String.join(",", chartParams
		            .stream()
		            .map(param -> ("string_agg(a." + param.replaceAll("\\s+", "_") + ", ',') as \""+param.replaceAll("\\s+", "_")+"\""))
		            .collect(Collectors.toList()));
			
			String whereQuery = String.join(",", chartParams
		            .stream()
		            .map(param -> ("json_array_elements(data_temp::json) ->> '" + param + "' as "+param.replaceAll("\\s+", "_")+""))
		            .collect(Collectors.toList()));
			
			query = "select a.source_id, "+selectQuery+" from (select source_id,  "+whereQuery+" from local_discovery	where site_key='"+siteKey+"' and lower(source_type)='"+sourceType.toLowerCase()+"') a group by a.source_id";
			System.out.println("----------Query----------" + query);
		}
		return query;
	}
	
	
}
