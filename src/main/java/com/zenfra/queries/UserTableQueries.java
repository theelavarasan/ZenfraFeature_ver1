package com.zenfra.queries;

import lombok.Data;

@Data
public class UserTableQueries {

	
	private String getUserByUserid;
	private String getUserByEmail;

	public String getGetUserByUserid() {
		return getUserByUserid;
	}

	public void setGetUserByUserid(String getUserByUserid) {
		this.getUserByUserid = getUserByUserid;
	}

	public String getGetUserByEmail() {
		return getUserByEmail;
	}

	public void setGetUserByEmail(String getUserByEmail) {
		this.getUserByEmail = getUserByEmail;
	}
	
	
	
}
