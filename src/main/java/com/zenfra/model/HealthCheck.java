package com.zenfra.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.json.simple.JSONObject;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel(value = "HealthCheck", description = "HealthCheck table  grid operations")
public class HealthCheck implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@ApiModelProperty(hidden = true)
	private String healthCheckId;

	@Column
	@ApiModelProperty(value = "SiteKey", name = "siteKey", dataType = "String", example = "")
	@NotBlank(message = "SiteKey must not be empty")
	private String siteKey;
	
	@Column
	@ApiModelProperty(value = "healthCheckName", name = "healthCheckName", dataType = "String", example = "")
	@NotBlank(message = "healthCheckName must not be empty")
	private String healthCheckName;
	
	@Column
	@ApiModelProperty(value = "componentType", name = "componentType", dataType = "String", example = "")
	@NotBlank(message = "componentType must not be empty")
	private String componentType;
	
	
	@Column
	@ApiModelProperty(value = "reportName", name = "reportName", dataType = "String", example = "")
	@NotBlank(message = "reportName must not be empty")
	private String reportName;
	
	@Column
	@ApiModelProperty(value = "reportBy", name = "reportBy", dataType = "String", example = "")
	@NotBlank(message = "reportBy must not be empty")
	private String reportBy;
	
	@Column
	@ApiModelProperty(value = "reportCondition", name = "reportCondition", dataType = "String", example = "")
	@NotBlank(message = "reportCondition must not be empty")
	private String reportCondition;
	
	@Column
	@ApiModelProperty(value = "siteAccessList", name = "siteAccessList", dataType = "String", example = "")
	//@NotBlank(message = "siteAccessList must not be empty")
	private String siteAccessList;
	
	@Column
	@ApiModelProperty(value = "userAccessList", name = "userAccessList", dataType = "String", example = "")
	//@NotBlank(message = "userAccessList must not be empty")
	private String userAccessList;
	
	@Column
    private String createBy;
	
	@Column
	private Date createdDate; 
	
	@Column
	private String userId;	

	@Column
	private String updateBy;
	
	@Column
	private Date updateDate;
	
	@Column
	private boolean isActive;
	
	@Transient
	private String authUserId;
	
	@Column
	private String analyticsType;
	
	
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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

	public String getReportCondition() {
		return reportCondition;
	}

	public void setReportCondition(String reportCondition) {
		this.reportCondition = reportCondition;
	}

	public String getSiteAccessList() {
		return siteAccessList;
	}

	public void setSiteAccessList(String siteAccessList) {
		this.siteAccessList = siteAccessList;
	}

	public String getUserAccessList() {
		return userAccessList;
	}

	public void setUserAccessList(String userAccessList) {
		this.userAccessList = userAccessList;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getAuthUserId() {
		return authUserId;
	}

	public void setAuthUserId(String authUserId) {
		this.authUserId = authUserId;
	}

	public String getAnalyticsType() {
		return analyticsType;
	}

	public void setAnalyticsType(String analyticsType) {
		this.analyticsType = analyticsType;
	}
	
}
