package com.zenfra.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.json.simple.JSONObject;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;


@Entity
@Table(name="chart")
@TypeDef(
	    name = "list-array",
	    typeClass = ListArrayType.class
	)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class ChartModel_v2 {

	public ChartModel_v2() {

	}

	

	public ChartModel_v2(String chartId, JSONObject chartConfiguration, String chartName, String chartType,
			String reportName, String siteKey, String userId, String createdTime, String chartDesc, boolean isVisible,
			boolean isDefault, String analyticsType, String analyticsFor, boolean dashboard, JSONObject filterProperty,
			boolean isActive, String updateTime, List<String> userAccessList, List<String> siteAccessList,
			List<String> categoryList,JSONObject chartDetails) {
		super();
		this.chartId = chartId;
		this.chartConfiguration = chartConfiguration;
		this.chartName = chartName;
		this.chartType = chartType;
		this.reportName = reportName;
		this.siteKey = siteKey;
		this.userId = userId;
		this.createdTime = createdTime;
		this.chartDesc = chartDesc;
		this.isVisible = isVisible;
		this.isDefault = isDefault;
		this.analyticsType = analyticsType;
		this.analyticsFor = analyticsFor;
		this.dashboard = dashboard;
		this.filterProperty = filterProperty;
		this.isActive = isActive;
		this.updateTime = updateTime;
		this.userAccessList = userAccessList;
		this.siteAccessList = siteAccessList;
		this.categoryList = categoryList;
		this.chartDetails=chartDetails;
	}



	@Id
	@Column(name="chart_id")
	private String chartId;
	
	@Type(type = "jsonb")
	@Column(name="chart_configuration")
	private JSONObject chartConfiguration;
	
	@Column(name="chart_name")
	private String chartName;
	
	@Column(name="chart_type")
	private String chartType;
	
	@Column(name="report_name")
	private String reportName;
	
	@Column(name="site_key")
	private String siteKey;
	
	@Column(name="user_id")
	private String userId;
	
	@Column(name="created_time")
	private String createdTime;
	
	@Column(name="chart_desc")
	private String chartDesc;
	
	@Column(name="is_visible")
	private boolean isVisible;
	
	@Column(name="is_default")
	private boolean isDefault;
	
	
	@Column(name="analytics_type")
	private String analyticsType;
	
	@Column(name="analytics_for")
	private String analyticsFor;
	
	@Column(name="is_dashboard")
	private boolean dashboard;
	
	@Type(type = "jsonb")
	@Column(name="filter_property")
	private JSONObject filterProperty;
	
	@Column(name="is_active")
	private boolean isActive;

	
	@Column(name="update_time")
	private String updateTime;

	@Type(type = "list-array")
	@Column(name="user_access_list",columnDefinition = "text[]")
	private List<String> userAccessList = new ArrayList<>();
	
	@Type(type = "list-array")
	@Column(name="site_access_list",columnDefinition = "text[]")
	private List<String> siteAccessList = new ArrayList<>();
	
	@Type(type = "list-array")
	@Column(name="category_list",columnDefinition = "text[]")
	private List<String> categoryList = new ArrayList<>();
	
	@Type(type = "jsonb")
	@Column(name="chart_details")
	private JSONObject chartDetails;
	
	
	
	@Column(name="report_label")
	private String reportLabel;

	public boolean getActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean getVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public String getChartDesc() {
		return chartDesc;
	}

	public void setChartDesc(String chartDesc) {
		this.chartDesc = chartDesc;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}


	public List<String> getUserAccessList() {
		return userAccessList;
	}

	public void setUserAccessList(List<String> userAccessList) {

		if (userAccessList == null) {
			userAccessList = new ArrayList<>();
		}

		this.userAccessList = userAccessList;
	}

	public List<String> getSiteAccessList() {
		return siteAccessList;
	}

	public void setSiteAccessList(List<String> siteAccessList) {

		if (siteAccessList == null) {
			siteAccessList = new ArrayList<>();
		}
		this.siteAccessList = siteAccessList;
	}

	public List<String> getCategoryList() {
		return categoryList;
	}

	public void setCategoryList(List<String> categoryList) {

		if (categoryList == null) {
			categoryList = new ArrayList<>();
		}

		this.categoryList = categoryList;
	}

	public JSONObject getChartConfiguration() {
		return chartConfiguration;
	}

	public void setChartConfiguration(JSONObject chartConfiguration) {
		this.chartConfiguration = chartConfiguration;
	}

	public String getChartName() {
		return chartName;
	}

	public void setChartName(String chartName) {
		this.chartName = chartName;
	}

	public String getChartType() {
		return chartType;
	}

	public void setChartType(String chartType) {
		this.chartType = chartType;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getChartId() {
		return chartId;
	}

	public void setChartId(String chartId) {
		this.chartId = chartId;
	}

	public boolean getDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getAnalyticsType() {
		return analyticsType;
	}

	public void setAnalyticsType(String analyticsType) {
		this.analyticsType = analyticsType;
	}

	public String getAnalyticsFor() {
		return analyticsFor;
	}

	public void setAnalyticsFor(String analyticsFor) {
		this.analyticsFor = analyticsFor;
	}

	public boolean isDashboard() {
		return dashboard;
	}

	public void setDashboard(boolean dashboard) {
		this.dashboard = dashboard;
	}

	

	public JSONObject getFilterProperty() {
		return filterProperty;
	}

	
	
	public void setFilterProperty(JSONObject filterProperty) {
		this.filterProperty = filterProperty;
	}

	
	public JSONObject getChartDetails() {
		return chartDetails;
	}



	public void setChartDetails(JSONObject chartDetails) {
		this.chartDetails = chartDetails;
	}
	
	
	
	public String getReportLabel() {
		return reportLabel;
	}



	public void setReportLabel(String reportLabel) {
		this.reportLabel = reportLabel;
	}


	@Override
	public String toString() {
		return "ChartModel_v2 [chartId=" + chartId + ", chartConfiguration=" + chartConfiguration + ", chartName="
				+ chartName + ", chartType=" + chartType + ", reportName=" + reportName + ", siteKey=" + siteKey
				+ ", userId=" + userId + ", createdTime=" + createdTime + ", chartDesc=" + chartDesc + ", isVisible="
				+ isVisible + ", isDefault=" + isDefault + ", analyticsType=" + analyticsType + ", analyticsFor="
				+ analyticsFor + ", dashboard=" + dashboard + ", filterProperty=" + filterProperty + ", isActive="
				+ isActive + ", updateTime=" + updateTime + ", userAccessList=" + userAccessList + ", siteAccessList="
				+ siteAccessList + ", categoryList=" + categoryList + "]";
	}


	
	
	
}
