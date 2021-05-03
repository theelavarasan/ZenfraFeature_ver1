package com.zenfra.queries;

import lombok.Data;

@Data
public class DashboardQueries {

	
	private String getDashboardLayoutChart;
	
	private String getDashboardLayoutChartLayout;

	public String getGetDashboardLayoutChart() {
		return getDashboardLayoutChart;
	}

	public void setGetDashboardLayoutChart(String getDashboardLayoutChart) {
		this.getDashboardLayoutChart = getDashboardLayoutChart;
	}

	public String getGetDashboardLayoutChartLayout() {
		return getDashboardLayoutChartLayout;
	}

	public void setGetDashboardLayoutChartLayout(String getDashboardLayoutChartLayout) {
		this.getDashboardLayoutChartLayout = getDashboardLayoutChartLayout;
	}
	
	
	
}
