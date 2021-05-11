package com.zenfra.dataframe.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.DashBoardDao;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.model.DashBoardCharts;
import com.zenfra.model.DashboardChartDetails;
import com.zenfra.model.DashboardDataResponseModel;
import com.zenfra.model.DashboardInputModel;
import com.zenfra.model.DashboardUserCustomization;
import com.zenfra.service.ChartService;
import com.zenfra.utils.CommonFunctions;

import lombok.val;

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

			//System.out.println(getDashboardLayoutChart);
			JSONObject tempChart=new JSONObject();
			if(layout!=null) {
				tempChart.put("layout", functions.convertStringToJsonArray(layout.getLayout()));
				tempChart.put("dataId", layout.getDataId());				
			}
			obj.put("chartLayout", tempChart);
			String getDashboardLayoutChartLayout=queries.dashboardQueries().getGetDashboardLayoutChartLayout()
					.replace(":user_id_value", userId).replace(":site_key_value", siteKey);
			
			System.out.println("chart::"+getDashboardLayoutChartLayout);
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
			
			System.out.println(queries.dashBoardChart().getGetByChartIdSiteKeyUserId());
			String query=queries.dashBoardChart().getGetByChartIdSiteKeyUserId()
					.replace(":user_id", userId)
					.replace(":site_key", siteKey)
					.replace(":chart_id", chartId);
			DashBoardCharts charts=(DashBoardCharts) dashDao.getEntityByColumn(query, DashBoardCharts.class);
			if(charts!=null) {
				charts.setActive(false);
				dashDao.updateEntity(DashBoardCharts.class, charts);
				dashDao.eveitEntity(charts);
				return true;
			}else {
				DashBoardCharts chart=new DashBoardCharts();
					chart.setDataId(functions.generateRandomId());
					chart.setUserId(userId);
					chart.setSiteKey(siteKey);
					chart.setChartId(chartId);
					chart.setActive(false);
					dashDao.saveEntity(DashBoardCharts.class, chart);
					dashDao.eveitEntity(chart);
			}
			
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
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
		
		ObjectMapper map=new ObjectMapper();
		JSONObject obj=new JSONObject();
		
		try {
			
			
			
			String query=queries.dashboardQueries().getGetDashboardChartDetails()
					.replace(":chart_id", dashboardInputModel.getChartId()).replace(":site_key",dashboardInputModel.getSiteKey());
			
			System.out.println(query);
			Object temp=dashDao.getObjectFromQuery(query);
			
			JSONArray arr=map.convertValue(temp, JSONArray.class);
			System.out.println(arr);
			if(arr!=null && !arr.isEmpty()) {				
				obj= map.convertValue(arr.get(0), JSONObject.class);
				obj= map.readValue(obj.get("chartDetails").toString(), JSONObject.class);
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

	public Boolean saveDashboardChartDetails(DashboardChartDetails dash) {
			try {
				
				
				dashDao.saveEntity(DashboardChartDetails.class, dash);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
	}

	public int saveOrUpdateDashboardChart(DashBoardCharts dash) {
		int responce=0;
		try {
			
			Map<String,Object> values=new HashMap<>();
				values.put(":data_id", dash.getDataId());
				values.put(":site_key",dash.getSiteKey());
				values.put(":favorite_view", dash.getFavoriteView());
				values.put(":analytics_type", dash.getAnalyticsType());
				values.put(":category", dash.getCategory());
				values.put(":user_id", dash.getUserId());
				values.put(":analytics_for", dash.getAnalyticsFor());
				values.put(":chart", dash.getChartList().toString());
				values.put(":chart_id", dash.getChartId());
				values.put(":is_active", dash.getActive());
				values.put(":created_by", dash.getCreatedBy());
				values.put(":created_time", dash.getCreatedTime());
				values.put(":updated_by", dash.getUpdatedBy());
				values.put(":updated_time", dash.getUpdatedTime());
				
			String query=queries.dashBoardChart().getSaveOrUpdateDashboardChart();
			
			for(String value:values.keySet()) {				
				query=query.replace(value, values.get(value).toString());
			}
			System.out.println(query);
			responce=dashDao.updateQuery(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return  responce;
	}

	public DashBoardCharts getDashChartsByUserIdSiteKey(String userId, String siteKey, String chartId) {
		DashBoardCharts dash=null;
		try {
			System.out.println(userId);
			System.out.println(siteKey);
			System.out.println(chartId);
			String query=queries.dashBoardChart().getGetSiteKeyUserIdChartId()
					.replace(":user_id", userId).replace(":site_key", siteKey)
					.replace(":chart_id", chartId);
		System.out.println(query);
			dash=(DashBoardCharts) dashDao.getEntityByColumn(query, DashBoardCharts.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dash;
	}

	public DashboardChartDetails getDashboardChartDetailsById(String data_id) {
		DashboardChartDetails dash=null;
		try {
			
			dash=(DashboardChartDetails) dashDao.findEntityById(DashboardChartDetails.class,data_id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return dash;
	}

	public boolean updateDashboardChartDetails(DashboardChartDetails exitObject) {

		try {
			dashDao.updateEntity(DashboardChartDetails.class, exitObject);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public DashboardChartDetails getDashboardChartDetailsBySiteKey(String siteKey, String chartId) {
		DashboardChartDetails dash=null;
		try {
			
			String query=queries.dashBoardChartDetails().getGetByChartIdSiteKey()
					.replace(":site_key", siteKey)
					.replace(":chart_id", chartId);
					
			dash=(DashboardChartDetails) dashDao.getEntityByColumn(query, DashboardChartDetails.class);
		} catch (Exception e) {
			
		}
		return dash;
	}

	public Object getProjectSummary(DashboardInputModel dashboardInputModel,String token) {
		Object obj=new Object();
		
			try {
				
				 Response  execute = Jsoup.connect("http://uat.zenfra.co:8080/zenfra-features/rest/dashboard/layout?siteKey=67483b74-b221-4057-be91-c06c4011fa66&tenantId=a0fe5fb8-f660-46fb-9c3d-eae1d3847238&userId=223247ff-9f05-4612-87b1-27e47d427003")
		                    .header("Content-Type", "application/json")
		                    .header("Accept", "application/json")
		                    //.header("Authorization", token)
		                    .followRedirects(true)
		                    .ignoreHttpErrors(true)
		                    .ignoreContentType(true)
		                    .userAgent("Mozilla/5.0 AppleWebKit/537.36 (KHTML," +
		                            " like Gecko) Chrome/45.0.2454.4 Safari/537.36")
		                    .method(org.jsoup.Connection.Method.GET)
		                    //.requestBody(dashboardInputModel.toString())
		                    .maxBodySize(1_000_000 * 30) // 30 mb ~
		                    .timeout(0) // infinite timeout
		                    .execute();
				/*Response  execute = Jsoup.connect("http://uat.zenfra.co:8080/ZenfraV2/rest/reports/dashboard/data")
	                    .header("Content-Type", "application/json")
	                    .header("Accept", "application/json")
	                    .header("Authorization", token)
	                    .followRedirects(true)
	                    .ignoreHttpErrors(true)
	                    .ignoreContentType(true)
	                    .userAgent("Mozilla/5.0 AppleWebKit/537.36 (KHTML," +
	                            " like Gecko) Chrome/45.0.2454.4 Safari/537.36")
	                    .method(org.jsoup.Connection.Method.POST)
	                    .requestBody(dashboardInputModel.toString())
	                    .maxBodySize(1_000_000 * 30) // 30 mb ~
	                    .timeout(0) // infinite timeout
	                    .execute();*/
				
				  String jsonString = execute.body();
				  System.out.println(jsonString);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return obj;
	}

	public JSONObject getChartFavMenuByAnalyticsType(String siteKey,
			String favouriteId) {
		JSONObject finalValue=new JSONObject();
		JSONArray jsonArray=new JSONArray();
		try {			
			String dashBoardQuery = "select fc.favourite_name as \"favouriteName\",dcd.chart_id  as \"chartId\" from dashboard_chart_details dcd\r\n" + 
					"left join favourite_view fc on fc.favourite_id = dcd.favourite_Id\r\n" + 
					"where dcd.site_Key = '"+siteKey+"'" + 
					"and dcd.favourite_Id = '"+favouriteId+"'";
				List<Map<String,Object>> map=dashDao.getListMapObjectById(dashBoardQuery);
		
				for(Map<String,Object> s:map) {					
					if (s.get("favouriteName")!=null && s.get("chartId")!=null && s.get("chartId").toString().contains("~")) {
						JSONObject chartObject = new JSONObject();
						String[] chartIdArray = s.get("chartId").toString().split("~");
						if (chartIdArray[2].equalsIgnoreCase("groupedBarChart")) {
							chartObject.put("label", "PROJECT-SUMMARY-" + s.get("favouriteName") + "-" + "PROJECT SUMMARY");
						} else {
							chartObject.put("label", "PROJECT-SUMMARY-" + s.get("favouriteName") + "-" + "OVERALL SUMMARY");
						}
						chartObject.put("chartId", s.get("chartId").toString());
						chartObject.put("type", chartIdArray[2]);
						 	
							jsonArray.add(chartObject);
						}
				}
				
				finalValue.put("chartDetails", jsonArray);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return finalValue;
	}
	 
	
	
	/*******************project summary********************/
	
}
