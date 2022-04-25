package com.zenfra.dao;

import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;
import com.zenfra.service.PasswordPolicyService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.DBUtils;

public class PasswordPolicyDao implements PasswordPolicyService {

	DBUtils dbUtils;

	Cipher cipher;  
	
	CommonFunctions commonFunctions = new CommonFunctions();

	/*
	 * @SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	 * 
	 * @Override public Response createPwdPolicy(String userId, PasswordPolicyModel
	 * model, List<ArrayList<String>>value) { Map<String, String> data = new
	 * HashMap(); data = dbUtils.getPostgres(); Response response = new Response();
	 * JSONObject jsonObject = new JSONObject(); try (Connection connection =
	 * DriverManager.getConnection(data.get("url"), data.get("userName"),
	 * data.get("password")); Statement statement = connection.createStatement();) {
	 * model.setPwdPolicyId(UUID.randomUUID().toString());
	 * model.setCreatedBy(userId); model.setUpdatedBy(userId);
	 * model.setCreatedTime(commonFunctions.getCurrentDateWithTime());
	 * model.setUpdatedTime(commonFunctions.getCurrentDateWithTime());
	 * model.setActive(true);
	 * 
	 * System.out.println("!!!!!!!!!!Password Policy PK:" + model.getPwdPolicyId());
	 * System.out.println("!!!!!!!!!Password Policy Expire:" +model.getPwdExpire());
	 * System.out.println("!!!!!!!!!Password Policy Lock:" +model.getPwdLock());
	 * System.out.println("!!!!!!!!!Password Policy Length:" +
	 * model.getPwdLength());
	 * System.out.println("!!!!!!!!!Password Policy AlphaUpper:	"
	 * +model.isAlphaUpper());
	 * System.out.println("!!!!!!!!!Password Policy AlphaLower:	" +
	 * model.isAlphaLower());
	 * System.out.println("!!!!!!!!!Password Policy NonAlphaNumeric:"
	 * +model.isNonAlphaNumeric());
	 * System.out.println("!!!!!!!!!Password Policy Numbers:	" +
	 * model.isNumbers()); System.out.println("!!!!!!!!!Password Policy NonFnIn:"
	 * +model.isNonFnIn());
	 * 
	 * 
	 * String result = null;
	 * 
	 * if (value.size() == 1) { result = ""+value.get(0).toString()+""; result =
	 * result.replace("[", "").replace("]", ""); System.out.println("-------One:"
	 * +result); model.setPassRegexFormat(result); }else if (value.size() == 2) {
	 * result = ""+value.get(0).toString()+"_"+value.get(1).toString()+""; result =
	 * result.replace("[", "").replace("]", ""); System.out.println("-------Two:"
	 * +result); model.setPassRegexFormat(result); }else if (value.size() == 3) {
	 * result =
	 * ""+value.get(0).toString()+"_"+value.get(1).toString()+"_"+value.get(2).
	 * toString()+""; result = result.replace("[", "").replace("]", "");
	 * System.out.println("-------Three:" +result);
	 * model.setPassRegexFormat(result); }else if (value.size() == 4) { result =
	 * ""+value.get(0).toString()+"_"+value.get(1).toString()+"_"+value.get(2).
	 * toString()+"_"+value.get(3).toString()+""; result = result.replace("[",
	 * "").replace("]", ""); System.out.println("-------Four:" +result);
	 * model.setPassRegexFormat(result); }
	 * 
	 * String createQuery =
	 * "insert into password_policy(pwd_policy_id, pwd_length, pwd_expire, pwd_lock, no_of_existing_pwd, alpha_upper, alpha_lower, numbers, non_alpha_numeric, is_non_fn_ln, created_by,"
	 * +
	 * "	updated_by, created_time, updated_time, is_active, pass_regex_format, site_key, tenant_id)\r\n"
	 * + "	values('" + model.getPwdPolicyId() + "', " + model.getPwdLength() + ", "
	 * + model.getPwdExpire() + "	, " + model.getPwdLock() + "," + " 	" +
	 * model.getNoOfExistingPwd() + ", " + "	" + model.isAlphaUpper() + "	, "
	 * + model.isAlphaLower() + ", " + model.isNumbers() + ", " +
	 * model.isNonAlphaNumeric() + ", " + model.isNonFnIn() + ", " + "	 '" +
	 * model.getCreatedBy() + "',  '" + model.getUpdatedBy() + "', '" +
	 * model.getCreatedTime() + "', '" + model.getUpdatedTime() +
	 * "', "+model.isActive()+", '"+model.getPassRegexFormat()+"', '"+model.
	 * getSiteKey()+"', '"+model.getTenantId()+"')";
	 * System.out.println("----------------------Password Policy Create Query:" +
	 * createQuery); statement.executeUpdate(createQuery);
	 * 
	 * jsonObject.put("pwdPolicyId", model.getPwdPolicyId());
	 * jsonObject.put("pwdLength", model.getPwdLength());
	 * jsonObject.put("pwdExpire", model.getPwdExpire()); jsonObject.put("pwdLock",
	 * model.getPwdLock()); jsonObject.put("noOfExistingPwd",
	 * model.getNoOfExistingPwd()); jsonObject.put("alphaUpper",
	 * model.isAlphaUpper()); jsonObject.put("alphaLower", model.isAlphaLower());
	 * jsonObject.put("numbers", model.isNumbers());
	 * jsonObject.put("nonAlphaNumeric", model.isNonAlphaNumeric());
	 * jsonObject.put("nonFnIn", model.isNonFnIn()); jsonObject.put("createdBy",
	 * model.getCreatedBy()); jsonObject.put("updatedBy", model.getUpdatedBy());
	 * jsonObject.put("createdTime", model.getCreatedTime());
	 * jsonObject.put("updatedTime", model.getUpdatedTime());
	 * jsonObject.put("isActive", model.isActive());
	 * jsonObject.put("passRegexFormat", model.getPassRegexFormat());
	 * jsonObject.put("siteKey", model.getSiteKey()); jsonObject.put("tenantId",
	 * model.getTenantId());
	 * 
	 * 
	 * response.setResponseCode(200); response.setResponseMsg("success");
	 * response.setjData(jsonObject); } catch (Exception e) {
	 * response.setResponseCode(500); response.setResponseMsg("failure");
	 * e.printStackTrace(); } return response; }
	 */

	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@Override
	public Response updatePwdPolicy(String userId, String tenantId, PasswordPolicyModel model) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			model.setTenantId(tenantId);
			System.out.println("---------------------TenantId"+model.getTenantId());
			model.setUpdatedBy(userId);
			System.out.println("---------------------UpdatedBy"+model.getUpdatedBy());
			model.setUpdatedTime(commonFunctions.getCurrentDateWithTime().toString());
			System.out.println("---------------------UpdatedTime"+model.getUpdatedTime());
			
