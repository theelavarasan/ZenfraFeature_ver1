package com.zenfra.queries;

import lombok.Data;

@Data
public class CategoryViewQueries {
	
	private String getCategoryViewBySiteKey;

	public String getGetCategoryViewBySiteKey() {
		return getCategoryViewBySiteKey;
	}

	public void setGetCategoryViewBySiteKey(String getCategoryViewBySiteKey) {
		this.getCategoryViewBySiteKey = getCategoryViewBySiteKey;
	}
	
	
	

}
