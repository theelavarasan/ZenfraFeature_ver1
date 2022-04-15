package com.zenfra.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.zenfra.model.PureConfigModel;
import com.zenfra.model.Response;
import com.zenfra.service.PureConfigService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.DBUtils;

public class PureConfigDao implements PureConfigService {

	//Response response = new Response();
	CommonFunctions commonFunctions = new CommonFunctions();
	DBUtils dbUtils;

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response insertPureConfig(String userId, PureConfigModel model) {
		Response response = new Response();
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			JSONArray jsonArray = new JSONArray();
			
			model.setPureKeyConfigId(UUID.randomUUID().toString());
			model.setActive(true);
			model.setCreatedBy(userId);
			model.setUpdatedBy(userId);
			model.setCreatedTime(commonFunctions.getCurrentDateWithTime());
			model.setUpdatedTime(commonFunctions.getCurrentDateWithTime());
			
			System.out.println("!!!!! 1");
			System.out.println("!!!!! hostName: " + model.getHostName());
			System.out.println("!!!!! siteKey: " + model.getSiteKey());
			System.out.println("!!!!! tenantId: " + model.getTenantId());
			String insertQuery = "insert into pure_key_config(pure_key_config_id, host_name, site_key, tenant_id, is_active, created_by, updated_by, "
					+ "	created_time, updated_time, api_token, user_name, password, connection_type) VALUES ('" + model.getPureKeyConfigId()+ "', '"+ model.getHostName() + "', "
					+ "	'" + model.getSiteKey() + "', '"+model.getTenantId()+"', "+model.isActive()+", '"+model.getCreatedBy()+"', '"+model.getUpdatedBy()+"', '" + model.getCreatedTime()+ "',"
					+ " 	'"+ model.getUpdatedTime() + "', '"+model.getApiToken()+"', '"+model.getUserName()+"', '"+model.getPassword()+"', '"+model.getConnectionType()+"')";
			System.out.println("-----------------Insert Query Pure:" + insertQuery);
			statement.executeUpdate(insertQuery);
			jsonObject.put("pureKeyConfigId", model.getPureKeyConfigId());
			jsonObject.put("hostName", model.getHostName());
			jsonObject.put("siteKey", model.getSiteKey());
			jsonObject.put("tenantId", model.getTenantId());
			jsonObject.put("isActive", model.isActive());
			jsonObject.put("createdBy", model.getCreatedBy());
			jsonObject.put("updatedBy", model.getUpdatedBy());
			jsonObject.put("createdTime", model.getCreatedTime());
			jsonObject.put("updatedTime", model.getUpdatedTime());
			jsonObject.put("apiToken", model.getApiToken());
			jsonObject.put("userName", model.getUserName());
			jsonObject.put("password", model.getPassword());
			jsonObject.put("connectionType", model.getConnectionType());
//			jsonObject.put("publicKey", model.getPublicKey().length() > 10 ? model.getPublicKey().substring(model.getPublicKey().length() - 10, model.getPublicKey().length()) : model.getPublicKey());
//			jsonObject.put("privateKey", model.getPrivateKey().length() > 10 ? model.getPrivateKey().substring(model.getPrivateKey().length() - 10, model.getPrivateKey().length()) : model.getPrivateKey());
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
		Response response = new Response();
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		model.setActive(true);
		model.setUpdatedBy(userId);
		model.setUpdatedTime(commonFunctions.getCurrentDateWithTime());
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String updateQuery = "update pure_key_config set host_name='" + model.getHostName() + "',"
					+ "	is_active = "+model.isActive()+", tenant_id='" + model.getTenantId() + "', updated_by = '"+model.getUpdatedBy()+"', "
					+ "	updated_time='" + model.getUpdatedTime()+ "', api_token='"+model.getApiToken()+"',"
					+ "	user_name='"+model.getUserName()+"', password='"+model.getPassword()+"', connection_type='"+model.getConnectionType()+"'  where pure_key_config_id='" + pureKeyConfigId + "'";
			System.out.println("---------------------Update Query Pure:" + updateQuery);
			statement.executeUpdate(updateQuery);
			jsonObject.put("pureKeyConfigId", model.getPureKeyConfigId());
			jsonObject.put("hostName", model.getHostName());
			jsonObject.put("siteKey", model.getSiteKey());
			jsonObject.put("tenantId", model.getTenantId());
			jsonObject.put("isActive", model.isActive());
			jsonObject.put("createdBy", model.getCreatedBy());
			jsonObject.put("updatedBy", model.getUpdatedBy());
			jsonObject.put("createdTime", model.getCreatedTime());
			jsonObject.put("updatedTime", model.getUpdatedTime());
			jsonObject.put("apiToken", model.getApiToken());
			jsonObject.put("userName", model.getUserName());
			jsonObject.put("password", model.getPassword());
			jsonObject.put("connectionType", model.getConnectionType());
//			jsonObject.put("publicKey", model.getPublicKey().length() > 10 ? model.getPublicKey().substring(model.getPublicKey().length() - 10, model.getPublicKey().length()) : model.getPublicKey());
//			jsonObject.put("privateKey", model.getPrivateKey().length() > 10 ? model.getPrivateKey().substring(model.getPrivateKey().length() - 10, model.getPrivateKey().length()) : model.getPrivateKey());
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

//	@SuppressWarnings({ "unchecked", "static-access" })
//	@Override
//	public Response getPureConfig(String pureKeyConfigId) {
//		Response response = new Response();
//		Map<String, String> data = new HashMap<>();
//		data = dbUtils.getPostgres();
//		JSONObject jsonObject = new JSONObject();
//		JSONObject jsonObject1 = new JSONObject();
//		String getQuery = "select * from pure_key_config where pure_key_config_id='" + pureKeyConfigId + "'";
//		System.out.println("------------------Get Query Pure:" + getQuery);
//		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
//				data.get("password"));
//				Statement statement = connection.createStatement();
//				ResultSet rs = statement.executeQuery(getQuery);) {
//			while (rs.next()) {
//				jsonObject.put("arrayName", rs.getString("array_name"));
//				jsonObject.put("pureKeyConfigId", rs.getString("pure_key_config_id"));
//				jsonObject.put("applicationId", rs.getString("application_id"));
//				jsonObject.put("publicKey", rs.getString("public_key"));
//				jsonObject.put("privateKey", rs.getString("private_key"));
//				jsonObject.put("createdBy", rs.getString("created_by"));
//				jsonObject.put("updatedBy", rs.getString("updated_by"));
//				jsonObject.put("createdTime", rs.getString("created_time"));
//				jsonObject.put("updatedTime", rs.getString("updated_time"));
//			}
//			
//			String[] array = {"arrayName", "applicationId", "pureKeyConfigId", "createdBy", "updatedBy", "createdTime", "updatedTime"};
////			jsonObject1.put("\"columnOrder\"", "\n\"arrayName\", \n\"applicationId\", \n\"createdBy\", \n\"updatedBy\", \n\"createdTime\", \n\"updatedTime\"");
//			
//			JSONArray headerInfo = new JSONArray ();
//			JSONObject obj1 = new JSONObject ();
//			obj1.put("actualName", "arrayName");
//			obj1.put("hide", false);
//			obj1.put("displayName", "Array Name");
//			obj1.put("dataType", "String");
//			headerInfo.add(obj1);
//			JSONObject obj2 = new JSONObject ();
//			obj2.put("actualName", "applicationId");
//			obj2.put("hide", false);
//			obj2.put("displayName", "Application ID");
//			obj2.put("dataType", "String");
//			headerInfo.add(obj2);
//			JSONObject obj3 = new JSONObject ();
//			obj3.put("actualName", "publicKey");
//			obj3.put("hide", true);
//			obj3.put("displayName", "Public Key");
//			obj3.put("dataType", "String");
//			headerInfo.add(obj3);
//			JSONObject obj4 = new JSONObject ();
//			obj4.put("actualName", "privateKey");
//			obj4.put("hide", true);
//			obj4.put("displayName", "Private Key");
//			obj4.put("dataType", "String");
//			headerInfo.add(obj4);
//			JSONObject obj5 = new JSONObject ();
//			obj5.put("actualName", "createdBy");
//			obj5.put("hide", false);
//			obj5.put("displayName", "Created By");
//			obj5.put("dataType", "String");
//			headerInfo.add(obj5);
//			JSONObject obj6 = new JSONObject ();
//			obj6.put("actualName", "updatedBy");
//			obj6.put("hide", false);
//			obj6.put("displayName", "Updated By");
//			obj6.put("dataType", "String");
//			headerInfo.add(obj6);
//			JSONObject obj7 = new JSONObject ();
//			obj7.put("actualName", "createdTime");
//			obj7.put("hide", false);
//			obj7.put("displayName", "Created Time");
//			obj7.put("dataType", "date");
//			headerInfo.add(obj7);
//			JSONObject obj8= new JSONObject ();
//			obj8.put("actualName", "updatedTime");
//			obj8.put("hide", false);
//			obj8.put("displayName", "Updated Time");
//			obj8.put("dataType", "date");
//			headerInfo.add(obj8);
//			JSONArray jsonArray = new JSONArray();
//			jsonObject1.put("columnOrder", array);
//			jsonArray.add(jsonObject);
//			jsonObject1.put("data", jsonArray);
//			jsonObject1.put("headerInfo", headerInfo);
//			response.setResponseCode(200);
//			response.setResponseMsg("success");
//			response.setjData(jsonObject1);
//		} catch (Exception e) {
//			response.setResponseCode(500);
//			response.setResponseMsg("failure");
//			e.printStackTrace();
//		}
//		return response;
//	}

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response listPureConfig(String siteKey) {
		Response response = new Response();
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		
		JSONObject jsonObject1 = new JSONObject();
		JSONArray resultArray = new JSONArray();
		String listQuery = "select pure_key_config_id, host_name, trim(concat(trim(ut1.first_name), ' ', trim(coalesce(ut1.last_name, '')))) as created_by, \r\n" + 
				"trim(concat(trim(ut2.first_name), ' ', trim(coalesce(ut2.last_name, '')))) as updated_by, \r\n" + 
				"to_char(to_timestamp(pc.created_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as created_time, \r\n" + 
				"to_char(to_timestamp(pc.updated_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as updated_time, pc.api_token, pc.user_name, pc.password, pc.connection_type from pure_key_config pc\r\n" + 
				"LEFT JOIN user_temp ut1 on ut1.user_id = pc.created_by\r\n" + 
				"LEFT JOIN user_temp ut2 on ut2.user_id = pc.updated_by\r\n" + 
				"where pc.is_active=true and pc.site_key = '" + siteKey + "'";
		System.out.println("-----------------List Query Pure:" + listQuery);
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password"));
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(listQuery);) {
			while (rs.next()) {
				JSONObject jsonObject = new JSONObject();
//				jsonObject.put("arrayName", rs.getString("array_name"));
				jsonObject.put("pureKeyConfigId", rs.getString("pure_key_config_id"));
				jsonObject.put("hostName", rs.getString("host_name"));	
				jsonObject.put("createdBy", rs.getString("created_by"));
				jsonObject.put("updatedBy", rs.getString("updated_by"));
				jsonObject.put("createdTime", rs.getString("created_time"));
				jsonObject.put("updatedTime", rs.getString("updated_time"));
				jsonObject.put("apiToken", rs.getString("api_token"));
				jsonObject.put("userName", rs.getString("user_name"));
				jsonObject.put("password", rs.getString("password"));
				jsonObject.put("connectionType", rs.getString("connection_type"));
				resultArray.add(jsonObject);
//				jsonObject.put("applicationId", rs.getString("application_id"));
//				jsonObject.put("publicKey", rs.getString("public_key").length() > 10 ? rs.getString("public_key").substring(rs.getString("public_key").length() - 10, rs.getString("public_key").length()) : rs.getString("public_key"));
//				jsonObject.put("privateKey", rs.getString("private_key").length() > 10 ? rs.getString("private_key").substring(rs.getString("private_key").length() - 10, rs.getString("private_key").length()) : rs.getString("private_key"));
			}
			String[] array = {"hostName", "connectionType", "apiToken", "userName", "password", "pureKeyConfigId", "createdBy", "updatedBy", "createdTime", "updatedTime"};
			
			JSONArray headerInfo = new JSONArray ();
			JSONObject obj1 = new JSONObject ();
			obj1.put("actualName", "hostName");
			obj1.put("dataType", "string");
			obj1.put("displayName", "Host Name");
			obj1.put("hide", false);
			headerInfo.add(obj1);
			JSONObject obj9 = new JSONObject ();
			obj9.put("actualName", "connectionType");
			obj9.put("displayName", "Connection Type");
			obj9.put("dataType", "select");
			obj9.put("hide", false);
			JSONArray option = new JSONArray();
			JSONObject optObject1 = new JSONObject();
			optObject1.put("label", "Using API Token");
			optObject1.put("value", "apiToken");
			JSONObject optObject2 = new JSONObject();
			optObject2.put("label", "Using Credentials");
			optObject2.put("value", "usingCredentials");
			option.add(optObject1);
			option.add(optObject2);
			obj9.put("option", option);
			headerInfo.add(obj9);
			JSONObject obj2 = new JSONObject ();
			obj2.put("actualName", "apiToken");
			obj2.put("dataType", "string");
			obj2.put("displayName", "API Token");
			obj2.put("hide", false);
			headerInfo.add(obj2);
			JSONObject obj3 = new JSONObject ();
			obj3.put("actualName", "userName");
			obj3.put("dataType", "string");
			obj3.put("displayName", "User Name");
			obj3.put("hide", false);
			headerInfo.add(obj3);
			JSONObject obj4 = new JSONObject ();
			obj4.put("actualName", "password");
			obj4.put("dataType", "password");
			obj4.put("displayName", "Password");
			obj4.put("hide", false);
			headerInfo.add(obj4);
			JSONObject obj5 = new JSONObject ();
			obj5.put("actualName", "createdBy");
			obj5.put("dataType", "string");
			obj5.put("displayName", "Created By");
			obj5.put("hide", false);
			headerInfo.add(obj5);
			JSONObject obj7 = new JSONObject ();
			obj7.put("actualName", "createdTime");
			obj7.put("dataType", "date");
			obj7.put("displayName", "Created Time");
			obj7.put("hide", false);
			headerInfo.add(obj7);
			JSONObject obj6 = new JSONObject ();
			obj6.put("actualName", "updatedBy");
			obj6.put("dataType", "string");
			obj6.put("displayName", "Updated By");
			obj6.put("hide", false);
			headerInfo.add(obj6);
			JSONObject obj8= new JSONObject ();
			obj8.put("actualName", "updatedTime");
			obj8.put("dataType", "date");
			obj8.put("displayName", "Updated Time");
			obj8.put("hide", false);
			headerInfo.add(obj8);
			//JSONArray jsonArray = new JSONArray();
			jsonObject1.put("columnOrder", array);
			//jsonArray.add(resultArray);
			jsonObject1.put("data", resultArray);
			jsonObject1.put("headerInfo", headerInfo);
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonObject1);
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
		Response response = new Response();
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

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response getPureKeyList(String siteKey) {
		Response response = new Response();
		
		
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		String query = "select pure_key_config_id, host_name from pure_key_config where is_active = true and site_key = '" + siteKey + "'";
		System.out.println("!!!!! pure key list query: " + query);
		try(Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(query)) {
			
			while(rs.next()) {
				jsonObject.put(rs.getString("pure_key_config_id"), rs.getString("host_name"));
			}
			System.out.println("!!!!! jsonObject: " + jsonObject);
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonObject);
			
		} catch(Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

}
