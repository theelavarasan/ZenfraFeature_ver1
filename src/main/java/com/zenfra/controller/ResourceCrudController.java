package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.ResourceCrudModel;
import com.zenfra.service.ResourceCrudService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/resource")
public class ResourceCrudController {
	@Autowired
	ResourceCrudService resourceCrudService;

	@PostMapping("/create")
	public ResponseEntity<?> insertData(@RequestBody ResourceCrudModel resourceCrudModel) {
		resourceCrudModel.setActive(true);
		return ResponseEntity.ok(resourceCrudService.insert(resourceCrudModel));

	}

	@GetMapping("/get-resource")
	public ResponseEntity<?> getData(@RequestParam (value = "resourceDataId", required = false) String resourceDataId) {
		return ResponseEntity.ok(resourceCrudService.get(resourceDataId));

	}


	@PutMapping("/update")
	public ResponseEntity<?> updateData(@RequestBody ResourceCrudModel resourceCrudModel) {
		resourceCrudModel.setActive(true);
		return ResponseEntity.ok(resourceCrudService.update(resourceCrudModel));

	}

	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteData(@RequestParam (value = "resourceDataId", required = false) String resourceDataId) {
		return ResponseEntity.ok(resourceCrudService.delete(resourceDataId));

	}

}
