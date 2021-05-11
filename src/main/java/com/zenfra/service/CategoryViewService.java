package com.zenfra.service;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.CategoryViewDao;
import com.zenfra.model.CategoryView;
import com.zenfra.utils.CommonFunctions;

@Service
public class CategoryViewService {

	@Autowired
	CategoryViewDao dao;

	@Autowired
	CommonQueriesData queries;
	
	
	@Autowired
	CommonFunctions functions;
	
	@Autowired
	UserService userSerice;
	
	public CategoryView getCategoryView(String id) {
		CategoryView obj = null;
		try {
			obj = (CategoryView)dao.findEntityById(CategoryView.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return obj;
	}

	public boolean saveCategoryView(CategoryView view) {

		try {

			dao.saveEntity(CategoryView.class, view);

		} catch (Exception e) {
			e.printStackTrace();

		}

		return true;
	}

	public JSONArray getCategoryViewAll(String siteKey) {
		JSONArray arr=new JSONArray();
		
		try {
			
			String query=queries.categoryViewQueries().getGetCategoryViewBySiteKey().replace(":site_key",siteKey);
			
			List<Object> list=dao.getEntityListByColumn(query, CategoryView.class);
			Map<String,String> userList=userSerice.getUserNames();
			 for(Object obj:list) {
				 CategoryView view=(CategoryView)obj;
				 	view.setUpdatedBy(userList.get(view.getUpdatedBy()));
				 arr.add(functions.convertEntityToJsonObject(obj));
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}

		return arr;
	}

	public boolean deleteCategoryView(CategoryView view) {
		try {
			dao.deleteByEntity(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean changeOldToNewCategoryView(String oldCategoryId, String newCategoryId) {
		
		
		try {
			String query=queries.categoryMappingQueries().getUpdate()
					.replace(":new_category_list", newCategoryId)
					.replace(":old_category_list", oldCategoryId);
			System.out.println(query);
			dao.updateQuery(query);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
		
	}

}