package com.zenfra.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import com.zenfra.model.PasswordPolicyModel;
import com.zenfra.model.Response;

@Component
public interface PasswordPolicyService {

	public Response createPwdPolicy(String userId, PasswordPolicyModel model, List<ArrayList<String>>value);

	public Response updatePwdPolicy(String userId, String tenantId, PasswordPolicyModel model, List<ArrayList<String>>value);

	public Response listPwdPolicy(String tenantId);

	public Response deletePwdPolicy(String tenantId);
}
