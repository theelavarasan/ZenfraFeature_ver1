package com.zenfra.model;

import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "eol_eos_hardware")

public class EolAndEosHardwareModel {

	@EmbeddedId
	private EolAndEosHardwareIdentityModel eolAndEosHardwareIdentityModel;

	@Transient
	private String vendor;

	@Transient
	private String model;

	@NotNull
	private String eol_eos_hw_id;

	private String end_of_life_cycle;
	private String end_of_extended_support;
	private String source_link;

	private String user_id;

	private boolean active;
	
	private boolean manual;	
	
	private boolean from_discovery = false;	
	
	private String updated_time;
	private String updated_by;

	public EolAndEosHardwareIdentityModel getEolAndEosHardwareIdentityModel() {
		return eolAndEosHardwareIdentityModel;
	}

	public void setEolAndEosHardwareIdentityModel(EolAndEosHardwareIdentityModel eolAndEosHardwareIdentityModel) {
		this.eolAndEosHardwareIdentityModel = eolAndEosHardwareIdentityModel;
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

	public String getEol_eos_hw_id() {
		return eol_eos_hw_id;
	}

	public void setEol_eos_hw_id(String eol_eos_hw_id) {
		this.eol_eos_hw_id = eol_eos_hw_id;
	}

	public String getEnd_of_life_cycle() {
		return end_of_life_cycle;
	}

	public void setEnd_of_life_cycle(String end_of_life_cycle) {
		this.end_of_life_cycle = end_of_life_cycle;
	}

	public String getEnd_of_extended_support() {
		return end_of_extended_support;
	}

	public void setEnd_of_extended_support(String end_of_extended_support) {
		this.end_of_extended_support = end_of_extended_support;
	}

	public String getSource_link() {
		return source_link;
	}

	public void setSource_link(String source_link) {
		this.source_link = source_link;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
		this.updated_by = user_id;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	

	public boolean isManual() {
		return manual;
	}

	public void setManual(boolean manual) {
		this.manual = manual;
	}

	public EolAndEosHardwareModel() {
		super();
	}	
	

	public String getUpdated_time() {
		return updated_time;
	}

	public void setUpdated_time(String updated_time) {
		this.updated_time = updated_time;
	}

	public String getUpdated_by() {
		return updated_by;
	}

	public void setUpdated_by(String updated_by) {
		this.updated_by = updated_by;
	}


	public boolean isFrom_discovery() {
		return from_discovery;
	}

	public void setFrom_discovery(boolean from_discovery) {
		this.from_discovery = from_discovery;
	}

	public EolAndEosHardwareModel(EolAndEosHardwareIdentityModel eolAndEosHardwareIdentityModel, String vendor,
			String model, @NotNull String eol_eos_hw_id, String end_of_life_cycle, String end_of_extended_support,
			String source_link, String user_id, boolean active, boolean manual, String updated_by, boolean from_discovery) {
		super();
		this.eolAndEosHardwareIdentityModel = eolAndEosHardwareIdentityModel;
		this.vendor = vendor;
		this.model = model;
		this.eol_eos_hw_id = eol_eos_hw_id;
		this.end_of_life_cycle = end_of_life_cycle;
		this.end_of_extended_support = end_of_extended_support;
		this.source_link = source_link;
		this.user_id = user_id;
		this.active = active;
		this.manual = manual;
		this.updated_by = user_id;
		this.from_discovery=from_discovery;
	}

	

	
	
}
