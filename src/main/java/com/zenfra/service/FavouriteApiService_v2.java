package com.zenfra.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dao.FavouriteDao_v2;
import com.zenfra.model.FavouriteModel;
import com.zenfra.model.FavouriteOrder;
import com.zenfra.queries.DashBoardChartsDetailsQueries;
import com.zenfra.queries.DashBoardChartsQueries;
import com.zenfra.queries.FavouriteOrderQueries;
import com.zenfra.queries.FavouriteViewQueries;

@Service
public class FavouriteApiService_v2 {

	@Autowired
	FavouriteDao_v2 daoFav;

	@Autowired
	FavouriteViewQueries queriesView;

	@Autowired
	FavouriteOrderQueries orderQueries;
	
	@Autowired
	DashBoardChartsQueries dashQueries;
	
	@Autowired
	DashBoardChartsDetailsQueries dashDetails;

	public JSONObject getFavView(String userId, String siteKey, String reportName, String projectId) {

		JSONObject arr = new JSONObject();
		try {
			JSONObject obj = new JSONObject();

			String favourite_view_query = queriesView.getGetFavView();
			favourite_view_query = favourite_view_query.replace(":report_name_value", reportName)
					.replace(":site_key_value", siteKey).replace(":user_id_value", userId);

			String favourite_order_query = orderQueries.getGetFavouriteOrder();
			favourite_order_query = favourite_order_query.replace(":report_name_value", reportName)
					.replace(":site_key_value", siteKey).replace(":user_id_value", userId);
			List<Map<String, Object>> rows = daoFav.getJsonarray(favourite_view_query);

			System.out.println(favourite_view_query);
			ObjectMapper map = new ObjectMapper();
			JSONArray viewArr = new JSONArray();
			JSONParser parser = new JSONParser();
			rows.forEach(row -> {
				try {
					if (row.get("userAccessList") != null) {
						System.out.println(row.get("userAccessList"));
						row.put("userAccessList",
								row.get("userAccessList").toString().replace("{", "").replace("}", "").split(","));
					}
					
					System.out.println("---row.get(\"siteAccessList\")"+row.get("siteAccessList"));
					row.put("filterProperty", (JSONArray) parser.parse(row.get("filterProperty").toString().replace("\\[", "").replace("\\]", "")));
					row.put("categoryList", (JSONArray) parser.parse(row.get("categoryList").toString().replace("\\[", "").replace("\\]", "")));
					row.put("siteAccessList", (JSONArray) parser.parse(row.get("siteAccessList").toString().replace("\\[", "").replace("\\]", "")));
					row.put("siteAccessList", (JSONArray) parser.parse(row.get("siteAccessList").toString().replace("\\[", "").replace("\\]", "")));
					row.put("groupedColumns", (JSONArray) parser.parse(row.get("groupedColumns").toString().replace("\\[", "").replace("\\]", "")));
					viewArr.add(map.convertValue(row, JSONObject.class));

				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			System.out.println();
			List<Map<String,Object>> orderArr= daoFav.getJsonarray(favourite_order_query);
			arr.put("view", viewArr);			
			if(orderArr!=null && !orderArr.isEmpty()) {
				System.out.println(orderArr);
				arr.put("order", orderArr.get(0));
			}else {
				arr.put("order",new JSONArray());
			}
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		return arr;
	}

	public Integer saveFavouriteView(FavouriteModel favouriteModel) {

		int responce = 0;
		try {

			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(":report_name", favouriteModel.getReportName());
			parameters.put(":is_active", favouriteModel.getIsActive());
			parameters.put(":group_by_period", favouriteModel.getGroupByPeriod());
			parameters.put(":site_key", favouriteModel.getSiteKey());
			parameters.put(":created_by", favouriteModel.getCreatedBy());
			parameters.put(":created_time", favouriteModel.getCreatedTime());
			parameters.put(":favourite_name", favouriteModel.getFavouriteName());
			parameters.put(":project_id", favouriteModel.getProjectId());
			parameters.put(":site_access_list", favouriteModel.getSiteAccessList());
			parameters.put(":user_access_list",
					favouriteModel.getUserAccessList().toString().replace("[", "{").replace("]", "}"));
			parameters.put(":grouped_columns", favouriteModel.getGroupedColumns());
			parameters.put(":category_list", favouriteModel.getCategoryList());
			parameters.put(":user_remove_list", null);
			parameters.put(":favourite_id", favouriteModel.getFavouriteId());
			parameters.put(":filter_property", favouriteModel.getFilterProperty().toJSONString());

			String updateQuery = queriesView.getSave();

			for (String key : parameters.keySet()) {
				updateQuery = (parameters.get(key) != null) ? updateQuery.replace(key, parameters.get(key).toString()) : updateQuery.replace(key, "");
			}
			System.out.println(updateQuery);
			responce = daoFav.updateQuery(updateQuery);

		} catch (Exception e) {
			e.printStackTrace();

		}

		return responce;
	}

	public Integer deleteFavouriteViewData(String userId, String favouriteId, String createdBy, String siteKey) {

		int responce = 0;
		try {
			Map<String,Object> params=new HashMap<String, Object>();
				params.put("is_active", false);
				params.put("favourite_id", favouriteId);
				params.put("user_id", userId);
				params.put("site_key", siteKey);				
			String updateFavView = "";
			if (createdBy.equalsIgnoreCase(userId)) {
				updateFavView =queriesView.getUpdateCreatedByEqualsUserId();
			} else {
				updateFavView = queriesView.getUpdateCreatedByNotEqualsUserId();
			}
			String dynamicChartDeleteQuery = dashDetails.getUpdateDynamicChartDetailsActiveFalseQuery();
			String dashBoardChartsDeleteQuery = dashQueries.getDelete();
			responce = daoFav.updateQuery(params,updateFavView);
			daoFav.updateQuery(params,dynamicChartDeleteQuery);
			daoFav.updateQuery(params,dashBoardChartsDeleteQuery);

		} catch (Exception e) {
			e.printStackTrace();

		}

		return responce;

	}

	public int saveFavouriteOrder(FavouriteOrder favouriteModel) {
		int responce = 0;
		try {

			SqlParameterSource parameters = new MapSqlParameterSource()
					// Map<String, Object> params = new HashMap<>();
					.addValue("updated_time", favouriteModel.getUpdatedTime())
					.addValue("site_key", favouriteModel.getSiteKey())
					.addValue("updated_by", favouriteModel.getUpdatedBy())
					.addValue("report_name", favouriteModel.getReportName())
					.addValue("created_by", favouriteModel.getCreatedBy())
					.addValue("created_time", favouriteModel.getCreatedTime())
					.addValue("is_active", favouriteModel.getIsActive())
					.addValue("project_id", favouriteModel.getProjectId())
					.addValue("order_id", favouriteModel.getOrderId())
					.addValue("orders", Arrays.asList(favouriteModel.getOrders()));

			// String query = "INSERT INTO favourite_order(data_id, site_key, report_name,
			// created_by, order_id, created_time, is_active, project_id, orders)"
			// + " VALUES
			// (gen_random_uuid(),:site_key,:report_name,:created_by,:order_id,:created_time,:is_active,:project_id,:orders)";

			String updateQuery = "UPDATE favourite_order\r\n" + "	SET  updated_time='"
					+ favouriteModel.getUpdatedTime() + "', updated_by='" + favouriteModel.getUpdatedBy() + "',orders='"
					+ favouriteModel.getOrders() + "' WHERE created_by='" + favouriteModel.getCreatedBy()
					+ "' and site_key='" + favouriteModel.getSiteKey() + "' and report_name='"
					+ favouriteModel.getReportName() + "';";

			if (daoFav.updateQuery(updateQuery) > 0) {
				return 1;
			}

			String query = "INSERT INTO favourite_order(data_id, updated_time, site_key, updated_by, report_name, created_by, order_id, created_time, is_active, project_id, orders)"
					+ "  VALUES (gen_random_uuid(),'" + favouriteModel.getUpdatedTime() + "','"
					+ favouriteModel.getSiteKey() + "','" + favouriteModel.getUpdatedBy() + "','"
					+ favouriteModel.getReportName() + "'," + "'" + favouriteModel.getCreatedBy() + "','"
					+ favouriteModel.getOrderId() + "','" + favouriteModel.getCreatedTime() + "','"
					+ favouriteModel.getIsActive() + "','" + favouriteModel.getProjectId() + "','"
					+ favouriteModel.getOrders() + "')";

			responce = daoFav.updateQuery(query);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return responce;
	}

	public Boolean checkfavouriteName(String userId, String siteKey, String favouriteName, String reportName) {

		boolean count = false;
		try {

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("is_active", true);
			params.put("favourite_name", favouriteName);
			params.put("report_name", reportName);

			String query = "select count(*) from favourite_view where is_active =true and " + " site_key='" + siteKey
					+ "' and report_name='" + reportName + "' " + " and lower(favourite_name)='" + favouriteName + "'";

			if (daoFav.getCount(query) > 0) {
				count = true;
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

		return count;
	}

	public int updateFavouriteView(String userId, FavouriteModel favouriteModel) {
		int responce = 0;
		try {
			ObjectMapper map = new ObjectMapper();
			String user = favouriteModel.getUserAccessList().toString().replace("[", "{").replace("]", "}");
			String site_access_list=map.convertValue(favouriteModel.getSiteAccessList(), JSONArray.class).toJSONString();
			String category_list=map.convertValue(favouriteModel.getCategoryList(), JSONArray.class).toJSONString();
			String grouped_columns=map.convertValue(favouriteModel.getGroupedColumns(), JSONArray.class).toJSONString();
			
			System.out.println(site_access_list);
			String query = "UPDATE favourite_view SET updated_time='" + favouriteModel.getUpdatedTime()
					+ "', updated_by='" + favouriteModel.getUpdatedBy() + "'"
					+ ", group_by_period='" + favouriteModel.getGroupByPeriod() + "', site_key='"
					+ favouriteModel.getSiteKey() + "', favourite_name='" + favouriteModel.getFavouriteName()
					+ "', project_id='" + favouriteModel.getProjectId() + "', " + " site_access_list='"
					+ site_access_list + "', grouped_columns='" + grouped_columns
					+ "', category_list='" + category_list + "', filter_property='"
					+ favouriteModel.getFilterProperty() + "', user_access_list='" + user + "' where favourite_id='"+favouriteModel.getFavouriteId()+"'";

			System.out.println(query);
			responce = daoFav.updateQuery(query);
		} catch (Exception e) {
			e.printStackTrace();

		}
		return responce;
	}

}
