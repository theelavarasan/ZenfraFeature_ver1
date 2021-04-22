package com.zenfra.controller;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.model.CategoryView;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.CategoryViewService;

@RestController
@RequestMapping("/category-view")
public class CategoryViewController{

	
	@Autowired
	CategoryViewService categoryService;
	
	@PostMapping
	public ResponseEntity<?> saveCategoryView(){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			CategoryView c=new CategoryView();
			
					c.setCategoryId("aravind-01");
			responseModel.setResponseMessage("Success");
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setResponseDescription("Category saved");
			//responseModel.setjData(categoryService.getCategoryView(id));
			categoryService.saveCategoryView(c);
			
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
			 ObjectMapper mapper = new ObjectMapper();
			
			responseModel.setResponseMessage("Success");
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setResponseDescription("Category saved");
			responseModel.setjData(mapper.convertValue(categoryService.getCategoryView(category_id),JSONObject.class));
		
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
	
		}
		return ResponseEntity.ok(responseModel);
	}
	
}
