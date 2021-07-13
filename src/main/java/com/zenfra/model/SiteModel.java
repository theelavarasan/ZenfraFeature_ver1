package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.json.simple.JSONArray;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel(value = "Site", description = "Site  grid operations")
public class SiteModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@ApiModelProperty(hidden = true)
	private String siteDataId;

	@Column
	@ApiModelProperty(hidden = true, value = "updated time", name = "updatedTime", dataType = "String", example = "01/05/2021 09:01:11 AM")
	// @NotBlank(message = "logType must not be empty")
	private String updatedTime;

	@Column
	@ApiModelProperty(hidden = true, value = "siteKey", name = "siteKey", dataType = "String", example = "64a1e8aa-0584-41ee-b9e4-fb7db5e554b1")
	// @NotBlank(message = "logType must not be empty")
	private String siteKey;

	@Column
	@ApiModelProperty(hidden = true, value = "updated by", name = "updatedBy", dataType = "String", example = "64a1e8aa-0584-41ee-b9e4-fb7db5e554b1")
	// @NotBlank(message = "logType must not be empty")
	private String updatedBy;

	@Column
	@ApiModelProperty(value = "columnOrder", name = "columnOrder", dataType = "Array", example = "['siteName','isActive']")
	// @NotBlank(message = "logType must not be empty")
	private JSONArray columnOrder;

	@Column
	@ApiModelProperty(value = "createBy", name = "createBy", dataType = "Array", example = "64a1e8aa-0584-41ee-b9e4-fb7db5e554b1")
	@NotBlank(message = "createBy must not be empty")
	private String createBy;

	@Column
	@ApiModelProperty(value = "tenantId", name = "tenantId", dataType = "String", example = "64a1e8aa-0584-41ee-b9e4-fb7db5e554b1")
	// @NotBlank(message = "tenantId must not be empty")
	private String tenantId;

	@Column
	@ApiModelProperty(value = "siteName", name = "siteName", dataType = "String", example = "64a1e8aa-0584-41ee-b9e4-fb7db5e554b1")
	@NotBlank(message = "Site name must not be empty")
	private String siteName;

	@Column
	@ApiModelProperty(value = "createTime", name = "createTime", dataType = "String", example = "01/05/2021 01:12:22 PM")
	@NotBlank(message = "createTime must not be empty")
	private String createTime;

	@Column
	@ApiModelProperty(value = "isActive", name = "isActive", dataType = "boolean", example = "true")
	// @NotBlank(message = "Activ must not be empty")
	private boolean isActive = true;

	@Column
	@ApiModelProperty(value = "email", name = "email", dataType = "String", example = "zenfra@zenfra.co")
	@NotBlank(message = "Email must not be empty")
	private String email;

	@Column
	@ApiModelProperty(value = "siteAddress", name = "siteAddress", dataType = "String", example = "39,test,coimbatore")
	// @NotBlank(message = "Email must not be empty")
	private String siteAddress;

	@Column
	@ApiModelProperty(value = "description", name = "description", dataType = "String", example = "Site test")
	// @NotBlank(message = "Email must not be empty")
	private String description;

	@Column
	@ApiModelProperty(value = "customerName", name = "customerName", dataType = "String", example = "VTG")
	@NotBlank(message = "CustomerName must not be empty")
	private String customerName;

	@Column
	@ApiModelProperty(value = "contactNumber", name = "contactNumber", dataType = "String", example = "9823467180")
	// @NotBlank(message = "CustomerName must not be empty")
	private String contactNumber;

	@Transient
	@ApiModelProperty(value = "userId", name = "userId", dataType = "String", example = "98asdfadf-2ads3-asdf467-asdf180")
	// @NotBlank(message = "CustomerName must not be empty")
	private String userId;

	@Column
	@ApiModelProperty(hidden=true,value = "columnOrder", name = "columnOrder", dataType = "Array", example = "['siteName','isActive']")
	// @NotBlank(message = "logType must not be empty")
	private String columnOrderValue;
	
	public String getSiteDataId() {
		return siteDataId;
	}

	public void setSiteDataId(String siteDataId) {
		this.siteDataId = siteDataId;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	
	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public boolean getActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSiteAddress() {
		return siteAddress;
	}

	public void setSiteAddress(String siteAddress) {
		this.siteAddress = siteAddress;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getContactNumber() {
		return contactNumber;
	}

	public void setContactNumber(String contactNumber) {
		this.contactNumber = contactNumber;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	
	public JSONArray getColumnOrder() {
		return columnOrder;
	}

	public void setColumnOrder(JSONArray columnOrder) {
		this.columnOrder = columnOrder;
	}

	public String getColumnOrderValue() {
		return columnOrderValue;
	}

	public void setColumnOrderValue(String columnOrderValue) {
		this.columnOrderValue = columnOrderValue;
	}

	public SiteModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	

}
