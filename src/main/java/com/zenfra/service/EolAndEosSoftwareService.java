package com.zenfra.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.zenfra.dao.EolAndEosSoftwareRepository;
import com.zenfra.model.EolAndEosHardwareModel;
import com.zenfra.model.EolAndEosSoftwareIdentityModel;
import com.zenfra.model.EolAndEosSoftwareModel;
import com.zenfra.model.ResponseModel_v2;

@Service
public class EolAndEosSoftwareService {

	@Autowired
	private EolAndEosSoftwareRepository eolAndEosSoftwareRepository;

	ResponseModel_v2 responseModel = new ResponseModel_v2();

	public ResponseEntity<?> saveData(List<EolAndEosSoftwareModel> models) {
		try {

			for (EolAndEosSoftwareModel model : models) {

				model.setEol_eos_sw_id(UUID.randomUUID().toString());
				model.setEolAndEosSoftwareIdentityModel(
						new EolAndEosSoftwareIdentityModel(model.getOs_version(), model.getOs_name()));
				eolAndEosSoftwareRepository.save(model);
			}

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(models);
			return ResponseEntity.ok(responseModel);

		}

		catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}

	}

	public ResponseEntity<?> update(List<EolAndEosSoftwareModel> models) {
		List<EolAndEosSoftwareModel> massUpdate = new ArrayList();
		try {
			for (EolAndEosSoftwareModel model : models) {
				EolAndEosSoftwareModel existing = eolAndEosSoftwareRepository.findBySwId(model
						.getEol_eos_sw_id());
				if (existing != null) {
					existing.setEol_eos_sw_id(model.getEol_eos_sw_id());
					existing.setSource_url(model.getSource_url());
					existing.setEnd_of_life_cycle(model.getEnd_of_life_cycle());
					existing.setOs_type(model.getOs_type());
					existing.setUser_id(model.getUser_id());
					existing.setOs_name(model.getOs_name());
					existing.setOs_version(model.getOs_version());
					existing.setEnd_of_extended_support(model.getEnd_of_extended_support());
					existing.setActive(model.isActive());
					existing.setUpdated_time(model.getUpdated_time());
					existing.setUpdated_by(model.getUpdated_by());
					existing.setFrom_discovery(model.isFrom_discovery());
					massUpdate.add(existing);

				} else {
					EolAndEosSoftwareModel newEntry = new EolAndEosSoftwareModel();
					newEntry.setEol_eos_sw_id(UUID.randomUUID().toString());
					newEntry.setEolAndEosSoftwareIdentityModel(new EolAndEosSoftwareIdentityModel(model.getOs_version(), model.getOs_name()));
					newEntry.setSource_url(model.getSource_url());
					newEntry.setEnd_of_life_cycle(model.getEnd_of_life_cycle());
					newEntry.setOs_type(model.getOs_type());
					newEntry.setUser_id(model.getUser_id());
					newEntry.setOs_name(model.getOs_name());
					newEntry.setOs_version(model.getOs_version());
					newEntry.setEnd_of_extended_support(model.getEnd_of_extended_support());
					newEntry.setActive(model.isActive());
					newEntry.setUpdated_time(model.getUpdated_time());
					newEntry.setUpdated_by(model.getUpdated_by());
					newEntry.setActive(true);
					newEntry.setManual(true);
					newEntry.setFrom_discovery(true);
					massUpdate.add(newEntry);
					
				}

			}
			eolAndEosSoftwareRepository.saveAll(massUpdate);
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(models);
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}

	}

	public ResponseEntity<?> delete(List<EolAndEosSoftwareModel> models) {
		try {
			List<String> ids = new ArrayList<String>();
			for(EolAndEosSoftwareModel model : models) {
				ids.add(model.getEol_eos_sw_id());
			}
			eolAndEosSoftwareRepository.deleteByEolEosSwId(ids);
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(models);
			return ResponseEntity.ok(responseModel);
		} catch(Exception e) {
			e.printStackTrace();
			e.printStackTrace();
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
			
		}
	}

}
