package com.zenfra.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
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
	public Response insertPureConfig(PureConfigModel model) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String insertQuery = "insert into pure_key_config(pure_key_config_id, array_name, public_key, private_key, site_key, tenant_id, is_active, created_by, updated_by, "
					+ "	created_time, updated_time) VALUES ('" + commonFunctions.generateRandomId() + "', '"+ model.getArrayName() + "', '" + model.getPublicKey() + "', '" + model.getPrivateKey() + "', "
					+ "	'" + model.getSiteKey() + "','" + model.getTenantId() + "', true, '" + model.getCreatedBy() + "', '" + model.getUpdatedBy() + "', " + "	'" + commonFunctions.getCurrentDateWithTime() + "',"
					+ " 	'"+ commonFunctions.getCurrentDateWithTime() + "')";
			System.out.println("-----------------Insert Query Pure:" + insertQuery);
			statement.executeUpdate(insertQuery);
			jsonObject.put("arrayName", model.getArrayName());
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonObject);
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response updatePureConfig(PureConfigModel model, String pureKeyConfigId) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String updateQuery = "update pure_key_config set array_name='" + model.getArrayName() + "', public_key='" + model.getPublicKey() + "', private_key='" + model.getPrivateKey() + "',"
					+ "is_active = true, tenant_id='" + model.getTenantId() + "' updated_by='" + model.getUpdatedBy() + "', updated_time='" + model.getUpdatedTime() + "' where pure_key_config_id='" + pureKeyConfigId + "'";
			System.out.println("---------------------Update Query Pure:" + updateQuery);
			statement.executeUpdate(updateQuery);
			jsonObject.put("arrayName", model.getArrayName());
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonObject);
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
		String getQuery = "select * from pure_key_config where pure_key_config_id='" + pureKeyConfigId + "'";
		System.out.println("------------------Get Query Pure:" + getQuery);
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password"));
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(getQuery);) {
			while (rs.next()) {
				jsonObject.put("id", rs.getString("pure_key_config_id"));
			}
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonObject);
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	@Override
	public Response listPureConfig(String pureKeyConfigId) {
		Map<String, String> data = new HashMap<>();
		data = dbUtils.getPostgres();
		JSONObject jsonObject = new JSONObject();
		String listQuery = "select * from pure_key_config where pure_key_config_id='" + pureKeyConfigId + "' and is_active=true";
		System.out.println("-----------------List Query Pure:" + listQuery);
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password"));
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(listQuery);) {
			while (rs.next()) {
				jsonObject.put("id", rs.getString("pure_key_config_id"));
			}
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(jsonObject);
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
//			String deleteQuery = "update is_active='false' where pure_key_config_id='"+pureKeyConfigId+"'";
			String deleteQuery = "delete from pure_key_config where pure_key_config_id ='" + pureKeyConfigId + "'";
			System.out.println("-----------------Delete Query Pure:" + deleteQuery);
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
