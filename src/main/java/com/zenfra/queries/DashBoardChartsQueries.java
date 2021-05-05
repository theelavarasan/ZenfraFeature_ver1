package com.zenfra.queries;

import lombok.Data;

@Data
public class DashBoardChartsQueries {

	private String delete;
	private String saveOrUpdateDashboardChart;
	
	private String getSiteKeyUserIdChartId;
	
	private String getByChartIdSiteKeyUserId;
	
	

	public String getGetByChartIdSiteKeyUserId() {
		return getByChartIdSiteKeyUserId;
	}

	public void setGetByChartIdSiteKeyUserId(String getByChartIdSiteKeyUserId) {
		this.getByChartIdSiteKeyUserId = getByChartIdSiteKeyUserId;
	}

	public String getGetSiteKeyUserIdChartId() {
		return getSiteKeyUserIdChartId;
	}

	public void setGetSiteKeyUserIdChartId(String getSiteKeyUserIdChartId) {
		this.getSiteKeyUserIdChartId = getSiteKeyUserIdChartId;
	}

	public String getSaveOrUpdateDashboardChart() {
		return saveOrUpdateDashboardChart;
	}

	public void setSaveOrUpdateDashboardChart(String saveOrUpdateDashboardChart) {
		this.saveOrUpdateDashboardChart = saveOrUpdateDashboardChart;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}

}
