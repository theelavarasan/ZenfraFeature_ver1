package com.zenfra.utils;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;

public class DBUtils {

	public static Map<String, String> getPostgres() {
		Map<String, String> data = new HashMap<>();
		ZookeeperConnection zkConnection = new ZookeeperConnection();
		try {
			ZKModel.zkData = zkConnection.getZKData();
			String url = ZKModel.getProperty(ZKConstants.POSTGRES_URL);
			String userName = ZKModel.getProperty(ZKConstants.POSTGRES_USER);
			String password = ZKModel.getProperty(ZKConstants.POSTGRES_PWD);
			String CLOUD_PYTHON = ZKModel.getProperty(ZKConstants.CLOUD_PYTHON);
			String aws_db_url=ZKModel.getProperty(ZKConstants.pg_db_url);
			String aws_db_port=ZKModel.getProperty(ZKConstants.pg_db_port);
			data.put("url", url);
			data.put("userName", userName);
			data.put("password", password);
			String dbUrl = url +"?user="+ userName +"&password="+ password; 
			data.put("dbUrl", dbUrl);
			data.put("aws_db_url", aws_db_url);
			data.put("aws_db_port", aws_db_port);
			data.put("CLOUD_PYTHON", CLOUD_PYTHON);
			
			System.out.println(data);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return data;

	}
	
	
	public static Map<String, String> getOrientDb() {
		Map<String, String> data = new HashMap<>();
		ZookeeperConnection zkConnection = new ZookeeperConnection();
		try {
			ZKModel.zkData = zkConnection.getZKData();
			String url = ZKModel.getProperty(ZKConstants.ORIENTDBIP);
			String userName = ZKModel.getProperty(ZKConstants.ORIENTDBUSER);
			String password = ZKModel.getProperty(ZKConstants.ORIENTDBPWD);
			String databse = ZKModel.getProperty(ZKConstants.ORIENTDBNAME);
			System.out.println("--------------url--------- " + url + " : " + databse);
			System.out.println("--------------userName--------- " + userName);
			System.out.println("--------------password--------- " + password);
			data.put("url", url);
			data.put("userName", userName);
			data.put("password", password);
			String dbUrl = url+databse+"?user="+ userName +"&password="+ password; 
			data.put("dbUrl", dbUrl);
			
			System.out.println("--------------dbUrl--------- " + dbUrl);
			
			System.out.println(data);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return data;

	}
	
	
	public static String getEmailURL() {
		String url="";
		try {
			ZookeeperConnection zkConnection = new ZookeeperConnection();			
			ZKModel.zkData = zkConnection.getZKData();
			url = ZKModel.getProperty(ZKConstants.SEND_MAIL_URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return url;
	}
	
	public static Map<String,String> getServerDetails(){
		ZookeeperConnection zkConnection = new ZookeeperConnection();
		Map<String,String> map=new HashMap<String, String>();		
		try {
			ZKModel.zkData = zkConnection.getZKData();
			map.put("protocol",ZKModel.getProperty(ZKConstants.APP_SERVER_PROTOCOL.toString()));
			map.put("host_name", ZKModel.getProperty(ZKConstants.APP_SERVER_IP));
			map.put("port",ZKModel.getProperty(ZKConstants.APP_SERVER_PORT));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
	
	public static String getParsingServerIP() {
		String url="";
		try {
			ZookeeperConnection zkConnection = new ZookeeperConnection();
			ZKModel.zkData = zkConnection.getZKData();
				url=ZKModel.getProperty(ZKConstants.parsing_server_ip)+":"+ZKModel.getProperty(ZKConstants.parsingServerPort);;
		} catch (Exception e) {
			e.printStackTrace();
		}		
	return url;	
	}
	
	
}
