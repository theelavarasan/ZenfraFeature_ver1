package com.zenfra.payload.model;

import com.zenfra.model.ftp.ProcessingStatus;

public class CallAwsScript {

	private String securityKey;
	
	private String accessKey;
	
	private String siteKey;
	
	private String userId;
	
	private String rid;
	
	private ProcessingStatus processingStatus;
	
	
	private String token;


	public String getSecurityKey() {
		return securityKey;
	}


	public void setSecurityKey(String securityKey) {
		this.securityKey = securityKey;
	}


	public String getAccessKey() {
		return accessKey;
	}


	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}


	public String getSiteKey() {
		return siteKey;
	}


	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}


	public String getUserId() {
		return userId;
	}


	public void setUserId(String userId) {
		this.userId = userId;
	}




	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}


	public void setProcessingStatus(ProcessingStatus processingStatus) {
		this.processingStatus = processingStatus;
	}


	public String getToken() {
		return token;
	}


	public void setToken(String token) {
		this.token = token;
	}


	public String getRid() {
		return rid;
	}


	public void setRid(String rid) {
		this.rid = rid;
	}


	
	
	
}
