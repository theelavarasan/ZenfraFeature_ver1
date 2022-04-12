package com.zenfra.service;

import org.springframework.stereotype.Component;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;

@Component
public interface PasswordPolicyService {

	public Response createPwdPolicy(String userId, PasswordPolicyModel model);

	public Response updatePwdPolicy(String userId, String pwdPolicyId, PasswordPolicyModel model);

	public Response listPwdPolicy(String pwdPolicyId);

	public Response deletePwdPolicy(String pwdPolicyId);
}
