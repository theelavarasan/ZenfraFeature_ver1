package com.zenfra.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.A;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.zenfra.configuration.AwsInventoryPostgresConnection;
import com.zenfra.model.AwsInventory;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.DBUtils;

import io.netty.handler.codec.http.HttpRequest;

@RestController
@RequestMapping("/rest/aws-inventory")
public class AwsInventoryController {
	
	@Autowired
	AwsInventoryPostgresConnection post;
	
	@Autowired
	CommonFunctions common;
	
	@PostMapping
	public ResponseModel_v2 saveAws(@RequestBody AwsInventory aws){
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			ObjectMapper map=new ObjectMapper();
			String lastFourKey=aws.getSecret_access_key().substring(aws.getSecret_access_key().length() - 4 ); 
			String sha256hex = DigestUtils.sha256Hex(aws.getSecret_access_key());
			
			String connection=checkConnection(aws.getAccess_key_id(), aws.getSecret_access_key());
				System.out.println(connection);
				
			if(connection.contains("exception") || connection.isEmpty() || (connection.contains("fail")&& connection.contains("InvalidClientTokenId"))) {
				responseModel.setResponseDescription("InvalidClientTokenId");
				responseModel.setResponseCode(HttpStatus.BAD_REQUEST);				
				if(connection.contains("fail")) {
					responseModel.setjData(map.readValue(connection, JSONObject.class));
				}
				return responseModel;
			}
				
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
		String query="select * from aws_cloud_credentials where sitekey=':sitekey_value'";
		query=query.replace(":sitekey_value", sitekey);
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

	
	
	@GetMapping("/call-script")
	public ResponseModel_v2 callScript(
			@RequestParam String siteKey,
			@RequestParam String userId,@RequestParam String tenantId,
			HttpServletRequest request
			) {
		
		//System.out.println("test");
		
		ResponseModel_v2 model=new ResponseModel_v2();
		try {
			String token=request.getHeader("Authorization").replace("Bearer ", "");
			token="Bearer "+token;
			System.out.println(token);
			
			Object insert=insertLogUploadTable(siteKey, tenantId, userId, token);
			
			ObjectMapper map=new ObjectMapper();
			
			JSONObject resJson=map.convertValue(insert, JSONObject.class);
			JSONObject body=map.readValue(resJson.get("body").toString(), JSONObject.class);
			
			
			System.out.println(body);
			if(body!=null && body.containsKey("status")&& !body.get("status").equals("200")) {
				model.setjData(resJson);
				model.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				return model;
			}
			
			Object res=callAwsScript(siteKey,userId,token) ; 
			
		
			model.setResponseCode(HttpStatus.OK);
			model.setjData(res);
			
		} catch (Exception e) {
			e.printStackTrace();
			model.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			model.setResponseDescription(e.getMessage());
		}
		
		return model;
	}
	
	
	private Object callAwsScript(String siteKey, String userId, String token) {
		
		String responce="";
		try {
			
			String path=" /opt/ZENfra/repo/cloud-inventory-collectors/aws/inventory_collection/aws_inventory.py";
			String cmd="python3 "+path+" --sitekey "+siteKey;
		
			Process process = Runtime.getRuntime().exec(cmd);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			    String line = "";			    
			    while ((line = reader.readLine()) != null) {
			    	responce+=line;
			    }
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return responce;
	}


	public String checkConnection(String access_id,String secret_key) {
		
		//Map<String, String> map=DBUtils.getPostgres();
		String responce="";
		try {
			String path="/opt/ZENfra/repo/cloud-inventory-collectors/aws/authentication.py";
			String cmd="python3 "+path+" --id "+access_id+" --key "+secret_key;
			
			Process process = Runtime.getRuntime().exec(cmd);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			    String line = "";			    
			    while ((line = reader.readLine()) != null) {
			    	responce+=line;
			    }
			
		} catch (Exception e) {
			e.printStackTrace();
			return "exception";
		}
		return responce;
	}
	
	
	
	
	public Object insertLogUploadTable(String siteKey,
		String tenantId,String userId,String token) {
		  Object responce=null;
		try {
			JSONObject obj=new JSONObject();
				obj.put("logType", "AWS");
				obj.put("description", "AWS data retrieval");
				obj.put("siteKey", siteKey);
				obj.put("userId", userId);
				obj.put("tenantId", tenantId);
				obj.put("uploadAndProcess", false);
			
		 RestTemplate restTemplate=new RestTemplate();
		 HttpEntity<Object> request = new HttpEntity<>(obj.toJSONString(),createHeaders(token));
          responce= restTemplate
                 .exchange("http://uat.zenfra.co:8080/parsing/upload", HttpMethod.POST, request, String.class);	
			
         System.out.println(responce);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return responce;
	}
	
	
	 HttpHeaders createHeaders(String token){
	        return new HttpHeaders() {{
	              set( "Authorization", token );
	            setContentType(MediaType.APPLICATION_JSON);
	        }};
	    }
}
