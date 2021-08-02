package com.zenfra.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "columnOrder", "headerInfo", "data" })
public class GridDataFormat {

	@JsonProperty("columnOrder")
	private List<Object> columnOrder = null;
	@JsonProperty("headerInfo")
	private List<GridHeader> headerInfo = null;
	@JsonProperty("data")
	private List<Object> data = null;

	@JsonProperty("columnOrder")
	public List<Object> getColumnOrder() {
		return columnOrder;
	}

	@JsonProperty("columnOrder")
	public void setColumnOrder(List<Object> columnOrder) {
		this.columnOrder = columnOrder;
	}

	@JsonProperty("headerInfo")
	public List<GridHeader> getHeaderInfo() {
		return headerInfo;
	}

	@JsonProperty("headerInfo")
	public void setHeaderInfo(List<GridHeader> headerInfo) {
		this.headerInfo = headerInfo;
	}

	@JsonProperty("data")
	public List<Object> getData() {
		return data;
	}

	@JsonProperty("data")
	public void setData(List<Object> data) {
		this.data = data;
	}

}