			String updateQuery = "update password_policy set min_length=" + model.getMinLength() + ", max_length="
					+ model.getMaxLength() + ", min_upper_case=" + model.getMinUpperCase() + "," + "	min_lower_case="
					+ model.getMinLowerCase() + ", min_numbers=" + model.getMinNumbers() + ", min_special="
					+ model.getMinSpecial() + ", prev_pwd_allowed=" + model.getPrevPwdAllowed() + "," + "	first_last_name="
					+ model.isFirstLastName() + ", no_of_pwd_attempt=" + model.getNoOfpwdAttempt() + ", pwd_expiry_days='"
					+ model.getPwdExpiryDays() + "', updated_by='" + model.getUpdatedBy() + "', updated_time= '"+model.getUpdatedTime()+"'"
					+ "	where tenant_id='" + model.getTenantId() + "'";
			System.out.println("----------------------Password Policy Update Query:" + updateQuery);
			
			statement.executeUpdate(updateQuery);

			jsonObject.put("minLength", model.getMinLength());
			jsonObject.put("maxLength", model.getMaxLength());
			jsonObject.put("minUpperCase", model.getMinUpperCase());
			jsonObject.put("minLowerCase", model.getMinLowerCase());
			jsonObject.put("minNumbers", model.getMinNumbers());
			jsonObject.put("minSpecial", model.getMinSpecial());
			jsonObject.put("prevPwdAllowed", model.getPrevPwdAllowed());
			jsonObject.put("firstLastName", model.isFirstLastName());
			jsonObject.put("noOfpwdAttempt", model.getNoOfpwdAttempt());
			jsonObject.put("pwdExpiryDays", model.getPwdExpiryDays());
			jsonObject.put("updatedBy", model.getUpdatedBy());
			jsonObject.put("updatedTime", model.getUpdatedTime());
			jsonObject.put("tenantId", model.getTenantId());
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
	public Response getPwdPolicy() {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		JSONObject jsonObject = new JSONObject();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			
			String getQuery = "SELECT pwd_policy_id, min_length, max_length, min_upper_case, min_lower_case, min_numbers, min_special, prev_pwd_allowed, \r\n"
					+ "first_last_name, no_of_pwd_attempt, pwd_expiry_days, \r\n"
					+ "trim(concat(trim(ut1.first_name), ' ', trim(coalesce(ut1.last_name, '')))) as updated_by, \r\n"
					+ "to_char(to_timestamp(pp.updated_time, 'yyyy-mm-dd HH24:MI:SS') at time zone 'utc'::text, 'MM-dd-yyyy HH24:MI:SS') as updated_time from password_policy pp\r\n"
					+ "LEFT JOIN user_temp ut1 on ut1.user_id = pp.updated_by ";
			System.out.println("--------------------------------------------Password Policy List Query:"+getQuery);
			ResultSet rs = statement.executeQuery(getQuery);
			while (rs.next()) {
				jsonObject.put("pwdPolicyId", rs.getString("pwd_policy_id"));
				jsonObject.put("minLength", rs.getInt("min_length"));
				jsonObject.put("maxLength", rs.getInt("max_length"));
				jsonObject.put("minUpperCase", rs.getInt("min_upper_case"));
				jsonObject.put("minLowerCase", rs.getInt("min_lower_case"));
				jsonObject.put("minNumbers", rs.getInt("min_numbers"));
				jsonObject.put("minSpecial", rs.getInt("min_special"));
				jsonObject.put("prevPwdAllowed", rs.getInt("prev_pwd_allowed"));
				jsonObject.put("firstLastName", rs.getBoolean("first_last_name"));
				jsonObject.put("noOfpwdAttempt", rs.getInt("no_of_pwd_attempt"));
				jsonObject.put("pwdExpiryDays", rs.getInt("pwd_expiry_days"));
				jsonObject.put("updatedBy", rs.getString("updated_by"));
				jsonObject.put("updatedTime", rs.getString("updated_time"));
				jsonObject.put("tenantId", rs.getString("tenant_id"));
			}
			response.setjData(jsonObject);
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
	public Response deletePwdPolicy(String tenantId) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String deleteQuery = "update password_policy set is_active='false' where pwd_policy_id='" + tenantId
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

	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@Override
	public Response existingPwdPolicy(String userId) {
		Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		JSONArray resultArray = new JSONArray();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {
			String existingQuery = "select user_id, email, password from(select user_id, email, password, row_number() over(partition by user_id, email order by updated_time desc) as row_num from user_pwd_audit)a"
					+ "	 where user_id = '"+userId+"' and row_num <=(select prev_pwd_allowed from password_policy)";
			System.out.println("-------------------Checking Last Password Query:"+existingQuery);
			ResultSet rs = statement.executeQuery(existingQuery);
			while (rs.next()) {
				byte[] result = rs.getString("password").getBytes();
				String decrypted=decrypt(result);
				    resultArray.add(decrypted);
			}
			response.setResponseCode(200);
			response.setResponseMsg("success");
			response.setjData(resultArray);
		} catch (Exception e) {
			response.setResponseCode(500);
			response.setResponseMsg("failure");
			e.printStackTrace();
		}
		
		return response;
	}

	private String decrypt(byte[] result) throws Exception {
		   

			 String UNICODE_FORMAT = "UTF8";
		     String DESEDE_ENCRYPTION_SCHEME = "DESede";
		     KeySpec ks;
		     SecretKeyFactory skf;
		     byte[] arrayBytes;
		     String myEncryptionKey;
		     String myEncryptionScheme;
		     SecretKey key;
		     
		myEncryptionKey = "ThisIsSpartaThisIsSparta";
        myEncryptionScheme = DESEDE_ENCRYPTION_SCHEME;
        arrayBytes = myEncryptionKey.getBytes(UNICODE_FORMAT);
        ks = new DESedeKeySpec(arrayBytes);
        skf = SecretKeyFactory.getInstance(myEncryptionScheme);
        cipher = Cipher.getInstance(myEncryptionScheme);
        key = skf.generateSecret(ks);
		 String decryptedText=null;
		 
	        try {
	            cipher.init(Cipher.DECRYPT_MODE, key);
	            byte[] encryptedText = Base64.getDecoder().decode(result);
	            byte[] plainText = cipher.doFinal(encryptedText);
	            decryptedText= new String(plainText);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return decryptedText;
	}

	
}
