package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
public class UserTableQueries {

	
	private String getUserByUserid;

	public String getGetUserByUserid() {
		return getUserByUserid;
	}

	public void setGetUserByUserid(String getUserByUserid) {
		this.getUserByUserid = getUserByUserid;
	}
	
	
}
