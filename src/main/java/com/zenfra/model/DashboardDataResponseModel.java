package com.zenfra.model;

import org.json.simple.JSONArray;

public class DashboardDataResponseModel {
	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JSONArray xaxis;
	private JSONArray yaxis;
	private JSONArray data;
	private JSONArray color;
	
	public JSONArray getData() {
		return data;
	}
	public void setData(JSONArray data) {
		this.data = data;
	}
	public JSONArray getXaxis() {
		return xaxis;
	}
	public void setXaxis(JSONArray xaxis) {
		this.xaxis = xaxis;
	}
	public JSONArray getYaxis() {
		return yaxis;
	}
	public void setYaxis(JSONArray yaxis) {
		this.yaxis = yaxis;
	}
	public JSONArray getColor() {
		return color;
	}
	public void setColor(JSONArray color) {
		this.color = color;
	}
	
	
	



}
