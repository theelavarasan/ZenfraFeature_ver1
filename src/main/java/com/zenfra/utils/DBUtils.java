package com.zenfra.utils;

import java.util.HashMap;
import java.util.Map;

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
			data.put("url", url);
			data.put("userName", userName);
			data.put("password", password);
			String dbUrl = url +"?user="+ userName +"&password="+ password; 
			data.put("dbUrl", dbUrl);
			
			System.out.println(data);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return data;

	}
}
