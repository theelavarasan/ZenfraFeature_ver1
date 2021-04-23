package com.zenfra.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.CategoryView;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.CategoryViewService;
import com.zenfra.utils.CommonFunctions;

@RestController
@RequestMapping("/category-view")
public class CategoryViewController{

	
	@Autowired
	CategoryViewService categoryService;
	
	@Autowired
	CommonFunctions functions;
	
	@PostMapping
	public ResponseEntity<?> saveCategoryView(@RequestBody CategoryView view){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
				view.setCategoryId(functions.generateRandomId());
				
				if(view.getCategoryId()==null && view.getCategoryId().isEmpty()) {
					view.setCreatedTime(functions.getCurrentDateWithTime());
				}				
			
				view.setUpdatedBy(view.getUserId());
				view.setUpdatedTime(functions.getCurrentDateWithTime());
				
			if (categoryService.saveCategoryView(view)) {
				responseModel.setjData(functions.convertEntityToJsonObject(view));
				responseModel.setResponseDescription("FavouriteView Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Favourite not inserted ");
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
	public  ResponseEntity<?> getCategoryView(@RequestParam String category_id){
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			Object obj=categoryService.getCategoryView(category_id);
			responseModel.setResponseMessage("Success");
				if(obj!=null) {
					responseModel.setResponseCode(HttpStatus.OK);					
					responseModel.setjData(functions.convertEntityToJsonObject(obj));
				}else {
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
	
	
	
	@GetMapping("/all")
	public  ResponseEntity<?> getCategoryViewAll(@RequestParam String userId){
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			List<Object> obj=categoryService.getCategoryViewAll(userId);
			responseModel.setResponseMessage("Success");
				if(obj!=null) {
					responseModel.setResponseCode(HttpStatus.OK);					
					responseModel.setjData(functions.convertEntityToJsonObject(obj));
				}else {
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
	
	
	
	
	
	
	
	
}
