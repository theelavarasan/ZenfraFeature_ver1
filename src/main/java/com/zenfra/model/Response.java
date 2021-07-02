package com.zenfra.model;

import java.io.Serializable;

import org.json.simple.JSONObject;

public class Response implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5394376835296146678L;
	private String responseMsg;
	private int responseCode;
	private String type;
	private String data;
	private JSONObject jData;

	public JSONObject getjData() {
		return jData;
	}

	public void setjData(JSONObject jData) {
		this.jData = jData;
	}

	public static final int RESPONSE_SUCCESS = 0;
	public static final int RESPONSE_ERROR = 1;

	public String getResponseMsg()
	{
		return responseMsg;
	}

	public int getResponseCode()
	{
		return responseCode;
	}

	public String getType()
	{
		return type;
	}

	public String getData()
	{
		return data;
	}

	public void setResponseMsg(String responseMsg)
	{
		this.responseMsg = responseMsg;
	}

	public void setResponseCode(int responseCode)
	{
		this.responseCode = responseCode;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public void setData(String data)
	{
		this.data = data;
	}

}
