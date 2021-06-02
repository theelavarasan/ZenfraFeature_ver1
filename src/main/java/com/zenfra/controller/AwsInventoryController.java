package com.zenfra.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.AESEncryptionDecryption;
import com.zenfra.configuration.AwsInventoryPostgresConnection;
import com.zenfra.ftp.scheduler.AwsScriptThread;
import com.zenfra.model.AwsInventory;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.ftp.ProcessingStatus;
import com.zenfra.payload.model.CallAwsScript;
import com.zenfra.service.ProcessService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Constants;

@RestController
@RequestMapping("/rest/aws-inventory")
public class AwsInventoryController {
	
	@Autowired
	AwsInventoryPostgresConnection post;
	
	@Autowired
	CommonFunctions common;
	
	@Autowired
	AESEncryptionDecryption aesEncrypt;
	
	
	@Autowired
	ProcessService serivce;
	
	@PostMapping
	public ResponseModel_v2 saveAws(@RequestBody AwsInventory aws){
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			AwsInventory awsExist=getAwsInventoryBySiteKey(aws.getSitekey());
				if(awsExist!=null && awsExist.getSitekey()!=null) {
					aws.setSecret_access_key(aesEncrypt.decrypt(awsExist.getSecret_access_key()));					
				}
			ObjectMapper map=new ObjectMapper();
			String lastFourKey=aws.getSecret_access_key().substring(aws.getSecret_access_key().length() - 4 ); 
			String connection=checkConnection(aws.getAccess_key_id(), aws.getSecret_access_key());
			System.out.println("Con::"+connection);
			
			if(connection.isEmpty() || connection.contains("SignatureDoesNotMatch") || connection.contains("exception") || connection.isEmpty() || (connection.contains("fail") || connection.contains("InvalidClientTokenId"))) {
				responseModel.setResponseDescription("InvalidClientTokenId or invalid script response");
				responseModel.setResponseCode(HttpStatus.BAD_REQUEST);				
				if(connection.contains("fail")) {
					responseModel.setjData(map.readValue(connection, JSONObject.class));
				}
				return responseModel;
			}
			
			String sha256hex = aesEncrypt.encrypt(aws.getSecret_access_key());
			
			aws.setSecret_access_key(sha256hex);
			
			aws.setCreated_date(common.getCurrentDateWithTime());
			aws.setUpdated_date(common.getCurrentDateWithTime());
			String query="INSERT INTO aws_cloud_credentials(userid, sitekey, access_key_id, secret_access_key, regions, description, created_date, data_id,lastfourkey)" + 
					"VALUES (':userid_value', ':sitekey_value', ':access_key_id_value', ':secret_access_key_value', ':regions_value'::json, ':description_value'," + 
					"':created_date_value', gen_random_uuid(),':lastFourKey_value')" + 
					"ON CONFLICT (sitekey,access_key_id) DO update set sitekey=':sitekey_value',access_key_id=':access_key_id_value'" + 
					",userid=':userid_value',secret_access_key=':secret_access_key_value' ,regions=':regions_value'::json,description=':description_value',updated_date=':updated_date_value',lastfourkey=':lastFourKey_value'";
			
			query=query.replace(":userid_value", aws.getUserid()).replace(":sitekey_value", aws.getSitekey()).replace(":access_key_id_value", aws.getAccess_key_id())
					.replace(":secret_access_key_value", aws.getSecret_access_key()).replace(":regions_value", aws.getRegions().toJSONString()).replace(":description_value", aws.getDescription())
					.replace(":created_date_value", aws.getCreated_date()).replace(":updated_date_value", aws.getUpdated_date()).replace(":lastFourKey_value", lastFourKey);
			
			System.out.println("query::"+query);
			Connection conn =post.getPostConnection();
			Statement stmt = conn.createStatement();
				stmt.executeUpdate(query);			
				responseModel.setResponseDescription("Done");
				responseModel.setResponseCode(HttpStatus.OK);
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			responseModel.setResponseDescription(e.getMessage());
		}
		
