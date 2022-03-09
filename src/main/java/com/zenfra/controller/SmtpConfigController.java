package com.zenfra.controller;

import java.util.UUID;

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

import com.zenfra.model.SmtpConfigModel;
import com.zenfra.service.SmtpConfigService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/smtp-config")

public class SmtpConfigController {

	@Autowired
	SmtpConfigService smtpConfigService;

	@PostMapping("/create")
	public ResponseEntity<?> insertSmtpData(@RequestBody SmtpConfigModel smtpConfigModel) {
		smtpConfigModel.setActive(true);
		smtpConfigModel.setConfigId(UUID.randomUUID().toString());

		return ResponseEntity.ok(smtpConfigService.saveSmtpData(smtpConfigModel));
	}

	@GetMapping("/get")
	public ResponseEntity<?> getSmtpData(@RequestParam String smtpConfigId) {

		return ResponseEntity.ok(smtpConfigService.getSmtpData(smtpConfigId));
	}
	
	@PutMapping("/update")
	public ResponseEntity<?> updateSmtpData(@RequestBody SmtpConfigModel smtpConfigModel) {

		return ResponseEntity.ok(smtpConfigService.updateSmtpData(smtpConfigModel));
	}
	
	@PutMapping("/delete")
	public ResponseEntity<?> deleteSmtpData(@RequestParam String smtpConfigId) {

		return ResponseEntity.ok(smtpConfigService.deleteSmtpData(smtpConfigId));
	}
	

}
