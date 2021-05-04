package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dash_board_charts")
public class DashBoardCharts {

	
	
	
	public DashBoardCharts() {
		super();
		// TODO Auto-generated constructor stub
	}

	public DashBoardCharts(String data_id, String mappingId, String siteKey, String favoriteView, String analyticsType,
			String category, String userId, String analyticsFor, String chartId, boolean isActive,
			String updatedTime, String updatedBy, String createdBy, String createdTime) {
		super();
		this.data_id = data_id;
		this.mappingId = mappingId;
		this.siteKey = siteKey;
		this.favoriteView = favoriteView;
		this.analyticsType = analyticsType;
		this.category = category;
		this.userId = userId;
		this.analyticsFor = analyticsFor;
		this.chartId = chartId;
		this.isActive = isActive;
		this.updatedTime = updatedTime;
		this.updatedBy = updatedBy;
		this.createdBy = createdBy;
		this.createdTime = createdTime;
	}

	@Id
	private String data_id;

	@Column(name = "mapping_id")
	private String mappingId;

	@Column(name = "site_key")
	private String siteKey;

	@Column(name = "favorite_view")
	private String favoriteView;

	@Column(name = "analytics_type")
	private String analyticsType;

	@Column(name = "category")
	private String category;

	@Column(name = "user_id")
	private String userId;

	@Column(name = "analytics_for")
	private String analyticsFor;

	

	@Column(name = "chart_id")
	private String chartId;

	@Column(name = "is_active")
	private boolean isActive;

	
	@Column(name="updated_time")
	private String updatedTime;
	
	@Column(name="updated_by")
	private String updatedBy;
	
	@Column(name="created_by")
	private String createdBy;
	
	@Column(name="created_time")
	private String createdTime;
	
	
	public String getData_id() {
		return data_id;
	}

	public void setData_id(String data_id) {
		this.data_id = data_id;
	}

	public String getMappingId() {
		return mappingId;
	}

	public void setMappingId(String mappingId) {
		this.mappingId = mappingId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getFavoriteView() {
		return favoriteView;
	}

	public void setFavoriteView(String favoriteView) {
		this.favoriteView = favoriteView;
	}

	public String getAnalyticsType() {
		return analyticsType;
	}

	public void setAnalyticsType(String analyticsType) {
		this.analyticsType = analyticsType;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getAnalyticsFor() {
		return analyticsFor;
	}

	public void setAnalyticsFor(String analyticsFor) {
		this.analyticsFor = analyticsFor;
	}

	

	public String getChartId() {
		return chartId;
	}

	public void setChartId(String chartId) {
		this.chartId = chartId;
	}

	public boolean getActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
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

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

}
