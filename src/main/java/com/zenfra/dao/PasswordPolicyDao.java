package com.zenfra.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONObject;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;
import com.zenfra.service.PasswordPolicyService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.DBUtils;

public class PasswordPolicyDao implements PasswordPolicyService {

	DBUtils dbUtils;

	CommonFunctions commonFunctions = new CommonFunctions();

	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@Override
	public Response createPwdPolicy(String userId, PasswordPolicyModel model) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			model.setPwdPolicyId(UUID.randomUUID().toString());
			model.setCreatedBy(userId);
			model.setUpdatedBy(userId);
			model.setCreatedTime(commonFunctions.getCurrentDateWithTime());
			model.setUpdatedTime(commonFunctions.getCurrentDateWithTime());

			System.out.println("!!!!!!!!!!Password Policy PK:" + model.getPwdPolicyId());
			System.out.println("!!!!!!!!!Password Policy Expire:" + model.getPwdExpire());
			System.out.println("!!!!!!!!!Password Policy Lock:" + model.getPwdLock());
			System.out.println("!!!!!!!!!Password Policy Length:" + model.getPwdLength());
			System.out.println("!!!!!!!!!Password Policy AlphaUpper:	" + model.isAlphaUpper());
			System.out.println("!!!!!!!!!Password Policy AlphaLower:	" + model.isAlphaLower());
			System.out.println("!!!!!!!!!Password Policy NonAlphaNumeric:" + model.isNonAlphaNumeric());
			System.out.println("!!!!!!!!!Password Policy Numbers:	" + model.isNumbers());
			System.out.println("!!!!!!!!!Password Policy NonFnIn:" + model.isNonFnIn());

			String createQuery = "insert into password_policy(pwd_policy_id, pwd_length, pwd_expire, pwd_lock, no_of_existing_pwd, alpha_upper, alpha_lower, numbers, non_alpha_numeric, is_non_fn_ln, created_by,"
					+ "	updated_by, created_time, updated_time)\r\n" + "	values('" + model.getPwdPolicyId() + "', "
					+ model.getPwdLength() + ", " + model.getPwdExpire() + "	, " + model.getPwdLock() + "," + " 	"
					+ model.getNoOfExistingPwd() + ", " + "	" + model.isAlphaUpper() + "	, " + model.isAlphaLower()
					+ ", " + model.isNumbers() + ", " + model.isNonAlphaNumeric() + ", " + model.isNonFnIn() + ", "
					+ "	 '" + model.getCreatedBy() + "',  '" + model.getUpdatedBy() + "', '" + model.getCreatedTime()
					+ "', '" + model.getUpdatedTime() + "')";
			System.out.println("----------------------Password Policy Create Query:" + createQuery);
			statement.executeUpdate(createQuery);

			jsonObject.put("pwdPolicyId", model.getPwdPolicyId());
			jsonObject.put("pwdLength", model.getPwdLength());
			jsonObject.put("pwdExpire", model.getPwdExpire());
			jsonObject.put("pwdLock", model.getPwdLock());
			jsonObject.put("noOfExistingPwd", model.getNoOfExistingPwd());
			jsonObject.put("alphaUpper", model.isAlphaUpper());
			jsonObject.put("alphaLower", model.isAlphaLower());
			jsonObject.put("numbers", model.isNumbers());
			jsonObject.put("nonAlphaNumeric", model.isNonAlphaNumeric());
			jsonObject.put("nonFnIn", model.isNonFnIn());
			jsonObject.put("createdBy", model.getCreatedBy());
			jsonObject.put("updatedBy", model.getUpdatedBy());
			jsonObject.put("createdTime", model.getCreatedTime());
			jsonObject.put("updatedTime", model.getUpdatedTime());

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

	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@Override
	public Response updatePwdPolicy(String userId, String pwdPolicyId, PasswordPolicyModel model) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			model.setPwdPolicyId(pwdPolicyId);
			model.setUpdatedBy(userId);
			model.setUpdatedTime(commonFunctions.getCurrentDateWithTime().toString());
			String updateQuery = "update password_policy set pwd_length=" + model.getPwdLength() + ", pwd_expire="
					+ model.getPwdExpire() + ", pwd_lock=" + model.getPwdLock() + "," + "	no_of_existing_pwd="
					+ model.getNoOfExistingPwd() + ", alpha_upper=" + model.isAlphaUpper() + ", alpha_lower="
					+ model.isAlphaLower() + ", numbers=" + model.isNumbers() + "," + "	non_alpha_numeric="
					+ model.isNonAlphaNumeric() + ", is_non_fn_ln=" + model.isNonFnIn() + ", updated_by='"
					+ model.getUpdatedBy() + "', updated_time='" + model.getUpdatedTime() + "'"
					+ "	where pwd_policy_id='" + model.getPwdPolicyId() + "'";
			System.out.println("----------------------Password Policy Update Query:" + updateQuery);
			statement.executeUpdate(updateQuery);

