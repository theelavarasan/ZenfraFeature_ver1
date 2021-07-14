package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.json.simple.JSONArray;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel(value = "Policy", description = "Policy table  grid operations")
public class PolicyModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	//@GeneratedValue//(strategy = GenerationType.IDENTITY)
	@ApiModelProperty(hidden = true)
	private String policyDataId;
	
	@Column
	@ApiModelProperty(value = "Log file update by", name = "updateBy", dataType = "String", example = "zenfra-user")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String updateBy;
	

	@Column
	@ApiModelProperty(value = "updatedDateTime", name = "updatedDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	//@NotBlank(message = "Site Key must not be empty")
	private String updatedDateTime;
	
	@Column
	@ApiModelProperty(value = "createdDateTime", name = "createdDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	//@NotBlank(message = "Site Key must not be empty")
	private String createdDateTime;
	
	
	@Column
	@ApiModelProperty(value = "Log file create by", name = "createBy", dataType = "String", example = "zenfra-user")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String createBy;
	
	
	@Column
	@ApiModelProperty(value = "Active status", name = "activ", dataType = "boolean", example = "true")
	//@NotBlank(message = "Site Key must not be empty")
	private boolean isActive=true;

	

	@Column
	@ApiModelProperty(value = "Policy id", name = "policyId", dataType = "String", example = "42cd38be-9f62-4440-aa45-d115b199c4c4")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String policy_id;
	
	
	@Column
	@ApiModelProperty(value = "Subject", name = "Subject", dataType = "String", example = "Admin")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String subject;
	
	@Column
	@ApiModelProperty(value = "policyType", name = "policyType", dataType = "String", example = "groupPolicy")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String policyType;
	
	
	@Column
	@ApiModelProperty(value = "tenantId", name = "tenantId", dataType = "String", example = "a0fe5fb8-f660-46fb-9c3d-eae1d3847238")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String tenantId;
	
	
	@Column
	@ApiModelProperty(value = "resources", name = "resources", dataType = "Array", example = "[{'name':'test'},{'name':'check'}]")
	//@NotBlank(message = "uploadedBy must not be empty")
	private JSONArray resources;
	
	
	@Column
	@ApiModelProperty(hidden = true,value = "resources", name = "resources", dataType = "Array", example = "[{'name':'test'},{'name':'check'}]")
	//@NotBlank(message = "uploadedBy must not be empty")
	@JsonIgnore
	private String resourcesString;
	
	@Column
	@ApiModelProperty(value = "description", name = "description", dataType = "String", example = "Data-Upload-Read-only")
	//@NotBlank(message = "uploadedBy must not be empty")
	private String description;

	
	@Transient
	@ApiModelProperty(value = "userId", name = "userId", dataType = "String", example = "98asdfadf-2ads3-asdf467-asdf180")
	// @NotBlank(message = "CustomerName must not be empty")
	private String userId;
	
	
	
	public String getPolicyDataId() {
		return policyDataId;
	}

	public void setPolicyDataId(String policyDataId) {
		this.policyDataId = policyDataId;
	}

	public String getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}

	public String getUpdatedDateTime() {
		return updatedDateTime;
	}

	public void setUpdatedDateTime(String updatedDateTime) {
		this.updatedDateTime = updatedDateTime;
	}

	public String getCreatedDateTime() {
		return createdDateTime;
	}

	public void setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
	}

	public boolean getActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getPolicy_id() {
		return policy_id;
	}

	public void setPolicy_id(String policy_id) {
		this.policy_id = policy_id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getPolicyType() {
		return policyType;
	}

	public void setPolicyType(String policyType) {
		this.policyType = policyType;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public JSONArray getResources() {
		return resources;
	}

	public void setResources(JSONArray resources) {
		this.resources = resources;
	}

	public String getResourcesString() {
		return resourcesString;
	}

	public void setResourcesString(String resourcesString) {
		this.resourcesString = resourcesString;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	
	
	
	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public PolicyModel() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	
	
	
	
	
	

}
