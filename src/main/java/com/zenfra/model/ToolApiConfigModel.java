package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "tool_api_config")
public class ToolApiConfigModel {

	@Id
	@Column(name = "api_config_id")
	private String apiConfigId;

	@Transient
	private String userId;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_time")
	private String createdTime;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "updated_time")
	private String updatedTime;

	@Column(name = "site_key")
	private String siteKey;

	@Column(name = "tenant_id")
	private String tenantId;

	@Column(name = "device_type")
	private String deviceType;

	@Column(name = "api_key")
	private String apiKey;

	@Column(name = "api_secret_key")
	private String apiSecretKey;

	@Column(name = "config_name")
	private String configName;

	@Column(name = "is_active")
	private boolean isActive;

	public String getApiConfigId() {
		return apiConfigId;
	}

	public void setApiConfigId(String apiConfigId) {
		this.apiConfigId = apiConfigId;
	}

	public String isCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiSecretKey() {
		return apiSecretKey;
	}

	public void setApiSecretKey(String apiSecretKey) {
		this.apiSecretKey = apiSecretKey;
	}

	public String getConfigName() {
		return configName;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public ToolApiConfigModel() {

	}

	public ToolApiConfigModel(String apiConfigId, String userId, String createdBy, String createdTime, String updatedBy,
			String updatedTime, String siteKey, String tenantId, String deviceType, String apiKey, String apiSecretKey,
			String configName, boolean isActive) {
		super();
		this.apiConfigId = apiConfigId;
		this.userId = userId;
		this.createdBy = createdBy;
		this.createdTime = createdTime;
		this.updatedBy = updatedBy;
		this.updatedTime = updatedTime;
		this.siteKey = siteKey;
		this.tenantId = tenantId;
		this.deviceType = deviceType;
		this.apiKey = apiKey;
		this.apiSecretKey = apiSecretKey;
		this.configName = configName;
		this.isActive = isActive;
	}

	@Override
	public String toString() {
		return "ToolApiConfigModel [apiConfigId=" + apiConfigId + ", userId=" + userId + ", createdBy=" + createdBy
				+ ", createdTime=" + createdTime + ", updatedBy=" + updatedBy + ", updatedTime=" + updatedTime
				+ ", siteKey=" + siteKey + ", tenantId=" + tenantId + ", deviceType=" + deviceType + ", apiKey="
				+ apiKey + ", apiSecretKey=" + apiSecretKey + ", configName=" + configName + ", isActive=" + isActive
				+ "]";
	}

}
