package com.zenfra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.CategoryViewDao;
import com.zenfra.model.CategoryView;

@Service
public class CategoryViewService{
	
	
	@Autowired
	CategoryViewDao dao;
	
	public Object getCategoryView(String id) {	
		Object obj=null;
		try {
			obj= dao.findEntityById(CategoryView.class, id);
		} catch (Exception e) {
			e.printStackTrace();			
		}
		
		return obj;
	}
	
	public boolean saveCategoryView(CategoryView view) {
		
		try {			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			 
		}
		
		return false;
	}

	public List<Object> getCategoryViewAll(String userId) {
		
		return null;
	}
	
}
