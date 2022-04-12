package com.zenfra.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
	public ResponseEntity<Response> createPwdPolicy(@RequestBody PasswordPolicyModel model) {
		System.out.println("!!!!!!!!!Password Policy Expire:" + model.getPwdExpire());
		System.out.println("!!!!!!!!!Password Policy Lock:" + model.getPwdLock());
		System.out.println("!!!!!!!!!Password Policy Length:" + model.getPwdLength());
		System.out.println("!!!!!!!!!Password Policy AlphaUpper:	" + model.isAlphaUpper());
		System.out.println("!!!!!!!!!Password Policy AlphaLower:	" + model.isAlphaLower());
		System.out.println("!!!!!!!!!Password Policy NonAlphaNumeric:" + model.isNonAlphaNumeric());
		System.out.println("!!!!!!!!!Password Policy Numbers:	" + model.isNumbers());
		System.out.println("!!!!!!!!!Password Policy NonFnIn:" + model.isNonFnIn());
		return ResponseEntity.ok(dao.createPwdPolicy(model));
	}

}
