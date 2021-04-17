package com.zenfra.model;

import java.io.Serializable;

import org.json.simple.JSONArray;
import org.springframework.http.HttpStatus;

public class ResponseModel implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String responseMessage;
	private HttpStatus responseCode;
	private String responseDescription;
	private JSONArray data;
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
	public void setResponseCode(HttpStatus ok) {
		this.responseCode = ok;
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
	
	
}
