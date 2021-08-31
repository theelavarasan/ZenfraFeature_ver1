package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel(value = "LogModels", description = "LogModels table operations")
public class LogModels implements Serializable{

	public LogModels() {
		super();
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;
	
	@Id
	@ApiModelProperty(hidden = true)
	private String logModelId;
	
	
	@Column(name="log_type")
	private String logType;
	
	
	@Column(name="created_date")
	private String createdDate;
	
	
	@Column(name="site_key")
	private String siteKey;
	
	
	@Column(name="unique_key")
	private String uniqueKey;
	
	
	@Column(name="updated_date")
	private String updatedDate;
	
	@Column(name="active")
	private boolean active;

	@Column(name="create_by")
	private String createBy;

	
	
	@Column(name="update_by")
	private String updateBy;
	
	
	
	public String getLogModelId() {
		return logModelId;
	}

	public void setLogModelId(String logModelId) {
		this.logModelId = logModelId;
	}

	public String getLogType() {
		return logType;
	}

	public void setLogType(String logType) {
		this.logType = logType;
	}

	public String getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public String getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(String updatedDate) {
		this.updatedDate = updatedDate;
	}

	

	public boolean getActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public String getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}
	
	
	
	
	
}