			jsonObject.put("pwdPolicyId", model.getPwdPolicyId());
			jsonObject.put("pwdLength", model.getPwdLength());
			jsonObject.put("pwdExpire", model.getPwdExpire());
			jsonObject.put("pwdLock", model.getPwdLock());
			jsonObject.put("noOfExistingPwd", model.getNoOfExistingPwd());
			jsonObject.put("alphaUpper", model.isAlphaUpper());
			jsonObject.put("alphaLower", model.isAlphaLower());
			jsonObject.put("numbers", model.isNumbers());
			jsonObject.put("nonAlphaNumeric", model.isNonAlphaNumeric());
			jsonObject.put("nonFnIn", model.isNonFnIn());
			jsonObject.put("updatedBy", model.getUpdatedBy());
			jsonObject.put("updatedTime", model.getUpdatedTime());
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

	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@Override
	public Response listPwdPolicy(String pwdPolicyId) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {

			String listQuery = "SELECT pwd_policy_id, pwd_length, pwd_expire, pwd_lock, no_of_existing_pwd, alpha_upper, alpha_lower, numbers, non_alpha_numeric, is_non_fn_ln, \r\n"
					+ "trim(concat(trim(ut1.first_name), ' ', trim(coalesce(ut1.last_name, '')))) created_by, \r\n"
					+ "trim(concat(trim(ut1.first_name), ' ', trim(coalesce(ut1.last_name, '')))) updated_by, \r\n"
					+ "to_char(to_timestamp(pp.created_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') created_time, \r\n"
					+ "to_char(to_timestamp(pp.updated_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS')updated_time from password_policy pp\r\n"
					+ "LEFT JOIN user_temp ut1 on ut1.user_id = pp.created_by \r\n"
					+ "LEFT JOIN user_temp ut2 on ut2.user_id = pp.updated_by \r\n"
					+ "where pp.is_active=true and pp.pwd_policy_id = '" + pwdPolicyId + "'";
			ResultSet rs = statement.executeQuery(listQuery);
			while (rs.next()) {
				jsonObject.put("pwdPolicyId", rs.getString("pwd_policy_id"));
				jsonObject.put("pwdLength", rs.getString("pwd_length"));
				jsonObject.put("pwdExpire", rs.getString("pwd_expire"));
				jsonObject.put("pwdLock", rs.getString("pwd_lock"));
				jsonObject.put("noOfExistingPwd", rs.getString("no_of_existing_pwd"));
				jsonObject.put("alphaUpper", rs.getString("alpha_upper"));
				jsonObject.put("alphaLower", rs.getString("alpha_lower"));
				jsonObject.put("numbers", rs.getString("numbers"));
				jsonObject.put("nonAlphaNumeric", rs.getString("non_alpha_numeric"));
				jsonObject.put("nonFnIn", rs.getString("is_non_fn_ln"));
				jsonObject.put("createdBy", rs.getString("created_by"));
				jsonObject.put("updatedBy", rs.getString("updated_by"));
				jsonObject.put("createdTime", rs.getString("created_time"));
				jsonObject.put("updatedTime", rs.getString("updated_time"));
				response.setjData(jsonObject);
			}
			response.setResponseCode(200);
			response.setResponseMsg("success");
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@Override
	public Response deletePwdPolicy(String pwdPolicyId) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String deleteQuery = "update password_policy set is_active='false' where pwd_policy_id='" + pwdPolicyId
					+ "'";
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
