package com.zenfra.queries;

import lombok.Data;

@Data
public class CategoryMappingQueries {

	
	private String save;
	
	private String update;

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
	
	
}

