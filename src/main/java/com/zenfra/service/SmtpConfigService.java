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

	public ResponseEntity<?> getSmtpData(String tenantId) {
		try {
			Optional<SmtpConfigModel> smtpData = smtpConfigRepository.findByTenantId(tenantId);

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
			SmtpConfigModel existingSmtpData = smtpConfigRepository
					.findByTenantId(smtpConfigModel.getTenantId() == null ? "" : smtpConfigModel.getTenantId())
					.orElse(smtpConfigModel);

			existingSmtpData
					.setFromAddress(smtpConfigModel.getFromAddress() == null ? "" : smtpConfigModel.getFromAddress());
			existingSmtpData
			.setSenderUsername(smtpConfigModel.getFromAddress() == null ? "" : smtpConfigModel.getFromAddress());
			existingSmtpData
					.setSenderHost(smtpConfigModel.getSenderHost() == null ? "" : smtpConfigModel.getSenderHost());
			existingSmtpData.setSenderPassword(
					smtpConfigModel.getSenderPassword() == null ? "" : smtpConfigModel.getSenderPassword());
			existingSmtpData
					.setSenderPort(smtpConfigModel.getSenderPort() == null ? "" : smtpConfigModel.getSenderPort());
			existingSmtpData.setSenderUsername(
					smtpConfigModel.getSenderUsername() == null ? "" : smtpConfigModel.getSenderUsername());
			existingSmtpData.setTransportProtocol(
					smtpConfigModel.getTransportProtocol() == null ? "" : smtpConfigModel.getTransportProtocol());
			existingSmtpData.setTenantId((smtpConfigModel.getTenantId() == null ? "" : smtpConfigModel.getTenantId()));

			smtpConfigRepository.save(existingSmtpData);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(existingSmtpData);
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}

	}

	public ResponseEntity<?> deleteSmtpData(String tenantId) {
		try {

			Optional<SmtpConfigModel> smtpData = smtpConfigRepository.findByTenantId(tenantId);

			if (smtpData.isPresent()) {
				SmtpConfigModel smtpDataObj = smtpData.get();
				smtpConfigRepository.delete(smtpDataObj);
			}

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
