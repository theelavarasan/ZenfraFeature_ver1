package com.zenfra.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;
import com.zenfra.service.PasswordPolicyService;
import com.zenfra.utils.DBUtils;

public class PasswordPolicyDao implements PasswordPolicyService {

	@Autowired
	DBUtils dbUtils;

	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@Override
	public Response createPwdPolicy(PasswordPolicyModel model) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			model.setPwdPolicyId(UUID.randomUUID().toString());
			System.out.println("!!!!!!!!!!Password Policy PK:" + model.getPwdPolicyId());
			System.out.println("!!!!!!!!!Password Policy Expire:" + model.getPwdExpire());
			System.out.println("!!!!!!!!!Password Policy Lock:" + model.getPwdLock());
			System.out.println("!!!!!!!!!Password Policy Length:" + model.getPwdLength());
			System.out.println("!!!!!!!!!Password Policy AlphaUpper:	" + model.isAlphaUpper());
			System.out.println("!!!!!!!!!Password Policy AlphaLower:	" + model.isAlphaLower());
			System.out.println("!!!!!!!!!Password Policy NonAlphaNumeric:" + model.isNonAlphaNumeric());
			System.out.println("!!!!!!!!!Password Policy Numbers:	" + model.isNumbers());
			System.out.println("!!!!!!!!!Password Policy NonFnIn:" + model.isNonFnIn());

			String createQuery = "insert into password_policy(pwd_policy_id, pwd_length, pwd_expire, pwd_lock, no_of_existing_pwd, alpha_upper, alpha_lower, numbers, non_alpha_numeric, is_non_fn_ln)\r\n"
					+ "	values('" + model.getPwdPolicyId() + "', " + model.getPwdLength() + ", " + model.getPwdExpire()
					+ ", " + model.getPwdLock() + ", " + model.getNoOfExistingPwd() + ", " + "	" + model.isAlphaUpper()
					+ ", " + model.isAlphaLower() + ", " + model.isNumbers() + ", " + model.isNonAlphaNumeric() + ", "
					+ model.isNonFnIn() + ")";
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

}
