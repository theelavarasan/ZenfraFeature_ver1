package com.zenfra.model;

import java.io.Serializable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



public class ReportResultModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String report_name;
	private String report_label;
	private JSONArray data = new JSONArray();
	private JSONArray columnOrder = new JSONArray();
	private JSONArray chartLayout = new JSONArray();
	private JSONArray chart = new JSONArray();
	private ZenfraJSONObject columnGroupInfo = new ZenfraJSONObject();
	private JSONArray headerInfo = new JSONArray();
	private JSONObject subLinkDetails = new JSONObject();
	private int chartOnly_enabled;
//	private JSONObject unit_conv_details;
	private ZenfraJSONObject unit_conv_details = new ZenfraJSONObject();
	
	private int responseCode;
	private String responseMessage;
	private String responseDescription;
	private JSONArray detailsData = new JSONArray();
	private JSONArray detailsHeaderInfo = new JSONArray();
	private JSONArray detailsColumnOrder = new JSONArray();
	private JSONArray groupedColumns = new JSONArray();
	private JSONArray sharedChartIds = new JSONArray();
	private boolean subReportAccess;
	
	public JSONArray getSharedChartIds() {
		return sharedChartIds;
	}
	public void setSharedChartIds(JSONArray sharedChartIds) {
		this.sharedChartIds = sharedChartIds;
	}
	public JSONArray getGroupedColumns() {
		return groupedColumns;
	}
	public void setGroupedColumns(JSONArray groupedColumns) {
		this.groupedColumns = groupedColumns;
	}
	public JSONArray getDetailsHeaderInfo() {
		return detailsHeaderInfo;
	}
	public void setDetailsHeaderInfo(JSONArray detailsHeaderInfo) {
		this.detailsHeaderInfo = detailsHeaderInfo;
	}
	public JSONArray getDetailsColumnOrder() {
		return detailsColumnOrder;
	}
	public void setDetailsColumnOrder(JSONArray detailsColumnOrder) {
		this.detailsColumnOrder = detailsColumnOrder;
	}
	public ZenfraJSONObject getUnit_conv_details()
	{
		return unit_conv_details;
	}
	public void setUnit_conv_details(ZenfraJSONObject unit_conv_details)
	{
		this.unit_conv_details = unit_conv_details;
	}
	public JSONArray getDetailsData() {
		return detailsData;
	}
	public void setDetailsData(JSONArray detailsData) {
		this.detailsData = detailsData;
	}
	public int getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	
	public String getResponseMessage() {
		return responseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
	
	public String getResponseDescription() {
		return responseDescription;
	}
	public void setResponseDescription(String responseDescription) {
		this.responseDescription = responseDescription;
	}
	
	public int getChartOnly_enabled() {
		return chartOnly_enabled;
	}
	public void setChartOnly_enabled(int chartOnly_enabled) {
		this.chartOnly_enabled = chartOnly_enabled;
	}
	
	public JSONObject getSubLinkDetails() {
		return subLinkDetails;
	}
	public void setSubLinkDetails(JSONObject subLinkDetails) {
		this.subLinkDetails = subLinkDetails;
	}
	
	public String getReport_name() {
		return report_name;
	}
	public void setReport_name(String report_name) {
		this.report_name = report_name;
	}
	
	public String getReport_label() {
		return report_label;
	}
	public void setReport_label(String report_label) {
		this.report_label = report_label;
	}
	
	public JSONArray getData() {
		return data;
	}
	public void setData(JSONArray data) {
		this.data = data;
	}
	
	public JSONArray getColumnOrder() {
		return columnOrder;
	}
	public void setColumnOrder(JSONArray columnOrder) {
		this.columnOrder = columnOrder;
	}
	
	public JSONArray getChartLayout() {
		return chartLayout;
	}
	public void setChartLayout(JSONArray chartLayout) {
		this.chartLayout = chartLayout;
	}
	
	public JSONArray getChart() {
		return chart;
	}
	public void setChart(JSONArray chart) {
		this.chart = chart;
	}
	
	public ZenfraJSONObject getColumnGroupInfo() {
		return columnGroupInfo;
	}
	public void setColumnGroupInfo(ZenfraJSONObject columnGroupInfo) {
		this.columnGroupInfo = columnGroupInfo;
	}
	
	public JSONArray getHeaderInfo() {
		return headerInfo;
	}
	public void setHeaderInfo(JSONArray headerInfo) {
		this.headerInfo = headerInfo;
	}

	public boolean isSubReportAccess() {
		return subReportAccess;
	}

	public void setSubReportAccess(boolean subReportAccess) {
		this.subReportAccess = subReportAccess;
	}
	
}
