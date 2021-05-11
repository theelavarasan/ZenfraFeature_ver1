package com.zenfra.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.zenfra.model.FavouriteModel;
import com.zenfra.model.FavouriteOrder;
import com.zenfra.model.ResponseModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.Users;
import com.zenfra.service.CategoryMappingService;
import com.zenfra.service.FavouriteApiService_v2;
import com.zenfra.service.UserService;
import com.zenfra.utils.CommonFunctions;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/reports")
public class FavouriteController_v2 {

	@Autowired
	FavouriteApiService_v2 service;
	
	@Autowired
	CommonFunctions functions;
	
	@Autowired
	UserService userService;
	
	@Autowired
	CategoryMappingService catService;

	@PostMapping("/get-all-favourite")
	public ResponseEntity<?> getFavouriteView(@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam(name = "siteKey") String siteKey,
			@RequestParam(name = "reportName", required = false) String reportName,
			@RequestParam(name = "projectId", required = false) String projectId) {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			responseModel.setResponseMessage("Success");
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setResponseDescription("FavouriteView Successfully Retrieved");
			responseModel.setjData(service.getFavView(userId, siteKey, reportName, projectId));
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Error");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

			return ResponseEntity.ok(responseModel);
		}

		return ResponseEntity.ok(responseModel);
	}

	@PostMapping("/save-filter-view")
	public ResponseEntity<?> saveFavouriteViewData(
			@RequestBody FavouriteModel favouriteModel) throws IOException, URISyntaxException,
			org.json.simple.parser.ParseException, ParseException, SQLException {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			Users user=userService.getUserByUserId(favouriteModel.getAuthUserId());
			if(user==null) {
				responseModel.setResponseDescription("User id is invalid");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				return ResponseEntity.ok(responseModel);
			}
			favouriteModel.setFavouriteId(functions.generateRandomId());
			if(favouriteModel.getReportName().equalsIgnoreCase("project-summary")) {
				favouriteModel.setReportLabel(favouriteModel.getFavouriteId());
			}
			favouriteModel.setIsActive(true);
			favouriteModel.setCreatedTime(functions.getCurrentDateWithTime());			
			favouriteModel.setUpdatedBy(favouriteModel.getCreatedBy());
			favouriteModel.setUpdatedTime(functions.getCurrentDateWithTime());
			

			if (service.saveFavouriteView(favouriteModel) == 1) {	
				//Object categoryArr=service.getViewCategoryMapping(favouriteModel.getFavouriteId());
				//favouriteModel.setCategoryColumns(categoryArr);
				favouriteModel.setCreatedBy((user.getFirst_name()+" "+user.getLast_name()));				
				responseModel.setjData(functions.convertEntityToJsonObject(favouriteModel));
				responseModel.setResponseDescription("FavouriteView Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
				catService.saveMap(favouriteModel.getCategoryList(), favouriteModel.getFavouriteId());
			} else {
				responseModel.setResponseDescription("Favourite not inserted ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			responseModel.setResponseMessage("Success!");
			

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

	}

	@PutMapping("/update-filter-view")
	public ResponseEntity<?> updateFavouriteViewData(
			@RequestBody FavouriteModel favouriteModel) throws IOException, URISyntaxException,
			org.json.simple.parser.ParseException, ParseException, SQLException {

		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {

			favouriteModel.setUpdatedBy(favouriteModel.getAuthUserId());
			favouriteModel.setIsActive(true);
			favouriteModel.setUpdatedTime(functions.getCurrentDateWithTime());

			if (service.updateFavouriteView(favouriteModel.getAuthUserId(), favouriteModel) == 1) {
				//Object categoryArr=service.getViewCategoryMapping(favouriteModel.getFavouriteId());
				//favouriteModel.setCategoryColumns(categoryArr);
				
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData(functions.convertEntityToJsonObject(favouriteModel));
				responseModel.setResponseDescription("FavouriteView Successfully updated");
				catService.deleteCategoryMappingFavouriteIdOrChartId(favouriteModel.getFavouriteId());
				catService.saveMap(favouriteModel.getCategoryList(), favouriteModel.getFavouriteId());
				
			} else {
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				responseModel.setResponseDescription("Favourite Id not found ");
			}

			responseModel.setResponseMessage("Success!");
			

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

		// return ResponseEntity.ok(body);
	}

	@DeleteMapping("/delete-favourite-filter-view")
	public ResponseEntity<?> deleteFavouriteViewData(@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam(name = "favouriteID") String FavouriteId, @RequestParam("createdBy") String createdBy,
			@RequestParam(name = "siteKey") String siteKey) throws IOException, URISyntaxException {

		ResponseModel responseModel = new ResponseModel();
		try {

			if (service.deleteFavouriteViewData(userId, FavouriteId, createdBy, siteKey) == 1) {
				responseModel.setResponseDescription("FavouriteView Successfully deleted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Favourite Id not found ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			responseModel.setResponseMessage("Success!");
			

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
			return ResponseEntity.ok(responseModel);
		}

		return ResponseEntity.ok(responseModel);
	}

	@PostMapping("/save-favourite-order")
	public ResponseEntity<?> saveFavouriteOrder(
			@RequestBody FavouriteOrder favouriteModel) throws IOException, URISyntaxException,
			org.json.simple.parser.ParseException, ParseException, SQLException {
		ResponseModel responseModel = new ResponseModel();

		try {

			favouriteModel.setIsActive(true);
			favouriteModel.setCreatedTime(functions.getCurrentDateWithTime());
			favouriteModel.setOrderId(favouriteModel.getCreatedBy() + "_" + favouriteModel.getReportName());

			if (service.saveFavouriteOrder(favouriteModel) == 1) {
				responseModel.setResponseDescription("FavouriteOrder Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
			} else {
				responseModel.setResponseDescription("Try again");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			responseModel.setResponseMessage("Success!");
			

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

		// return ResponseEntity.ok(body);
	}

	@PostMapping("/check-favourite-name")
	public ResponseEntity<?> checkfavouriteName(@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam(name = "siteKey") String siteKey,
			@RequestParam(name = "reportName", required = false) String reportName,
			@RequestParam(name = "favouriteName", required = false) String favouriteName) throws IOException,
			URISyntaxException, org.json.simple.parser.ParseException, ParseException, SQLException {

		ResponseModel responseModel = new ResponseModel();

		try {
			responseModel.setValidation(service.checkfavouriteName(userId, siteKey, favouriteName, reportName));
			responseModel.setResponseMessage("Success");
			responseModel.setResponseCode(HttpStatus.OK);
		
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());
			responseModel.setData(new JSONArray());

		} finally {
			return ResponseEntity.ok(responseModel);
		}

		// return ResponseEntity.ok(body);
	}



}
