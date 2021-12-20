package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.EolAndEosSoftwareModel;
import com.zenfra.service.EolAndEosSoftwareService;

@RestController
@RequestMapping("/rest/eol-eos-sw")
public class EolAndEosSoftwareController {

	@Autowired
	private EolAndEosSoftwareService eolAndEosSoftwareService;

	@PostMapping("/insert")
	public ResponseEntity<String> insertData(@RequestBody EolAndEosSoftwareModel model) {
		model.setActive(true);
		return ResponseEntity.ok(eolAndEosSoftwareService.saveData(model));

	}

	@PutMapping("/update")
	public ResponseEntity<String> updateData(@RequestBody EolAndEosSoftwareModel model) {
		model.setActive(true);
		return ResponseEntity.ok(eolAndEosSoftwareService.update(model));
	}

	@DeleteMapping("/delete")
	public ResponseEntity<String> deletedata(@RequestBody EolAndEosSoftwareModel model) {
		model.setActive(false);
		return ResponseEntity.ok(eolAndEosSoftwareService.update(model));
	}

}
