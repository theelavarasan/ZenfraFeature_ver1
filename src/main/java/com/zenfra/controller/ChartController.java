package com.zenfra.controller;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ChartDetailsModel;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.CategoryMappingService;
import com.zenfra.service.ChartService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.NullAwareBeanUtilsBean;

@RestController
@RequestMapping("/rest/chart")
public class ChartController {

	
	
	@Autowired
	CommonFunctions functions;
	
	
	@Autowired
	ChartService chartService;
	
	@Autowired
	CategoryMappingService mapService;
	
	@PostMapping
	public ResponseEntity<?> createChartConfig(@RequestBody ChartModel_v2 chartModel) {
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try 
		{
			System.out.println(chartModel.getUserId());
				
			
			
			if(chartModel.getChartId()==null || chartModel.getChartId().trim().isEmpty()) {
				chartModel.setCreatedTime(functions.getCurrentDateWithTime());		
				chartModel.setChartId(functions.generateRandomId());
				chartModel.setActive(true);
			}else {
				
				chartModel.setActive(true);
				chartModel.setUpdateTime(functions.getCurrentDateWithTime());				
			}
			responseModel.setResponseMessage("Success");
			if(chartService.saveChart(chartModel)) {				
				responseModel.setjData(functions.convertEntityToJsonObject(chartModel));
				responseModel.setResponseDescription("Chart Successfully saved");
				responseModel.setResponseCode(HttpStatus.OK);
				mapService.saveMap(chartModel.getCategoryList(), chartModel.getChartId());
			}else {
				responseModel.setResponseDescription("Chart not saved");
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
	
	@PutMapping
	public ResponseEntity<?> updateChartConfig(@RequestBody ChartModel_v2 chartModel) {
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try 
		{
			
			responseModel.setResponseMessage("Success");
			ChartModel_v2 chartExit=chartService.getChartByChartId(chartModel.getChartId());
			
			if(chartExit==null) {
				responseModel.setResponseDescription("Chart not found");
				responseModel.setResponseCode(HttpStatus.NOT_FOUND);
				return ResponseEntity.ok(responseModel);
			}
			BeanUtils.copyProperties(chartModel, chartExit, NullAwareBeanUtilsBean.getNullPropertyNames(chartModel));	
			chartModel.setActive(true);
			chartModel.setUpdateTime(functions.getCurrentDateWithTime());				
			
			
			if(chartService.saveChart(chartExit)) {				
				responseModel.setjData(functions.convertEntityToJsonObject(chartExit));
				responseModel.setResponseDescription("Chart Successfully saved");
				responseModel.setResponseCode(HttpStatus.OK);
				mapService.saveMap(chartModel.getCategoryList(), chartModel.getChartId());
			}else {
				responseModel.setResponseDescription("Chart not saved");
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
				responseModel.setResponseDescription("Chart Successfully Retrieved");
				responseModel.setResponseCode(HttpStatus.OK);
			}else {
				responseModel.setResponseDescription("Chart not Retrieved");
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
	
	
	@GetMapping("/user/{userId}")
	public ResponseEntity<?> getChartByUserId(@PathVariable String userId){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			
			List<ChartModel_v2> chart=chartService.getChartByUserId(userId);
			responseModel.setResponseMessage("Success");
			if(chart!=null) {
				responseModel.setjData(chart);
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

		} finally {
			return ResponseEntity.ok(responseModel);
		}
	}
	
	
	@DeleteMapping
	public ResponseEntity<?> delelteChartByChartId(@RequestParam String chartId){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			responseModel.setResponseMessage("Success");			
			ChartModel_v2 chart=chartService.getChartByChartId(chartId);
				if(chart==null || chart.getChartId().isEmpty()) {					
					responseModel.setResponseDescription("Chart not found ");
					responseModel.setResponseCode(HttpStatus.NOT_FOUND);			
					return ResponseEntity.ok(responseModel);
				}		
			
				chart.setActive(false);
			if(chartService.deleteChartByObject(chart)) {
				//responseModel.setjData(functions.convertEntityToJsonObject(chartService.getChartByChartId(chartId)));
				responseModel.setResponseDescription("Chart Successfully deleted");
				responseModel.setResponseCode(HttpStatus.OK);
			}else {
				responseModel.setResponseDescription("Chart not deleted");
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
	
	
	@GetMapping("/report")
	public ResponseEntity<?> getMigarationReport(@RequestParam String siteKey,
			@RequestParam String reportName,@RequestParam String userId){
		
		ResponseModel_v2 responseModel = new ResponseModel_v2();
		try {
			responseModel.setResponseMessage("Success");
			JSONArray responce=chartService.getMigarationReport(siteKey,userId,reportName);
		
		if(responce!=null) {				
				responseModel.setjData(responce);
				responseModel.setResponseDescription("Chart Successfully inserted");
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
}
