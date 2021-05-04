package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="dashboard_chart_details")
public class DashboardChartDetails {

	
	@Id
	private String data_id;
	
	@Column(name="chart_id")
	private String chartId;
	

	@Column(name="site_key")
	private String siteKey;
	

	@Column(name="favourite_id")
	private String favouriteId;
	

	@Column(name="chart_details")
	private String chartDetails;
	

	@Column(name="is_active")
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

	public String getChartId() {
		return chartId;
	}

	public void setChartId(String chartId) {
		this.chartId = chartId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getFavouriteId() {
		return favouriteId;
	}

	public void setFavouriteId(String favouriteId) {
		this.favouriteId = favouriteId;
	}

	public String getChartDetails() {
		return chartDetails;
	}

	public void setChartDetails(String chartDetails) {
		this.chartDetails = chartDetails;
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
