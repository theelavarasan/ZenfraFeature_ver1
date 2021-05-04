package com.zenfra.queries;

import lombok.Data;

@Data
public class DashBoardChartsDetailsQueries {

	private String updateDynamicChartDetailsActiveFalseQuery;

	public String getUpdateDynamicChartDetailsActiveFalseQuery() {
		return updateDynamicChartDetailsActiveFalseQuery;
	}

	public void setUpdateDynamicChartDetailsActiveFalseQuery(String updateDynamicChartDetailsActiveFalseQuery) {
		this.updateDynamicChartDetailsActiveFalseQuery = updateDynamicChartDetailsActiveFalseQuery;
	}

}
