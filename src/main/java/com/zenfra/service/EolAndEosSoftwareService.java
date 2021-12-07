package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.EolAndEosSoftwareRepository;
import com.zenfra.model.EolAndEosSoftwareIdentityModel;
import com.zenfra.model.EolAndEosSoftwareModel;

@Service
public class EolAndEosSoftwareService {

	@Autowired
	private EolAndEosSoftwareRepository eolAndEosSoftwareRepository;

	public String saveData(EolAndEosSoftwareModel model) {
		try {
			model.setEolAndEosSoftwareIdentityModel(
					new EolAndEosSoftwareIdentityModel(model.getOsName(), model.getOsVersion()));
			eolAndEosSoftwareRepository.save(model);
			return "Success";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Error";

	}

	public String update(EolAndEosSoftwareModel model) {
		try {
			EolAndEosSoftwareModel existing = eolAndEosSoftwareRepository.findById(new EolAndEosSoftwareIdentityModel(model.getOsName(), model.getOsVersion())).orElse(null);
			existing.setSourceUrl(model.getSourceUrl());
			existing.setEndOfLifeCycle(model.getEndOfLifeCycle());
			existing.setOsType(model.getOsType());
			existing.setUserId(model.getUserId());
			existing.setOsName(model.getOsName());
			existing.setEndOfExtendedSupport(model.getEndOfExtendedSupport());
			existing.setActive(model.isActive());

			eolAndEosSoftwareRepository.save(existing);
			return "Success";

		} catch (Exception e) {
			// TODO: handle exception
			return "Error";
		}

	}

}
