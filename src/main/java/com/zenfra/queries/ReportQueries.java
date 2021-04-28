package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
public class ReportQueries {

	private String header;
	
	private String numbericalHeader;
	
	private String headerFilter;
	
	private String chartLayout;
	
	private String reportUserCustomData;
	
}
