package com.zenfra.model.ftp;

public class FileWithPath {

	private String name;

	private String path;
	private String logType;
	private String checkSum;
	
	private boolean subFolder;

	private String subFolderPath;
	
	
	private FTPServerModel serverModel;
	
	public String getLogType() {
		return logType;
	}

	public void setLogType(String logType) {
		this.logType = logType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(String checkSum) {
		this.checkSum = checkSum;
	}

	public FTPServerModel getServerModel() {
		return serverModel;
	}

	public void setServerModel(FTPServerModel serverModel) {
		this.serverModel = serverModel;
	}

	public boolean isSubFolder() {
		return subFolder;
	}

	public void setSubFolder(boolean subFolder) {
		this.subFolder = subFolder;
	}

	public String getSubFolderPath() {
		return subFolderPath;
	}

	public void setSubFolderPath(String subFolderPath) {
		this.subFolderPath = subFolderPath;
	}

	@Override
	public String toString() {
		return "FileWithPath [name=" + name + ", path=" + path + ", logType=" + logType + ", checkSum=" + checkSum
				+ ", subFolder=" + subFolder + ", subFolderPath=" + subFolderPath + ", serverModel=" + serverModel
				+ "]";
	}

	

	

}
