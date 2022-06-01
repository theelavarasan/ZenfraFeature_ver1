package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "email_config")

public class SmtpConfigModel {

	@Id
	@Column(name = "config_id")
	private String configId;

	@Column(name = "debug")
	private boolean debug;

	@Column(name = "from_address")
	private String fromAddress;

	@Column(name = "sender_host")
	private String senderHost;

	@Column(name = "sender_password")
	private String senderPassword;

	@Column(name = "sender_port")
	private String senderPort;

	@Column(name = "sender_username")
	private String senderUsername;

	@Column(name = "smtp_auth")
	private boolean smtpAuth;

	@Column(name = "smtp_starttls_enable")
	private boolean smtpStarttlsEnable;

	@Column(name = "transport_protocol")
	private String transportProtocol;

	@Column(name = "tenant_id")
	private String tenantId;

	@Column(name = "is_active")
	private boolean isActive;

	public String getConfigId() {
		return configId;
	}

	public void setConfigId(String configId) {
		this.configId = configId;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public String getSenderHost() {
		return senderHost;
	}

	public void setSenderHost(String senderHost) {
		this.senderHost = senderHost;
	}

	public String getSenderPassword() {
		return senderPassword;
	}

	public void setSenderPassword(String senderPassword) {
		this.senderPassword = senderPassword;
	}

	public String getSenderPort() {
		return senderPort;
	}

	public void setSenderPort(String senderPort) {
		this.senderPort = senderPort;
	}

	public String getSenderUsername() {
		return senderUsername;
	}

	public void setSenderUsername(String senderUsername) {
		this.senderUsername = senderUsername;
	}

	public boolean isSmtpAuth() {
		return smtpAuth;
	}

	public void setSmtpAuth(boolean smtpAuth) {
		this.smtpAuth = smtpAuth;
	}

	public boolean isSmtpStarttlsEnable() {
		return smtpStarttlsEnable;
	}

	public void setSmtpStarttlsEnable(boolean smtpStarttlsEnable) {
		this.smtpStarttlsEnable = smtpStarttlsEnable;
	}

	public String getTransportProtocol() {
		return transportProtocol;
	}

	public void setTransportProtocol(String transportProtocol) {
		this.transportProtocol = transportProtocol;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public SmtpConfigModel() {
		super();
	}

	public SmtpConfigModel(String configId, boolean debug, String fromAddress, String senderHost, String senderPassword,
			String senderPort, String senderUsername, boolean smtpAuth, boolean smtpStarttlsEnable,
			String transportProtocol, String tenantId, boolean isActive) {
		super();
		this.configId = configId;
		this.debug = debug;
		this.fromAddress = fromAddress;
		this.senderHost = senderHost;
		this.senderPassword = senderPassword;
		this.senderPort = senderPort;
		this.senderUsername = senderUsername;
		this.smtpAuth = smtpAuth;
		this.smtpStarttlsEnable = smtpStarttlsEnable;
		this.transportProtocol = transportProtocol;
		this.tenantId = tenantId;
		this.isActive = isActive;
	}

}
