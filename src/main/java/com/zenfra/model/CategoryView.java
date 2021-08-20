package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Entity
@Data
@Table(name = "category_view")
public class CategoryView {

	@Id
	@Column(name = "category_id")
	private String categoryId;

	@NotBlank(message = "categoryName must not be empty")
	@Column(name = "category_name")
	private String categoryName;
	
	@NotBlank(message = "siteKey must not be empty")
	@Column(name = "site_key")
	private String siteKey;
	
	@NotBlank(message = "userId must not be empty")
	@Column(name = "user_id")
	private String userId;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "created_time")
	private String createdTime;
	
	@Column(name = "updated_time")
	private String updatedTime;
	
	
	@Column(name = "report_by")
	private String reportBy;

	@Column(name = "is_active")
	private boolean isActive = true;

	@Transient
	private String authUserId;
	
	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	@Column(name = "create_by")
	private String createBy;
	
	public String getAuthUserId() {
		return authUserId;
	}

	public void setAuthUserId(String authUserId) {
		this.authUserId = authUserId;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
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

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getReportBy() {
		return reportBy;
	}

	public void setReportBy(String reportBy) {
		this.reportBy = reportBy;
	}

	public boolean getActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	
	
	
}
