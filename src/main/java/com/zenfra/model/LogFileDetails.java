package com.zenfra.model;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel(value = "LogFileDetails", description = "Log upload grid operations")
public class LogFileDetails implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	// @GeneratedValue//(strategy = GenerationType.IDENTITY)
	@ApiModelProperty(hidden = true)
	private String logFileId;

	@Column
	private String logId;

	@Column
	@ApiModelProperty(value = "Log file type", name = "logType", dataType = "String", example = "AUTO")
	@NotBlank(message = "logType must not be empty")
	private String logType;

	@Column
	@ApiModelProperty(value = "Log file upload by", name = "uploadedBy", dataType = "String", example = "zenfra-user")
	// @NotBlank(message = "uploadedBy must not be empty")
	private String uploadedBy;

	@Column
	@ApiModelProperty(value = "siteKey", name = "siteKey", dataType = "String", example = "a12598-asdf45sda-4ds")
	@NotBlank(message = "Site Key must not be empty")
	private String siteKey;

	@Column
	@ApiModelProperty(value = "createdDateTime", name = "createdDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	// @NotBlank(message = "Site Key must not be empty")
	private String createdDateTime;

	@Column
	@ApiModelProperty(value = "updatedDateTime", name = "updatedDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	// @NotBlank(message = "Site Key must not be empty")
	private String updatedDateTime;

	@Column
	@ApiModelProperty(value = "fileName", name = "fileName", dataType = "String", example = "test.log")
	// @NotBlank(message = "Site Key must not be empty")
	private String fileName;

	@Column
	@ApiModelProperty(value = "fileSize", name = "fileSize", dataType = "String", example = "1236958")
	// @NotBlank(message = "Site Key must not be empty")
	private String fileSize;

	@Column
	@ApiModelProperty(value = "status", name = "status", dataType = "String", example = "Processing")
	// @NotBlank(message = "Site Key must not be empty")
	private String status;

	@Column
	@ApiModelProperty(value = "response", name = "response", dataType = "String", example = "sucessfully parsing")
	// @NotBlank(message = "Site Key must not be empty")
	private String response;

	@Column
	@ApiModelProperty(value = "tenantId", name = "tenantId", dataType = "String", example = "a12598-asdf45sda-4ds-35asdf45asdf")
	// @NotBlank(message = "Site Key must not be empty")
	private String tenantId;

	@Column
	@ApiModelProperty(value = "Log file description", name = "description", dataType = "String", example = "")
	// @NotBlank(message = "Site Key must not be empty")
	private String description;

	@Column
	@ApiModelProperty(value = "extracted file path", name = "extractedPath", dataType = "String", example = "/opt/zenfra/")
	// @NotBlank(message = "Site Key must not be empty")
	private String extractedPath;

	@Column
	@ApiModelProperty(value = "uploadedLogs files", name = "uploadedLogs", dataType = "String", example = "/opt/zenfra/test.log")
	// @NotBlank(message = "Site Key must not be empty")
	private String uploadedLogs;

	@Column
	@ApiModelProperty(value = "masterLogs files", name = "masterLogs", dataType = "String", example = "/opt/zenfra/test.log")
	// @NotBlank(message = "Site Key must not be empty")
	private String masterLogs;

	@Column
	@ApiModelProperty(value = "Active status", name = "activ", dataType = "boolean", example = "true")
	// @NotBlank(message = "Site Key must not be empty")
	private boolean isActive = true;

	@Column
	@ApiModelProperty(value = "Parsing Status", name = "parsingStatus", dataType = "String", example = "queue")
	// @NotBlank(message = "Site Key must not be empty")
	private String parsingStatus;

	@Column
	@ApiModelProperty(value = "Message Status", name = "message", dataType = "String", example = "queue")
	// @NotBlank(message = "Site Key must not be empty")
	private String message;

	@Column
	@ApiModelProperty(value = "Parsing Start Time", name = "parsingStartTime", dataType = "String", example = "01-01-2021 01:20:22 AM")
	// @NotBlank(message = "Site Key must not be empty")
	private String parsingStartTime;

	@Column
	@ApiModelProperty(value = "parsedDateTime", name = "parsedDateTime", dataType = "String", example = "01-01-2021 01:20:22 AM")
	// @NotBlank(message = "Site Key must not be empty")
	private String parsedDateTime;

	@Column
	@ApiModelProperty(value = "CmdStatusParsing", name = "CmdStatusParsing", dataType = "String", example = "queue")
	// @NotBlank(message = "Site Key must not be empty")
	private String CmdStatusParsing;

	@Column
	@ApiModelProperty(value = "CmdStatusInsertion", name = "CmdStatusInsertion", dataType = "String", example = "queue")
	// @NotBlank(message = "Site Key must not be empty")
	private String CmdStatusInsertion;

	@Column
	@ApiModelProperty(value = "tempStatus", name = "tempStatus", dataType = "String", example = "queue")
	// @NotBlank(message = "Site Key must not be empty")
	private String tempStatus;

	@Column
	@ApiModelProperty(value = "filePaths", name = "filePaths", dataType = "String", example = "queue")
	// @NotBlank(message = "Site Key must not be empty")
	private String filePaths;

	@Column
	@ApiModelProperty(value = "fileName1", name = "fileName1", dataType = "String", example = "queue")
	// @NotBlank(message = "Site Key must not be empty")
	private String fileName1;

	@Transient
	Object tempData;

	@Transient
	private String username;

	@Column
	private String collectionDate;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getTempStatus() {
		return tempStatus;
	}

	public void setTempStatus(String tempStatus) {
		this.tempStatus = tempStatus;
	}

	public String getCmdStatusInsertion() {
		return CmdStatusInsertion;
	}

	public void setCmdStatusInsertion(String cmdStatusInsertion) {
		CmdStatusInsertion = cmdStatusInsertion;
	}

	public String getCmdStatusParsing() {
		return CmdStatusParsing;
	}

	public void setCmdStatusParsing(String cmdStatusParsing) {
		CmdStatusParsing = cmdStatusParsing;
	}

	public String getParsedDateTime() {
		return parsedDateTime;
	}

	public void setParsedDateTime(String parsedDateTime) {
		this.parsedDateTime = parsedDateTime;
	}

	public String getParsingStatus() {
		return parsingStatus;
	}

	public void setParsingStatus(String parsingStatus) {
		this.parsingStatus = parsingStatus;
	}

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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getParsingStartTime() {
		return parsingStartTime;
	}

	public void setParsingStartTime(String parsingStartTime) {
		this.parsingStartTime = parsingStartTime;
	}

	public String getFilePaths() {
		return filePaths;
	}

	public void setFilePaths(String filePaths) {
		this.filePaths = filePaths;
	}

	public String getLogFileId() {
		return logFileId;
	}

	public void setLogFileId(String logFileId) {
		this.logFileId = logFileId;
	}

	public Object getTempData() {
		return tempData;
	}

	public void setTempData(Object tempData) {
		this.tempData = tempData;
	}

	public String getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(String collectionDate) {
		this.collectionDate = collectionDate;
	}

	public String getFileName1() {
		return fileName1;
	}

	public void setFileName1(String fileName1) {
		this.fileName1 = fileName1;
	}

	public LogFileDetails() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LogFileDetails(String logFileId, String logId,
			@NotBlank(message = "logType must not be empty") String logType, String uploadedBy,
			@NotBlank(message = "Site Key must not be empty") String siteKey, String createdDateTime,
			String updatedDateTime, String fileName, String fileSize, String status, String response, String tenantId,
			String description, String extractedPath, String uploadedLogs, String masterLogs, String parsingStatus,
			String message, String parsingStartTime, String parsedDateTime, String cmdStatusParsing,
			String cmdStatusInsertion, String tempStatus, String filePaths, String fileName1, String username,
			String collectionDate) {
		super();
		this.logFileId = logFileId;
		this.logId = logId;
		this.logType = logType;
		this.uploadedBy = uploadedBy;
		this.siteKey = siteKey;
		this.createdDateTime = createdDateTime;
		this.updatedDateTime = updatedDateTime;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.status = status;
		this.response = response;
		this.tenantId = tenantId;
		this.description = description;
		this.extractedPath = extractedPath;
		this.uploadedLogs = uploadedLogs;
		this.masterLogs = masterLogs;
		this.parsingStatus = parsingStatus;
		this.message = message;
		this.parsingStartTime = parsingStartTime;
		this.parsedDateTime = parsedDateTime;
		CmdStatusParsing = cmdStatusParsing;
		CmdStatusInsertion = cmdStatusInsertion;
		this.tempStatus = tempStatus;
		this.filePaths = filePaths;
		this.fileName1 = fileName1;
		this.username = username;
		this.collectionDate = collectionDate;
	}

	

}