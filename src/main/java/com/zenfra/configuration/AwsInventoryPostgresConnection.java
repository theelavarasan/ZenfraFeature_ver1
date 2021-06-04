package com.zenfra.configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.zenfra.utils.DBUtils;

@Component
public class AwsInventoryPostgresConnection {

	

	public Connection getPostConnection() {
		Map<String, String> data=DBUtils.getPostgres();
		try {
			Class.forName("org.postgresql.Driver");
			Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/aws_inventory", data.get("userName"),data.get("password"));
			return connection;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	

	
	
}
