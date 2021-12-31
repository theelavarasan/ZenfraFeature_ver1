package com.zenfra.model;

import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "eol_eos_software")
public class EolAndEosSoftwareModel {

	@EmbeddedId
	private EolAndEosSoftwareIdentityModel eolAndEosSoftwareIdentityModel;

	private String source_url;
	private String end_of_life_cycle;
	private String os_type;

	@NotNull
	private String eol_eos_sw_id;

	@Transient
	private String os_version;

	private String user_id;

	@Transient
	private String os_name;

	private String end_of_extended_support;

	private boolean active;
	
	private boolean manual;
	
	private Date updated_time;
	private String updated_by;

	public EolAndEosSoftwareIdentityModel getEolAndEosSoftwareIdentityModel() {
		return eolAndEosSoftwareIdentityModel;
	}

	public void setEolAndEosSoftwareIdentityModel(EolAndEosSoftwareIdentityModel eolAndEosSoftwareIdentityModel) {
		this.eolAndEosSoftwareIdentityModel = eolAndEosSoftwareIdentityModel;
	}

	public String getSource_url() {
		return source_url;
	}

	public void setSource_url(String source_url) {
		this.source_url = source_url;
	}

	public String getEnd_of_life_cycle() {
		return end_of_life_cycle;
	}

	public void setEnd_of_life_cycle(String end_of_life_cycle) {
		this.end_of_life_cycle = end_of_life_cycle;
	}

	public String getOs_type() {
		return os_type;
	}

	public void setOs_type(String os_type) {
		this.os_type = os_type;
	}

	public String getEol_eos_sw_id() {
		return eol_eos_sw_id;
	}

	public void setEol_eos_sw_id(String eol_eos_sw_id) {
		this.eol_eos_sw_id = eol_eos_sw_id;
	}

	public String getOs_version() {
		return os_version;
	}

	public void setOs_version(String os_version) {
		this.os_version = os_version;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public String getOs_name() {
		return os_name;
	}

	public void setOs_name(String os_name) {
		this.os_name = os_name;
	}

	public String getEnd_of_extended_support() {
		return end_of_extended_support;
	}

	public void setEnd_of_extended_support(String end_of_extended_support) {
		this.end_of_extended_support = end_of_extended_support;
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

	public EolAndEosSoftwareModel() {
		super();
	}	

	

	public Date getUpdated_time() {
		return updated_time;
	}

	public void setUpdated_time(Date updated_time) {
		this.updated_time = updated_time;
	}

	public String getUpdated_by() {
		return updated_by;
	}

	public void setUpdated_by(String updated_by) {
		this.updated_by = updated_by;
	}

	public EolAndEosSoftwareModel(EolAndEosSoftwareIdentityModel eolAndEosSoftwareIdentityModel, String source_url,
			String end_of_life_cycle, String os_type, @NotNull String eol_eos_sw_id, String os_version, String user_id,
			String os_name, String end_of_extended_support, boolean active, boolean manual, String updated_by) {
		super();
		this.eolAndEosSoftwareIdentityModel = eolAndEosSoftwareIdentityModel;
		this.source_url = source_url;
		this.end_of_life_cycle = end_of_life_cycle;
		this.os_type = os_type;
		this.eol_eos_sw_id = eol_eos_sw_id;
		this.os_version = os_version;
		this.user_id = user_id;
		this.os_name = os_name;
		this.end_of_extended_support = end_of_extended_support;
		this.active = active;
		this.manual = manual;
		this.updated_by = user_id;
	}

	



	
	

}
