package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data

@Component
@ConfigurationProperties("chart.table")
@PropertySource("classpath:quries.properties")
public class CategoryMappingQueries {

	
	private String save;

	public String getSave() {
		return save;
	}

	public void setSave(String save) {
		this.save = save;
	}
	
	
}

