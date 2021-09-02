package com.zenfra.model;

import java.io.Serializable;





public class BaseModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String siteKey;
	private String tenantId;
	private String userId;
	private String logDate;
	private String siteName;
	private String rid;
	private String path;
	private String logId;
	private String type;
	
	public String getSiteKey() {
		return siteKey;
	}
	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getLogDate() {
		return logDate;
	}
	public void setLogDate(String logDate) {
		this.logDate = logDate;
	}
	public String getSiteName() {
		return siteName;
	}
	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}
	public String getRid() {
		return rid;
	}
	public void setRid(String rid) {
		this.rid = rid;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getLogId() {
		return logId;
	}
	public void setLogId(String logId) {
		this.logId = logId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String toString() {

     return siteKey +" : " + tenantId + " : " + logDate + " : " + siteName + " : " + path + " : " + type;
			
		 }

}
