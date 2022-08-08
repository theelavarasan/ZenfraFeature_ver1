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
	
	private String reportUserCustomDataBySiteKey;
	
	private String headerForCompatibility;
	
	private String securityAddSourceHeader;
	
	private String taniumHeader;
	
	private String taniumGroup;

	private String userSummaryHeader;
	
	private String serverSummaryHeader;
	
	private String taniumUserSummaryGroup;
	
	private String taniumServerSummaryGroup;
	
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

	public String getReportUserCustomDataBySiteKey() {
		return reportUserCustomDataBySiteKey;
	}

	public void setReportUserCustomDataBySiteKey(String reportUserCustomDataBySiteKey) {
		this.reportUserCustomDataBySiteKey = reportUserCustomDataBySiteKey;
	}


	public String getHeaderForCompatibility() {
		return headerForCompatibility;
	}

	public void setHeaderForCompatibility(String headerForCompatibility) {
		this.headerForCompatibility = headerForCompatibility;
	}

	public String getSecurityAddSourceHeader() {
		return securityAddSourceHeader;
	}

	public void setSecurityAddSourceHeader(String securityAddSourceHeader) {
		this.securityAddSourceHeader = securityAddSourceHeader;
	}

	public String getTaniumHeader() {
		return taniumHeader;
	}

	public void setTaniumHeader(String taniumHeader) {
		this.taniumHeader = taniumHeader;
	}

	public String getTaniumGroup() {
		return taniumGroup;
	}

	public void setTaniumGroup(String taniumGroup) {
		this.taniumGroup = taniumGroup;
	}

	public String getUserSummaryHeader() {
		return userSummaryHeader;
	}

	public void setUserSummaryHeader(String userSummaryHeader) {
		this.userSummaryHeader = userSummaryHeader;
	}

	public String getServerSummaryHeader() {
		return serverSummaryHeader;
	}

	public void setServerSummaryHeader(String serverSummaryHeader) {
		this.serverSummaryHeader = serverSummaryHeader;
	}

	public String getTaniumUserSummaryGroup() {
		return taniumUserSummaryGroup;
	}

	public void setTaniumUserSummaryGroup(String taniumUserSummaryGroup) {
		this.taniumUserSummaryGroup = taniumUserSummaryGroup;
	}

	public String getTaniumServerSummaryGroup() {
		return taniumServerSummaryGroup;
	}

	public void setTaniumServerSummaryGroup(String taniumServerSummaryGroup) {
		this.taniumServerSummaryGroup = taniumServerSummaryGroup;
	}
	
}
