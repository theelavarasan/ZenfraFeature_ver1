package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
public class ReportQueries {

	private String header;
	
	private String numbericalHeader;
	
	private String headerFilter;
	
	private String chartLayout;
	
	private String reportUserCustomData;
	
	private String headerForCompatibility;

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getNumbericalHeader() {
		return numbericalHeader;
	}

	public void setNumbericalHeader(String numbericalHeader) {
		this.numbericalHeader = numbericalHeader;
	}

	public String getHeaderFilter() {
		return headerFilter;
	}

	public void setHeaderFilter(String headerFilter) {
		this.headerFilter = headerFilter;
	}

	public String getChartLayout() {
		return chartLayout;
	}

	public void setChartLayout(String chartLayout) {
		this.chartLayout = chartLayout;
	}

	public String getReportUserCustomData() {
		return reportUserCustomData;
	}

	public void setReportUserCustomData(String reportUserCustomData) {
		this.reportUserCustomData = reportUserCustomData;
	}

	public String getHeaderForCompatibility() {
		return headerForCompatibility;
	}

	public void setHeaderForCompatibility(String headerForCompatibility) {
		this.headerForCompatibility = headerForCompatibility;
	}

	
	
	
	
}
