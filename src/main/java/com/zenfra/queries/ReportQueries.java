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
	
	private String sudoersSummaryHeader;
	
	private String taniumSudoersSummaryGroup;
	
	private String cedHeader;
	private String cedHeaderGroup;
	
	private String cedOtherHeader;
	private String cedOtherHeaderGroup;
	
	private String sudoersDetailHeader;
	private String taniumSudoersDetailGroup;
	
	private String adUserSummaryReportHeader;
	private String adUserSummaryReportGroup;
	
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

	public String getSudoersSummaryHeader() {
		return sudoersSummaryHeader;
	}

	public void setSudoersSummaryHeader(String sudoersSummaryHeader) {
		this.sudoersSummaryHeader = sudoersSummaryHeader;
	}

	public String getTaniumSudoersSummaryGroup() {
		return taniumSudoersSummaryGroup;
	}

	public void setTaniumSudoersSummaryGroup(String taniumSudoersSummaryGroup) {
		this.taniumSudoersSummaryGroup = taniumSudoersSummaryGroup;
	}

	public String getCedHeader() {
		return cedHeader;
	}

	public void setCedHeader(String cedHeader) {
		this.cedHeader = cedHeader;
	}

	public String getCedHeaderGroup() {
		return cedHeaderGroup;
	}

	public void setCedHeaderGroup(String cedHeaderGroup) {
		this.cedHeaderGroup = cedHeaderGroup;
	}

	public String getCedOtherHeader() {
		return cedOtherHeader;
	}

	public void setCedOtherHeader(String cedOtherHeader) {
		this.cedOtherHeader = cedOtherHeader;
	}

	public String getCedOtherHeaderGroup() {
		return cedOtherHeaderGroup;
	}

	public void setCedOtherHeaderGroup(String cedOtherHeaderGroup) {
		this.cedOtherHeaderGroup = cedOtherHeaderGroup;
	}

	public String getSudoersDetailHeader() {
		return sudoersDetailHeader;
	}

	public void setSudoersDetailHeader(String sudoersDetailHeader) {
		this.sudoersDetailHeader = sudoersDetailHeader;
	}

	public String getTaniumSudoersDetailGroup() {
		return taniumSudoersDetailGroup;
	}

	public void setTaniumSudoersDetailGroup(String taniumSudoersDetailGroup) {
		this.taniumSudoersDetailGroup = taniumSudoersDetailGroup;
	}

	public String getAdUserSummaryReportHeader() {
		return adUserSummaryReportHeader;
	}

	public void setAdUserSummaryReportHeader(String adUserSummaryReportHeader) {
		this.adUserSummaryReportHeader = adUserSummaryReportHeader;
	}

	public String getAdUserSummaryReportGroup() {
		return adUserSummaryReportGroup;
	}

	public void setAdUserSummaryReportGroup(String adUserSummaryReportGroup) {
		this.adUserSummaryReportGroup = adUserSummaryReportGroup;
	}
	
	
	
}
