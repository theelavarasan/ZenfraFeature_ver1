package com.zenfra.model;

import org.json.simple.JSONArray;

public class DashboardInputModel {
	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String tenantId;
	private String siteKey;
	private String userId;
	private String projectId;
	private String reportBy;
	private String groupBy;
	private String filterBy;
	private String filterValue;
	private String type;
	private String reportName;
	private JSONArray filterProperty;
	private String groupByPeriod;
	private String favouriteId;
	private String chartId;
	private String chartName;
	private String categoryId;
	
	
	public String getCategoryId() {
		return categoryId;
	}
	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}
	public String getChartId() {
		return chartId;
	}
	public void setChartId(String chartId) {
		this.chartId = chartId;
	}
	public String getChartName() {
		return chartName;
	}
	public void setChartName(String chartName) {
		this.chartName = chartName;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
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
	public String getProjectId() {
		return projectId;
	}
	public String getReportBy() {
		return reportBy;
	}
	public void setReportBy(String reportBy) {
		this.reportBy = reportBy;
	}
	public String getGroupBy() {
		return groupBy;
	}
	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}
	public String getFilterBy() {
		return filterBy;
	}
	public void setFilterBy(String filterBy) {
		this.filterBy = filterBy;
	}
	public String getFilterValue() {
		return filterValue;
	}
	public void setFilterValue(String filterValue) {
		this.filterValue = filterValue;
	}
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	public String getReportName() {
		return reportName;
	}
	public void setReportName(String reportName) {
		this.reportName = reportName;
	}
	public JSONArray getFilterProperty() {
		return filterProperty;
	}
	public void setFilterProperty(JSONArray filterProperty) {
		this.filterProperty = filterProperty;
	}
	public String getGroupByPeriod() {
		return groupByPeriod;
	}
	public void setGroupByPeriod(String groupByPeriod) {
		this.groupByPeriod = groupByPeriod;
	}
	public String getFavouriteId() {
		return favouriteId;
	}
	public void setFavouriteId(String favouriteId) {
		this.favouriteId = favouriteId;
	}
	
	


}
