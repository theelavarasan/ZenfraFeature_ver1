package com.zenfra.model;

import javax.persistence.Entity;


public class ValidationModel {
	
	private String authUserId;
	
	private String siteKey;
	
	private String reportBy;
	
	private String columnName;
	
	private String category;
	
	private String deviceType;
	
	private String reportList;

	public String getAuthUserId() {
		return authUserId;
	}

	public void setAuthUserId(String authUserId) {
		this.authUserId = authUserId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getReportBy() {
		return reportBy;
	}

	public void setReportBy(String reportBy) {
		this.reportBy = reportBy;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getReportList() {
		return reportList;
	}

	public void setReportList(String reportList) {
		this.reportList = reportList;
	}

	public ValidationModel() {
		super();
	}

	public ValidationModel(String authUserId, String siteKey, String reportBy, String columnName, String category,
			String deviceType, String reportList) {
		super();
		this.authUserId = authUserId;
		this.siteKey = siteKey;
		this.reportBy = reportBy;
		this.columnName = columnName;
		this.category = category;
		this.deviceType = deviceType;
		this.reportList = reportList;
	}
	
	
	
	
	

}
