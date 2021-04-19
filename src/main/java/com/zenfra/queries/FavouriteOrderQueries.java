package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("favourite.order")
@PropertySource("classpath:quries.properties")
public class FavouriteOrderQueries {

	
	private String getFavouriteOrder;

	public String getGetFavouriteOrder() {
		return getFavouriteOrder;
	}

	public void setGetFavouriteOrder(String getFavouriteOrder) {
		this.getFavouriteOrder = getFavouriteOrder;
	}

	

	
	
	
}
