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

	public EolAndEosSoftwareIdentityModel() {
		super();
	}

	public EolAndEosSoftwareIdentityModel(@NotNull String osVersion, @NotNull String osName) {
		super();
		this.osVersion = osVersion;
		this.osName = osName;
	}
	
	

}
