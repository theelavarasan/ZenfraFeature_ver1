package com.zenfra.model.ftp;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

@Entity
public class FTPServerModel implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	private String serverId;
	
	@NotBlank(message = "siteKey must not be empty")
	private String siteKey;	
	
	@NotBlank(message = "ftpName must not be empty")
	private String ftpName;
	
	@NotBlank(message = "serverUsername must not be empty")
	private String serverUsername;
	
	@NotBlank(message = "port must not be empty")
	private String port;
	private String create_by;
	private String create_time;
	
	private String update_by;
	private String updated_time;
	
	private String timeoutLimit;
	
	public boolean isNas;
	
	private boolean isActive;
	public boolean isActive() {
		return isActive;
	}
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	public boolean isNas() {
		return isNas;
	}

	public void setNas(boolean isNas) {
		this.isNas = isNas;
	}
	
	@NotBlank(message = "ipAddress must not be empty")
	private  String ipAddress;
	
	@NotBlank(message = "serverPath must not be empty")
	private String serverPath;
	
	@NotBlank(message = "serverPassword must not be empty")
	private String serverPassword;
	
	@NotBlank(message = "userId must not be empty")
	private String userId;
	
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public String getSiteKey() {
		return siteKey;
	}
	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getFtpName() {
		return ftpName;
	}
	public void setFtpName(String ftpName) {
		this.ftpName = ftpName;
	}
	public String getServerUsername() {
		return serverUsername;
	}
	public void setServerUsername(String serverUsername) {
		this.serverUsername = serverUsername;
	}
	

	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getServerPath() {
		return serverPath;
	}
	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}
	public String getServerPassword() {
		return serverPassword;
	}
	public void setServerPassword(String serverPassword) {
		this.serverPassword = serverPassword;
	}
	public String getCreate_by() {
		return create_by;
	}
	public void setCreate_by(String create_by) {
		this.create_by = create_by;
	}
	public String getCreate_time() {
		return create_time;
	}
	public void setCreate_time(String create_time) {
		this.create_time = create_time;
	}
	public String getUpdate_by() {
		return update_by;
	}
	public void setUpdate_by(String update_by) {
		this.update_by = update_by;
	}
	public String getUpdated_time() {
		return updated_time;
	}
	public void setUpdated_time(String updated_time) {
		this.updated_time = updated_time;
	}
	public String getTimeoutLimit() {
		return timeoutLimit;
	}
	public void setTimeoutLimit(String timeoutLimit) {
		this.timeoutLimit = timeoutLimit;
	}

	

}
