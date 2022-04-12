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

	@RequestMapping(value = "/pwd/policy/create", method = RequestMethod.POST)
	public ResponseEntity<Response> createPwdPolicy(@RequestParam String userId,
			@RequestBody PasswordPolicyModel model) {
		System.out.println("!!!!!!!!!Password Policy Expire:" + model.getPwdExpire());
		System.out.println("!!!!!!!!!Password Policy Lock:" + model.getPwdLock());
		System.out.println("!!!!!!!!!Password Policy Length:" + model.getPwdLength());
		System.out.println("!!!!!!!!!Password Policy AlphaUpper:	" + model.isAlphaUpper());
		System.out.println("!!!!!!!!!Password Policy AlphaLower:	" + model.isAlphaLower());
		System.out.println("!!!!!!!!!Password Policy NonAlphaNumeric:" + model.isNonAlphaNumeric());
		System.out.println("!!!!!!!!!Password Policy Numbers:	" + model.isNumbers());
		System.out.println("!!!!!!!!!Password Policy NonFnIn:" + model.isNonFnIn());
		return ResponseEntity.ok(dao.createPwdPolicy(userId, model));
	}

	@RequestMapping(value = "/pwd/policy/update", method = RequestMethod.PUT)
	public ResponseEntity<Response> updatePwdPolicy(@RequestParam String userId, @RequestParam String pwdPolicyId,
			@RequestBody PasswordPolicyModel model) {
		return ResponseEntity.ok(dao.updatePwdPolicy(userId, pwdPolicyId, model));
	}

	@RequestMapping(value = "/pwd/policy/list", method = RequestMethod.GET)
	public ResponseEntity<Response> listPwdPolicy(@RequestParam String pwdPolicyId) {
		return ResponseEntity.ok(dao.listPwdPolicy(pwdPolicyId));
	}

	@RequestMapping(value = "/pwd/policy/delete", method = RequestMethod.GET)
	public ResponseEntity<Response> deletePwdPolicy(@RequestParam String pwdPolicyId) {
		return ResponseEntity.ok(dao.deletePwdPolicy(pwdPolicyId));
	}
}
