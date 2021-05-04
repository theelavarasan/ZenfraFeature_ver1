package com.zenfra.model;

import java.io.Serializable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;

public class ResponseModel_v2 implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String responseMessage;
	private HttpStatus responseCode;
	private String responseDescription;
	private JSONArray data;
	private Object jData;
	private boolean validation;
	
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
	
	
	
}
