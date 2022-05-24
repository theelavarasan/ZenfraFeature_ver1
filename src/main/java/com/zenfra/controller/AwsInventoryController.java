package com.zenfra.controller;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.AESEncryptionDecryption;
import com.zenfra.configuration.AwsInventoryPostgresConnection;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.model.AwsInventory;
import com.zenfra.model.LogFileDetails;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.Users;
import com.zenfra.model.ftp.ProcessingStatus;
import com.zenfra.service.LogFileDetailsService;
import com.zenfra.service.ProcessService;
import com.zenfra.service.UserCreateService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Contants;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@RestController
@RequestMapping("/rest/aws-inventory")
public class AwsInventoryController {

	@Autowired
	CommonFunctions common;

	@Autowired
	AESEncryptionDecryption aesEncrypt;

	@Autowired
	ProcessService serivce;

	@Autowired
	UserCreateService userCreateService;

	@Autowired
	LogFileDetailsService logFileService;
	
	@Autowired
	private DataframeService dataframeService;

	@PostMapping
	public ResponseModel_v2 saveAws(@RequestBody AwsInventory aws, HttpServletRequest request) {
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			String token = request.getHeader("Authorization");
			AwsInventory awsExist = getAwsInventoryBySiteKey(aws.getSitekey());
			if (awsExist != null && awsExist.getSitekey() != null) {
				aws.setSecret_access_key(aesEncrypt.decrypt(awsExist.getSecret_access_key()));
			}
			ObjectMapper map = new ObjectMapper();
			String lastFourKey = aws.getSecret_access_key().substring(aws.getSecret_access_key().length() - 4);
			String connection = checkConnection(aws.getAccess_key_id(), aws.getSecret_access_key(), token);
			System.out.println("Con::" + connection);

			if (connection.isEmpty() || connection.contains("SignatureDoesNotMatch") || connection.contains("exception")
					|| connection.isEmpty()
					|| (connection.contains("fail") || connection.contains("InvalidClientTokenId"))) {
				responseModel.setResponseDescription("InvalidClientTokenId or invalid script response");
				responseModel.setResponseCode(HttpStatus.BAD_REQUEST);
				if (connection.contains("fail")) {
					responseModel.setjData(map.readValue(connection, JSONObject.class));
				}
				return responseModel;
			}

			String sha256hex = aesEncrypt.encrypt(aws.getSecret_access_key());

			aws.setSecret_access_key(sha256hex);

			aws.setCreated_date(common.getCurrentDateWithTime());
			aws.setUpdated_date(common.getCurrentDateWithTime());
			String query = "INSERT INTO aws_cloud_credentials(userid, sitekey, access_key_id, secret_access_key, regions, description, created_date, data_id,lastfourkey)"
					+ "VALUES (':userid_value', ':sitekey_value', ':access_key_id_value', ':secret_access_key_value', ':regions_value'::json, ':description_value',"
					+ "':created_date_value', gen_random_uuid(),':lastFourKey_value')"
					+ "ON CONFLICT (sitekey,access_key_id) DO update set sitekey=':sitekey_value',access_key_id=':access_key_id_value'"
					+ ",userid=':userid_value',secret_access_key=':secret_access_key_value' ,regions=':regions_value'::json,description=':description_value',updated_date=':updated_date_value',lastfourkey=':lastFourKey_value'";

			query = query.replace(":userid_value", aws.getUserid()).replace(":sitekey_value", aws.getSitekey())
					.replace(":access_key_id_value", aws.getAccess_key_id())
					.replace(":secret_access_key_value", aws.getSecret_access_key())
					.replace(":regions_value", aws.getRegions().toJSONString())
					.replace(":description_value", aws.getDescription())
					.replace(":created_date_value", aws.getCreated_date())
					.replace(":updated_date_value", aws.getUpdated_date()).replace(":lastFourKey_value", lastFourKey);

			System.out.println("query::" + query);
			Connection conn = AwsInventoryPostgresConnection.dataSource.getConnection();
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(query);
			responseModel.setResponseDescription("Done");
			responseModel.setResponseCode(HttpStatus.OK);
			stmt.close();
			conn.close();
			AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			responseModel.setResponseDescription(e.getMessage());
		}

