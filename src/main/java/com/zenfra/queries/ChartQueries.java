package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("chart.table")
@PropertySource("classpath:quries.properties")
public class ChartQueries {

	private String save;
	
	private String getChartsByUserId;
	
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
