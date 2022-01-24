package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "resource")
public class ResourceCrudModel {

	@Id
	@Column(name = "resource_data_id")
	private String resourceDataId;

	@Column(name = "parent")
	private String parent;

	@Column(name = "updated_time")
	private String updatedTime;

	@Column(name = "resource_id")
	private String resourceId;

	@Column(name = "icon")
	private String icon;

	@Column(name = "description")
	private String description;

	@Column(name = "class_name")
	private String className;

	@Column(name = "label")
	private String label;

	@Column(name = "priority")
	private String priority;

	@Column(name = "is_active")
	private boolean isActive;

	@Column(name = "is_menu")
	private boolean isMenu;

	@Column(name = "menu_class_name")
	private String menuClassName;

	@Column(name = "children")
	private String children;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_time")
	private String createdTime;

	@Column(name = "href")
	private String href;

	@Column(name = "value")
	private String value;

	@Column(name = "seq")
	private String seq;

	@Column(name = "create_by")
	private String create_by;

	@Column(name = "created_date_time")
	private String createdDateTime;

	@Column(name = "parent_id")
	private String parentId;

	@Column(name = "updated_date_time")
	private String updatedDateTime;

	public String getResourceDataId() {
		return resourceDataId;
	}

	public void setResourceDataId(String resourceDataId) {
		this.resourceDataId = resourceDataId;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
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

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isMenu() {
		return isMenu;
	}

	public void setMenu(boolean isMenu) {
		this.isMenu = isMenu;
	}

	public String getMenuClassName() {
		return menuClassName;
	}

	public void setMenuClassName(String menuClassName) {
		this.menuClassName = menuClassName;
	}

	public String getChildren() {
		return children;
	}

	public void setChildren(String children) {
		this.children = children;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
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

	public String getCreate_by() {
		return create_by;
	}

	public void setCreate_by(String create_by) {
		this.create_by = create_by;
	}

	public String getCreatedDateTime() {
		return createdDateTime;
	}

	public void setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getUpdatedDateTime() {
		return updatedDateTime;
	}

	public void setUpdatedDateTime(String updatedDateTime) {
		this.updatedDateTime = updatedDateTime;
	}

	public ResourceCrudModel() {
		super();
	}

	public ResourceCrudModel(String resourceDataId, String parent, String updatedTime, String resourceId, String icon,
			String description, String className, String label, String priority, boolean isActive, boolean isMenu,
			String menuClassName, String children, String createdBy, String createdTime, String href, String value,
			String seq, String create_by, String createdDateTime, String parentId, String updatedDateTime) {
		super();
		this.resourceDataId = resourceDataId;
		this.parent = parent;
		this.updatedTime = updatedTime;
		this.resourceId = resourceId;
		this.icon = icon;
		this.description = description;
		this.className = className;
		this.label = label;
		this.priority = priority;
		this.isActive = isActive;
		this.isMenu = isMenu;
		this.menuClassName = menuClassName;
		this.children = children;
		this.createdBy = createdBy;
		this.createdTime = createdTime;
		this.href = href;
		this.value = value;
		this.seq = seq;
		this.create_by = create_by;
		this.createdDateTime = createdDateTime;
		this.parentId = parentId;
		this.updatedDateTime = updatedDateTime;
	}
	
	

}
