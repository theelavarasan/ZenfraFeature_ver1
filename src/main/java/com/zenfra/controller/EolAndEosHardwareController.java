package com.zenfra.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.EolAndEosHardwareModel;
import com.zenfra.service.EolAndEosHardwareService;
import com.zenfra.utils.CommonFunctions;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/eol-eos-hw")
public class EolAndEosHardwareController {

	@Autowired
	private EolAndEosHardwareService eolAndEosHardwareService;
	
	@Autowired
	CommonFunctions commonFunctions;

	@PostMapping("/insert")
	public ResponseEntity<?> insertData(@RequestBody List<EolAndEosHardwareModel> models) {
		for (EolAndEosHardwareModel model : models) {
			model.setActive(true);
			model.setManual(true);			
			model.setUpdated_time(commonFunctions.getUtcDateTime());
		}
			
		return ResponseEntity.ok(eolAndEosHardwareService.saveData(models));

	}

	@PutMapping("/update")
	public ResponseEntity<?> updatedata(@RequestBody List<EolAndEosHardwareModel> models) {
		for (EolAndEosHardwareModel model : models) {
			model.setActive(true);			
			model.setUpdated_time(commonFunctions.getUtcDateTime());
		}
			
		return ResponseEntity.ok(eolAndEosHardwareService.update(models));
	}
	

	@PutMapping("/delete")
	public ResponseEntity<?> deleteData(@RequestBody List<EolAndEosHardwareModel> models) {
		for (EolAndEosHardwareModel model : models) {
			model.setActive(false);
			model.setUpdated_time(commonFunctions.getUtcDateTime());
		}
			
		return ResponseEntity.ok(eolAndEosHardwareService.update(models));

	}

}
