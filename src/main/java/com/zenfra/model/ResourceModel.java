package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.json.simple.JSONArray;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel(value = "Resource", description = "Resource table operations")
public class ResourceModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	// @GeneratedValue//(strategy = GenerationType.IDENTITY)
	@ApiModelProperty(hidden = true)
	private String resourceDatatId;

	@Column
	@ApiModelProperty(value = "Parent Id", name = "parentId", dataType = "String", example = "5d8a6b37-7c1g-4fa6-a346-444f51c153b6")
	@NotBlank(message = "parentId must not be empty")
	private String parentId;

	@Column
	@ApiModelProperty(value = "createdDateTime", name = "createdDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	// @NotBlank(message = "Site Key must not be empty")
	private String createdDateTime;

	@Column
	@ApiModelProperty(value = "updatedDateTime", name = "updatedDateTime", dataType = "String", example = "01-01-2021 01:20:22")
	// @NotBlank(message = "Site Key must not be empty")
	private String updatedDateTime;

	@Column
	@ApiModelProperty(value = "updatedBy", name = "updatedBy", dataType = "String", example = "00bddaf2-bf99-443c-b67b-e44f3b3d4b35")
	// @NotBlank(message = "Site Key must not be empty")
	private String updatedBy;

	@Column
	@ApiModelProperty(value = "createBy", name = "createBy", dataType = "String", example = "00bddaf2-bf99-443c-b67b-e44f3b3d4b35")
	// @NotBlank(message = "Site Key must not be empty")
	private String createBy;

	@Column
	@ApiModelProperty(value = "resourceId", name = "resourceId", dataType = "String", example = "00bddaf2-bf99-443c-b67b-e44f3b3d4b35")
	// @NotBlank(message = "Site Key must not be empty")
	private String resourceId;

	@Column
	@ApiModelProperty(value = "icon", name = "icon", dataType = "String", example = "fa fa-forward")
	// @NotBlank(message = "Site Key must not be empty")
	private String icon;

	@Column
	@ApiModelProperty(value = "description", name = "description", dataType = "String", example = "Resource")
	// @NotBlank(message = "Site Key must not be empty")
	private String description;

	@Column
	@ApiModelProperty(value = "label", name = "label", dataType = "String", example = "Analytics")
	// @NotBlank(message = "Site Key must not be empty")
	private String label;

	@Column
	@ApiModelProperty(value = "className", name = "className", dataType = "String", example = "optimization-reports-label")
	// @NotBlank(message = "Site Key must not be empty")
	private String className;

	@Column
	@ApiModelProperty(value = "priority", name = "priority", dataType = "String", example = "1")
	// @NotBlank(message = "Site Key must not be empty")
	private String priority;

	@Column
	@ApiModelProperty(value = "menuClassName", name = "menuClassName", dataType = "String", example = "optimization-reports-label")
	// @NotBlank(message = "Site Key must not be empty")
	private String menuClassName;

	@Column
	@ApiModelProperty(value = "children", name = "children", dataType = "String", example = "['test','test']")
	// @NotBlank(message = "Site Key must not be empty")
	private JSONArray children;

	@Column
	@JsonIgnore
	@ApiModelProperty(hidden = true, value = "children", name = "children", dataType = "String", example = "['test','test']")
	// @NotBlank(message = "Site Key must not be empty")
	private String childrenString;

	@Column
	@ApiModelProperty(value = "href", name = "href", dataType = "String", example = "/reports/detailed/detailed/dynamicchart")
	// @NotBlank(message = "Site Key must not be empty")
	private String href;

	@Column
	@ApiModelProperty(value = "value", name = "value", dataType = "String", example = "636de8e4-bb2d-4cd1-aab9-fa2e116104a1")
	// @NotBlank(message = "Site Key must not be empty")
	private String value;

	@Column
	@ApiModelProperty(hidden = true, value = "seq", name = "seq", dataType = "String", example = "1")
	// @NotBlank(message = "Site Key must not be empty")
	private String seq;

	@Transient
	@ApiModelProperty(value = "userId", name = "userId", dataType = "String", example = "1adsf-dsf23a-0adsfasdf-123jksl")
	// @NotBlank(message = "Site Key must not be empty")
	private String userId;

	@Column
	@ApiModelProperty(value = "Active status", name = "active", dataType = "boolean", example = "true")
	// @NotBlank(message = "Site Key must not be empty")
	private boolean isActive = true;

	@Column
	@ApiModelProperty(value = "Menu status", name = "Menu", dataType = "boolean", example = "true")
	// @NotBlank(message = "Site Key must not be empty")
	private boolean isMenu = false;

	public String getResourceDatatId() {
		return resourceDatatId;
	}

	public void setResourceDatatId(String resourceDatatId) {
		this.resourceDatatId = resourceDatatId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getCreatedDateTime() {
		return createdDateTime;
	}

	public void setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
	}

	public String getUpdatedDateTime() {
		return updatedDateTime;
	}

	public void setUpdatedDateTime(String updatedDateTime) {
		this.updatedDateTime = updatedDateTime;
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

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getMenuClassName() {
		return menuClassName;
	}

	public void setMenuClassName(String menuClassName) {
		this.menuClassName = menuClassName;
	}

	public JSONArray getChildren() {
		return children;
	}

	public void setChildren(JSONArray children) {
		this.children = children;
	}

	public String getChildrenString() {
		return childrenString;
	}

	public void setChildrenString(String childrenString) {
		this.childrenString = childrenString;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public boolean getActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean getMenu() {
		return isMenu;
	}

	public void setMenu(boolean isMenu) {
		this.isMenu = isMenu;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	 public ResourceModel() {
		super();
		// TODO Auto-generated constructor stub
	}

}
