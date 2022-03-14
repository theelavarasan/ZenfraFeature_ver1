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
	
	private String analyticsType;
	
	private String model;
	
	private String projectId;
	
	private String osType;

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

	public String getAnalyticsType() {
		return analyticsType;
	}

	public void setAnalyticsType(String analyticsType) {
		this.analyticsType = analyticsType;
	}
	
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	
	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	
	public String getOsType() {
		return osType;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}

	

	public ValidationModel() {
		super();
	}

	public ValidationModel(String authUserId, String siteKey, String reportBy, String columnName, String category,
			String deviceType, String reportList, String analyticsType, String model, String osType) {
		super();
		this.authUserId = authUserId;
		this.siteKey = siteKey;
		this.reportBy = reportBy;
		this.columnName = columnName;
		this.category = category;
		this.deviceType = deviceType;
		this.reportList = reportList;
		this.analyticsType = analyticsType;
		this.model = model;
		this.osType = osType;
	}

	

}
