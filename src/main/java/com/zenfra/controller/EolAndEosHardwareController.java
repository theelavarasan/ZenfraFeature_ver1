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

import com.zenfra.model.EolAndEosHardwareModel;
import com.zenfra.service.EolAndEosHardwareService;

@RestController
@RequestMapping("/rest/eol-eos-hw")
public class EolAndEosHardwareController {

	@Autowired
	private EolAndEosHardwareService eolAndEosHardwareService;

	@PostMapping("/insert")
	public EolAndEosHardwareModel addData(@RequestBody EolAndEosHardwareModel model) {
		model.setActive(true);
		return eolAndEosHardwareService.saveData(model);

	}

	@GetMapping("/getData")
	public EolAndEosHardwareModel getData(@RequestParam String vendor) {
		return eolAndEosHardwareService.getData(vendor);

	}

	@PutMapping("/update")

	public EolAndEosHardwareModel update(@RequestBody EolAndEosHardwareModel model) {
		model.setActive(true);
		return eolAndEosHardwareService.update(model);
	}

	@DeleteMapping("/delete")
	public EolAndEosHardwareModel delete(@RequestBody EolAndEosHardwareModel model) {
		model.setActive(false);
		return eolAndEosHardwareService.update(model);

	}

}
