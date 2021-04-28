package com.zenfra.dao;

import org.springframework.stereotype.Component;

import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.model.CategoryMapping;

@Component
public class CategoryMappingDao extends CommonEntityManager{
	
	
	public boolean saveCategoryMapping(CategoryMapping maping) {
		try {
			
			saveEntity(CategoryMapping.class, maping);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
