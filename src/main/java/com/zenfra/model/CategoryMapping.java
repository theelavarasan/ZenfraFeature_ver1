package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="category_mapping")
public class CategoryMapping {

	
	@Column(name="id")
	private String id;
	
	@Column(name="category_list")
	private String category_list;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCategory_list() {
		return category_list;
	}

	public void setCategory_list(String category_list) {
		this.category_list = category_list;
	}
	
	
	
	
}
