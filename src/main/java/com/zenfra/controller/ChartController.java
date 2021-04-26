package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ChartDetailsModel;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.ChartService;
import com.zenfra.utils.CommonFunctions;

@RestController
@RequestMapping("/chart")
public class ChartController {

	
	@Autowired
	CommonFunctions functions;
	
	@Autowired
	ChartService chartService;
	
	
	@PostMapping
	public ResponseEntity<?> createChartConfig(@RequestBody ChartModel_v2 chartModel) {
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try 
		{
			chartModel.setCreatedTime(functions.getCurrentDateWithTime());		
			chartModel.setUpdateTime(functions.getCurrentDateWithTime());		
			chartModel.setChartId(functions.generateRandomId());
			System.out.println(chartModel.getUserId());
			
			responseModel.setResponseMessage("Success");
			if(chartService.saveChart(chartModel)) {
				responseModel.setjData(functions.convertEntityToJsonObject(chartModel));
				responseModel.setResponseDescription("Chart Successfully Retrieved");
				responseModel.setResponseCode(HttpStatus.OK);
			}else {
				responseModel.setResponseDescription("Chart not inserted ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}
		
		
		
	}
	
	
	@GetMapping("/{chartId}")
	public ResponseEntity<?> getChartByChartId(@PathVariable String chartId){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			ChartModel_v2 chart=chartService.getChartByChartId(chartId);
			responseModel.setResponseMessage("Success");
			if(chart!=null) {
				responseModel.setjData(functions.convertEntityToJsonObject(chart));
				responseModel.setResponseDescription("Chart Successfully removed");
				responseModel.setResponseCode(HttpStatus.OK);
			}else {
				responseModel.setResponseDescription("Chart not Retrieved ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		responseModel.setResponseMessage("Failed");
		responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
		responseModel.setResponseDescription(e.getMessage());

		} finally {
			return ResponseEntity.ok(responseModel);
		}
	}
	
	
	@DeleteMapping("/{chartId}")
	public ResponseEntity<?> delelteChartByChartId(@PathVariable String chartId){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			responseModel.setResponseMessage("Success");			
			ChartModel_v2 chart=chartService.getChartByChartId(chartId);
				if(chart==null || chart.getChartId().isEmpty()) {					
					responseModel.setResponseDescription("Chart not found ");
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);			
					return ResponseEntity.ok(responseModel);
				}		
			
			if(chartService.deleteChartByObject(chart)) {
				//responseModel.setjData(functions.convertEntityToJsonObject(chartService.getChartByChartId(chartId)));
				responseModel.setResponseDescription("Chart Successfully Retrieved");
				responseModel.setResponseCode(HttpStatus.OK);
			}else {
				responseModel.setResponseDescription("Chart not Retrieved ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
			responseModel.setResponseDescription(e.getMessage());			
		}finally {
			return ResponseEntity.ok(responseModel);
		}
	}
	

	@PostMapping("/create-chart-details")
	public ResponseEntity<?> createChartDetails(@RequestAttribute(name = "authUserId", required = false) String userId, @RequestBody ChartDetailsModel chartDetailsModel){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			ChartModel_v2 chart=chartService.getChartByChartId(chartDetailsModel.getChartId());
					chart.setChartDetails(chartDetailsModel.getChartDetails());
			
					
			if(chartService.saveChart(chart)) {				
				responseModel.setjData(functions.convertEntityToJsonObject(chart));
				responseModel.setResponseDescription("Chart Successfully inserted");
				responseModel.setResponseCode(HttpStatus.OK);
			}else {
				responseModel.setResponseDescription("Chart not Retrieved ");
				responseModel.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setResponseMessage("Failed");
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			responseModel.setResponseDescription(e.getMessage());
			
		}finally {
			return ResponseEntity.ok(responseModel);
		}
		
	}
}
