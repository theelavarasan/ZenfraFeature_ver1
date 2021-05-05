package com.zenfra.queries;

import lombok.Data;

@Data
public class CategoryMappingQueries {

	
	private String save;
	
	private String update;
	
	private String deleteCategoryMappingFavouriteIdOrChartId;

	public String getSave() {
		return save;
	}

	public void setSave(String save) {
		this.save = save;
	}

	public String getUpdate() {
		return update;
	}

	public void setUpdate(String update) {
		this.update = update;
	}

	public String getDeleteCategoryMappingFavouriteIdOrChartId() {
		return deleteCategoryMappingFavouriteIdOrChartId;
	}

	public void setDeleteCategoryMappingFavouriteIdOrChartId(String deleteCategoryMappingFavouriteIdOrChartId) {
		this.deleteCategoryMappingFavouriteIdOrChartId = deleteCategoryMappingFavouriteIdOrChartId;
	}

	
	
}

