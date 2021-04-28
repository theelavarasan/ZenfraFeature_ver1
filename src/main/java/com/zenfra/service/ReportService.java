package com.zenfra.service;

import java.io.File;
import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.dao.ReportDao;
import com.zenfra.dataframe.service.DataframeService;

@Component
public class ReportService {
	
	@Autowired
	private ReportDao reportDao;	

	 @Autowired
     DataframeService dataframeService;
	
	 @Autowired
	 ChartService chartService;
	 
	public String getReportHeader(String reportName, String deviceType, String reportBy, String siteKey, String reportList) {
		JSONArray result = new JSONArray();
		if(reportName.equalsIgnoreCase("migrationautomation")) { //get headers from dataframe
			 
			 result = dataframeService.getReportHeaderForMigrationMethod(siteKey, deviceType);
			 
		} else {
			result = reportDao.getReportHeader(reportName, deviceType, reportBy);
		}
		
		 	String report_label = reportList + " " + deviceType + " by "+  reportBy;
	        String report_name = reportList + "_" + deviceType + "_by_"+  reportBy;	       
	        
	        JSONObject resultObject = new JSONObject();
	        resultObject.put("headerInfo", result);
	        resultObject.put("report_label", report_label);
	        resultObject.put("report_name", report_name);	        
		return resultObject.toString();
	}

	public List<String> getReportNumericalHeaders(String reportName, String source_type, String reportBy, String siteKey) {
		// TODO Auto-generated method stub
		return reportDao.getReportNumericalHeaders(reportName, source_type, reportBy, siteKey);
	}

	public List<String> getChartLayout(String userId, String siteKey, String reportName) {
		// TODO Auto-generated method stub
		return reportDao.getChartLayout(userId, siteKey, reportName);
	}

	public JSONObject getReportUserCustomData(String userId, String siteKey, String reportName) {
		// TODO Auto-generated method stub
		JSONObject reportDataObj =  reportDao.getReportUserCustomData(userId, siteKey, reportName);
		JSONArray chartData = chartService.getMigarationReport(siteKey, userId, reportName);
		reportDataObj.put("chart", chartData);
		return reportDataObj;
	}
	
	
	

}
