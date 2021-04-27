package com.zenfra.queries;

import lombok.Data;

@Data
public class ChartQueries {
	
	
	
	private String save;
	
	private String getChartsByUserId;
	
	private String migarationReport;
	
	
	
	public String getMigarationReport() {
		return migarationReport;
	}

	public void setMigarationReport(String migarationReport) {
		this.migarationReport = migarationReport;
	}

	public String getSave() {
		return save;
	}

	public void setSave(String save) {
		this.save = save;
	}

	public String getGetChartsByUserId() {
		return getChartsByUserId;
	}

	public void setGetChartsByUserId(String getChartsByUserId) {
		this.getChartsByUserId = getChartsByUserId;
	}

	
}
