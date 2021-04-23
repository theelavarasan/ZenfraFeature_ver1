package com.zenfra.model;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ChartModel {
	
		
	public ChartModel(JSONObject chartConfiguration, String chartName, String chartType, String reportName,
			String siteKey, String userId, String chartId, String createdTime, String chartDesc, boolean isVisible,
			boolean isDefault, String analyticsType, String analyticsFor, boolean dashboard, JSONObject filterProperty,
			String updateTime, List<String> userAccessList, List<String> siteAccessList, List<String> categoryList) {
		super();
		this.chartConfiguration = chartConfiguration;
		this.chartName = chartName;
		this.chartType = chartType;
		this.reportName = reportName;
		this.siteKey = siteKey;
		this.userId = userId;
		this.chartId = chartId;
		this.createdTime = createdTime;
		this.chartDesc = chartDesc;
		this.isVisible = isVisible;
		this.isDefault = isDefault;
		this.analyticsType = analyticsType;
		this.analyticsFor = analyticsFor;
		this.dashboard = dashboard;
		this.filterProperty = filterProperty;
		this.updateTime = updateTime;
		this.userAccessList = userAccessList;
		this.siteAccessList = siteAccessList;
		this.categoryList = categoryList;
	}
	private JSONObject chartConfiguration;
	private String chartName;
	private String chartType;
	private String reportName;
	private String siteKey;
	private String userId = "";
	private String chartId;
	private String createdTime;
	private String chartDesc;
	private boolean isVisible = true;
	private boolean isDefault = false;
	private String analyticsType = "";
	private String analyticsFor = "";
	private boolean dashboard = false;
	private JSONObject filterProperty;
	
	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public String getChartDesc() {
		return chartDesc;
	}

	public void setChartDesc(String chartDesc) {
		this.chartDesc = chartDesc;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	private String updateTime;
	
	
	private List<String> userAccessList = new ArrayList<>();
	private List<String> siteAccessList = new ArrayList<>();
	private List<String> categoryList = new ArrayList<>();
	public List<String> getUserAccessList() {
		return userAccessList;
	}

	public void setUserAccessList(List<String> userAccessList) {
		
		if(userAccessList == null)
		{
			userAccessList = new ArrayList<>();
		}
		
		this.userAccessList = userAccessList;
	}

	public List<String> getSiteAccessList() {
		return siteAccessList;
	}

	public void setSiteAccessList(List<String> siteAccessList) {
		
		if(siteAccessList == null)
		{
			siteAccessList = new ArrayList<>();
		}
		this.siteAccessList = siteAccessList;
	}
	public List<String> getCategoryList() {
		return categoryList;
	}
	public void setCategoryList(List<String> categoryList) {
		
		if(categoryList == null)
		{
			categoryList = new ArrayList<>();
		}
		
		this.categoryList = categoryList;
	}
	public JSONObject getChartConfiguration() {
		return chartConfiguration;
	}
	public void setChartConfiguration(JSONObject chartConfiguration) {
		this.chartConfiguration = chartConfiguration;
	}
	public String getChartName() {
		return chartName;
	}
	public void setChartName(String chartName) {
		this.chartName = chartName;
	}
	public String getChartType() {
		return chartType;
	}
	public void setChartType(String chartType) {
		this.chartType = chartType;
	}
	public String getReportName() {
		return reportName;
	}
	public void setReportName(String reportName) {
		this.reportName = reportName;
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
	public String getChartId() {
		return chartId;
	}
	public void setChartId(String chartId) {
		this.chartId = chartId;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setIsDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getAnalyticsType() {
		return analyticsType;
	}

	public void setAnalyticsType(String analyticsType) {
		this.analyticsType = analyticsType;
	}

	public String getAnalyticsFor() {
		return analyticsFor;
	}

	public void setAnalyticsFor(String analyticsFor) {
		this.analyticsFor = analyticsFor;
	}

	public boolean isDashboard() {
		return dashboard;
	}

	public void setDashboard(boolean dashboard) {
		this.dashboard = dashboard;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public JSONObject getFilterProperty() {
		return filterProperty;
	}

	public void setFilterProperty(JSONObject filterProperty) {
		this.filterProperty = filterProperty;
	}
}
