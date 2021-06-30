package com.zenfra.model;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel(value = "LogFileDetails", description = "Log upload grid operations")
public class LogFileDetails implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	//@GeneratedValue//(strategy = GenerationType.IDENTITY)
	@ApiModelProperty(hidden = true)
	private String logId;
	
	
	@Column
	@ApiModelProperty(value = "Log file type", name = "logType", dataType = "String", example = "AUTO")
	@NotBlank(message = "logType must not be empty")
	private String logType;
	
	@Column
	@ApiModelProperty(value = "Log file upload by", name = "uploadedBy", dataType = "String", example = "zenfra-user")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String uploadedBy;
	
	
	@Column
	@ApiModelProperty(value = "siteKey", name = "siteKey", dataType = "String", example = "a12598-asdf45sda-4ds")
	@NotBlank(message = "Site Key must not be empty")
	private String siteKey;
	
	
	@Column
	@ApiModelProperty(value = "createdDateTime", name = "createdDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	//@NotBlank(message = "Site Key must not be empty")
	private String createdDateTime;
	
	
	@Column
	@ApiModelProperty(value = "updatedDateTime", name = "updatedDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	//@NotBlank(message = "Site Key must not be empty")
	private String updatedDateTime;
	
	@Column
	@ApiModelProperty(value = "fileName", name = "fileName", dataType = "String", example = "test.log")
	//@NotBlank(message = "Site Key must not be empty")
	private String fileName;
	
	
	@Column
	@ApiModelProperty(value = "fileSize", name = "fileSize", dataType = "String", example = "1236958")
	//@NotBlank(message = "Site Key must not be empty")
	private String fileSize;
	
	@Column
	@ApiModelProperty(value = "status", name = "status", dataType = "String", example = "Processing")
	//@NotBlank(message = "Site Key must not be empty")
	private String status;
	
	
	
	
	@Column
	@ApiModelProperty(value = "response", name = "response", dataType = "String", example = "sucessfully parsing")
	//@NotBlank(message = "Site Key must not be empty")
	private String response;
	
	
	@Column
	@ApiModelProperty(value = "tenantId", name = "tenantId", dataType = "String", example = "a12598-asdf45sda-4ds-35asdf45asdf")
	//@NotBlank(message = "Site Key must not be empty")
	private String tenantId;
	
	
	@Column
	@ApiModelProperty(value = "Log file description", name = "description", dataType = "String", example = "")
	//@NotBlank(message = "Site Key must not be empty")
	private String description;
	
	
	@Column
	@ApiModelProperty(value = "extracted file path", name = "extractedPath", dataType = "String", example = "/opt/zenfra/")
	//@NotBlank(message = "Site Key must not be empty")
	private String extractedPath;
	
	@Column
	@ApiModelProperty(value = "uploadedLogs files", name = "uploadedLogs", dataType = "String", example = "/opt/zenfra/test.log")
	//@NotBlank(message = "Site Key must not be empty")
	private String uploadedLogs;
	
	
	@Column
	@ApiModelProperty(value = "masterLogs files", name = "masterLogs", dataType = "String", example = "/opt/zenfra/test.log")
	//@NotBlank(message = "Site Key must not be empty")
	private String masterLogs;
	
	
	@Column
	@ApiModelProperty(value = "masterLogs files", name = "masterLogs", dataType = "String", example = "/opt/zenfra/test.log")
	//@NotBlank(message = "Site Key must not be empty")
	private boolean isActive=true;


	




	public String getLogId() {
		return logId;
	}


	public void setLogId(String logId) {
		this.logId = logId;
	}


	public String getLogType() {
		return logType;
	}


	public void setLogType(String logType) {
		this.logType = logType;
	}


	public String getUploadedBy() {
		return uploadedBy;
	}


	public void setUploadedBy(String uploadedBy) {
		this.uploadedBy = uploadedBy;
	}


	public String getSiteKey() {
		return siteKey;
	}


	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}


	public String getCreatedDateTime() {
		return createdDateTime;
	}


	public void setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
	}


	public String getUpdatedDateTime() {
		return updatedDateTime;
	}


	public void setUpdatedDateTime(String updatedDateTime) {
		this.updatedDateTime = updatedDateTime;
	}


	public String getFileName() {
		return fileName;
	}


	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	public String getFileSize() {
		return fileSize;
	}


	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}


	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}


	public String getResponse() {
		return response;
	}


	public void setResponse(String response) {
		this.response = response;
	}


	public String getTenantId() {
		return tenantId;
	}


	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getExtractedPath() {
		return extractedPath;
	}


	public void setExtractedPath(String extractedPath) {
		this.extractedPath = extractedPath;
	}


	public String getUploadedLogs() {
		return uploadedLogs;
	}


	public void setUploadedLogs(String uploadedLogs) {
		this.uploadedLogs = uploadedLogs;
	}


	public String getMasterLogs() {
		return masterLogs;
	}


	public void setMasterLogs(String masterLogs) {
		this.masterLogs = masterLogs;
	}


	public boolean getActive() {
		return isActive;
	}


	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}


	@Override
	public String toString() {
		return "LogFileDetails [logId=" + logId + ", logType=" + logType + ", uploadedBy=" + uploadedBy + ", siteKey="
				+ siteKey + ", createdDateTime=" + createdDateTime + ", updatedDateTime=" + updatedDateTime
				+ ", fileName=" + fileName + ", fileSize=" + fileSize + ", status=" + status + ", response=" + response
				+ ", tenantId=" + tenantId + ", description=" + description + ", extractedPath=" + extractedPath
				+ ", uploadedLogs=" + uploadedLogs + ", masterLogs=" + masterLogs + ", isActive=" + isActive + "]";
	}


	public LogFileDetails() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	
	
	
}
