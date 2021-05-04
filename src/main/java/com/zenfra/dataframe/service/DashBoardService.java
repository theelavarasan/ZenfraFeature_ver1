package com.zenfra.dataframe.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.DashBoardDao;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.model.DashBoardCharts;
import com.zenfra.model.DashboardDataResponseModel;
import com.zenfra.model.DashboardInputModel;
import com.zenfra.model.DashboardUserCustomization;
import com.zenfra.service.ChartService;
import com.zenfra.utils.CommonFunctions;

@Service
public class DashBoardService {

	@Autowired
	DashBoardDao dashDao;

	@Autowired
	CommonFunctions functions;

	@Autowired
	CommonQueriesData queries;
	
	@Autowired
	ChartService chartService;

	public JSONObject getDasboardLayout(String userId, String siteKey) {
		JSONObject obj = new JSONObject();
		try {
			
		String getDashboardLayoutChart = queries.dashboardQueries().getGetDashboardLayoutChart()
					.replace(":user_id_value", userId).replace(":site_key_value", siteKey);
			DashboardUserCustomization layout = (DashboardUserCustomization) dashDao
					.getEntityByColumn(getDashboardLayoutChart, DashboardUserCustomization.class);

			JSONObject tempChart=new JSONObject();
			if(layout!=null) {
				tempChart.put("layout", functions.convertStringToJsonArray(layout.getLayout()));
				tempChart.put("dataId", layout.getData_id());				
			}
			obj.put("chartLayout", tempChart);
			String getDashboardLayoutChartLayout=queries.dashboardQueries().getGetDashboardLayoutChartLayout()
					.replace(":user_id_value", userId).replace(":site_key_value", siteKey);
			
			System.out.println(getDashboardLayoutChartLayout);
			List<Map<String,Object>> chartDetails=dashDao.getListMapObjectById(getDashboardLayoutChartLayout);
				JSONArray chartObj=new JSONArray();
				ObjectMapper mapper = new ObjectMapper();
				for(Map<String,Object> list:chartDetails) {	
					JSONObject tempBreak = mapper.convertValue(list.get("filterProperty"), JSONObject.class);
					JSONObject obtemp=mapper.convertValue(list, JSONObject.class);
					if(tempBreak!=null && tempBreak.containsKey("value") && tempBreak.get("value")!=null) {
						JSONObject tempFilter=mapper.readValue(tempBreak.get("value").toString(), JSONObject.class);
						obtemp.put("filterProperty", tempFilter);						
					}
					chartObj.add(obtemp);		
				}
			obj.put("chartDetails", chartObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

	public Boolean deleteDashboardChart(String chartId, String siteKey, String userId) {
		try {
			//need to get first chart entry
			ChartModel_v2 chart=chartService.getChartByChartId(chartId);
			
			//need to check create by equals userId 
				if(chart.getUserId().equals(userId)) {
					//create by equals userId need to setactive false
					chart.setActive(false);
					chartService.saveChart(chart);
					chartService.eveitEntity(chart);
				}else if(chart.getUserAccessList().contains(userId)){
					//user accesslist have request userId 
					//need to delete userAccessList all values and add userId as create by also set active flag is false
					chart.setUserAccessList(null);
					chart.setUserId(userId);
					chart.setChartId(functions.generateRandomId());
					chart.setActive(false);
					chartService.saveChart(chart);
					chartService.eveitEntity(chart);
				}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Boolean saveDashboardLayout(DashboardUserCustomization dash) {
		try {
			
			dashDao.saveEntity(DashboardUserCustomization.class, dash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	public Boolean updateDashboardLayout(DashboardUserCustomization dash) {
		try {
			
			dashDao.updateEntity(DashboardUserCustomization.class, dash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}

	public Boolean saveDashboardChart(DashBoardCharts dash) {
		try {
			
			dashDao.saveEntity(DashBoardCharts.class, dash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	public Boolean updateDashboardChart(DashBoardCharts dash) {
		try {
			
			dashDao.updateEntity(DashBoardCharts.class, dash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}

	public JSONObject getDashboardChartDetails(DashboardInputModel dashboardInputModel) {
		
		JSONObject obj=new JSONObject();
		try {
			String query=queries.dashboardQueries().getGetDashboardChartDetails()
					.replace(":chart_id", dashboardInputModel.getChartId()).replace(":site_key",dashboardInputModel.getSiteKey());
			
			Object temp=dashDao.getObjectFromQuery(query);
			
			if(temp!=null) {
				obj.put("chart_details", temp);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

	public JSONObject getChatForFavMenu(String favoriteViewId, String userId, String siteKey) {
		JSONObject obj=new JSONObject(); 
		try {
			
			String getChatForFavMenu=queries.dashboardQueries().getGetChatForFavMenu()
					.replace(":favourite_id", favoriteViewId)
					.replace(":user_id_value", userId).replace(":site_key_value", siteKey);

			System.out.println(getChatForFavMenu);
			List<Map<String,Object>> chartDetails=dashDao.getListMapObjectById(getChatForFavMenu);
				JSONArray chartObj=new JSONArray();
				ObjectMapper mapper = new ObjectMapper();
				for(Map<String,Object> list:chartDetails) {	
					JSONObject tempBreak = mapper.convertValue(list.get("filterProperty"), JSONObject.class);
					JSONObject obtemp=mapper.convertValue(list, JSONObject.class);
					if(tempBreak!=null && tempBreak.containsKey("value") && tempBreak.get("value")!=null) {
						JSONObject tempFilter=mapper.readValue(tempBreak.get("value").toString(), JSONObject.class);
						obtemp.put("filterProperty", tempFilter);						
					}
					chartObj.add(obtemp);		
				}
			obj.put("chartDetails", chartObj);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return obj;
	}

	 
	 public DashboardUserCustomization getDashboardUserCustomizationById(String dataId) {
		 DashboardUserCustomization dash=new DashboardUserCustomization();
		 try {
			
			 dash=(DashboardUserCustomization) dashDao.findEntityById(DashboardUserCustomization.class, dataId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		 
		 return dash;
	 }
	 
	 public boolean evitObj(Object obj) {
		 try {
			
			 dashDao.eveitEntity(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		 
		 return true;
	 }
	 

	 private DashboardDataResponseModel getProjectSummaryDetails() throws org.json.simple.parser.ParseException, URISyntaxException, ClientProtocolException, IOException {

	        DashboardDataResponseModel dashboardDataResponseModel = new DashboardDataResponseModel();

	        String reportType = "";
	        String groupBy = "";
	        String filterBy = "";
	        String filterValue = "";
	        String fromDate = "";
	        String toDate = "";

	        JSONArray filterArray = new JSONArray();
	        if (!filterArray.isEmpty()) {
	            for (int i = 0; i < filterArray.size(); i++) {
	                LinkedHashMap<?, ?> jsonObject = (LinkedHashMap<?, ?>) filterArray.get(i);
	                String selectionValue = "";
	                if (jsonObject.containsKey("selection")) {
	                    selectionValue = jsonObject.get("selection").toString();
	                }

	                if (jsonObject.get("name").toString().equalsIgnoreCase("report_type")) {
	                    reportType = selectionValue;
	                } else if (jsonObject.get("name").toString().equalsIgnoreCase("groupBy")) {
	                    groupBy = selectionValue;
	                } else if (jsonObject.get("name").toString().equalsIgnoreCase("filterBy")) {
	                    filterBy = selectionValue;
	                } else if (jsonObject.get("name").toString().equalsIgnoreCase("filterValue")) {
	                    filterValue = selectionValue.replace("[", "").replace("]", "").replace(", ", ",");
	                    String[] filterValueArray = filterValue.trim().split(",");
	                    if (filterValueArray.length == 2) {
	                        if (filterValueArray[0].trim().length() == 10 && filterValueArray[1].trim().length() == 10) {
	                            if (filterValueArray[0].trim().contains("-") && filterValueArray[1].trim().contains("-")) {
	                                fromDate = filterValueArray[0].trim();
	                                toDate = filterValueArray[1].trim();
	                            }
	                        }
	                    }
	                }

	            }
	        }

	        if (!filterBy.equalsIgnoreCase("")) {
	            String filterByValue = filterBy.substring(filterBy.indexOf("_") + 1, filterBy.length());
	            filterValue = filterValue.replace(filterByValue + "_", "");
	        }
	   
	    
	        return dashboardDataResponseModel;
	    }
	 
	
}
