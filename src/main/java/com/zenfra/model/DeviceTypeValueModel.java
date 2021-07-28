package com.zenfra.model;

import java.io.Serializable;

public class DeviceTypeValueModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String value;
	private String label;
	private String belongsTo;
	private int isDefault;
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getBelongsTo() {
		return belongsTo;
	}
	public void setBelongsTo(String belongsTo) {
		this.belongsTo = belongsTo;
	}
	public int getIsDefault() {
		return isDefault;
	}
	public void setIsDefault(int isDefault) {
		this.isDefault = isDefault;
	}
	
	
	

}
