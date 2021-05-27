package com.zenfra.model.ftp;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.json.simple.JSONArray;


@Entity
public class FileNameSettingsModel implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Id
	private String fileNameSettingId;
	
	private String userId;
	private  String ipAddress;
	private  String siteKey;
	private JSONArray pattern;
	private boolean isActive;
	private String ftpName;
	
	private String toPath;
	
	
	public String getToPath() {
		return toPath;
	}
	public void setToPath(String toPath) {
		this.toPath = toPath;
	}
	public String getFileNameSettingId() {
		return fileNameSettingId;
	}
	public void setFileNameSettingId(String fileNameSettingId) {
		this.fileNameSettingId = fileNameSettingId;
	}
	public String getFtpName() {
		return ftpName;
	}
	public void setFtpName(String ftpName) {
		this.ftpName = ftpName;
	}
	public boolean getActive() {
		return isActive;
	}
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getSiteKey() {
		return siteKey;
	}
	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}
	public JSONArray getPattern() {
		return pattern;
	}
	public void setPattern(JSONArray pattern) {
		this.pattern = pattern;
	}
	
	
	
}
