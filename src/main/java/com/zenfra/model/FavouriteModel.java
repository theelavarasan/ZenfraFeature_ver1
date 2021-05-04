package com.zenfra.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

public class FavouriteModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String favouriteName;
	private String siteKey;
	private String createdTime;
	private String createdBy;
	private String updatedTime;
	private String updatedBy;
	private JSONArray filterProperty;
	private boolean isActive;
	private String reportName;
	private String favouriteId;
	private String groupByPeriod;
	private JSONArray groupedColumns;
	private List<String> categoryList = new ArrayList<>();

	private List<String> userAccessList = new ArrayList<>();
	private List<String> siteAccessList = new ArrayList<>();
	private String projectId;
	private boolean isDefault;
	private String authUserId;
	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

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

	public String getFavouriteName() {
		return favouriteName;
	}

	public void setFavouriteName(String favouriteName) {
		this.favouriteName = favouriteName;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}
	public JSONArray getGroupedColumns() {
		return groupedColumns;
	}

	public void setGroupedColumns(JSONArray groupedColumns) {
		this.groupedColumns = groupedColumns;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getAuthUserId() {
		return authUserId;
	}

	public void setAuthUserId(String authUserId) {
		this.authUserId = authUserId;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

}
