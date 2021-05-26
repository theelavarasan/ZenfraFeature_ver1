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

	
	
	

}
