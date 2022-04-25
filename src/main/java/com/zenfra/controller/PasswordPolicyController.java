package com.zenfra.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zenfra.dao.PasswordPolicyDao;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;
import com.zenfra.service.PasswordPolicyService;
import io.swagger.annotations.Api;

@CrossOrigin("*")
@RestController
@RequestMapping("/rest")
@Api(value = "Password Policy Controller", description = "Password Policy CRUD Operations")
public class PasswordPolicyController {
	
	PasswordPolicyService dao = new PasswordPolicyDao();

	@RequestMapping(value = "/pwd/policy/update", method = RequestMethod.PUT)
	public ResponseEntity<Response> updatePwdPolicy(@RequestParam String userId, @RequestParam String tenantId,
			@RequestBody PasswordPolicyModel model) {
		return ResponseEntity.ok(dao.updatePwdPolicy(userId, tenantId, model));
	}

	@RequestMapping(value = "/pwd/policy/get", method = RequestMethod.GET)
	public ResponseEntity<Response> getPwdPolicy() {
		return ResponseEntity.ok(dao.getPwdPolicy());
	}

	@RequestMapping(value = "/pwd/policy/delete", method = RequestMethod.DELETE)
	public ResponseEntity<Response> deletePwdPolicy(@RequestParam String tenantId) {
		return ResponseEntity.ok(dao.deletePwdPolicy(tenantId));
	}
	
	@RequestMapping(value = "/pwd/policy/last-passwords", method = RequestMethod.GET)
	public ResponseEntity<Response> existingPwdPolicy(String userId) {
		return ResponseEntity.ok(dao.existingPwdPolicy(userId));
	}
}

