package com.zenfra.queries;

import lombok.Data;

@Data
public class CategoryMappingQueries {

	private String save;

	private String update;

	private String getById;

	private String deleteCategoryMappingFavouriteIdOrChartId;
	
	private String getCategoryLabelById;

	public String getGetById() {
		return getById;
	}

	public void setGetById(String getById) {
		this.getById = getById;
	}

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

	public String getGetCategoryLabelById() {
		return getCategoryLabelById;
	}

	public void setGetCategoryLabelById(String getCategoryLabelById) {
		this.getCategoryLabelById = getCategoryLabelById;
	}

}
