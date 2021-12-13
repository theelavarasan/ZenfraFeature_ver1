package com.zenfra.model;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "eol_eos_software")
public class EolAndEosSoftwareModel {

	@EmbeddedId
	private EolAndEosSoftwareIdentityModel eolAndEosSoftwareIdentityModel;

	private String sourceUrl;
	private String endOfLifeCycle;
	private String osType;

	@Transient
	private String osVersion;

	private String userId;

	@Transient
	private String osName;

	private String endOfExtendedSupport;
	
	@Transient
	private String eolEosSwId;
	
	private boolean active;

	public EolAndEosSoftwareIdentityModel getEolAndEosSoftwareIdentityModel() {
		return eolAndEosSoftwareIdentityModel;
	}

	public void setEolAndEosSoftwareIdentityModel(EolAndEosSoftwareIdentityModel eolAndEosSoftwareIdentityModel) {
		this.eolAndEosSoftwareIdentityModel = eolAndEosSoftwareIdentityModel;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getEndOfLifeCycle() {
		return endOfLifeCycle;
	}

	public void setEndOfLifeCycle(String endOfLifeCycle) {
		this.endOfLifeCycle = endOfLifeCycle;
	}

	public String getOsType() {
		return osType;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getEndOfExtendedSupport() {
		return endOfExtendedSupport;
	}

	public void setEndOfExtendedSupport(String endOfExtendedSupport) {
		this.endOfExtendedSupport = endOfExtendedSupport;
	}

	public String getEolEosSwId() {
		return eolEosSwId;
	}

	public void setEolEosSwId(String eolEosSwId) {
		this.eolEosSwId = eolEosSwId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public EolAndEosSoftwareModel() {
		super();
	}

	public EolAndEosSoftwareModel(EolAndEosSoftwareIdentityModel eolAndEosSoftwareIdentityModel, String sourceUrl,
			String endOfLifeCycle, String osType, String osVersion, String userId, String osName,
			String endOfExtendedSupport, String eolEosSwId, boolean active) {
		super();
		this.eolAndEosSoftwareIdentityModel = eolAndEosSoftwareIdentityModel;
		this.sourceUrl = sourceUrl;
		this.endOfLifeCycle = endOfLifeCycle;
		this.osType = osType;
		this.osVersion = osVersion;
		this.userId = userId;
		this.osName = osName;
		this.endOfExtendedSupport = endOfExtendedSupport;
		this.eolEosSwId = eolEosSwId;
		this.active = active;
	}

	
}
