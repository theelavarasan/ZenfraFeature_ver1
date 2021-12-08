package com.zenfra.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.OktaLoginRepository;
import com.zenfra.model.OktaLoginModel;

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

	public String getData(OktaLoginModel OktaLoginModel) {

		try {
			OktaLoginRepository.findById(OktaLoginModel.getId()).orElse(null);
			return "Success";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "Failure";
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

	public String deleteData(OktaLoginModel OktaLoginModel) {

		try {
			OktaLoginRepository.deleteById(OktaLoginModel.getId());
			return "Success";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "Failure";
		}
	}
}
