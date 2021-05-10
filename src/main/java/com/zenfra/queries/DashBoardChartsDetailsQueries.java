package com.zenfra.queries;

import lombok.Data;

@Data
public class DashBoardChartsDetailsQueries {

	private String updateDynamicChartDetailsActiveFalseQuery;
	
	private String getByChartIdSiteKey;
	
	

	public String getGetByChartIdSiteKey() {
		return getByChartIdSiteKey;
	}

	public void setGetByChartIdSiteKey(String getByChartIdSiteKey) {
		this.getByChartIdSiteKey = getByChartIdSiteKey;
	}

	public String getUpdateDynamicChartDetailsActiveFalseQuery() {
		return updateDynamicChartDetailsActiveFalseQuery;
	}

	public void setUpdateDynamicChartDetailsActiveFalseQuery(String updateDynamicChartDetailsActiveFalseQuery) {
		this.updateDynamicChartDetailsActiveFalseQuery = updateDynamicChartDetailsActiveFalseQuery;
	}

}
