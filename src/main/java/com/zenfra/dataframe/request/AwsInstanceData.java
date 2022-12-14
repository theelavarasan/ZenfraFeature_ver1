package com.zenfra.dataframe.request;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AwsInstanceData implements Serializable {
	
	private String region;
	private String instancetype;
	private String instanceid;
	private String memoryinfo;
	private String vcpuinfo;
	private String platformdetails;
	private String description;
	private String updated_date;
	private String actualOsType;
	private String serverType;
	private String report_by;
	private String customExcelSrcId;
	
	public AwsInstanceData(String region, String instancetype, String memoryinfo, String vcpuinfo, String platformdetails, String description, String instanceid, String updated_date, String actualOsType, String serverType, String report_by, String customExcelSrcId) {
		super();
		this.region = region;
		this.instancetype = instancetype;
		this.memoryinfo = memoryinfo;
		this.vcpuinfo = vcpuinfo;
		this.platformdetails = platformdetails;
		this.description = description;
		this.instanceid = instanceid;
		this.updated_date = updated_date;
		this.actualOsType = actualOsType;
		this.serverType = serverType;
		this.report_by = report_by;
		this.customExcelSrcId = customExcelSrcId;
		
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
	
	
	
	public String getInstanceid() {
		return instanceid;
	}

	public void setInstanceid(String instanceid) {
		this.instanceid = instanceid;
	}
	
	

	public String getUpdated_date() {
		return updated_date;
	}

	public void setUpdated_date(String updated_date) {
		this.updated_date = updated_date;
	}
	

	public String getActualOsType() {
		return actualOsType;
	}

	public void setActualOsType(String actualOsType) {
		this.actualOsType = actualOsType;
	}
	

	public String getServerType() {
		return serverType;
	}

	public void setServerType(String serverType) {
		this.serverType = serverType;
	}
	
	

	public String getReport_by() {
		return report_by;
	}

	public void setReport_by(String report_by) {
		this.report_by = report_by;
	}	
	

	public String getCustomExcelSrcId() {
		return customExcelSrcId;
	}

	public void setCustomExcelSrcId(String customExcelSrcId) {
		this.customExcelSrcId = customExcelSrcId;
	}

	public String toString() {
	     return new ToStringBuilder(this).
	       append("region", region).
	       append("instancetype", instancetype).
	       append("memoryinfo", memoryinfo).
	       append("vcpuinfo", vcpuinfo).
	       append("platformdetails", platformdetails).
	       append("description", description).
	       append("instanceid", instanceid).
	       append("updated_date", updated_date).
	       append("actualOsType", actualOsType).
	       toString();
	   }

}