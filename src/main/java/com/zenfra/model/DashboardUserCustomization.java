package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

@Entity
@Table(name="dashboard_user_customization")
public class DashboardUserCustomization {
	
	
	
	public DashboardUserCustomization() {
		
	}
	
	
	public DashboardUserCustomization(String data_id, String siteKey, String reportName, String tenantId, String userId,
			String layout) {
		super();
		this.dataId = data_id;
		this.siteKey = siteKey;
		this.reportName = reportName;
		this.tenantId = tenantId;
		this.userId = userId;
		this.layout = layout;
	}

	@Id
	@Column(name="data_id")
	private String dataId;
	
	@Column(name="site_key")
	private String siteKey;
	
	@Column(name="report_name")
	private String reportName;
	
	@Column(name="tenant_id")
	private String tenantId;
	
	@Column(name="user_id")
	private String userId;
	
	
	@Column(name="layout")
	private String layout;
	
	@Transient
	private JSONArray layoutArray;

	

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
	
	

	public String getDataId() {
		return dataId;
	}


	public void setDataId(String dataId) {
		this.dataId = dataId;
	}


	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}


	public String getLayout() {
		return layout;
	}


	public void setLayout(String layout) {
		this.layout = layout;
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


	public JSONArray getLayoutArray() {
		return layoutArray;
	}


	public void setLayoutArray(JSONArray layoutArray) {
		this.layoutArray = layoutArray;
	}



	
	
	

}
