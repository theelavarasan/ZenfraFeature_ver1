package com.zenfra.service;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dao.OktaLoginRepository;
import com.zenfra.model.OktaLoginModel;

@Service
public class OktaLoginService {

	@Autowired
	private OktaLoginRepository oktaLoginRepository;

	public JSONObject saveData(OktaLoginModel OktaLoginModel) {
		JSONObject result = new JSONObject();
		ObjectMapper mapper = new ObjectMapper();
		try {
			OktaLoginModel res = oktaLoginRepository.findById(OktaLoginModel.getId()).orElse(null);
			
			if (res == null) {
				OktaLoginModel.setActive(true);
				res = oktaLoginRepository.save(OktaLoginModel);
				JSONObject jsonData = mapper.convertValue(res, JSONObject.class);
				result.put("data", jsonData);
				result.put("msg", "sucess");
				return result;
			} else {
				return result;
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return result;
	}

	public JSONObject getData(String id) {
		JSONObject resObject = new JSONObject();
		try {

			OktaLoginModel res = oktaLoginRepository.findById(id).orElse(null);
			resObject.put("id", res.getId());
			resObject.put("publisherUrl", res.getPublisherUrl());
			resObject.put("clientId", res.getClientId());

			return resObject;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return resObject;
		}

	}

	public String updateData(OktaLoginModel OktaLoginModel) {

		try {
			OktaLoginModel existingData = oktaLoginRepository.findById(OktaLoginModel.getId()).orElse(null);
			existingData.setClientId(OktaLoginModel.getClientId());
			existingData.setPublisherUrl(OktaLoginModel.getPublisherUrl());
			existingData.setActive(OktaLoginModel.isActive());
			oktaLoginRepository.save(existingData);
			return "Success";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "Failure";
		}
	}

	public String deleteData(String id) {

		try {
			oktaLoginRepository.deleteById(id);
			return "Success";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "Failure";
		}
	}
}
