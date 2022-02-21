package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.CategoryMappingDao;
import com.zenfra.model.CategoryMapping;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class CategoryMappingService {

	@Autowired
	CategoryMappingDao catDao;

	@Autowired
	CommonQueriesData queries;

	public boolean saveMap(List<String> maping, String favId) {

		try {
			for (String map : maping) {
				CategoryMapping cate = new CategoryMapping();
				cate.setCategory(map);
				cate.setId(favId);
				catDao.saveCategoryMappingjdbc(cate);
			}

			return false;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
	}

	public boolean deleteCategoryMappingFavouriteIdOrChartId(String id) {
		boolean responce = false;
		try {

			responce = catDao.deleteCategoryMapping(id);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return false;
	}

	public JSONArray getCategoryLabelById(String id) {

		try {
			JSONParser parser = new JSONParser();
			String query = queries.categoryMappingQueries().getGetCategoryLabelById().replace(":id", id);

			Object obj = catDao.getObjectByQuery(query, Object.class);
			if (obj != null && !obj.toString().equals("[]")) {
				return (JSONArray) parser.parse(obj.toString().replace("\\[", "").replace("\\]", ""));
			} else {
				return new JSONArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return new JSONArray();
	}

}
