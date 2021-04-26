package com.zenfra.model;

import org.json.simple.JSONObject;

public class ChartDetailsModel {
	

	private String chartName;
	private String chartId;
	private JSONObject chartDetails;
	private String siteKey;
	private String favouriteId;
	public String getChartName() {
		return chartName;
	}
	public void setChartName(String chartName) {
		this.chartName = chartName;
	}
	public String getChartId() {
		return chartId;
	}
	public void setChartId(String chartId) {
		this.chartId = chartId;
	}
	public JSONObject getChartDetails() {
		return chartDetails;
	}
	public void setChartDetails(JSONObject chartDetails) {
		this.chartDetails = chartDetails;
	}
	public String getSiteKey() {
		return siteKey;
	}
	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}
	public String getFavouriteId() {
		return favouriteId;
	}
	public void setFavouriteId(String favouriteId) {
		this.favouriteId = favouriteId;
	}
	
	

}
