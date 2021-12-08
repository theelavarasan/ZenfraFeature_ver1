package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "okta_login_details")
public class OktaLoginModel {

	@Id
	private String id;
	@Column(name = "publisher_url")
	private String publisherUrl;
	@Column(name = "client_id")
	private String clientId;

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

	public OktaLoginModel(String id, String publisherUrl, String clientId) {
		super();
		this.id = id;
		this.publisherUrl = publisherUrl;
		this.clientId = clientId;
	}

	public OktaLoginModel() {
		super();
		// TODO Auto-generated constructor stub
	}

}
