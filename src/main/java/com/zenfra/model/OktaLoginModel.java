package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "okta_login_details")
public class OktaLoginModel {

	@Id
	private String id;
	@Column(name = "publisher_url")
	private String publisherUrl;
	@Column(name = "client_id")
	private String clientId;
	@Column(name = "active")
	private boolean active;
	@Column(name = "defaultsitename")
	private String defaultSiteName;
	@Column(name = "defaultpolicy")
	private String defaultPolicy;
	
	@Transient
	private String defaultPolicyName;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPublisherUrl() {
		return publisherUrl;
	}

	public void setPublisherUrl(String publisherUrl) {
		this.publisherUrl = publisherUrl;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getDefaultSiteName() {
		return defaultSiteName;
	}

	public void setDefaultSiteName(String defaultSiteName) {
		this.defaultSiteName = defaultSiteName;
	}

	public String getDefaultPolicy() {
		return defaultPolicy;
	}

	public void setDefaultPolicy(String defaultPolicy) {
		this.defaultPolicy = defaultPolicy;
	}

	public OktaLoginModel(String id, String publisherUrl, String clientId, boolean active, String defaultSiteName,
			String defaultPolicyName) {
		super();
		this.id = id;
		this.publisherUrl = publisherUrl;
		this.clientId = clientId;
		this.active = active;
		this.defaultSiteName = defaultSiteName;
		this.defaultPolicy = defaultPolicyName;
		
	}
	
	

	public String getDefaultPolicyName() {
		return defaultPolicyName;
	}

	public void setDefaultPolicyName(String defaultPolicyName) {
		this.defaultPolicyName = defaultPolicyName;
	}

	public OktaLoginModel() {
		super();
		// TODO Auto-generated constructor stub
	}

}
