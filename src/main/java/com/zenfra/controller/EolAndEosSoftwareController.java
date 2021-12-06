package com.zenfra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.model.EolAndEosSoftwareModel;
import com.zenfra.service.EolAndEosSoftwareService;

@RestController
@RequestMapping("/rest/eol-eos-sw")
public class EolAndEosSoftwareController {

	@Autowired
	private EolAndEosSoftwareService eolAndEosSoftwareService;

	@PostMapping("/insert")
	public EolAndEosSoftwareModel addData(@RequestBody EolAndEosSoftwareModel model) {
		model.setActive(true);
		return eolAndEosSoftwareService.saveData(model);
		
	}

	@GetMapping("/getData")
	public EolAndEosSoftwareModel getData(@RequestParam String osVersion) {
		return eolAndEosSoftwareService.getData(osVersion);

	}

	@PutMapping("/update")
	public EolAndEosSoftwareModel update(@RequestBody EolAndEosSoftwareModel model) {
		model.setActive(true);
		return eolAndEosSoftwareService.update(model);
	}

	@DeleteMapping("/delete")
	public EolAndEosSoftwareModel delete(@RequestBody EolAndEosSoftwareModel model) {
		 model.setActive(false);
		 return eolAndEosSoftwareService.update(model);

	}

}
