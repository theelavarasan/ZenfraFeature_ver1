package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.EolAndEosSoftwareRepository;
import com.zenfra.model.EolAndEosSoftwareModel;

@Service
public class EolAndEosSoftwareService {

	@Autowired
	private EolAndEosSoftwareRepository eolAndEosSoftwareRepository;

	public EolAndEosSoftwareModel saveData(EolAndEosSoftwareModel model) {
		try {

			return eolAndEosSoftwareRepository.save(model);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return model;

	}

	public EolAndEosSoftwareModel getData(String osVersion) {
		return eolAndEosSoftwareRepository.findById(osVersion).orElse(null);

	}

	public EolAndEosSoftwareModel update(EolAndEosSoftwareModel model) {

		EolAndEosSoftwareModel existing = eolAndEosSoftwareRepository.findById(model.getOsVersion()).orElse(null);
		existing.setSourceUrl(model.getSourceUrl());
		existing.setEndOfLifeCycle(model.getEndOfLifeCycle());
		existing.setOsType(model.getOsType());
		existing.setUserId(model.getUserId());
		existing.setOsName(model.getOsName());
		existing.setEndOfExtendedSupport(model.getEndOfExtendedSupport());

		return eolAndEosSoftwareRepository.save(existing);

	}

}
