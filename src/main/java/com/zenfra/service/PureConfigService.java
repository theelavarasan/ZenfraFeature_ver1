package com.zenfra.service;

import org.springframework.stereotype.Component;
import com.zenfra.model.PureConfigModel;
import com.zenfra.model.Response;

@Component
public interface PureConfigService {

	public Response insertPureConfig(String userId, PureConfigModel model);

	public Response updatePureConfig(String userId, PureConfigModel model, String pureKeyConfigId);

//	public Response getPureConfig(String pureKeyConfigId);

	public Response listPureConfig(String siteKey);

	public Response deletePureConfig(String pureKeyConfigId);
	
	public Response getPureKeyList(String siteKey);

}