		return responseModel;
	}
	
	
	@GetMapping("/sitekey/{sitekey}")
	public ResponseModel_v2 getListBySiteKey(@PathVariable("sitekey") String sitekey) {
		ResponseModel_v2 model=new ResponseModel_v2();
		try {
			
			String query="select * from aws_cloud_credentials where sitekey=':sitekey_value'";
			query=query.replace(":sitekey_value", sitekey);//.replace(":access_key_id", accountid);
			System.out.println(query);
		   Connection conn =post.getPostConnection();
		   Statement stmt = conn.createStatement();			
	       ResultSet rs = stmt.executeQuery(query);
		List<AwsInventory> list=new ArrayList<AwsInventory>();
		ObjectMapper map=new ObjectMapper();
	       while(rs.next()){
	    	   AwsInventory aws=new AwsInventory();
	    	   	aws.setLastFourKey( rs.getString("lastFourKey")!=null ? rs.getString("lastFourKey").toString() : " " );
	    	   	aws.setAccess_key_id( rs.getString("access_key_id")!=null ? rs.getString("access_key_id").toString() : " " );
	    	   	aws.setCreated_date(rs.getString("created_date")!=null ? rs.getString("created_date").toString() : " ");
	    	   	aws.setData_id(rs.getString("data_id")!=null ? rs.getString("data_id").toString() : " ");
	    	   	aws.setDescription(rs.getString("description")!=null ? rs.getString("description").toString() : " ");
	    	   	aws.setSecret_access_key(rs.getString("secret_access_key")!=null ? rs.getString("secret_access_key").toString() : " ");
	    	   	aws.setSitekey(rs.getString("sitekey")!=null ? rs.getString("sitekey").toString() : " ");
	    	   	aws.setUserid(rs.getString("userid")!=null ? rs.getString("userid").toString() : " ");
	    	   	aws.setUpdated_date(rs.getString("updated_date")!=null ? rs.getString("updated_date").toString() : " ");
	    	   	aws.setRegions(rs.getString("regions")!=null ? map.readValue(rs.getString("regions"), JSONArray.class) : new JSONArray());
	    	 list.add(aws);  	
	       }
	       
			
			stmt.close();
			conn.close();
		model.setjData(list);			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model;
	}
	

	@GetMapping("/four/{sitekey}")
	public ResponseModel_v2 getLastFourValuesBySiteKey(@PathVariable String sitekey) {
		
		ResponseModel_v2 model=new ResponseModel_v2();
	try {		
		model.setjData(getAwsInventoryBySiteKey(sitekey));			
	} catch (Exception e) {
		e.printStackTrace();
	}
	return model;
	}

	
	
	
	@GetMapping("/call-script")
	public ResponseModel_v2 callScript(
			@RequestParam String siteKey,
			@RequestParam String userId,@RequestParam String tenantId,
			@RequestParam String data_id,
			HttpServletRequest request
			) {
		
		ResponseModel_v2 model=new ResponseModel_v2();
		try {
			
			//String token=request.getHeader("Authorization");
			String token="Bearer "+common.getZenfraToken("aravind.krishnasamy@virtualtechgurus.com", "Aravind@123");
			System.out.println(token);
			
		
			
		
			Object insert=insertLogUploadTable(siteKey, tenantId, userId, token,"Processing");
			
			ObjectMapper map=new ObjectMapper();
			
			JSONObject resJson=map.convertValue(insert, JSONObject.class);
			System.out.println("resJson::"+resJson);
			JSONObject body=map.readValue(resJson.get("body").toString(), JSONObject.class);			
			System.out.println("body.get(\"responseCode\")"+body.get("responseCode"));
			JsonNode root = map.readTree(resJson.get("body").toString());	
			if(body!=null && !body.get("responseCode").toString().equals("200")) {
				model.setjData(body);
				model.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				model.setResponseDescription("Unable to insert log upload table!");
				return model;
			}
			final String rid=root.get("jData").get("logFileDetails").get(0).get("rid").toString();
			
			
			AwsInventory aws=getAwsInventoryByDataId(data_id);
			ProcessingStatus status=new ProcessingStatus();
				status.setId(common.generateRandomId());
				status.setFile("");
				status.setLogType("");
				status.setUserId(userId);
				status.setSiteKey(siteKey);
				status.setTenantId(tenantId);
				status.setDataId(aws.getData_id());
				status.setProcessingType("aws");
				status.setStatus("Processing");
			serivce.saveProcess(status);
			 
			String sha256hex = aesEncrypt.decrypt(aws.getSecret_access_key());
			if(aws!=null) {	
				
				CallAwsScript script=new CallAwsScript();
					script.setSecurityKey(sha256hex);
					script.setAccessKey(aws.getAccess_key_id());
					script.setSiteKey(siteKey);
					script.setUserId(userId);
					script.setToken(token);
					script.setProcessingStatus(status);
					script.setRid(rid);
					
				//AwsScriptThread awsScript=new AwsScriptThread(script);
				//	awsScript.run();
				
					new Thread(new Runnable() {
				        public void run(){
				        	callAwsScript(sha256hex,aws.getAccess_key_id(),siteKey,userId,token,status, rid); 
					     }
				    }).start();
					
				
				model.setResponseCode(HttpStatus.OK);
				model.setjData("Script successfully started");				
			}else {
				model.setResponseCode(HttpStatus.NOT_FOUND);
				return model;
			}
	
		
			
			
		} catch (Exception e) {
			e.printStackTrace();
			model.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			model.setResponseDescription(e.getMessage());
		}
		
		return model;
	}
	
	public  Object callAwsScript(String secret_access_key,String access_key_id,String siteKey, String userId, String token, ProcessingStatus status,String rid) {
		
		System.out.println("status::"+status.toString());
		String response="";
		try {
			
			System.out.println("Call aws script......");
			String path=" /opt/ZENfra/repo/cloud-inventory-collectors/aws/inventory_collection/aws_inventory.py";
			String cmd="python3 "+path+" --akid "+access_key_id+" --sakey "+secret_access_key+" --sitekey "+siteKey;
		
			System.out.println("cmd::"+cmd);
			
			Process process = Runtime.getRuntime().exec(cmd);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			    String line = "";			    
			    while ((line = reader.readLine()) != null) {
			    	response+=line;
			    	System.out.println(response);
			    }
			    System.out.println("aws data script response::"+response);
		 String query="update LogFileDetails set parsingStatus='success',status='success',response='"+response+"' where @rid='"+rid+"'";
		 MultiValueMap<String, Object> json=new LinkedMultiValueMap<String, Object>();
			 		json.add("method", "update");
			 		json.add("query", query);
			 		
			Object responseRest=common.updateLogFile(json);
			status.setResponse(response+"~"+responseRest!=null && !responseRest.toString().isEmpty() ? responseRest.toString() : "unable to update logupload API");
			serivce.updateMerge(status);
		} catch (Exception e) {
			e.printStackTrace();
			status.setResponse(response+"~"+e.getMessage());
			serivce.updateMerge(status);
			
		}
		
		return response;
	}


	public String checkConnection(String access_id,String secret_key) {
		
		//Map<String, String> map=DBUtils.getPostgres();
		String response="";
		try {
			String path="/opt/ZENfra/repo/cloud-inventory-collectors/aws/authentication.py";
			String cmd="python3 "+path+" --id "+access_id+" --key "+secret_key;
			
			System.out.println("cmd::"+cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			    String line = "";			    
			    while ((line = reader.readLine()) != null) {
			    	response+=line;
			    	System.out.println(response);
			    }
			
			    System.out.println("checkConnection script response::"+response);
		} catch (Exception e) {
			e.printStackTrace();
			return "exception";
		}
		return response;
	}
	
	
	
	
	public Object insertLogUploadTable(String siteKey,
		String tenantId,String userId,String token,String status) {
		  Object responce=null;
		try {
			
			System.out.println("Start insertLogUploadTable..... ");
			 String fileName = "json/temp.json";
		        ClassLoader classLoader = getClass().getClassLoader();
		 
		        File file = new File(classLoader.getResource(fileName).getFile());
				
		MultiValueMap<String, Object> body= new LinkedMultiValueMap<>();
			      //body.add("parseFile", file);
			      body.add("logType", "AWS");
			      body.add("description", "AWS data retrieval");
			      body.add("siteKey", siteKey);
			      body.add("userId", userId);
			      body.add("tenantId", tenantId);
			      body.add("uploadAndProcess", false);
			      body.add("status", status);
		
			      
		 RestTemplate restTemplate=new RestTemplate();
		 HttpEntity<Object> request = new HttpEntity<>(body,createHeaders(token));
          responce= restTemplate
                 .exchange(Constants.current_url+"/parsing/upload", HttpMethod.POST, request, String.class);	
			
        
          System.out.println("End insertLogUploadTable..... ");
           
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return responce;
	}
	

	
	 HttpHeaders createHeaders(String token){
	        return new HttpHeaders() {{
	              set( "Authorization", token );
	            setContentType(MediaType.MULTIPART_FORM_DATA);
	        }};
	    }
	 
	 
	 public AwsInventory getAwsInventoryByDataId(String data_id) {
		 
		 AwsInventory aws=new AwsInventory();
		 try {
			 String query="select * from aws_cloud_credentials where data_id=':data_id_value'";
				query=query.replace(":data_id_value", data_id);
				System.out.println(query);
			   Connection conn =post.getPostConnection();
			   Statement stmt = conn.createStatement();			
		       ResultSet rs = stmt.executeQuery(query);
		       ObjectMapper map=new ObjectMapper();
		   	
		       while(rs.next()){
		    	    aws.setLastFourKey( rs.getString("lastFourKey")!=null ? rs.getString("lastFourKey").toString() : " " );
		    	   	aws.setAccess_key_id( rs.getString("access_key_id")!=null ? rs.getString("access_key_id").toString() : " " );
		    	  	aws.setSecret_access_key(rs.getString("secret_access_key")!=null ? rs.getString("secret_access_key").toString() : " ");
		    	   	aws.setSitekey(rs.getString("sitekey")!=null ? rs.getString("sitekey").toString() : " ");
		    	   	aws.setUserid(rs.getString("userid")!=null ? rs.getString("userid").toString() : " ");
		    	   	aws.setUpdated_date(rs.getString("updated_date")!=null ? rs.getString("updated_date").toString() : " ");
		    	   	aws.setRegions(rs.getString("regions")!=null ? map.readValue(rs.getString("regions"), JSONArray.class) : new JSONArray());
		       }
		 		stmt.close();
				conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		 
		 return aws;
	 }
	 
 public AwsInventory getAwsInventoryBySiteKey(String siteKey) {
		 
		 AwsInventory aws=new AwsInventory();
		 try {String query="select * from aws_cloud_credentials where sitekey=':sitekey_value'";
			query=query.replace(":sitekey_value", siteKey);
			System.out.println(query);
		   Connection conn =post.getPostConnection();
		   Statement stmt = conn.createStatement();			
	       ResultSet rs = stmt.executeQuery(query);
	       List<AwsInventory> list=new ArrayList<AwsInventory>();
	       ObjectMapper map=new ObjectMapper();
	       while(rs.next()){
	    	   	aws.setLastFourKey( rs.getString("lastFourKey")!=null ? rs.getString("lastFourKey").toString() : " " );
	    	   	aws.setAccess_key_id( rs.getString("access_key_id")!=null ? rs.getString("access_key_id").toString() : " " );
	    	  	aws.setSecret_access_key(rs.getString("secret_access_key")!=null ? rs.getString("secret_access_key").toString() : " ");
	    	   	aws.setSitekey(rs.getString("sitekey")!=null ? rs.getString("sitekey").toString() : " ");
	    	   	aws.setUserid(rs.getString("userid")!=null ? rs.getString("userid").toString() : " ");
	    	   	aws.setUpdated_date(rs.getString("updated_date")!=null ? rs.getString("updated_date").toString() : " ");
	    	   	aws.setRegions(rs.getString("regions")!=null ? map.readValue(rs.getString("regions"), JSONArray.class) : new JSONArray());
	    	  list.add(aws);  	
	       }
	       
			
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		 
		 return aws;
	 }
}



