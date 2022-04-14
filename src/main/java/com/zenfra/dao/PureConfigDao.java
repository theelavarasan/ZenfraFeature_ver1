package com.zenfra.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.zenfra.model.PureConfigModel;
import com.zenfra.model.Response;
import com.zenfra.service.PureConfigService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.DBUtils;

public class PureConfigDao implements PureConfigService {

	Response response = new Response();
	CommonFunctions commonFunctions = new CommonFunctions();
	DBUtils dbUtils;

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response insertPureConfig(String userId, PureConfigModel model) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			JSONArray jsonArray = new JSONArray();
			int id = model.getPureKeyConfigId();
			id = 0;
			System.out.println("!!!!! 1");
			System.out.println("!!!!! name: " + model.getArrayName());
			System.out.println("!!!!! siteKey: " + model.getSiteKey());
			System.out.println("!!!!! tenantId: " + model.getTenantId());
			String insertQuery = "insert into pure_key_config(pure_key_config_id, array_name, application_id,  public_key, private_key, site_key, tenant_id, is_active, created_by, updated_by, "
					+ "	created_time, updated_time) VALUES ('" +id+++ "', '"+ model.getArrayName() + "', '"+model.getApplicationId()+"', '" + model.getPublicKey() + "', '" + model.getPrivateKey() + "', "
					+ "	'" + model.getSiteKey() + "', '"+model.getTenantId()+"', true, (select concat(first_name, ' ',last_name) as created_by from user_temp where user_id='"+userId+"'), "
					+ "	(select concat(first_name, ' ',last_name) as created_by from user_temp where user_id='"+userId+"'), '" + commonFunctions.getCurrentDateWithTime() + "',"
					+ " 	'"+ commonFunctions.getCurrentDateWithTime() + "')";
			System.out.println("-----------------Insert Query Pure:" + insertQuery);
			statement.executeUpdate(insertQuery);
			jsonObject.put("pureKeyConfigId", model.getPureKeyConfigId());
			jsonObject.put("arrayName", model.getArrayName());
			jsonObject.put("applicationId", model.getApplicationId());
			jsonObject.put("siteKey", model.getSiteKey());
			jsonObject.put("tenantId", model.getTenantId());
			jsonObject.put("isActive", "true");
			jsonObject.put("createdBy", model.getCreatedBy());
			jsonObject.put("updatedBy", model.getUpdatedBy());
			jsonObject.put("createdTime", model.getCreatedTime());
			jsonObject.put("updatedTime", model.getUpdatedTime());
			jsonArray.add(jsonObject);
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonArray);
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response updatePureConfig(String userId, PureConfigModel model, String pureKeyConfigId) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		String name = "(select concat(first_name, ' ',last_name) as created_by from user_temp where user_id='"+userId+"')";
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String updateQuery = "update pure_key_config set array_name='" + model.getArrayName() + "', application_id='"+model.getApplicationId()+"', public_key='" + model.getPublicKey() + "', private_key='" + model.getPrivateKey() + "',"
					+ "is_active = true, tenant_id='" + model.getTenantId() + "', updated_by ="+name+", "
					+ "updated_time='" + commonFunctions.getCurrentDateWithTime() + "' where pure_key_config_id='" + pureKeyConfigId + "'";
			System.out.println("---------------------Update Query Pure:" + updateQuery);
			statement.executeUpdate(updateQuery);
			jsonObject.put("pureKeyConfigId", model.getPureKeyConfigId());
			jsonObject.put("arrayName", model.getArrayName());
			jsonObject.put("applicationId", model.getApplicationId());
			jsonObject.put("siteKey", model.getSiteKey());
			jsonObject.put("tenantId", model.getTenantId());
			jsonObject.put("isActive", "true");
			jsonObject.put("updatedBy", "name");
			jsonObject.put("updatedTime", commonFunctions.getCurrentDateWithTime());
			jsonArray.add(jsonObject);
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonArray);
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response getPureConfig(String pureKeyConfigId) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonObject1 = new JSONObject();
		String getQuery = "select * from pure_key_config where pure_key_config_id='" + pureKeyConfigId + "'";
		System.out.println("------------------Get Query Pure:" + getQuery);
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password"));
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(getQuery);) {
			while (rs.next()) {
				jsonObject.put("arrayName", rs.getString("array_name"));
				jsonObject.put("applicationId", rs.getString("application_id"));
				jsonObject.put("publicKey", rs.getString("public_key"));
				jsonObject.put("privateKey", rs.getString("private_key"));
				jsonObject.put("createdBy", rs.getString("created_by"));
				jsonObject.put("updatedBy", rs.getString("updated_by"));
				jsonObject.put("createdTime", rs.getString("created_time"));
				jsonObject.put("updatedTime", rs.getString("updated_time"));
			}
			
//			jsonObject1.put("\"columnOrder\"", "\n\"arrayName\", \n\"applicationId\", \n\"createdBy\", \n\"updatedBy\", \n\"createdTime\", \n\"updatedTime\"");
			jsonObject1.put("columnOrder", "arrayName, applicationId, createdBy, updatedBy, createdTime, updatedTime");
			
			JSONArray headerInfo = new JSONArray ();
			JSONObject obj1 = new JSONObject ();
			obj1.put("actualName", "arrayName");
			obj1.put("hide", false);
			obj1.put("displayName", "Array Name");
			obj1.put("dataType", "String");
			headerInfo.add(obj1);
			JSONObject obj2 = new JSONObject ();
			obj2.put("actualName", "applicationId");
			obj2.put("hide", false);
			obj2.put("displayName", "Application ID");
			obj2.put("dataType", "String");
			headerInfo.add(obj2);
			JSONObject obj3 = new JSONObject ();
			obj3.put("actualName", "publicKey");
			obj3.put("hide", false);
			obj3.put("displayName", "Public Key");
			obj3.put("dataType", "String");
			headerInfo.add(obj3);
			JSONObject obj4 = new JSONObject ();
			obj4.put("actualName", "privateKey");
			obj4.put("hide", false);
			obj4.put("displayName", "Private Key");
			obj4.put("dataType", "String");
			headerInfo.add(obj4);
			JSONObject obj5 = new JSONObject ();
			obj5.put("actualName", "createdBy");
			obj5.put("hide", false);
			obj5.put("displayName", "Created By");
			obj5.put("dataType", "String");
			headerInfo.add(obj5);
			JSONObject obj6 = new JSONObject ();
			obj6.put("actualName", "updatedBy");
			obj6.put("hide", false);
			obj6.put("displayName", "Updated By");
			obj6.put("dataType", "String");
			headerInfo.add(obj6);
			JSONObject obj7 = new JSONObject ();
			obj7.put("actualName", "createdTime");
			obj7.put("hide", false);
			obj7.put("displayName", "Created Time");
			obj7.put("dataType", "date");
			headerInfo.add(obj7);
			JSONObject obj8= new JSONObject ();
			obj8.put("actualName", "updatedTime");
			obj8.put("hide", false);
			obj8.put("displayName", "Updated Time");
			obj8.put("dataType", "date");
			headerInfo.add(obj8);
			JSONArray jsonArray = new JSONArray();
			jsonArray.add(jsonObject1);
			jsonArray.add(headerInfo);
			jsonArray.add(jsonObject);
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonArray);
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response listPureConfig() {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonObject1 = new JSONObject();
		String listQuery = "select * from pure_key_config where is_active=true";
		System.out.println("-----------------List Query Pure:" + listQuery);
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password"));
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(listQuery);) {
			while (rs.next()) {
				jsonObject.put("arrayName", rs.getString("array_name"));
				jsonObject.put("applicationId", rs.getString("application_id"));
				jsonObject.put("publicKey", rs.getString("public_key"));
				jsonObject.put("privateKey", rs.getString("private_key"));
				jsonObject.put("createdBy", rs.getString("created_by"));
				jsonObject.put("updatedBy", rs.getString("updated_by"));
				jsonObject.put("createdTime", rs.getString("created_time"));
				jsonObject.put("updatedTime", rs.getString("updated_time"));
			}
	jsonObject1.put("columnOrder", "arrayName, applicationId, createdBy, updatedBy, createdTime, updatedTime");
			
			JSONArray headerInfo = new JSONArray ();
			JSONObject obj1 = new JSONObject ();
			obj1.put("actualName", "arrayName");
			obj1.put("hide", false);
			obj1.put("displayName", "Array Name");
			obj1.put("dataType", "String");
			headerInfo.add(obj1);
			JSONObject obj2 = new JSONObject ();
			obj2.put("actualName", "applicationId");
			obj2.put("hide", false);
			obj2.put("displayName", "Application ID");
			obj2.put("dataType", "String");
			headerInfo.add(obj2);
			JSONObject obj3 = new JSONObject ();
			obj3.put("actualName", "publicKey");
			obj3.put("hide", false);
			obj3.put("displayName", "Public Key");
			obj3.put("dataType", "String");
			headerInfo.add(obj3);
			JSONObject obj4 = new JSONObject ();
			obj4.put("actualName", "privateKey");
			obj4.put("hide", false);
			obj4.put("displayName", "Private Key");
			obj4.put("dataType", "String");
			headerInfo.add(obj4);
			JSONObject obj5 = new JSONObject ();
			obj5.put("actualName", "createdBy");
			obj5.put("hide", false);
			obj5.put("displayName", "Created By");
			obj5.put("dataType", "String");
			headerInfo.add(obj5);
			JSONObject obj6 = new JSONObject ();
			obj6.put("actualName", "updatedBy");
			obj6.put("hide", false);
			obj6.put("displayName", "Updated By");
			obj6.put("dataType", "String");
			headerInfo.add(obj6);
			JSONObject obj7 = new JSONObject ();
			obj7.put("actualName", "createdTime");
			obj7.put("hide", false);
			obj7.put("displayName", "Created Time");
			obj7.put("dataType", "date");
			headerInfo.add(obj7);
			JSONObject obj8= new JSONObject ();
			obj8.put("actualName", "updatedTime");
			obj8.put("hide", false);
			obj8.put("displayName", "Updated Time");
			obj8.put("dataType", "date");
			headerInfo.add(obj8);
			JSONArray jsonArray = new JSONArray();
			jsonArray.add(jsonObject1);
			jsonArray.add(headerInfo);
			jsonArray.add(jsonObject);
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonArray);
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings("static-access")
	@Override
	public Response deletePureConfig(String pureKeyConfigId) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String deleteQuery = "update pure_key_config set is_active='false' where pure_key_config_id='"+pureKeyConfigId+"'";
//			String deleteQuery = "delete from pure_key_config where pure_key_config_id ='" + pureKeyConfigId + "'";
			System.out.println("-----------------Delete Query Pure:" + deleteQuery);
			statement.executeUpdate(deleteQuery);
			response.setResponseCode(200);
			response.setResponseMsg("success");
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

}