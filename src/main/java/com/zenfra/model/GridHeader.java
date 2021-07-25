package com.zenfra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "displayName", "actualName", "dataType", "optional", "hide" })
public class GridHeader {

	@JsonProperty("displayName")
	private String displayName;
	@JsonProperty("actualName")
	private String actualName;
	@JsonProperty("dataType")
	private String dataType;

	@JsonProperty("hide")
	private boolean hide = false;

	@JsonProperty("optional")
	private Object optional;

	@JsonProperty("hide")
	public boolean getHide() {
		return hide;
	}

	@JsonProperty("hide")
	public void setHide(boolean hide) {
		this.hide = hide;
	}

	@JsonProperty("optional")
	public Object getOptional() {
		return optional;
	}

	@JsonProperty("optional")
	public void setOptional(Object optional) {
		this.optional = optional;
	}

	@JsonProperty("displayName")
	public String getDisplayName() {
		return displayName;
	}

	@JsonProperty("displayName")
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@JsonProperty("actualName")
	public String getActualName() {
		return actualName;
	}

	@JsonProperty("actualName")
	public void setActualName(String actualName) {
		this.actualName = actualName;
	}

	@JsonProperty("dataType")
	public String getDataType() {
		return dataType;
	}

	@JsonProperty("dataType")
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

}