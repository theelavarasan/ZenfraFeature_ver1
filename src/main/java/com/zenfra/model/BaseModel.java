package com.zenfra.model;

public class BaseModel
{
	
	String userId;
	public String getUserId()
	{
		return userId;
	}
	public void setUserId(String userId)
	{
		this.userId = userId;
	}
	public String getTenantId()
	{
		return tenantId;
	}
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}
	public String getSiteKey()
	{
		return siteKey;
	}
	public void setSiteKey(String siteKey)
	{
		this.siteKey = siteKey;
	}
	String tenantId;
	String siteKey;

}
