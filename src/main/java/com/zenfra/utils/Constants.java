package com.zenfra.utils;

import org.springframework.stereotype.Component;

@Component
public class Constants {
	public static String current_url=DBUtils.getServerUrl();
	//public static String current_url="http://localhost:8080";
	public static String ftp_email="zenfra.alerts@zenfra.co";
	public static String ftp_password="Zenfra@123$";

	public static String ftp_sucess="FTP - :ftp_name Data processing initiated successfully";
	public static String ftp_fail="FTP - :ftp_name Data processing initiation failed";
	public static String ftp_Partially_Processed="FTP - :ftp_name Data processing successfully initiated via FTP";

}
