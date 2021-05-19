package com.zenfra.model;

import javax.validation.constraints.NotBlank;

import org.json.simple.JSONArray;

public class AwsInventory {

	
	
	private String data_id;
	
	@NotBlank
	private String userid;
	
	@NotBlank
	private String sitekey;
	
	@NotBlank
	private String access_key_id;
	
	@NotBlank
	private String secret_access_key;
	
	@NotBlank
	private String description;
	
	@NotBlank
	private JSONArray regions;
	
	
	private String created_date;
	
	private String updated_date;
	
	private String lastFourKey;
	
	
	

	public String getLastFourKey() {
		return lastFourKey;
	}

	public void setLastFourKey(String lastFourKey) {
		this.lastFourKey = lastFourKey;
	}

	public String getData_id() {
		return data_id;
	}

	public void setData_id(String data_id) {
		this.data_id = data_id;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getSitekey() {
		return sitekey;
	}

	public void setSitekey(String sitekey) {
		this.sitekey = sitekey;
	}

	public String getAccess_key_id() {
		return access_key_id;
	}

	public void setAccess_key_id(String access_key_id) {
		this.access_key_id = access_key_id;
	}

	public String getSecret_access_key() {
		return secret_access_key;
	}

	public void setSecret_access_key(String secret_access_key) {
		this.secret_access_key = secret_access_key;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public JSONArray getRegions() {
		return regions;
	}

	public void setRegions(JSONArray regions) {
		this.regions = regions;
	}

	public String getCreated_date() {
		return created_date;
	}

	public void setCreated_date(String created_date) {
		this.created_date = created_date;
	}

	public String getUpdated_date() {
		return updated_date;
	}

	public void setUpdated_date(String updated_date) {
		this.updated_date = updated_date;
	}
	
	
	
	
	
	
}
