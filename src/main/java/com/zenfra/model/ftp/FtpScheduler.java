package com.zenfra.model.ftp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

@Entity
public class FtpScheduler {

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	@Column(name = "sheduler_id")
	private long id;

	@Column
	private String fileNameSettingsId;

	@Column
	private String type;

	
	
	@Column
	private String schedulerCorn;

	@Column
	private boolean isActive;
	
	@Column
	private String tenantId;
	
	@Column
	private String siteKey;
	
	@Column
	private String userId;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getFileNameSettingsId() {
		return fileNameSettingsId;
	}

	public void setFileNameSettingsId(String fileNameSettingsId) {
		this.fileNameSettingsId = fileNameSettingsId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSchedulerCorn() {
		return schedulerCorn;
	}

	public void setSchedulerCorn(String schedulerCorn) {
		this.schedulerCorn = schedulerCorn;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	
	
	

}
