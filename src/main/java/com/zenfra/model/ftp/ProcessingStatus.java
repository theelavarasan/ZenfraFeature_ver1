package com.zenfra.model.ftp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ProcessingStatus {

	@Id
	@Column(name = "processing_id")
	private String id;
	
	
	private String file;
	
	private String siteKey;
	
	private String tenantId;
	
	private String logType;
	
	private String userId;
	
	private String path;
	
	private String response;

	private String dataId;
	
	
	private String status;
	
	
	private String processingType;
	
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
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

	public String getLogType() {
		return logType;
	}

	public void setLogType(String logType) {
		this.logType = logType;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getProcessingType() {
		return processingType;
	}

	public void setProcessingType(String processingType) {
		this.processingType = processingType;
	}

	@Override
	public String toString() {
		return "ProcessingStatus [id=" + id + ", file=" + file + ", siteKey=" + siteKey + ", tenantId=" + tenantId
				+ ", logType=" + logType + ", userId=" + userId + ", path=" + path + ", response=" + response
				+ ", dataId=" + dataId + ", status=" + status + ", processingType=" + processingType + "]";
	}

	
	
	
	
}
