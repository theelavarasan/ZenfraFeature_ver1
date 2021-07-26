package com.zenfra.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.swagger.annotations.ApiModelProperty;

public class HealthCheckModel {
	
	private String healthCheckId;
	
	private String siteKey;	
	
	private String healthCheckName;	
	
	private String componentType;	
	
	private String reportName;	
	
	private String reportBy;	
	
	private JSONArray reportCondition;	
	
	private List<String> siteAccessList;	
	
	private List<String> userAccessList;
	
	private String createBy;
	
	private Date createdDate; 
	
	private String authUserId;	

	private String updateBy;
	
	private Date updateDate;
	
	public String getAuthUserId() {
		return authUserId;
	}

	public void setAuthUserId(String authUserId) {
		this.authUserId = authUserId;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}


	public String getHealthCheckId() {
		return healthCheckId;
	}

	public void setHealthCheckId(String healthCheckId) {
		this.healthCheckId = healthCheckId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getHealthCheckName() {
		return healthCheckName;
	}

	public void setHealthCheckName(String healthCheckName) {
		this.healthCheckName = healthCheckName;
	}

	public String getComponentType() {
		return componentType;
	}

	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getReportBy() {
		return reportBy;
	}

	public void setReportBy(String reportBy) {
		this.reportBy = reportBy;
	}

	
	public JSONArray getReportCondition() {
		return reportCondition;
	}

	public void setReportCondition(JSONArray reportCondition) {
		this.reportCondition = reportCondition;
	}

	public List<String> getSiteAccessList() {
		return siteAccessList;
	}

	public void setSiteAccessList(List<String> siteAccessList) {
		this.siteAccessList = siteAccessList;
	}

	public List<String> getUserAccessList() {
		return userAccessList;
	}

	public void setUserAccessList(List<String> userAccessList) {
		this.userAccessList = userAccessList;
	}
	
	

}
