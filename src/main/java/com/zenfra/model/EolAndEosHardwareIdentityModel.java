package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class EolAndEosHardwareIdentityModel implements Serializable {

	 @NotNull
	private String vendor;

	 @NotNull
	private String model;

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public EolAndEosHardwareIdentityModel() {
		super();
	}

	public EolAndEosHardwareIdentityModel(@NotNull String vendor, @NotNull String model) {
		super();
		this.vendor = vendor;
		this.model = model;
	}

	
	 

	
}
