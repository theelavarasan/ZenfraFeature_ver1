package com.zenfra.configuration;

import java.sql.Connection;
import java.sql.DriverManager;

import org.springframework.stereotype.Component;

@Component
public class AwsInventoryPostgresConnection {

	

	public Connection getPostConnection() {
		
		try {
			Class.forName("org.postgresql.Driver");
			Connection connection = DriverManager.getConnection("jdbc:postgresql://uatdb.zenfra.co:5432/aws_inventory", "postgres", "UatPg5ql");
			return connection;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	

	
	
}
