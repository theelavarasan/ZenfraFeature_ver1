package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
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
import org.springframework.util.LinkedCaseInsensitiveMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.FavouriteDao_v2;
import com.zenfra.model.FavouriteModel;
import com.zenfra.model.FavouriteOrder;
import com.zenfra.model.FavouriteView_v2;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class FavouriteApiService_v2 {

	final ObjectMapper map = new ObjectMapper();

	@Autowired
	FavouriteDao_v2 daoFav;

	@Autowired
	CommonQueriesData queries;

	@Autowired
	CommonFunctions common;

	@Autowired
	ReportService reportService;

	@Autowired
	HealthCheckService healthCheckService;

	private ObjectMapper objectMapper = new ObjectMapper();
	private JSONParser jSONParser = new JSONParser();

	public JSONObject getFavView(String userId, String siteKey, String reportName, String projectId) {

		JSONObject arr = new JSONObject();
		String reportNameRef = reportName;
		try {
			JSONObject obj = new JSONObject();

			if (reportName.equalsIgnoreCase("migrationreport")) {
				reportName = "'migrationreport','discovery','compatability','migration-method'";
			} else {
				reportName = "'" + reportName + "'";
			}
			String favourite_view_query = queries.favouriteView().getGetFavView();
			favourite_view_query = favourite_view_query.replace(":report_name_value", reportName)
					.replace(":site_key_value", siteKey).replace(":user_id_value", userId);

			String favourite_order_query = queries.favouriteOrder().getGetFavouriteOrder();
			favourite_order_query = favourite_order_query.replace(":report_name_value", reportName)
					.replace(":site_key_value", siteKey).replace(":user_id_value", userId);
			List<Map<String, Object>> rows = daoFav.getJsonarray(favourite_view_query);

			System.out.println(
					"*****************************!!!!!!!!!!!!!!!!!!!!!!!!favourite_view_query!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*********************************"
							+ favourite_view_query);
			ObjectMapper map = new ObjectMapper();
			JSONArray viewArr = new JSONArray();
			JSONParser parser = new JSONParser();

			// System.out.println(rows.size() + " :: " + rows);

			rows.forEach(row -> {
				try {
					if (row.get("userAccessList") != null) {
						row.put("userAccessList",
								row.get("userAccessList").toString().replace("{", "").replace("}", "").split(","));
					}
					row = common.getFavViewCheckNull(row);
					// Map<String, Object> rowMap = row;
					// rowMap = setDeviceType(rowMap);
					viewArr.add(row);

				} catch (Exception e) {
					e.printStackTrace();
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					String ex = errors.toString();
					ExceptionHandlerMail.errorTriggerMail(ex);
				}
			});

			Object orderArr = daoFav.getSingleColumnAsObject(favourite_order_query);
			arr.put("view", viewArr);
			if (orderArr != null) {
				arr.put("order", common.convertObjectToJsonArray(orderArr));
			} else {
				arr.put("order", new JSONArray());
			}

			if (reportNameRef.equalsIgnoreCase("healthcheck")) {
				JSONArray hcFilterArray = new JSONArray();

				for (int i = 0; i < viewArr.size(); i++) {
					LinkedCaseInsensitiveMap hcData = (LinkedCaseInsensitiveMap) viewArr.get(i);
					String reportLabled = (String) hcData.get("reportLabel");
					String[] sa = reportLabled.split("-");
					String hcId = "";
					for (int j = 0; j <= 4; j++) {
						if (hcId.isEmpty()) {
							hcId = sa[j];
						} else {
							hcId = hcId + "-" + sa[j];
						}
					}
					JSONObject healthCheck = healthCheckService.getHealthCheck(hcId, null);
					if (healthCheck != null) {
						JSONArray filterArray = (JSONArray) hcData.get("filterProperty");

						JSONObject componentType = new JSONObject();
						componentType.put("label", "Component Type");
						componentType.put("name", "componentType");
						componentType.put("selection", healthCheck.get("componentType"));

						JSONObject analyticsType = new JSONObject();
						analyticsType.put("label", "Analytics Type");
						analyticsType.put("name", "analyticsType");
						analyticsType.put("selection", healthCheck.get("analyticsType"));

						JSONObject reportListType = new JSONObject();
						reportListType.put("label", "Discovery Type");
						reportListType.put("name", "reportList");
						reportListType.put("selection", healthCheck.get("reportName"));

						JSONObject reportBy = new JSONObject();
						reportBy.put("label", "Analytics By");
						reportBy.put("name", "reportBy");
						reportBy.put("selection", healthCheck.get("reportBy"));

						filterArray.add(componentType);
						filterArray.add(analyticsType);
						filterArray.add(reportListType);
						filterArray.add(reportBy);

						hcData.put("filterProperty", filterArray);
						hcFilterArray.add(hcData);

						System.out.println("---filterArray-----------" + filterArray);

					}

				}
				arr.put("view", hcFilterArray);
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return arr;
	}

	public Integer saveFavouriteView(FavouriteModel favouriteModel) {

		int responce = 0;
		try {

			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(":updated_time", favouriteModel.getUpdatedTime());
			parameters.put(":updated_by", favouriteModel.getUpdatedBy());
			parameters.put(":report_name", favouriteModel.getReportName());
			parameters.put(":report_name", favouriteModel.getReportName());
			parameters.put(":is_active", favouriteModel.getIsActive());
			parameters.put(":is_default", favouriteModel.getIsDefault());
			parameters.put(":group_by_period", favouriteModel.getGroupByPeriod());
			parameters.put(":site_key", favouriteModel.getSiteKey());
			parameters.put(":created_by", favouriteModel.getCreatedBy());
			parameters.put(":created_time", favouriteModel.getCreatedTime());
			parameters.put(":favourite_name", favouriteModel.getFavouriteName());
			parameters.put(":project_id", favouriteModel.getProjectId());
			parameters.put(":site_access_list",
					map.convertValue(favouriteModel.getSiteAccessList(), JSONArray.class).toJSONString());
//			parameters.put(":user_access_list",
//					favouriteModel.getUserAccessList().toString().replace("[", "{").replace("]", "}"));
			
			parameters.put(":user_access_list",
					map.convertValue(favouriteModel.getUserAccessList(), JSONArray.class).toJSONString());
			parameters.put(":grouped_columns", favouriteModel.getGroupedColumns());
			parameters.put(":category_list",
					map.convertValue(favouriteModel.getCategoryList(), JSONArray.class).toJSONString());
			parameters.put(":user_remove_list", null);
			parameters.put(":favourite_id", favouriteModel.getFavouriteId());
			parameters.put(":filter_property", favouriteModel.getFilterProperty().toJSONString());
			parameters.put(":report_label", favouriteModel.getReportLabel());
			parameters.put(":os_type", favouriteModel.getOsType());

			System.out.println("-------------------------------" + parameters.get(":user_access_list").toString());
			
			String updateQuery = queries.favouriteView().getSave();

			for (String key : parameters.keySet()) {
				updateQuery = (parameters.get(key) != null) ? updateQuery.replace(key, parameters.get(key).toString())
						: updateQuery.replace(key, "");
			}
			System.out.println(updateQuery);
			responce = daoFav.updateQuery(updateQuery);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}

		return responce;
	}

	public Integer deleteFavouriteViewData(String userId, String favouriteId, String createdBy, String siteKey) {

		int responce = 0;
		try {

			FavouriteView_v2 favView = daoFav.getFavouriteViewByFavouriteId(
					queries.favouriteView().getSelectByFavouriteId().replace(":favourite_id", favouriteId));
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("is_active", false);
			params.put("favourite_id", favouriteId);
			params.put("user_id", userId);
			params.put("site_key", siteKey);

			String updateFavView = "";
			if (favView != null && favView.getCreated_by().equalsIgnoreCase(userId)) {
				updateFavView = queries.favouriteView().getUpdateCreatedByEqualsUserId().replace(":favourite_id",
						favouriteId);
				System.out.println("3::" + updateFavView);
			} else if (favView != null && favView.getUser_access_list().contains("All")) {
				String user_remove_list = favView.getUser_remove_list();
				if (user_remove_list != null && !user_remove_list.isEmpty()) {
					if (!user_remove_list.contains(userId)) {
						user_remove_list = user_remove_list.replace("]", (",\"" + userId + "\"]"));
					}

				} else {
					user_remove_list = "[\"" + userId + "\"]";
				}
				params.put("user_remove_list", user_remove_list);
				System.out.println(user_remove_list);
				updateFavView = queries.favouriteView().getUpdateCreatedByNotEqualsUserIdUserRemoveUpdate()
						.replace(":user_remove_list", user_remove_list).replace(":favourite_id", favouriteId);
				System.out.println("1::" + updateFavView);

			} else {
				updateFavView = queries.favouriteView().getUpdateCreatedByNotEqualsUserIdUserAccessUpdate()
						.replace(":user_id", userId).replace(":favourite_id", favouriteId);
				System.out.println("2::" + updateFavView);
			}
			String dynamicChartDeleteQuery = queries.dashBoardChartDetails()
					.getUpdateDynamicChartDetailsActiveFalseQuery().replace(":is_active", String.valueOf(false))
					.replace(":favourite_id", favouriteId);
			String dashBoardChartsDeleteQuery = queries.dashBoardChart().getDelete().replace(":user_id", userId)
					.replace(":favourite_id", favouriteId);

			System.out.println(updateFavView);
			responce = daoFav.updateQuery(updateFavView);

			daoFav.updateQuery(dynamicChartDeleteQuery);
			daoFav.updateQuery(dashBoardChartsDeleteQuery);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

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
					.addValue("order_id", favouriteModel.getOrderId());

			String orders = map.convertValue(favouriteModel.getOrders(), JSONArray.class).toJSONString();

			String updateQuery = "UPDATE favourite_order\r\n" + "	SET  updated_time='"
					+ favouriteModel.getUpdatedTime() + "', updated_by='" + favouriteModel.getUpdatedBy() + "',orders='"
					+ orders + "' WHERE created_by='" + favouriteModel.getCreatedBy() + "' and site_key='"
					+ favouriteModel.getSiteKey() + "' and report_name='" + favouriteModel.getReportName() + "'";

			if (daoFav.updateQuery(updateQuery) > 0) {
				return 1;
			}

			String query = "INSERT INTO favourite_order(data_id, updated_time, site_key, updated_by, report_name, created_by, order_id, created_time, is_active, project_id, orders)"
					+ "  VALUES (gen_random_uuid(),'" + favouriteModel.getUpdatedTime() + "','"
					+ favouriteModel.getSiteKey() + "','" + favouriteModel.getUpdatedBy() + "','"
					+ favouriteModel.getReportName() + "'," + "'" + favouriteModel.getCreatedBy() + "','"
					+ favouriteModel.getOrderId() + "','" + favouriteModel.getCreatedTime() + "','"
					+ favouriteModel.getIsActive() + "','" + favouriteModel.getProjectId() + "','" + orders + "')";

			responce = daoFav.updateQuery(query);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
					+ "' and report_name='" + reportName + "' " + " and lower(favourite_name)=lower('" + favouriteName
					+ "')" + "  and user_remove_list not like '%" + userId + "%';";

			System.out.println(query);
			int s = daoFav.getCount(query);
			if (s > 0) {
				count = true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}

		return count;
	}

	public int updateFavouriteView(String userId, FavouriteModel favouriteModel) {
		int responce = 0;
		try {
			ObjectMapper map = new ObjectMapper();
			String user = favouriteModel.getUserAccessList().toString().replace("[", "{").replace("]", "}");
			String site_access_list = map.convertValue(favouriteModel.getSiteAccessList(), JSONArray.class)
					.toJSONString();
			JSONArray category_list = map.convertValue(favouriteModel.getCategoryList(), JSONArray.class);

			String grouped_columns = map.convertValue(favouriteModel.getGroupedColumns(), JSONArray.class)
					.toJSONString();

			String updateQuery = common.getUpdateFavQuery(favouriteModel);

			String query = "UPDATE favourite_view SET updated_time='" + favouriteModel.getUpdatedTime()
					+ "', updated_by='" + favouriteModel.getUpdatedBy() + "' " + updateQuery + " where favourite_id='"
					+ favouriteModel.getFavouriteId() + "'";

			System.out.println(query);
			responce = daoFav.updateQuery(query);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}
		return responce;
	}

	public void checkAndUpdateDefaultFavView(String siteKey, String parsedLogType, String userId) {
		try {
			String query = "select log_type, favourite_name, filter_property, report_label from default_favourite_view where is_active=true and lower(report_type)='discovery' and lower(log_type)='"
					+ parsedLogType.toLowerCase() + "'";
			List<Map<String, Object>> result = daoFav.getJsonarray(query);
			userId = daoFav.getTenantId();

			if (result.size() == 1) {
				String defaultFavName = result.get(0).get("favourite_name").toString();
				String defaultFilterProperty = result.get(0).get("filter_property").toString();
				String report_label = result.get(0).get("report_label").toString();
				defaultFilterProperty = common.convertStringToJsonArray(defaultFilterProperty).toString();

				String checkFavViewquery = "select count(*) from favourite_view where is_active=true and is_default=true and lower(report_name)='discovery' and lower(favourite_name)= '"
						+ defaultFavName.toLowerCase() + "' and site_key='" + siteKey + "' and filter_property = '"
						+ defaultFilterProperty + "'"; //

				if (daoFav.getCount(checkFavViewquery) == 0) {
					FavouriteModel favouriteModel = new FavouriteModel();
					favouriteModel.setCreatedTime(common.getCurrentDateWithTime());
					favouriteModel.setUpdatedTime(common.getCurrentDateWithTime());
					favouriteModel.setFavouriteId(common.generateRandomId());
					favouriteModel.setCreatedBy(userId);
					favouriteModel.setUpdatedBy(userId);
					favouriteModel.setFavouriteName(defaultFavName);
					favouriteModel.setSiteKey(siteKey);
					favouriteModel.setReportName("discovery");
					favouriteModel.setFilterProperty(common.convertStringToJsonArray(defaultFilterProperty));
					List<String> userAccess = new ArrayList<>();
					userAccess.add("All");
					favouriteModel.setUserAccessList(userAccess);
					favouriteModel.setIsDefault(true);
					favouriteModel.setIsActive(true);
					favouriteModel.setReportLabel(report_label);
					favouriteModel.setOsType(parsedLogType);
					saveFavouriteView(favouriteModel);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	public Object getViewCategoryMapping(String favouriteId) {
		JSONArray responce = new JSONArray();
		try {
			ObjectMapper map = new ObjectMapper();
			String query = queries.categoryMappingQueries().getGetById().replace(":id", favouriteId);
			System.out.println(query);
			Object obj = daoFav.getSingleColumnAsObject(query);
			System.out.println(obj.toString());
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return responce;
	}

	public List<Map<String, Object>> getFavouriteList(String catgoryid) {
		List<Map<String, Object>> object = new ArrayList<Map<String, Object>>();
		try {
			String query = "select * from favourite_view where category_list like '%:catgoryid%'".replace(":catgoryid",
					catgoryid);
			object = daoFav.getFavouriteList(query);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return object;
	}

	private Map<String, Object> setDeviceType(Map<String, Object> favArray) {
		Map<String, Object> resultArray = new HashMap<String, Object>();

		for (Map.Entry<String, Object> map : favArray.entrySet()) {
			String key = map.getKey();
			if (key.equalsIgnoreCase("filterProperty")) {
				JSONArray filterData = new JSONArray();
				JSONArray filterArray = new JSONArray();
				try {
					filterArray = objectMapper.readValue(map.getValue().toString(), JSONArray.class);

					String deviceType = null;
					boolean isDeviceTypeSet = false;
					for (int i = 0; i < filterArray.size(); i++) {
						try {
							String jsonString = objectMapper.writeValueAsString(filterArray.get(i));
							JSONObject jsonObject = (JSONObject) jSONParser.parse(jsonString);

							if (jsonObject.containsKey("name")
									&& jsonObject.get("name").toString().equalsIgnoreCase("category")) {
								deviceType = (String) jsonObject.get("selection");
							}

							if (jsonObject.containsKey("name")
									&& jsonObject.get("name").toString().equalsIgnoreCase("reportList")
									&& deviceType != null && !deviceType.trim().isEmpty()) {
								jsonObject.put("selection", deviceType);
								isDeviceTypeSet = true;
							}
							filterData.add(jsonObject);
						} catch (Exception e) {
							e.printStackTrace();
							StringWriter errors = new StringWriter();
							e.printStackTrace(new PrintWriter(errors));
							String ex = errors.toString();
							ExceptionHandlerMail.errorTriggerMail(ex);
						}
					}

					if (!isDeviceTypeSet) {
						for (int i = 0; i < resultArray.size(); i++) {
							try {
								String jsonString = objectMapper.writeValueAsString(resultArray.get(i));
								JSONObject jsonObject = (JSONObject) jSONParser.parse(jsonString);
								if (jsonObject.containsKey("name")
										&& jsonObject.get("name").toString().equalsIgnoreCase("reportList")
										&& deviceType != null && !deviceType.trim().isEmpty()) {
									jsonObject.put("selection", deviceType);
								}
								filterData.add(jsonObject);
							} catch (Exception e) {
								e.printStackTrace();
								StringWriter errors = new StringWriter();
								e.printStackTrace(new PrintWriter(errors));
								String ex = errors.toString();
								ExceptionHandlerMail.errorTriggerMail(ex);
							}
						}

					}

					resultArray.put(map.getKey(), filterData);
				} catch (Exception e1) {
					e1.printStackTrace();
				}

			} else {
				resultArray.put(map.getKey(), map.getValue());
			}
		}

		return resultArray;
	}

}
