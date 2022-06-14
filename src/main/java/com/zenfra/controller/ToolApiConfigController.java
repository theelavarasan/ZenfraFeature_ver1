package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.zenfra.model.ToolApiConfigModel;
import com.zenfra.service.ToolApiConfigService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/tool-api-config")
public class ToolApiConfigController {

	@Autowired
	ToolApiConfigService toolApiConfigService;

	@PostMapping("/create")
	public ResponseEntity<?> createApiConfig(@RequestBody ToolApiConfigModel apiConfigId) throws JsonMappingException, JsonProcessingException {
		return ResponseEntity.ok(toolApiConfigService.createApiConfig(apiConfigId));
	}

	@GetMapping("/get")
	public ResponseEntity<?> getToolApiData(@RequestParam String toolApiConfigId) {
		return ResponseEntity.ok(toolApiConfigService.getToolApiData(toolApiConfigId));
	}

	@GetMapping("/list")
	public ResponseEntity<?> getListToolApiData(@RequestParam String siteKey, @RequestParam String deviceType) {
		return ResponseEntity.ok(toolApiConfigService.getListToolApiData(siteKey, deviceType));
	}
	
	@GetMapping("/list-configName")
	public ResponseEntity<?> getListConfigName(@RequestParam String siteKey) {
		return ResponseEntity.ok(toolApiConfigService.getListConfigName(siteKey));
	}

	@PutMapping("/update")
	public ResponseEntity<?> updateListToolApiData(@RequestBody ToolApiConfigModel toolApiConfigModel) {
		return ResponseEntity.ok(toolApiConfigService.updateListToolApiData(toolApiConfigModel));
	}
	
	@PutMapping("/delete")
	public ResponseEntity<?> deleteToolApiData(@RequestParam String apiConfigId) {
		return ResponseEntity.ok(toolApiConfigService.deleteToolApiData(apiConfigId));
	}

}
