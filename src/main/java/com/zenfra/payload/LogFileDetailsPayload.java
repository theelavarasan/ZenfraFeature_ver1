package com.zenfra.payload;

import java.util.List;

import javax.validation.constraints.NotEmpty;

public class LogFileDetailsPayload {

	
	
	private String logtype;
	
	
	private String description;
	
	
	private List<String> logFileIds;

	public String getLogtype() {
		return logtype;
	}

	public void setLogtype(String logtype) {
		this.logtype = logtype;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getLogFileIds() {
		return logFileIds;
	}

	public void setLogFileIds(List<String> logFileIds) {
		this.logFileIds = logFileIds;
	}
	
	
	
	
}
