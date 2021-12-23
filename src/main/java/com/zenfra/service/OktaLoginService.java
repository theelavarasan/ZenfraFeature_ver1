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

	public JSONObject saveData(OktaLoginModel oktaLoginModel) {
		System.out.println("----------oktaLoginModel----------" + oktaLoginModel.getDefaultPolicyName());
		JSONObject result = new JSONObject();
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(oktaLoginModel);
		try {
			OktaLoginModel res = oktaLoginRepository.findById(oktaLoginModel.getId()).orElse(null);
			System.out.println(res);
			if (res == null) {
				oktaLoginModel.setActive(true);
				res = oktaLoginRepository.save(oktaLoginModel);
				result.put("data", res);

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
//			resObject.put("id", res.getId());
//			resObject.put("publisherUrl", res.getPublisherUrl());
//			resObject.put("clientId", res.getClientId());
//			resObject.put("defaultSiteName", res.getDefaultSiteName());
//			resObject.put("defaultPolicy", res.getDefaultPolicy());

			resObject.put("data", res);
			return resObject;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return resObject;
		}

	}

	public JSONObject updateData(OktaLoginModel oktaLoginModel) {
		JSONObject resObject = new JSONObject();

		try {
			OktaLoginModel existingData = oktaLoginRepository.findById(oktaLoginModel.getId()).orElse(null);
			existingData.setClientId(oktaLoginModel.getClientId());
			existingData.setPublisherUrl(oktaLoginModel.getPublisherUrl());
			existingData.setDefaultSiteName(oktaLoginModel.getDefaultSiteName());
			existingData.setDefaultPolicy(oktaLoginModel.getDefaultPolicy());
			existingData.setActive(oktaLoginModel.isActive());
			oktaLoginRepository.save(existingData);
			
			resObject.put("data",existingData);
			
			return resObject;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return resObject;
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
