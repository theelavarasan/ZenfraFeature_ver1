package com.zenfra.model.ftp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class CheckSumDetails {

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	@Column(name = "check_sum_id")
	private long data_id;
	
	private String fileName;
	
	private String checkSum;
	
	private String clientFtpServerId;
	
	private String siteKey;

	public long getData_id() {
		return data_id;
	}

	public void setData_id(long data_id) {
		this.data_id = data_id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(String checkSum) {
		this.checkSum = checkSum;
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
	
	
}
