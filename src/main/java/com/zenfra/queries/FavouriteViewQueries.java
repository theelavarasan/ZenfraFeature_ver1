package com.zenfra.queries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("favourite.view")
@PropertySource("classpath:quries.properties")
public class FavouriteViewQueries {

    
	private String getFavView;
	
	private String save;
	
	private String updateCreatedByEqualsUserId;
	
	private String updateCreatedByNotEqualsUserId;

	public String getGetFavView() {
		return getFavView;
	}

	public void setGetFavView(String getFavView) {
		this.getFavView = getFavView;
	}

	public String getSave() {
		return save;
	}

	public void setSave(String save) {
		this.save = save;
	}

	public String getUpdateCreatedByEqualsUserId() {
		return updateCreatedByEqualsUserId;
	}

	public void setUpdateCreatedByEqualsUserId(String updateCreatedByEqualsUserId) {
		this.updateCreatedByEqualsUserId = updateCreatedByEqualsUserId;
	}

	public String getUpdateCreatedByNotEqualsUserId() {
		return updateCreatedByNotEqualsUserId;
	}

	public void setUpdateCreatedByNotEqualsUserId(String updateCreatedByNotEqualsUserId) {
		this.updateCreatedByNotEqualsUserId = updateCreatedByNotEqualsUserId;
	}
	
	
}
