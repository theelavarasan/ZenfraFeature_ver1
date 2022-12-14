package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.zenfra.dao.EolAndEosHardwareRepository;
import com.zenfra.model.EolAndEosHardwareIdentityModel;
import com.zenfra.model.EolAndEosHardwareModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class EolAndEosHardwareService {

	@Autowired
	private EolAndEosHardwareRepository eolAndEosHardwareRepository;

	ResponseModel_v2 responseModel = new ResponseModel_v2();

	public ResponseEntity<?> saveData(List<EolAndEosHardwareModel> models) {
		try {
			for (EolAndEosHardwareModel model : models) {
				model.setEol_eos_hw_id(UUID.randomUUID().toString());
				model.setEolAndEosHardwareIdentityModel(
						new EolAndEosHardwareIdentityModel(model.getVendor(), model.getModel()));
				eolAndEosHardwareRepository.save(model);
			}

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(models);
			return ResponseEntity.ok(responseModel);
		} catch (Exception e) {

			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}

	}

	public ResponseEntity<?> update(List<EolAndEosHardwareModel> models) {
		List<EolAndEosHardwareModel> massUpdate = new ArrayList();
		try {
			for (EolAndEosHardwareModel model : models) {
				/*
				 * EolAndEosHardwareModel existing = eolAndEosHardwareRepository .findById(new
				 * EolAndEosHardwareIdentityModel(model.getVendor(),
				 * model.getModel())).orElse(null);
				 */
				EolAndEosHardwareModel existing = eolAndEosHardwareRepository.findByHwId(model.getEol_eos_hw_id());
				if (existing != null) {
					existing.setEnd_of_life_cycle(model.getEnd_of_life_cycle());
					existing.setEnd_of_extended_support(model.getEnd_of_extended_support());
					existing.setSource_link(model.getSource_link());
					existing.setEol_eos_hw_id(model.getEol_eos_hw_id());
					existing.setUser_id(model.getUser_id());
					existing.setActive(model.isActive());
					existing.setUpdated_time(model.getUpdated_time());
					existing.setUpdated_by(model.getUpdated_by());
					existing.setVendor(model.getVendor());
					existing.setModel(model.getModel());
					existing.setFrom_discovery(model.isFrom_discovery());
					massUpdate.add(existing);
				} else {
					EolAndEosHardwareModel newEntry = new EolAndEosHardwareModel();
					newEntry.setEol_eos_hw_id(UUID.randomUUID().toString());
					newEntry.setEolAndEosHardwareIdentityModel(
							new EolAndEosHardwareIdentityModel(model.getVendor(), model.getModel()));
					newEntry.setEnd_of_life_cycle(model.getEnd_of_life_cycle());
					newEntry.setEnd_of_extended_support(model.getEnd_of_extended_support());
					newEntry.setSource_link(model.getSource_link());
					newEntry.setUser_id(model.getUser_id());
					newEntry.setActive(model.isActive());
					newEntry.setUpdated_time(model.getUpdated_time());
					newEntry.setUpdated_by(model.getUpdated_by());
					newEntry.setVendor(model.getVendor());
					newEntry.setModel(model.getModel());
					newEntry.setActive(true);
					newEntry.setManual(true);
					newEntry.setFrom_discovery(true);
					massUpdate.add(newEntry);
				}

			}
			eolAndEosHardwareRepository.saveAll(massUpdate);
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(models);
			return ResponseEntity.ok(responseModel);
		}

		catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}

	}

	public ResponseEntity<?> delete(List<EolAndEosHardwareModel> models) {
		try {
			List<String> ids = new ArrayList<String>();
			for (EolAndEosHardwareModel model : models) {
				ids.add(model.getEol_eos_hw_id());
			}

			eolAndEosHardwareRepository.deleteByEolEosHwId(ids);
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(models);
			return ResponseEntity.ok(responseModel);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();

		}
	}

}
