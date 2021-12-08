
package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.OktaLoginModel;
import com.zenfra.service.OktaLoginService;

@RestController
@RequestMapping("/rest/LoginOkta")
public class OktaLoginController {

	@Autowired
	OktaLoginService OktaLoginService;

	@PostMapping("/insert")
	public ResponseEntity<?> insertController(@RequestBody OktaLoginModel OktaLoginModel) {
		return ResponseEntity.ok(OktaLoginService.saveData(OktaLoginModel));

	}

	@GetMapping("/get")
	public ResponseEntity<?> selectController(@RequestParam String id) {
		return ResponseEntity.ok(OktaLoginService.getData(id));
	}

	@PutMapping("/update")
	public ResponseEntity<?> updateController(@RequestBody OktaLoginModel OktaLoginModel) {

		return ResponseEntity.ok(OktaLoginService.updateData(OktaLoginModel));
	}

	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteController(@RequestParam String id) {

		return ResponseEntity.ok(OktaLoginService.deleteData(id));

	}
}
