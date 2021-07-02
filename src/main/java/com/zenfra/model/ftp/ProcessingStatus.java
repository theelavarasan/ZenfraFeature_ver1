package com.zenfra.model.ftp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.json.simple.JSONArray;

@Entity
public class ProcessingStatus {

	@Id
	@Column(name = "processing_id")
	private String processing_id;
	
	
	private String file;
	
	private String siteKey;
	
	private String tenantId;
	
	private String logType;
	
	private String userId;
	
	private String path;
	
	private String response;

	private String processDataId;
	
	
	private String status;
	
	
	private String processingType;
	
	
	private String startTime;
	
	
	private String endTime;
	
	@Transient
	private JSONArray fileList;
	
	private String logCount;

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

	

	

	public String getProcessDataId() {
		return processDataId;
	}

	public void setProcessDataId(String processDataId) {
		this.processDataId = processDataId;
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


	public String getProcessing_id() {
		return processing_id;
	}

	public void setProcessing_id(String processing_id) {
		this.processing_id = processing_id;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public JSONArray getFileList() {
		return fileList;
	}

	public void setFileList(JSONArray fileList) {
		this.fileList = fileList;
	}

	public String getLogCount() {
		return logCount;
	}

	public void setLogCount(String logCount) {
		this.logCount = logCount;
	}

	
	
	
	
}
