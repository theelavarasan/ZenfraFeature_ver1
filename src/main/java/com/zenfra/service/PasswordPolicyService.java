package com.zenfra.service;

import org.springframework.stereotype.Component;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;

@Component
public interface PasswordPolicyService {

	public Response createPwdPolicy(PasswordPolicyModel model);
}
