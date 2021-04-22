package com.zenfra.service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.CommonEntityManager;
import com.zenfra.model.CategoryView;
import com.zenfra.repo.CategoryViewRepo;

@Service
public class CategoryViewService{
	
	
	@Autowired
	CategoryViewRepo categoryRepo;
	
	
	
	public CategoryView getCategoryView(String id) {
		
		try {			
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void saveCategoryView(CategoryView view) {
		
		try {			
			
		} catch (Exception e) {
			e.printStackTrace();
			 
		}
	}
	
}
