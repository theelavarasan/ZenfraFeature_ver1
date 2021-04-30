package com.zenfra.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.model.CategoryMapping;
import com.zenfra.queries.CategoryMappingQueries;

@Component
public class CategoryMappingDao extends CommonEntityManager{
	
	
	
	@Autowired
	CategoryMappingQueries data;
	
	public boolean saveCategoryMapping(CategoryMapping maping) {
		try {
			
			saveEntity(CategoryMapping.class, maping);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void saveCategoryMappingjdbc(CategoryMapping cate) {
	try {
		
		String query=data.getSave()
				.replace(":id", cate.getId()).replace(":category_list", cate.getCategory_list());
		
		updateQuery(query);
		
	} catch (Exception e) {
		e.printStackTrace();
	}
	}

}
