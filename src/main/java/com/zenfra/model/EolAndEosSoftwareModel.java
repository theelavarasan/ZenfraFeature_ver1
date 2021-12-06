package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "eol_eos_software")
@IdClass(EolAndEosSoftwareModel.class)
public class EolAndEosSoftwareModel implements Serializable {

	private String sourceUrl;
	private String endOfLifeCycle;
	private String osType;

	@Id
	private String osVersion;
	
	private String userId;

	@Id
	private String osName;
	
	private String endOfExtendedSupport;
	private boolean active;

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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public EolAndEosSoftwareModel() {
		super();
	}

	public EolAndEosSoftwareModel(String sourceUrl, String endOfLifeCycle, String osType, String osVersion,
			String userId, String osName, String endOfExtendedSupport, boolean active) {
		super();
		this.sourceUrl = sourceUrl;
		this.endOfLifeCycle = endOfLifeCycle;
		this.osType = osType;
		this.osVersion = osVersion;
		this.userId = userId;
		this.osName = osName;
		this.endOfExtendedSupport = endOfExtendedSupport;
		this.active = active;
	}

}
