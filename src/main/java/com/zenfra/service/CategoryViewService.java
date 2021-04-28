package com.zenfra.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.CategoryViewDao;
import com.zenfra.model.CategoryView;

@Service
public class CategoryViewService {

	@Autowired
	CategoryViewDao dao;

	@Autowired
	CommonQueriesData queries;
	
	
	public Object getCategoryView(String id) {
		Object obj = null;
		try {
			obj = dao.findEntityById(CategoryView.class, id);
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

		return false;
	}

	public List<Object> getCategoryViewAll(String siteKey) {
		List<Object> list = new ArrayList<Object>();
		try {
			
			String query=queries.categoryViewQueries().getGetCategoryViewBySiteKey().replace(":site_key",siteKey);

			return dao.getEntityListByColumn(query, CategoryView.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public boolean deleteCategoryView(CategoryView view) {
		try {
			dao.deleteByEntity(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

}
