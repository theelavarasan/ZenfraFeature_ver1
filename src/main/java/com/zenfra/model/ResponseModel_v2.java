package com.zenfra.model;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONArray;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseModel_v2 implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String responseMessage;
	private HttpStatus responseCode;
	private int statusCode;
	private String responseDescription;
	private JSONArray data;
	private Object jData;
	private boolean validation;
	private List<Object> columnOrder;
	@JsonProperty("headerInfo")
	private List<GridHeader> headerInfo = null;
	
	private List<Object> groupedColumns;
	
	public String getResponseMessage() {
		return responseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
	public JSONArray getData() {
		return data;
	}
	public void setData(JSONArray data) {
		this.data = data;
	}
	public HttpStatus getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(HttpStatus responseCode) {
		this.responseCode = responseCode;
	}
	public String getResponseDescription() {
		return responseDescription;
	}
	public void setResponseDescription(String responseDescription) {
		this.responseDescription = responseDescription;
	}
	public void setValidation(boolean validation)
	{
		this.validation=validation;
		
	}
	public Boolean getValidation() {
		return validation;
	}
	public Object getjData() {
		return jData;
	}
	public void setjData(Object jData) {
		this.jData = jData;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public List<Object> getColumnOrder() {
		return columnOrder;
	}
	public void setColumnOrder(List<Object> columnOrder) {
		this.columnOrder = columnOrder;
	}
	public List<GridHeader> getHeaderInfo() {
		return headerInfo;
	}
	public void setHeaderInfo(List<GridHeader> headerInfo) {
		this.headerInfo = headerInfo;
	}
	public List<Object> getGroupedColumns() {
		return groupedColumns;
	}
	public void setGroupedColumns(List<Object> groupedColumns) {
		this.groupedColumns = groupedColumns;
	}
	
	
	
}
