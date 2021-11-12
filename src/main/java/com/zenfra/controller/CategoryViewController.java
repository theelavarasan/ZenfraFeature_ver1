package com.zenfra.controller;

import java.util.List;

import javax.validation.Valid;

import org.json.simple.JSONArray;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.CategoryView;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.Users;
import com.zenfra.service.CategoryViewService;
import com.zenfra.service.UserCreateService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.NullAwareBeanUtilsBean;

@RestController
@RequestMapping("/rest/category-view")
public class CategoryViewController {

	@Autowired
	CategoryViewService categoryService;

	@Autowired
	CommonFunctions functions;
	
	@Autowired
	UserCreateService userCreateService;

	@PostMapping
	public ResponseEntity<?> saveCategoryView(@Valid @RequestBody CategoryView view) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			view.setUpdatedTime(functions.getCurrentDateWithTime());			
			view.setCreatedTime(functions.getCurrentDateWithTime());
			view.setCategoryId(functions.generateRandomId());
			
			if(view.getSiteKey()==null) {
				responseModel.setResponseMessage("Validation Error!");
				responseModel.setResponseDescription("Category not inserted:: Please sent valid site key");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				return ResponseEntity.ok(responseModel);
			}
			
			view.setCreatedBy(view.getUserId());
			view.setUpdatedBy(view.getUserId());
			view.setUpdatedTime(functions.getCurrentDateWithTime());

			if (categoryService.saveCategoryView(view)) {
				Users user=userCreateService.getUserByUserId(view.getUserId());
				view.setUpdatedBy(user.getFirst_name()+" "+user.getLast_name());
				responseModel.setjData(functions.convertEntityToJsonObject(view));
				responseModel.setResponseDescription("Category Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Category not inserted ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			responseModel.setResponseMessage("Success!");

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
		}
		return ResponseEntity.ok(responseModel);
	}
	
	@PostMapping("/update")
	public ResponseEntity<?> updateCategoryView(@RequestBody CategoryView view) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			CategoryView viewExit=categoryService.getCategoryView(view.getCategoryId());
			
			if(viewExit==null) {
				responseModel.setResponseDescription("Category not found");
				responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				return ResponseEntity.ok(responseModel);
			}
			BeanUtils.copyProperties(view, viewExit, NullAwareBeanUtilsBean.getNullPropertyNames(view));	
			viewExit.setActive(true);
			viewExit.setUpdatedBy(view.getUserId());
			viewExit.setUpdatedTime(functions.getCurrentDateWithTime());			
			
		
			if (categoryService.saveCategoryView(viewExit)) {
				responseModel.setjData(functions.convertEntityToJsonObject(viewExit));
				responseModel.setResponseDescription("Category Successfully updated");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Category not updated");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			responseModel.setResponseMessage("Success!");

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
		}
		return ResponseEntity.ok(responseModel);
	}

	@GetMapping
	public ResponseEntity<?> getCategoryView(@RequestParam String category_id) {
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			Object obj = categoryService.getCategoryView(category_id);
			responseModel.setResponseMessage("Success");
			if (obj != null) {
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData(functions.convertEntityToJsonObject(obj));
			} else {
				responseModel.setResponseCode(HttpStatus.NOT_FOUND);
			}

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			responseModel.setResponseDescription(e.getMessage());

		}
		return ResponseEntity.ok(responseModel);
	}

	@GetMapping("/site-key")
	public ResponseEntity<?> getCategoryViewAll(@RequestParam String siteKey) {
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			JSONArray arr = categoryService.getCategoryViewAll(siteKey);
			responseModel.setResponseMessage("Success");
			if (arr != null) {
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData(arr);
			} else {
				responseModel.setResponseCode(HttpStatus.NOT_FOUND);
			}

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			responseModel.setResponseDescription(e.getMessage());

		}
		return ResponseEntity.ok(responseModel);
	}

	@PostMapping("/delete")
	public ResponseEntity<?> deleteCategoryView(@RequestParam String categoryId) {
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			responseModel.setResponseMessage("Success!");
			CategoryView view=(CategoryView)categoryService.getCategoryView(categoryId);
			
			
			if(view==null) {
				responseModel.setResponseDescription("Category not found ");
				responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				return ResponseEntity.ok(responseModel);
			}
			
			boolean status=categoryService.getCategoryStatus(categoryId);
			
			if(status) {
				responseModel.setResponseDescription("Category map to chart or favourite view");
				responseModel.setResponseCode(HttpStatus.CONFLICT);
				responseModel.setStatusCode(HttpStatus.CONFLICT.value());
				return ResponseEntity.ok(responseModel);
			}
			
			if (categoryService.deleteCategoryView(view)) {
				responseModel.setResponseDescription("Category Successfully deleted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Category not deleted ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
		}
		return ResponseEntity.ok(responseModel);
	}

	
	@GetMapping("/replace")
	public ResponseEntity<?> replaceCategoryView(@RequestParam String oldCategoryId
			,@RequestParam String newCategoryId) {
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			
			
			if (categoryService.changeOldToNewCategoryView(oldCategoryId,newCategoryId)) {
				responseModel.setResponseDescription("Category Successfully updated");
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setResponseMessage("Success!");
			} else {
				responseModel.setResponseMessage("Error!");
				responseModel.setResponseDescription("Category not updated ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
		}
		return ResponseEntity.ok(responseModel);
	}
}
