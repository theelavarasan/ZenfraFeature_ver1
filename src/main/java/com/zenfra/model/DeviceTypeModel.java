package com.zenfra.model;

import java.io.Serializable;
import java.util.List;

public class DeviceTypeModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String label;
	private String type;
	private boolean isMandatory = true;
	public boolean isMandatory() {
		return isMandatory;
	}
	public void setMandatory(boolean isMandatory) {
		this.isMandatory = isMandatory;
	}
	private List<DeviceTypeValueModel> value;
	
	
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public List<DeviceTypeValueModel> getValue() {
		return value;
	}
	public void setValue(List<DeviceTypeValueModel> value) {
		this.value = value;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

	
	
}
