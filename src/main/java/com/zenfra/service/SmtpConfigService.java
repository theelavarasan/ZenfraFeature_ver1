package com.zenfra.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.zenfra.dao.SmtpConfigRepository;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.SmtpConfigModel;

@Component
public class SmtpConfigService {

	ResponseModel_v2 responseModel = new ResponseModel_v2();

	@Autowired
	SmtpConfigRepository smtpConfigRepository;

	public ResponseEntity<?> saveSmtpData(SmtpConfigModel smtpConfigModel) {
		try {
			smtpConfigRepository.save(smtpConfigModel);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(smtpConfigModel);
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}

	}

	public ResponseEntity<?> getSmtpData(String smtpConfigId) {
		try {
			Optional<SmtpConfigModel> smtpData = smtpConfigRepository.findById(smtpConfigId);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(smtpData);
			return ResponseEntity.ok(responseModel);
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);

		}

	}

	public ResponseEntity<?> updateSmtpData(SmtpConfigModel smtpConfigModel) {
		try {
			SmtpConfigModel existingSmtpData = smtpConfigRepository.findById(smtpConfigModel.getConfigId()== null ? "" : smtpConfigModel.getConfigId()).orElse(smtpConfigModel);
			
			existingSmtpData.setConfigId(smtpConfigModel.getConfigId() == null ? "" : smtpConfigModel.getConfigId());
			existingSmtpData.setFromAddress(smtpConfigModel.getFromAddress() == null ? "" : smtpConfigModel.getFromAddress());
			existingSmtpData.setSenderHost(smtpConfigModel.getSenderHost() == null ? "" : smtpConfigModel.getSenderHost());
			existingSmtpData.setSenderPassword(smtpConfigModel.getSenderPassword() == null ? "" : smtpConfigModel.getSenderPassword());
			existingSmtpData.setSenderPort(smtpConfigModel.getSenderPort() == null ? "" : smtpConfigModel.getSenderPort());
			existingSmtpData.setSenderUsername(smtpConfigModel.getSenderUsername() == null ? "" : smtpConfigModel.getSenderUsername());
			existingSmtpData.setTransportProtocol(smtpConfigModel.getTransportProtocol() == null ? "" : smtpConfigModel.getTransportProtocol());
			
			smtpConfigRepository.save(existingSmtpData);
			
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(existingSmtpData);
			return ResponseEntity.ok(responseModel);
			
		}catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
		
		
		
	}

	public ResponseEntity<?> deleteSmtpData(String smtpConfigId) {
		try {
			smtpConfigRepository.deleteById(smtpConfigId);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData("Data Removed Successfully");
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
	}

}
