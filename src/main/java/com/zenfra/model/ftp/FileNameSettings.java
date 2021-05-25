package com.zenfra.model.ftp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class FileNameSettings {

	
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	@Column
	private long id;

	@Column
	private  String siteKey;
	
	
	@Column
	private String connectionName;
	
	
	
	@Column
	private String pattern;
	
	
	@Column
	private String logType;
	
	
	@Column
	private String toPath;


	
	
	public String getToPath() {
		return toPath;
	}


	public void setToPath(String toPath) {
		this.toPath = toPath;
	}


	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}


	public String getSiteKey() {
		return siteKey;
	}


	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}


	public String getConnectionName() {
		return connectionName;
	}


	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}


	public String getPattern() {
		return pattern;
	}


	public void setPattern(String pattern) {
		this.pattern = pattern;
	}


	public String getLogType() {
		return logType;
	}


	public void setLogType(String logType) {
		this.logType = logType;
	}
	
	
	
	
}
