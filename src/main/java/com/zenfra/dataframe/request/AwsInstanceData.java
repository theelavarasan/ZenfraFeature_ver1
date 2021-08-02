package com.zenfra.dataframe.request;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AwsInstanceData implements Serializable {
	
	private String region;
	private String instancetype;
	private String memoryinfo;
	private String vcpuinfo;
	private String platformdetails;
	private String description;
	
	public AwsInstanceData(String region, String instancetype, String memoryinfo, String vcpuinfo, String platformdetails, String description) {
		super();
		this.region = region;
		this.instancetype = instancetype;
		this.memoryinfo = memoryinfo;
		this.vcpuinfo = vcpuinfo;
		this.platformdetails = platformdetails;
		this.description = description;
		
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getInstancetype() {
		return instancetype;
	}

	public void setInstancetype(String instancetype) {
		this.instancetype = instancetype;
	}

	public String getMemoryinfo() {
		return memoryinfo;
	}

	public void setMemoryinfo(String memoryinfo) {
		this.memoryinfo = memoryinfo;
	}

	public String getVcpuinfo() {
		return vcpuinfo;
	}

	public void setVcpuinfo(String vcpuinfo) {
		this.vcpuinfo = vcpuinfo;
	}

	public String getPlatformdetails() {
		return platformdetails;
	}

	public void setPlatformdetails(String platformdetails) {
		this.platformdetails = platformdetails;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String toString() {
	     return new ToStringBuilder(this).
	       append("region", region).
	       append("instancetype", instancetype).
	       append("memoryinfo", memoryinfo).
	       append("vcpuinfo", vcpuinfo).
	       append("platformdetails", platformdetails).
	       append("description", description).
	       toString();
	   }

}