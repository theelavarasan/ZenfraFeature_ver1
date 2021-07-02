package com.zenfra.model.ftp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CheckSumDetails {

	@Id
	@Column(name = "check_sum_id")
	private String data_id;
	
	private String fileName;
	
	
	private String clientFtpServerId;
	
	private String siteKey;

	private String fileSize;
	
	private String createDate;

	public String getData_id() {
		return data_id;
	}



	public void setData_id(String data_id) {
		this.data_id = data_id;
	}

	

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	

	public String getClientFtpServerId() {
		return clientFtpServerId;
	}

	public void setClientFtpServerId(String clientFtpServerId) {
		this.clientFtpServerId = clientFtpServerId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}



	public String getFileSize() {
		return fileSize;
	}



	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}



	public String getCreateDate() {
		return createDate;
	}



	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}
	
	
}
