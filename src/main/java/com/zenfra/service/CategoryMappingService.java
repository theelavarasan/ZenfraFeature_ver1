package com.zenfra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.CategoryMappingDao;
import com.zenfra.model.CategoryMapping;

@Service
public class CategoryMappingService{

	
	@Autowired
	CategoryMappingDao catDao;
	
	
	public boolean saveMap(List<String> maping,String favId) {
		
		try {			
				for(String map:maping) {
					CategoryMapping cate=new CategoryMapping();
						cate.setCategory(map);
						cate.setId(favId);						
					catDao.saveCategoryMappingjdbc(cate);
				}
			
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}


	public boolean deleteCategoryMappingFavouriteIdOrChartId(String id) {
		boolean responce=false;
		try {
			
			responce= catDao.deleteCategoryMapping(id);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
