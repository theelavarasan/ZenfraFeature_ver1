package com.zenfra.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.zenfra.dao.PureConfigDao;
import com.zenfra.model.PureConfigModel;
import com.zenfra.model.Response;
import com.zenfra.service.PureConfigService;

import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@CrossOrigin("*")
@RestController
@RequestMapping("/rest")
@Api(value = "Pure Config Controller", description = "Pure CRUD Operations")
public class PureConfigController {

	PureConfigService dao = new PureConfigDao();

	@RequestMapping(value = "/pure/insert", method = RequestMethod.POST)
	public ResponseEntity<Response> insertPureConfig(@RequestBody PureConfigModel model) {
		System.out.println("!!!!! 1");
		System.out.println("!!!!! Id: " + model.getPureKeyConfigId());
		System.out.println("!!!!! name: " + model.getArrayName());
		System.out.println("!!!!! siteKey: " + model.getSiteKey());
		System.out.println("!!!!! tenantId: " + model.getTenantId());
		
		return ResponseEntity.ok(dao.insertPureConfig(model));
	}

	@RequestMapping(value = "/pure/update", method = RequestMethod.PUT)
	public ResponseEntity<Response> updatePureConfig(@RequestBody PureConfigModel model, @RequestAttribute String pureKeyConfigId) {
		return ResponseEntity.ok(dao.updatePureConfig(model, pureKeyConfigId));
	}

	@RequestMapping(value = "/pure/get", method = RequestMethod.GET)
	public ResponseEntity<Response> getPureConfig(@RequestAttribute String pureKeyConfigId) {
		return ResponseEntity.ok(dao.getPureConfig(pureKeyConfigId));
	}

	@RequestMapping(value = "/pure/list", method = RequestMethod.GET)
	public ResponseEntity<Response> listPureConfig(String pureKeyConfigId) {
		return ResponseEntity.ok(dao.listPureConfig(pureKeyConfigId));
	}

	@RequestMapping(value = "/pure/delete", method = RequestMethod.DELETE)
	public ResponseEntity<Response> deletePureConfig(@RequestAttribute String pureKeyConfigId) {
		return ResponseEntity.ok(dao.deletePureConfig(pureKeyConfigId));
	}
}
