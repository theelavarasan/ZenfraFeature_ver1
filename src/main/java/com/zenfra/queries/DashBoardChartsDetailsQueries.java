package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("dashboard.charts.details")
@PropertySource("classpath:quries.properties")
public class DashBoardChartsDetailsQueries {

	private String updateDynamicChartDetailsActiveFalseQuery;

	public String getUpdateDynamicChartDetailsActiveFalseQuery() {
		return updateDynamicChartDetailsActiveFalseQuery;
	}

	public void setUpdateDynamicChartDetailsActiveFalseQuery(String updateDynamicChartDetailsActiveFalseQuery) {
		this.updateDynamicChartDetailsActiveFalseQuery = updateDynamicChartDetailsActiveFalseQuery;
	}

	
	
}
