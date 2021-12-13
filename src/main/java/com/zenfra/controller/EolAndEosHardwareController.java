package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.EolAndEosHardwareModel;
import com.zenfra.service.EolAndEosHardwareService;

@RestController
@RequestMapping("/rest/eol-eos-hw")
public class EolAndEosHardwareController {

	@Autowired
	private EolAndEosHardwareService eolAndEosHardwareService;

	@PostMapping("/insert")
	public ResponseEntity<?> insertData(@RequestBody EolAndEosHardwareModel model) {
		model.setActive(true);
		return ResponseEntity.ok(eolAndEosHardwareService.saveData(model));

	}

	@PutMapping("/update")
	public ResponseEntity<?> updatedata(@RequestBody EolAndEosHardwareModel model) {
		model.setActive(true);
		return ResponseEntity.ok(eolAndEosHardwareService.update(model));
	}
	

	@PutMapping("/delete")
	public ResponseEntity<?> deleteData(@RequestBody EolAndEosHardwareModel model) {
		model.setActive(false);
		return ResponseEntity.ok(eolAndEosHardwareService.update(model));

	}

}
