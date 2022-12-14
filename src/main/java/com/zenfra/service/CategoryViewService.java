package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.CategoryViewDao;
import com.zenfra.model.CategoryView;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class CategoryViewService {

	@Autowired
	CategoryViewDao dao;

	@Autowired
	CommonQueriesData queries;

	@Autowired
	CommonFunctions functions;

	@Autowired
	UserCreateService userSerice;

	@Autowired
	ChartService chartService;

	@Autowired
	FavouriteApiService_v2 favService;

	public CategoryView getCategoryView(String id) {
		CategoryView obj = null;
		try {
			obj = (CategoryView) dao.findEntityById(CategoryView.class, id);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return obj;
	}

	public boolean saveCategoryView(CategoryView view) {

		try {

			dao.saveEntity(CategoryView.class, view);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}

		return true;
	}

	public JSONArray getCategoryViewAll(String siteKey) {
		JSONArray arr = new JSONArray();

		try {
			System.out.println("-*-*-*-*-*-*-*-*-*-Log 3-*-*-*-*-*-*-*-*-**-*-*-*--*");

			String query = queries.categoryViewQueries().getGetCategoryViewBySiteKey().replace(":site_key", siteKey);
			System.out.println("-*-*-*-*-*-*-*-*-**--* Query  : " + query);

			List<Object> list = dao.getEntityListByColumn(query, CategoryView.class);
			System.out.println("-*-*-*-*-*-*-*-*-*-Log 4-*-*-*-*-*-*-*-*-**-*-*-*--*");

			Map<String, String> userList = userSerice.getUserNames();
			for (Object obj : list) {
				System.out.println("-*-*-*-*-*-*-*-*-*-Log 5-*-*-*-*-*-*-*-*-**-*-*-*--*");

				CategoryView view = (CategoryView) obj;
				// System.out.println("report::"+view.getReportBy());
				// System.out.println("updateBY::"+view.getUpdatedBy());
				view.setUpdatedBy(view.getUpdatedBy() != null ? userList.get(view.getUpdatedBy()) : "");
				view.setCreatedBy(view.getCreatedBy() != null ? userList.get(view.getCreatedBy()) : "");

				view.setCreatedTime(dateFormat(view.getCreatedTime()));
				view.setUpdatedTime(dateFormat(view.getUpdatedTime()));
				System.out.println("-*-*-*-*-*-*--* CreatedTime :" + dateFormat(view.getCreatedTime()));
				System.out.println("-*-*-*-*-*-*--* updatedTime :" + dateFormat(view.getUpdatedTime()));
				arr.add(functions.convertEntityToJsonObject(view));
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return arr;
	}

	public String dateFormat(String dateFormat) throws ParseException {
		System.out.println("-*-*-*-*-*-*-*-*-*-Log 6-*-*-*-*-*-*-*-*-**-*-*-*--*");

		String df = "yyyy/MM/dd HH:mm:ss";
		if (dateFormat.contains("-")) {
			df = "yyyy-MM-dd HH:mm:ss";
		}
		DateFormat formatterIST = new SimpleDateFormat(df);
		formatterIST.setTimeZone(TimeZone.getDefault()); // better than using IST
		Date date = formatterIST.parse(dateFormat);
		DateFormat formatterUTC = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
		if (dateFormat != null && !dateFormat.isEmpty()) {
			formatterUTC.setTimeZone(TimeZone.getTimeZone("UTC")); // UTC timezone
		}
		System.out.println("date format : " + formatterUTC.format(date));
		return formatterUTC.format(date);
	}

	public boolean deleteCategoryView(CategoryView view) {
		try {
			dao.deleteByEntity(view);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return true;
	}

	public boolean changeOldToNewCategoryView(String oldCategoryId, String newCategoryId) {

		try {
			String query = queries.categoryMappingQueries().getUpdate().replace(":new_category_list", newCategoryId)
					.replace(":old_category_list", oldCategoryId);
			System.out.println(query);
			String query1 = "delete from category_view where category_id = '" + oldCategoryId + "'";
			System.out.println(query1);
			dao.updateQuery(query);
			dao.updateQuery(query1);
			
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return true;

	}

	public boolean getCategoryStatus(String categoryId) {

		try {

			List<Object> chart = chartService.getChartByCategoryId(categoryId);
			if (chart != null && !chart.isEmpty()) {
				return true;
			}

			List<Map<String, Object>> favList = favService.getFavouriteList(categoryId);

			if (favList != null && !favList.isEmpty()) {
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return false;
	}

}