		return responseModel;
	}

	@GetMapping("/sitekey/{sitekey}")
	public ResponseModel_v2 getListBySiteKey(@PathVariable("sitekey") String sitekey) {
		ResponseModel_v2 model = new ResponseModel_v2();
		try {

			String query = "select * from aws_cloud_credentials where sitekey=':sitekey_value'";
			query = query.replace(":sitekey_value", sitekey);// .replace(":access_key_id", accountid);
			System.out.println(query);
			Connection conn = AwsInventoryPostgresConnection.dataSource.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			List<AwsInventory> list = new ArrayList<AwsInventory>();
			ObjectMapper map = new ObjectMapper();
			while (rs.next()) {
				AwsInventory aws = new AwsInventory();
				// aws.setLastFourKey( rs.getString("lastFourKey")!=null ?
				// rs.getString("lastFourKey").toString() : " " );
				aws.setAccess_key_id(rs.getString("access_key_id") != null
						? rs.getString("access_key_id").toString().substring(rs.getString("access_key_id").length() - 4)
						: " ");
				aws.setCreated_date(
						rs.getString("created_date") != null ? rs.getString("created_date").toString() : " ");
				aws.setData_id(rs.getString("data_id") != null ? rs.getString("data_id").toString() : " ");
				aws.setDescription(rs.getString("description") != null ? rs.getString("description").toString() : " ");
				aws.setSecret_access_key(
						rs.getString("lastFourKey") != null ? rs.getString("lastFourKey").toString() : " ");
				aws.setSitekey(rs.getString("sitekey") != null ? rs.getString("sitekey").toString() : " ");
				aws.setUserid(rs.getString("userid") != null ? rs.getString("userid").toString() : " ");
				aws.setUpdated_date(
						rs.getString("updated_date") != null ? rs.getString("updated_date").toString() : " ");
				aws.setRegions(rs.getString("regions") != null ? map.readValue(rs.getString("regions"), JSONArray.class)
						: new JSONArray());
				list.add(aws);

			}

			stmt.close();
			conn.close();
			AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
			model.setjData(list);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return model;
	}

	@GetMapping("/four/{sitekey}")
	public ResponseModel_v2 getLastFourValuesBySiteKey(@PathVariable String sitekey) {

		ResponseModel_v2 model = new ResponseModel_v2();
		try {
			model.setjData(getAwsInventoryBySiteKey(sitekey));
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return model;
	}

	@GetMapping("/call-script")
	public ResponseModel_v2 callScript(@RequestParam String siteKey, @RequestParam String userId,
			@RequestParam String tenantId, @RequestParam String data_id, HttpServletRequest request) {

		ResponseModel_v2 model = new ResponseModel_v2();
		try {

			String token = request.getHeader("Authorization");

			System.out.println(token);

			LogFileDetails insert = insertLogUploadTable(siteKey, tenantId, userId, token, "Processing");

			ObjectMapper map = new ObjectMapper();

			System.out.println("resJson::" + insert.toString());

			if (insert == null) {
				model.setjData(insert);
				model.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				model.setResponseDescription("Unable to insert log upload table!");
				return model;
			}
			/*
			 * JsonNode root = map.readTree(resJson.get("body").toString()); JSONObject arr
			 * = map.readValue(root.get("jData").get("logFileDetails").get(0).toString(),
			 * JSONObject.class); System.out.println("map::" + arr); arr.put("status",
			 * "parsing"); JSONArray array = new JSONArray(); array.add(arr); JSONObject obj
			 * = new JSONObject(); obj.put("logFileDetails", arr);
			 * 
			 * 
			 * final String rid =
			 * root.get("jData").get("logFileDetails").get(0).get("rid").toString().replace(
			 * "\"", ""); System.out.println("rid::" + rid);
			 */

			model.setjData(insert);

			AwsInventory aws = getAwsInventoryByDataId(data_id);
			ProcessingStatus status = new ProcessingStatus();
			status.setProcessing_id(common.generateRandomId());
			status.setFile("AWS");
			status.setLogType("AWS");
			status.setUserId(userId);
			status.setSiteKey(siteKey);
			status.setTenantId(tenantId);
			status.setProcessDataId(aws.getData_id());
			status.setProcessingType("aws");
			status.setStatus("Processing");
			serivce.saveProcess(status);

			String sha256hex = aesEncrypt.decrypt(aws.getSecret_access_key());
			if (aws != null) {

				Runnable myrunnable = new Thread() {
					public void run() {
						callAwsScript(sha256hex, aws.getAccess_key_id(), siteKey, userId, token, status,
								insert.getLogFileId());
					}
				};
				new Thread(myrunnable).start();
				model.setResponseCode(HttpStatus.OK);

				// delete AWS dataframe
				try { // remove orient db dataframe
					String dataframePath = File.separator + "opt" + File.separator + "ZENfra" + File.separator
							+ "Dataframe" + File.separator + "OrientDB" + File.separator + siteKey
							+ File.separator; // + sourceType + File.separator;
					File[] directories = new File(dataframePath).listFiles(File::isDirectory);
					for (File dir : directories) {
						if (dir.getName().equalsIgnoreCase("AWS")) {
							FileSystemUtils.deleteRecursively(dir);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					String ex = errors.toString();
					ExceptionHandlerMail.errorTriggerMail(ex);
				}
			} else {
				model.setResponseCode(HttpStatus.NOT_FOUND);
				return model;
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			model.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			model.setResponseDescription(e.getMessage());
		}

		return model;
	}

	public Object callAwsScript(String secret_access_key, String access_key_id, String siteKey, String userId,
			String token, ProcessingStatus status, String logFileId) {

		System.out.println("status::" + status.toString());
		String response = "";
		try {

			System.out.println("Call aws script......");
			// String path="
			// /opt/ZENfra/repo/cloud-inventory-collectors/aws/inventory_collection/aws_inventory.py";
			String path = DBUtils.awsScriptData();
			String cmd = "python3 " + path + " --akid " + access_key_id + " --sakey " + secret_access_key
					+ " --sitekey " + siteKey;

			System.out.println("cmd::" + cmd);
			JSONObject request = new JSONObject();
			request.put("cmd", cmd);
			request.put("aws_type", "call aws script");
			request.put("rid", logFileId);

			StringBuilder builder = new StringBuilder();
			builder.append("?cmd=");
			builder.append(URLEncoder.encode(cmd, StandardCharsets.UTF_8.toString()));
			builder.append("&logFileId=");
			builder.append(URLEncoder.encode(logFileId, StandardCharsets.UTF_8.toString()));

			Object responseRest = common.callAwsScriptAPI(builder.toString(), token);
			status.setResponse(
					logFileId + "~" + response + "~" + responseRest != null && !responseRest.toString().isEmpty()
							? responseRest.toString()
							: "unable to update logupload API");
			
			dataframeService.putAwsInstanceDataToPostgres(siteKey, "All");
			
			status.setStatus("complete");
			System.out.println("responseRest::" + responseRest.toString());
			serivce.updateMerge(status);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			status.setStatus("complete");
			status.setResponse(response + "~" + e.getMessage());
			serivce.updateMerge(status);

		}

		return response;
	}

	public String checkConnection(String access_id, String secret_key, String token) {

		// Map<String, String> map=DBUtils.getPostgres();
		String response = "";
		try {
			String path = DBUtils.awsScriptAuth(); // "/opt/ZENfra/repo/cloud-inventory-collectors/aws/authentication.py";
			String cmd = "python3 " + path + " --id " + access_id + " --key " + secret_key;

			System.out.println("testconnection cmd::" + cmd);
			JSONObject request = new JSONObject();
			request.put("cmd", cmd);
			request.put("aws_type", "testconnection");
			request.put("rid", "");

			response = common.updateLogFile(request, token).toString();
			System.out.println("--------------------------------Update Log:"+response);

			System.out.println("checkConnection script response::" + response);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return "exception";
		}
		return response;
	}

	public LogFileDetails insertLogUploadTable(String siteKey, String tenantId, String userId, String token,
			String status) {
		LogFileDetails logFile = new LogFileDetails();
		try {

			Users saveUser = userCreateService.getUserByUserId(userId);

			logFile.setActive(true);
			logFile.setCreatedDateTime(common.getCurrentDateWithTime());
			logFile.setDescription("AWS data retrieval");
			logFile.setExtractedPath("aws");
			logFile.setFileName("aws_" + common.getCurrentDateWithTime());
			logFile.setFileSize("0");
			logFile.setLogFileId(common.generateRandomId());
			logFile.setLogType("AWS");
			logFile.setStatus(Contants.LOG_FILE_STATUS_PARSING);
			logFile.setUsername((saveUser.getFirst_name() != null ? saveUser.getFirst_name() : "") + " "
					+ (saveUser.getLast_name() != null ? saveUser.getLast_name() : ""));
			logFile.setTenantId(tenantId);
			logFile.setSiteKey(siteKey);
			logFile.setUpdatedDateTime(common.getCurrentDateWithTime());
			logFile.setUploadedBy(userId);

			logFile = logFileService.save(logFile);

			return logFile;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}

	}

	HttpHeaders createHeaders(String token) {
		return new HttpHeaders() {
			{
				set("Authorization", token);
				setContentType(MediaType.MULTIPART_FORM_DATA);
			}
		};
	}

	public AwsInventory getAwsInventoryByDataId(String data_id) {

		AwsInventory aws = new AwsInventory();
		try {
			String query = "select * from aws_cloud_credentials where data_id=':data_id_value'";
			query = query.replace(":data_id_value", data_id);
			System.out.println(query);
			Connection conn = AwsInventoryPostgresConnection.dataSource.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			ObjectMapper map = new ObjectMapper();
			while (rs.next()) {
				aws.setLastFourKey(rs.getString("lastFourKey") != null ? rs.getString("lastFourKey").toString() : " ");
				aws.setAccess_key_id(
						rs.getString("access_key_id") != null ? rs.getString("access_key_id").toString() : " ");
				aws.setSecret_access_key(
						rs.getString("secret_access_key") != null ? rs.getString("secret_access_key").toString() : " ");
				aws.setSitekey(rs.getString("sitekey") != null ? rs.getString("sitekey").toString() : " ");
				aws.setUserid(rs.getString("userid") != null ? rs.getString("userid").toString() : " ");
				aws.setUpdated_date(
						rs.getString("updated_date") != null ? rs.getString("updated_date").toString() : " ");
				aws.setRegions(rs.getString("regions") != null ? map.readValue(rs.getString("regions"), JSONArray.class)
						: new JSONArray());
			}
			stmt.close();
			conn.close();
			AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return aws;
	}

	public AwsInventory getAwsInventoryBySiteKey(String siteKey) {

		AwsInventory aws = new AwsInventory();
		try {
			String query = "select * from aws_cloud_credentials where sitekey=':sitekey_value'";
			query = query.replace(":sitekey_value", siteKey);
			System.out.println(query);
			Connection conn = AwsInventoryPostgresConnection.dataSource.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			List<AwsInventory> list = new ArrayList<AwsInventory>();
			ObjectMapper map = new ObjectMapper();
			while (rs.next()) {
				aws.setLastFourKey(rs.getString("lastFourKey") != null ? rs.getString("lastFourKey").toString() : " ");
				aws.setAccess_key_id(
						rs.getString("access_key_id") != null ? rs.getString("access_key_id").toString() : " ");
				aws.setSecret_access_key(
						rs.getString("secret_access_key") != null ? rs.getString("secret_access_key").toString() : " ");
				aws.setSitekey(rs.getString("sitekey") != null ? rs.getString("sitekey").toString() : " ");
				aws.setUserid(rs.getString("userid") != null ? rs.getString("userid").toString() : " ");
				aws.setUpdated_date(
						rs.getString("updated_date") != null ? rs.getString("updated_date").toString() : " ");
				aws.setRegions(rs.getString("regions") != null ? map.readValue(rs.getString("regions"), JSONArray.class)
						: new JSONArray());
				list.add(aws);
			}

			stmt.close();
			conn.close();
			AwsInventoryPostgresConnection.dataSource.evictConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return aws;
	}

}
