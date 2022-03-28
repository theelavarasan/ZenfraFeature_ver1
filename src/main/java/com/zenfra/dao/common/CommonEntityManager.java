package com.zenfra.dao.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

import com.zenfra.model.ChartModel_v2;
import com.zenfra.model.ZenfraJSONObject;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public abstract class CommonEntityManager extends JdbcCommonOperations {
	
	final ObjectMapper map = new ObjectMapper();

	@PersistenceContext
	private EntityManager entityManager;
	

	DBUtils dbUtils;

	public Object findEntityById(Class c, String id) {
		Object obj = new Object();
		try {
			obj = entityManager.find(c, id);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	public Boolean saveEntityNew(ChartModel_v2 chart) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		String insertQuery = "";
		try {
			insertQuery = "INSERT INTO chart("
					+ "	chart_id, chart_configuration, is_dashboard, site_key, report_name, chart_name, filter_property, "
					+ "chart_type, created_time, update_time, is_active, user_id, user_access_list, site_access_list, chart_desc, "
					+ "is_visible, is_default, analytics_for, analytics_type, category_list, chart_details, report_label)"
					+ "	VALUES ('" + chart.getChartId() + "', '" + chart.getChartConfiguration() + "', '" + chart.isDashboard() + "', '"
					+ chart.getSiteKey() + "', '" + chart.getReportName() + "', '" + chart.getChartName() + "', '" + chart.getFilterProperty() + "', '"
					+ chart.getChartType() + "', '" + chart.getCreatedTime() + "', '" + chart.getUpdateTime() + "', '" + chart.isActive() + "', '"
					+ chart.getUserId() + "', '" + map.convertValue(chart.getUserAccessList(), JSONArray.class).toJSONString() + "', '"
					+ map.convertValue(chart.getSiteAccessList(), JSONArray.class).toJSONString() + "', "
					+ "'" + chart.getChartDesc() + "', '"
					+ chart.isVisible() + "', '" + chart.isDefault() + "', '" + chart.getAnalyticsFor() + "', '" + chart.getAnalyticsType() + "', '"
					+ map.convertValue(chart.getCategoryList(), JSONArray.class).toJSONString() + "', '" + chart.getChartDetails() + "', '" + chart.getReportLabel() + "');";
			System.out.println("-----insertQuery:------------ " + insertQuery );
			try(Connection con = DriverManager.getConnection(data.get("url"), data.get("userName"), data.get("password"));Statement statement = con.createStatement(); ){
				statement.executeQuery(insertQuery);
				System.out.println("-----------Data Inserted------------");
			}catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Transactional
	public Boolean saveEntity(Class c, Object obj) {

		try {
			entityManager.persist(obj);
			entityManager.flush();
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
		return true;
	}
	
	@Transactional
	public Boolean updateEntity(Class c, Object obj) {

		try {
			entityManager.merge(obj);
			entityManager.flush();
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
		return true;
	}
	
	public Boolean updateEntityNew(ChartModel_v2 chart) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONParser jsonParser = new JSONParser();
		String updateQuery = "";
		
		try {
			updateQuery = "UPDATE chart"
					+ "	SET chart_configuration= '" + chart.getChartConfiguration().toString()  + "', is_dashboard= '" + chart.isDashboard() + "', "
					+ "site_key= '" + chart.getSiteKey() + "', report_name= '" + chart.getReportName() + "', "
					+ "chart_name= '" + chart.getChartName() + "', filter_property= '" + chart.getFilterProperty()  + "',"
					+ "chart_type= '" + chart.getChartType() + "', created_time= '" + chart.getCreatedTime() + "', "
					+ "update_time= '" + chart.getUpdateTime() + "', is_active= '" + chart.getIsActive() + "', user_id= '" + chart.getUserId() + "', "
					+ "user_access_list= '" + map.convertValue(chart.getUserAccessList() , JSONArray.class).toJSONString() + "', "
					+ "site_access_list= '" + map.convertValue(chart.getSiteAccessList() , JSONArray.class).toJSONString() + "', chart_desc= '" + chart.getChartDesc() + "', "
					+ "is_visible= '" + chart.getIsVisible() + "', is_default= '" + chart.getIsDefault() + "', analytics_for= '" + chart.getAnalyticsFor() + "', analytics_type= '" + chart.getAnalyticsType() + "', "
					+ "category_list= '" + chart.getCategoryList() + "', chart_details= '" + chart.getChartDetails() + "', report_label= '" + chart.getReportLabel() + "'"
					+ "	WHERE chart_id = '" + chart.getChartId() + "';";
		} catch (Exception e) {
			e.printStackTrace();
		}		
		System.out.println("------------------updateQuery-------------------" + updateQuery);
		try(Connection con = DriverManager.getConnection(data.get("url"), data.get("userName"), data.get("password"));Statement statement = con.createStatement(); 
				){
			int resultSet = statement.executeUpdate(updateQuery);
			System.out.println("-----------Data Updated------------");
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public ChartModel_v2 getChartNew(String chartId) {
		ZenfraJSONObject object = new ZenfraJSONObject();
		JSONParser jsonParser = new JSONParser();
		ChartModel_v2 chartModel_v2 = new ChartModel_v2();
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		String getChartNew = "";
		try(Connection con = DriverManager.getConnection(data.get("url"), data.get("userName"), data.get("password"));Statement statement = con.createStatement();){
			
			getChartNew = "select * from chart where chart_id = '" + chartId + "'";
			ResultSet resultSet = statement.executeQuery(getChartNew);
			while(resultSet.next()) {
				  object.put("chartId", resultSet.getString("chart_id"));
				  object.put("chartConfiguration", resultSet.getObject("chart_configuration"));
				  object.put("dashboard", resultSet.getBoolean("is_dashboard"));
				  object.put("siteKey", resultSet.getString("site_key"));
				  object.put("reportName", resultSet.getString("report_name"));
				  object.put("chartName", resultSet.getString("chart_name"));
				  object.put("filterProperty", resultSet.getObject("filter_property"));
				  object.put("chartType", resultSet.getString("chart_type"));
				  object.put("createdTime", resultSet.getString("created_time"));
				  object.put("updateTime", resultSet.getString("update_time"));
				  object.put("isActive", resultSet.getBoolean("is_active"));
				  object.put("userId", resultSet.getString("user_id"));
				 
				
				List<String> uList = new ArrayList<String>();
				uList.addAll(Arrays.asList(resultSet != null ? resultSet.getString("user_access_list").replace("[", "").replace("]", "") : null));

				List<String> sList = new ArrayList<String>();
				sList.addAll(Arrays.asList(resultSet != null ? resultSet.getString("site_access_list").replace("[", "").replace("]", "") : null)); 
				
				List<String> cList = new ArrayList<String>();
				cList.addAll(Arrays.asList(resultSet != null ? resultSet.getString("category_list").replace("[", "").replace("]", "") : null)); 				
				object.put("userAccessList", uList);
				object.put("siteAccessList", sList);
				object.put("chartDesc", resultSet.getString("chart_desc"));
				object.put("isVisible", resultSet.getBoolean("is_visible"));
				object.put("isDefault", resultSet.getBoolean("is_default"));
				object.put("analyticsFor", resultSet.getString("analytics_for"));
				object.put("analyticsType", resultSet.getString("analytics_type"));
				object.put("categoryList", cList);
				object.put("chartDetails", resultSet.getObject("chart_details"));
				object.put("reportLabel", resultSet.getString("report_label"));
			}
			chartModel_v2 = map.convertValue(object, ChartModel_v2.class);
			chartModel_v2.setChartConfiguration(map.readValue(object.get("chartConfiguration").toString(), JSONObject.class));
			chartModel_v2.setFilterProperty(map.readValue(object.get("filterProperty").toString(), JSONObject.class));
			chartModel_v2.setSiteAccessList(map.readValue(object.get("siteAccessList").toString(), JSONArray.class));
			chartModel_v2.setUserAccessList(map.readValue(object.get("userAccessList").toString(), JSONArray.class));
			chartModel_v2.setChartDetails(map.readValue(object.get("chartDetails").toString(), JSONObject.class));
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return chartModel_v2;
	}
	
	public Object getEntityByColumn(String query, Class c) {

		Object obj = null;
		try {
			obj = entityManager.createNativeQuery(query, c).getSingleResult();

		} catch (NoResultException e) {

		}
		return obj;
	}

	public List<Object> getEntityListByColumn(String query, Class c) {

		List<Object> obj = new ArrayList<Object>();
		try {
			obj = entityManager.createNativeQuery(query, c).getResultList();
			System.out.println(obj);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	@Transactional
	public boolean deleteByEntity(Object obj) {

		try {
			this.entityManager.remove(obj);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}

	}

	public Boolean eveitEntity(Object obj) {

		try {

			entityManager.detach(obj);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
		return true;
	}

}
