package com.zenfra.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "eol_eos_hardware")
@IdClass(EolAndEosHardwareModel.class)
public class EolAndEosHardwareModel implements Serializable {

	
	@Id
	private String vendor;
	
	@Id
	private String model;


	private String endOfLifeCycle;
	private String endOfExtendedSupport;
	private String sourceLink;
	private String eolEosHwId;
	private String userId;
	private boolean active;
	

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

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

	public String getEndOfLifeCycle() {
		return endOfLifeCycle;
	}

	public void setEndOfLifeCycle(String endOfLifeCycle) {
		this.endOfLifeCycle = endOfLifeCycle;
	}

	public String getEndOfExtendedSupport() {
		return endOfExtendedSupport;
	}

	public void setEndOfExtendedSupport(String endOfExtendedSupport) {
		this.endOfExtendedSupport = endOfExtendedSupport;
	}

	public String getSourceLink() {
		return sourceLink;
	}

	public void setSourceLink(String sourceLink) {
		this.sourceLink = sourceLink;
	}

	public String getEolEosHwId() {
		return eolEosHwId;
	}

	public void setEolEosHwId(String eolEosHwId) {
		this.eolEosHwId = eolEosHwId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public EolAndEosHardwareModel() {
		super();
	}

	public EolAndEosHardwareModel(String vendor, String model, String endOfLifeCycle, String endOfExtendedSupport,
			String sourceLink, String eolEosHwId, String userId) {
		super();
		this.vendor = vendor;
		this.model = model;
		this.endOfLifeCycle = endOfLifeCycle;
		this.endOfExtendedSupport = endOfExtendedSupport;
		this.sourceLink = sourceLink;
		this.eolEosHwId = eolEosHwId;
		this.userId = userId;
	}
}
