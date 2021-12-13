package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class EolAndEosSoftwareIdentityModel implements Serializable{
	
	@NotNull
	private String osVersion;
	
	@NotNull
	private String osName;
	
	@NotNull
	private String eolEosSwId;

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getEolEosSwId() {
		return eolEosSwId;
	}

	public void setEolEosSwId(String eolEosSwId) {
		this.eolEosSwId = eolEosSwId;
	}

	public EolAndEosSoftwareIdentityModel() {
		super();
	}

	public EolAndEosSoftwareIdentityModel(@NotNull String osVersion, @NotNull String osName,
			@NotNull String eolEosSwId) {
		super();
		this.osVersion = osVersion;
		this.osName = osName;
		this.eolEosSwId = eolEosSwId;
	}

	
	

}
