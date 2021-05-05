package com.zenfra.queries;

import lombok.Data;

@Data
public class DashboardQueries {

	
	private String getDashboardLayoutChart;
	
	private String getDashboardLayoutChartLayout;
	
	private String getChatForFavMenu;
	
	private String getDashboardChartDetails;
	
	
	public String getGetDashboardChartDetails() {
		return getDashboardChartDetails;
	}

	public void setGetDashboardChartDetails(String getDashboardChartDetails) {
		this.getDashboardChartDetails = getDashboardChartDetails;
	}

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

	public String getGetChatForFavMenu() {
		return getChatForFavMenu;
	}

	public void setGetChatForFavMenu(String getChatForFavMenu) {
		this.getChatForFavMenu = getChatForFavMenu;
	}
	
	
	
}
