package com.zenfra.dao;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.model.CategoryMapping;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class CategoryMappingDao extends CommonEntityManager {

	@Autowired
	CommonQueriesData data;

	public boolean saveCategoryMapping(CategoryMapping maping) {
		try {

			saveEntity(CategoryMapping.class, maping);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return false;
	}

	public void saveCategoryMappingjdbc(CategoryMapping cate) {
		try {

			String query = data.categoryMappingQueries().getSave().replace(":id", cate.getId())
					.replace(":category_list", cate.getCategory());

			updateQuery(query);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
	}

	public boolean deleteCategoryMapping(String id) {
		try {

			String query = data.categoryMappingQueries().getDeleteCategoryMappingFavouriteIdOrChartId().replace(":id",
					id);
			updateQuery(query);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return true;
	}

}
