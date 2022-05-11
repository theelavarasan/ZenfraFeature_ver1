package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="report_columns")
public class HeaderInfoModel {

	@Column(name="device_type")
	private String deviceType;
	
	@Id
	@Column(name="report_data_id")
	private String reportDataId;
	
	@Column(name="report_name")
	private String reportName;
	
	@Column(name="data_type")
	private String dataType;
	
	@Column(name="is_size_metrics")
	private String isSizeMetrics;
	
	@Column(name="seq")
	private String seq;
	
	@Column(name="column_name")
	private String columnName;
	
	@Column(name="report_by")
	private String reportBy;
	
	@Column(name="db_field_name")
	private String dbFieldName;
	
	@Column(name="is_pinned")
	private String isPinned;
	
	@Column(name="takslist_sub_category")
	private String takslistSubCategory;
	
	@Column(name="alias_name")
	private String aliasName;
	
	@Column(name="devices")
	private String devices;
	
	@Column(name="tasklist_category")
	private String takslistCategory;
	
	@Column(name="category_seq")
	private String categorySeq;
	
	@Column(name="sub_category_seq")
	private String subCategorySeq;

	@Column(name="hide")
	private boolean hide;

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getReportDataId() {
		return reportDataId;
	}

	public void setReportDataId(String reportDataId) {
		this.reportDataId = reportDataId;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getIsSizeMetrics() {
		return isSizeMetrics;
	}

	public void setIsSizeMetrics(String isSizeMetrics) {
		this.isSizeMetrics = isSizeMetrics;
	}

	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getReportBy() {
		return reportBy;
	}

	public void setReportBy(String reportBy) {
		this.reportBy = reportBy;
	}

	public String getDbFieldName() {
		return dbFieldName;
	}

	public void setDbFieldName(String dbFieldName) {
		this.dbFieldName = dbFieldName;
	}

	public String getIsPinned() {
		return isPinned;
	}

	public void setIsPinned(String isPinned) {
		this.isPinned = isPinned;
	}

	public String getTakslistSubCategory() {
		return takslistSubCategory;
	}

	public void setTakslistSubCategory(String takslistSubCategory) {
		this.takslistSubCategory = takslistSubCategory;
	}

	public String getAliasName() {
		return aliasName;
	}

	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}

	public String getDevices() {
		return devices;
	}

	public void setDevices(String devices) {
		this.devices = devices;
	}

	public String getTakslistCategory() {
		return takslistCategory;
	}

	public void setTakslistCategory(String takslistCategory) {
		this.takslistCategory = takslistCategory;
	}

	public String getCategorySeq() {
		return categorySeq;
	}

	public void setCategorySeq(String categorySeq) {
		this.categorySeq = categorySeq;
	}

	public String getSubCategorySeq() {
		return subCategorySeq;
	}

	public void setSubCategorySeq(String subCategorySeq) {
		this.subCategorySeq = subCategorySeq;
	}

	public boolean getHide() {
		return hide;
	}

	public void setHide(boolean hide) {
		this.hide = hide;
	}

	public HeaderInfoModel(String deviceType, String reportDataId, String reportName, String dataType,
			String isSizeMetrics, String seq, String columnName, String reportBy, String dbFieldName, String isPinned,
			String takslistSubCategory, String aliasName, String devices, String takslistCategory, String categorySeq,
			String subCategorySeq, boolean hide) {
		super();
		this.deviceType = deviceType;
		this.reportDataId = reportDataId;
		this.reportName = reportName;
		this.dataType = dataType;
		this.isSizeMetrics = isSizeMetrics;
		this.seq = seq;
		this.columnName = columnName;
		this.reportBy = reportBy;
		this.dbFieldName = dbFieldName;
		this.isPinned = isPinned;
		this.takslistSubCategory = takslistSubCategory;
		this.aliasName = aliasName;
		this.devices = devices;
		this.takslistCategory = takslistCategory;
		this.categorySeq = categorySeq;
		this.subCategorySeq = subCategorySeq;
		this.hide = hide;
	}

	public HeaderInfoModel() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	

}
