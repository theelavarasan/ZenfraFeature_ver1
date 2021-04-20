package com.zenfra.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.model.FavouriteModel;
import com.zenfra.model.FavouriteOrder;
import com.zenfra.model.ResponseModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.queries.FavouriteViewQueries;
import com.zenfra.service.FavouriteApiService_v2;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/reports")
public class FavouriteController_v2 {

	@Autowired
	FavouriteApiService_v2 service;

	@PostMapping("/get-all-favourite-v2-temp")
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

			ObjectMapper mapper = new ObjectMapper();

			UUID uuid = UUID.randomUUID();
			String randomUUIDString = uuid.toString();

			favouriteModel.setIsActive(true);
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			String currentTime = dtf.format(now);
			favouriteModel.setCreatedTime(currentTime);
			favouriteModel.setFavouriteId(randomUUIDString);

			if (service.saveFavouriteView(favouriteModel) == 1) {
				responseModel.setjData((JSONObject) new JSONParser().parse(mapper.writeValueAsString(favouriteModel)));
				responseModel.setResponseDescription("FavouriteView Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
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

			ObjectMapper mapper = new ObjectMapper();
			favouriteModel.setUpdatedBy(favouriteModel.getAuthUserId());
			favouriteModel.setIsActive(true);
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			String currentTime = dtf.format(now);
			favouriteModel.setUpdatedTime(currentTime);

			if (service.updateFavouriteView(favouriteModel.getAuthUserId(), favouriteModel) == 1) {
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData((JSONObject) new JSONParser().parse(mapper.writeValueAsString(favouriteModel)));
				responseModel.setResponseDescription("FavouriteView Successfully updated");
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

			UUID uuid = UUID.randomUUID();
			String randomUUIDString = uuid.toString();

			
			favouriteModel.setIsActive(true);
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			String currentTime = dtf.format(now);
			favouriteModel.setCreatedTime(currentTime);
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
			responseModel
					.setResponseDescription("FavouriteView " + favouriteName + " already available for the " + siteKey);

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

	@Autowired
	FavouriteViewQueries query;
	
	@GetMapping("/test")
	public String run() {
		try {

		//	System.out.println(query.getName());
			//return query.getName();//repo.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
		return null;
	}

}
