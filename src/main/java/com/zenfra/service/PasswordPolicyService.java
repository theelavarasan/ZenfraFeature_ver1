package com.zenfra.service;

import org.springframework.stereotype.Component;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;

@Component
public interface PasswordPolicyService {

	public Response updatePwdPolicy(String userId, String tenantId, PasswordPolicyModel model);

	public Response getPwdPolicy(String tenantId);

	public Response deletePwdPolicy(String tenantId);

	public Response existingPwdPolicy();
}
