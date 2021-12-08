package com.zenfra.service;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.zenfra.dao.OktaLoginRepository;
import com.zenfra.model.OktaLoginModel;
import com.zenfra.model.ResponseModel_v2;

@Service
public class OktaLoginService {

	@Autowired
	private OktaLoginRepository OktaLoginRepository;

	public String saveData(OktaLoginModel OktaLoginModel) {

		try {
			UUID uuid = UUID.randomUUID();
			OktaLoginModel.setId(uuid.toString());
			OktaLoginRepository.save(OktaLoginModel);
			return "Success";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return "Failure";
	}

	public OktaLoginModel getData(String id) {
		try {
		
			return OktaLoginRepository.findById(id).orElse(null);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}

	}

	public String updateData(OktaLoginModel OktaLoginModel) {

		try {
			OktaLoginModel existingData = OktaLoginRepository.findById(OktaLoginModel.getId()).orElse(null);
			existingData.setClientId(OktaLoginModel.getClientId());
			existingData.setPublisherUrl(OktaLoginModel.getPublisherUrl());
			OktaLoginRepository.save(existingData);
			return "Success";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "Failure";
		}
	}

	public String deleteData(String id) {

		try {
			OktaLoginRepository.deleteById(id);
			return "Success";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "Failure";
		}
	}
}
