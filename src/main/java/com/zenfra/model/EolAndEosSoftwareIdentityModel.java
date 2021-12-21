package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class EolAndEosSoftwareIdentityModel implements Serializable {

	@NotNull
	private String os_version;

	@NotNull
	private String os_name;

	public String getOs_version() {
		return os_version;
	}

	public void setOs_version(String os_version) {
		this.os_version = os_version;
	}

	public String getOs_name() {
		return os_name;
	}

	public void setOs_name(String os_name) {
		this.os_name = os_name;
	}

	public EolAndEosSoftwareIdentityModel() {
		super();
	}

	public EolAndEosSoftwareIdentityModel(@NotNull String os_version, @NotNull String os_name) {
		super();
		this.os_version = os_version;
		this.os_name = os_name;
	}

	
	

	
}
