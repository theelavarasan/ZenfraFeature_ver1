package com.zenfra.model.ftp;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.json.simple.JSONArray;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

@Entity
public class FileNameSettingsModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String fileNameSettingId;

	@NotBlank(message = "userId must not be empty")
	private String userId;

	@NotBlank(message = "ipAddress must not be empty")
	private String ipAddress;

	@NotBlank(message = "siteKey must not be empty")
	private String siteKey;

	private String patternString;

	private boolean isActive;

	public boolean isNas;

	@NotBlank(message = "ftpName must not be empty")
	private String ftpName;

	@JsonProperty(access = Access.WRITE_ONLY)
	private String toPath;

	private JSONArray pattern;

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

	public boolean isNas() {
		return isNas;
	}

	public void setNas(boolean isNas) {
		this.isNas = isNas;
	}

	@Override
	public String toString() {
		return "FileNameSettingsModel [fileNameSettingId=" + fileNameSettingId + ", userId=" + userId + ", ipAddress="
				+ ipAddress + ", siteKey=" + siteKey + ", pattern=" + pattern + ", isActive=" + isActive + ", ftpName="
				+ ftpName + ", toPath=" + toPath + "]";
	}

	public String getPatternString() {
		return patternString;
	}

	public void setPatternString(String patternString) {
		this.patternString = patternString;
	}

}
