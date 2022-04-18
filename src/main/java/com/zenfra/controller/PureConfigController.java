package com.zenfra.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zenfra.dao.PureConfigDao;
import com.zenfra.model.PureConfigModel;
import com.zenfra.model.Response;
import com.zenfra.service.PureConfigService;
import io.swagger.annotations.Api;

@CrossOrigin("*")
@RestController
@RequestMapping("/rest")
@Api(value = "Pure Config Controller", description = "Pure CRUD Operations")
public class PureConfigController {

	PureConfigService dao = new PureConfigDao();

	@RequestMapping(value = "/pure/insert", method = RequestMethod.POST)
	public ResponseEntity<Response> insertPureConfig(@RequestAttribute("authUserId") String userId, @RequestBody PureConfigModel model) {
		System.out.println("!!!!! 1");
		System.out.println("!!!!! name: " + model.getHostName());
		System.out.println("!!!!! siteKey: " + model.getSiteKey());
		System.out.println("!!!!! tenantId: " + model.getTenantId());
		
		return ResponseEntity.ok(dao.insertPureConfig(userId, model));
	}

	@RequestMapping(value = "/pure/update", method = RequestMethod.PUT)
	public ResponseEntity<Response> updatePureConfig(@RequestAttribute("authUserId") String userId, @RequestBody PureConfigModel model, @RequestAttribute("pureKeyConfigId") String pureKeyConfigId) {
		return ResponseEntity.ok(dao.updatePureConfig(userId, model, pureKeyConfigId));
	}

//	@RequestMapping(value = "/pure/get", method = RequestMethod.GET)
//	public ResponseEntity<Response> getPureConfig(@RequestAttribute("authUserId") String userId, @RequestParam("pureKeyConfigId")  String pureKeyConfigId) {
//		return ResponseEntity.ok(dao.getPureConfig(pureKeyConfigId));
//	}

	@RequestMapping(value = "/pure/list", method = RequestMethod.GET)
	public ResponseEntity<Response> listPureConfig(@RequestAttribute("authUserId") String userId,  @RequestParam("siteKey")  String siteKey) {
		return ResponseEntity.ok(dao.listPureConfig(siteKey));
	}

	@RequestMapping(value = "/pure/delete", method = RequestMethod.DELETE)
	public ResponseEntity<Response> deletePureConfig(@RequestAttribute("authUserId") String userId, @RequestParam("pureKeyConfigId")  String pureKeyConfigId) {
		return ResponseEntity.ok(dao.deletePureConfig(pureKeyConfigId));
	}
	
	@RequestMapping(value = "/pure/key-list", method = RequestMethod.GET)
	public ResponseEntity<Response> getKeyList(@RequestAttribute("authUserId") String userId, @RequestParam("siteKey")  String siteKey) {
		return ResponseEntity.ok(dao.getPureKeyList(siteKey));
	}
}
