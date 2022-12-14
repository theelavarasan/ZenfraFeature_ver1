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

import com.zenfra.model.EolAndEosSoftwareModel;
import com.zenfra.service.EolAndEosSoftwareService;
import com.zenfra.utils.CommonFunctions;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/eol-eos-sw")
public class EolAndEosSoftwareController {

	@Autowired
	private EolAndEosSoftwareService eolAndEosSoftwareService;
	
	@Autowired
	CommonFunctions commonFunctions;

	@PostMapping("/insert")
	public ResponseEntity<?> insertData(@RequestBody List<EolAndEosSoftwareModel> models) {
		for (EolAndEosSoftwareModel model : models) {
			model.setActive(true);
			model.setManual(true);			
			model.setUpdated_time(commonFunctions.getUtcDateTime());
		}
		return ResponseEntity.ok(eolAndEosSoftwareService.saveData(models));

	}

	@PutMapping("/update")
	public ResponseEntity<?> updateData(@RequestBody List<EolAndEosSoftwareModel> models) {
		for (EolAndEosSoftwareModel model : models) {
			model.setActive(true);
			model.setUpdated_time(commonFunctions.getUtcDateTime());
		}
		return ResponseEntity.ok(eolAndEosSoftwareService.update(models));
	}

	@PutMapping("/delete")
	public ResponseEntity<?> deletedata(@RequestBody List<EolAndEosSoftwareModel> models) {
		for (EolAndEosSoftwareModel model : models) {
			model.setActive(false);
			model.setUpdated_time(commonFunctions.getUtcDateTime());
		}
		return ResponseEntity.ok(eolAndEosSoftwareService.delete(models));
	}

}
