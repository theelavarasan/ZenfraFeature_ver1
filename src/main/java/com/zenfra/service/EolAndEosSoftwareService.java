package com.zenfra.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.zenfra.dao.EolAndEosSoftwareRepository;
import com.zenfra.model.EolAndEosSoftwareIdentityModel;
import com.zenfra.model.EolAndEosSoftwareModel;
import com.zenfra.model.ResponseModel_v2;

@Service
public class EolAndEosSoftwareService {

	@Autowired
	private EolAndEosSoftwareRepository eolAndEosSoftwareRepository;
	
	ResponseModel_v2 responseModel = new ResponseModel_v2();

	public ResponseEntity<?> saveData(EolAndEosSoftwareModel model) {
		try {
			model.setEolEosSwId(UUID.randomUUID().toString());
			model.setEolAndEosSoftwareIdentityModel(
					new EolAndEosSoftwareIdentityModel(model.getOsVersion(), model.getOsName(), model.getEolEosSwId()));
			eolAndEosSoftwareRepository.save(model);
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			return ResponseEntity.ok(model.getEolEosSwId());
		
		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}
		

	}

	public ResponseEntity<?> update(EolAndEosSoftwareModel model) {
		try {
			EolAndEosSoftwareModel existing = eolAndEosSoftwareRepository.findById(new EolAndEosSoftwareIdentityModel(model.getOsVersion(), model.getOsName(), model.getEolEosSwId())).orElse(null);
			existing.setSourceUrl(model.getSourceUrl());
			existing.setEndOfLifeCycle(model.getEndOfLifeCycle());
			existing.setOsType(model.getOsType());
			existing.setUserId(model.getUserId());
			existing.setOsName(model.getOsName());
			existing.setEndOfExtendedSupport(model.getEndOfExtendedSupport());
			existing.setActive(model.isActive());

			eolAndEosSoftwareRepository.save(existing);
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			return ResponseEntity.ok(model.getEolEosSwId());
			

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}
		

	}

}
