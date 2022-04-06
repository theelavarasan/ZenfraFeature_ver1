package com.zenfra.service;

import org.springframework.stereotype.Component;
import com.zenfra.model.PureConfigModel;
import com.zenfra.model.Response;

@Component
public interface PureConfigService {

	public Response insertPureConfig(PureConfigModel model);

	public Response updatePureConfig(PureConfigModel model);

	public Response getPureConfig(String pureKeyConfigId);

	public Response listPureConfig(String pureKeyConfigId);

	public Response deletePureConfig(String pureKeyConfigId);

}
