package com.zenfra.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dao.FavouriteDao_v2;
import com.zenfra.model.FavouriteModel;
import com.zenfra.model.FavouriteOrder;

@Service
public class FavouriteApiService_v2 {

	@Autowired
	FavouriteDao_v2 daoFav;

	public JSONObject getFavView(String userId, String siteKey, String reportName, String projectId) {

		JSONObject arr = new JSONObject();
		try {
			JSONObject obj = new JSONObject();

			String favourite_view_query = "select fa.updated_time as \"updatedTime\",fa.updated_by as \"updatedBy\",fa.report_name as \"reportName\",fa.favourite_id as \"favouriteId\",fa.filter_property as \"filterProperty\",fa.is_active as \"isActive\",fa.user_access_list as \"userAccessList\",fa.group_by_period as \"groupByPeriod\",fa.site_key as \"siteKey\",fa.grouped_columns as \"groupedColumns\",fa.created_by as \"createdBy\",fa.category_list as \"categoryList\",fa.created_time as \"createdTime\",fa.favourite_name as \"favouriteName\",fa.site_access_list as \"siteAccessList\",fa.project_id as \"projectId\",fa.is_default as \"isDefault\",fa.rid as \"rid\""
					+ ",CASE\r\n" + "when fa.created_by = 'user_id_value' then True\r\n"
					+ "when usr.is_tenant_admin = 'true' then true\r\n" + "else false\r\n" + "END isWriteAccess\r\n"
					+ "from favourite_view fa\r\n" + "left join user_temp usr on usr.user_id = fa.created_by\r\n"
					+ "where\r\n" + "fa.report_name = 'report_name_value'\r\n"
					+ "and fa.site_key = 'site_key_value'\r\n" + "and fa.is_active = true\r\n" + "and (\r\n"
					+ "    fa.created_by = 'user_id_value'\r\n" + "    or\r\n"
					+ "    ((fa.user_access_list @> Array['user_id_value'] or fa.user_access_list = Array['All']) and\r\n"
					+ "    (fa.site_access_list like '%site_key_value%' or fa.site_access_list = '[\"All\"]'))\r\n"
					+ "    )\r\n"
					+ "and (fa.user_remove_list not like '%user_id_value%' or fa.user_remove_list is null)";

			favourite_view_query = favourite_view_query.replace("report_name_value", reportName)
					.replace("site_key_value", siteKey).replace("user_id_value", userId);

			String favourite_order_query = "select orders from favourite_order"
					+ " where site_key = 'site_key_value'  and created_by = 'user_id_value' "
					+ " and report_name = 'report_name_value'";

			favourite_order_query = favourite_order_query.replace("report_name_value", reportName)
					.replace("site_key_value", siteKey).replace("user_id_value", userId);

			System.out.println(favourite_view_query);
			
			List<Map<String, Object>> rows=	daoFav.getJsonarray(favourite_view_query);
					
			ObjectMapper map=new ObjectMapper();
			JSONArray viewArr=new JSONArray();
			 JSONParser parser = new JSONParser();
			rows.forEach(row->{				
					try {
						if(row.get("userAccessList")!=null) {
							System.out.println( row.get("userAccessList"));
							row.put("userAccessList",  row.get("userAccessList").toString().replace("{", "").replace("}", "").split(","));
							
						}
						row.put("filterProperty",(JSONArray)parser.parse(row.get("filterProperty").toString().replace("\\[", "").replace("\\]", "")));
						row.put("categoryList",(JSONArray)parser.parse(row.get("categoryList").toString().replace("\\[", "").replace("\\]", "")));
						row.put("siteAccessList",(JSONArray)parser.parse(row.get("siteAccessList").toString().replace("\\[", "").replace("\\]", "")));
						row.put("siteAccessList",(JSONArray)parser.parse(row.get("siteAccessList").toString().replace("\\[", "").replace("\\]", "")));
						row.put("groupedColumns",(JSONArray)parser.parse(row.get("groupedColumns").toString().replace("\\[", "").replace("\\]", "")));
						viewArr.add(map.convertValue(row, JSONObject.class));
			
					} catch (Exception e) {
						e.printStackTrace();					
					}
				});
	        
			
			arr.put("view", viewArr);
			arr.put("order", daoFav.getJsonarray(favourite_order_query).get(0));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return arr;
	}

	public Integer saveFavouriteView(FavouriteModel favouriteModel) {

		int responce = 0;
		try {

			SqlParameterSource parameters = new MapSqlParameterSource()
					.addValue("updated_time", favouriteModel.getUpdatedTime())
					.addValue("updated_by", favouriteModel.getUpdatedBy())
					.addValue("updated_by", favouriteModel.getUpdatedBy())
					.addValue("report_name", favouriteModel.getReportName())
					.addValue("is_active", favouriteModel.getIsActive())
					.addValue("group_by_period", favouriteModel.getGroupByPeriod())
					.addValue("site_key", favouriteModel.getSiteKey())
					.addValue("created_by", favouriteModel.getCreatedBy())
					.addValue("created_time", favouriteModel.getCreatedTime())
					.addValue("favourite_name", favouriteModel.getFavouriteName())
					.addValue("project_id", favouriteModel.getProjectId())
					.addValue("site_access_list", favouriteModel.getSiteAccessList())
					.addValue("user_access_list", favouriteModel.getUserAccessList())
					.addValue("grouped_columns", favouriteModel.getGroupedColumns())
					.addValue("category_list", favouriteModel.getCategoryList()).addValue("user_remove_list", null)
					.addValue("favourite_id", "gen_random_uuid()")
					.addValue("filter_property", favouriteModel.getFilterProperty().toJSONString());

			String user = favouriteModel.getUserAccessList().toString().replace("[", "{").replace("]", "}");

			
			String updateQuery = "INSERT INTO favourite_view(favourite_id,"
						+ "updated_time, updated_by, report_name, is_active, group_by_period, site_key, created_by, created_time, favourite_name, project_id, user_remove_list, site_access_list, grouped_columns, category_list, filter_property, user_access_list)\r\n"
						+ "	VALUES('"+favouriteModel.getFavouriteId()+"','" + favouriteModel.getUpdatedTime() + "', '"
						+ favouriteModel.getUpdatedBy() + "', '" + favouriteModel.getReportName() + "', "
						+ favouriteModel.getIsActive() + ", '" + favouriteModel.getGroupByPeriod() + "'," + "'"
						+ favouriteModel.getSiteKey() + "', '" + favouriteModel.getCreatedBy() + "', '"
						+ favouriteModel.getCreatedTime() + "', '" + favouriteModel.getFavouriteName() + "', '"
						+ favouriteModel.getProjectId() + "', null, '" + favouriteModel.getSiteAccessList() + "'," + " '"
						+ favouriteModel.getGroupedColumns() + "','" + favouriteModel.getCategoryList() + "', '"
						+ favouriteModel.getFilterProperty() + "', '" + user + "')";
		
			
			responce = daoFav.updateQuery(parameters, updateQuery);

		} catch (Exception e) {
			e.printStackTrace();

		}

		return responce;
	}

	public Integer deleteFavouriteViewData(String userId, String favouriteId, String createdBy, String siteKey) {

		int responce = 0;
		try {

			String updateFavView = "";
			if (createdBy.equalsIgnoreCase(userId)) {
				updateFavView = "update favourite_view set is_active=:is_active where favourite_id=':favourite_id'";
				updateFavView = updateFavView.replace(":is_active", "false").replace(":favourite_id", favouriteId);
			} else {
				updateFavView = "update favourite_view SET user_access_list=array_remove(user_access_list,':user_id') where favourite_id=':favourite_id'";
				updateFavView = updateFavView.replace(":user_id", userId).replace(":favourite_id", favouriteId);
			}

			String dynamicChartDeleteQuery = "update dash_board_chart_details set is_active =':is_active' where favourite_id =':favourite_id'";
			dynamicChartDeleteQuery = dynamicChartDeleteQuery.replace(":is_active", "false").replace(":favourite_id",
					favouriteId);
			String dashBoardChartDeleteQuery = "delete from dash_board_charts where favorite_view=':favourite_id' and user_id=':user_id'";
			dashBoardChartDeleteQuery = dashBoardChartDeleteQuery.replace(":user_id", userId).replace(":favourite_id",
					favouriteId);
			responce = daoFav.updateQuery(updateFavView);
			daoFav.updateQuery(dynamicChartDeleteQuery);
			daoFav.updateQuery(dashBoardChartDeleteQuery);

		} catch (Exception e) {
			e.printStackTrace();

		}

		return responce;

	}

	public int saveFavouriteOrder(String userId, FavouriteOrder favouriteModel) {
		int responce = 0;
		try {
			
			

			Map<String, Object> params = new HashMap<>();
			params.put("data_id", "gen_random_uuid()");
			params.put("updated_time", favouriteModel.getUpdatedTime());
			params.put("site_key", favouriteModel.getSiteKey());
			params.put("updated_by", favouriteModel.getUpdatedBy());
			params.put("report_name", favouriteModel.getReportName());
			params.put("created_by", favouriteModel.getCreatedBy());
			params.put("created_time", favouriteModel.getCreatedTime());
			params.put("is_active", favouriteModel.getIsActive());
			params.put("project_id", favouriteModel.getProjectId());
			params.put("order_id", favouriteModel.getOrderId());
			params.put("orders", favouriteModel.getOrders());

			//String query = "INSERT INTO favourite_order(data_id, updated_time, site_key, updated_by, report_name, created_by, order_id, created_time, is_active, project_id, orders)"
			//		+ "  VALUES (:data_id,:updated_time,:site_key,:updated_by,:report_name,:created_by,:order_id,:created_time,:is_active,:project_id,:orders)";

			
			String updateQuery="UPDATE favourite_order\r\n" + 
					"	SET  updated_time='"+favouriteModel.getUpdatedTime()+"', updated_by='"+favouriteModel.getUpdatedBy()+"',orders='"+favouriteModel.getOrders()+"' WHERE created_by='"+favouriteModel.getCreatedBy()+"' and site_key='"+favouriteModel.getSiteKey()+"' and report_name='"+favouriteModel.getReportName()+"';";
			
			
			if(daoFav.updateQuery(updateQuery)>0) {				
				return 1;
			}
			
			String query = "INSERT INTO favourite_order(data_id, updated_time, site_key, updated_by, report_name, created_by, order_id, created_time, is_active, project_id, orders)"
					+ "  VALUES (gen_random_uuid(),'"+favouriteModel.getUpdatedTime()+"','"+favouriteModel.getSiteKey()+"','"+favouriteModel.getUpdatedBy()+"','"+favouriteModel.getReportName()+"',"
							+ "'"+favouriteModel.getCreatedBy()+"','"+favouriteModel.getOrderId()+"','"+favouriteModel.getCreatedTime()+"','"+favouriteModel.getIsActive()+"','"+favouriteModel.getProjectId()+"','"+favouriteModel.getOrders()+"')";
			
			
			responce=daoFav.updateQuery(query);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return responce;
	}

	public Boolean checkfavouriteName(String userId, String siteKey, String favouriteName,String reportName) {

		boolean count = false;
		try {

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("is_active", true);
			params.put("favourite_name", favouriteName);
			params.put("report_name", reportName);

			String query = "select count(*) from favourite_view where is_active =true and "
					+ " site_key='"+siteKey+"' and report_name='"+reportName+"' "
					+ " and lower(favourite_name)='"+favouriteName+"'";

			
			
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
			
			String user = favouriteModel.getUserAccessList().toString().replace("[", "{").replace("]", "}");
			
			String query="UPDATE favourite_view\r\n" + 
					"	SET updated_time='"+favouriteModel.getUpdatedTime()+"', updated_by='"+favouriteModel.getUpdatedBy()+"', favourite_id='"+favouriteModel.getFavouriteId()+"', "
					+ "is_active='"+favouriteModel.getIsActive()+"', group_by_period='"+favouriteModel.getGroupByPeriod()+"', site_key='"+favouriteModel.getSiteKey()+"', favourite_name='"+favouriteModel.getFavouriteName()+"', project_id='"+favouriteModel.getProjectId()+"', "
					+ " site_access_list='"+favouriteModel.getSiteAccessList()+"', grouped_columns='"+favouriteModel.getGroupByPeriod()+"', category_list='"+favouriteModel.getCategoryList()+"', filter_property='"+favouriteModel.getFilterProperty()+"', user_access_list='"+ user+"'"
					+ "where  report_name='"+favouriteModel.getReportName()+"'  and site_key='"+favouriteModel.getSiteKey()+"' and created_by='"+userId+"'";
					
						
			responce=daoFav.updateQuery(query);
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return responce;
	}

}
