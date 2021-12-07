package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.EolAndEosHardwareRepository;
import com.zenfra.model.EolAndEosHardwareModel;

@Service
public class EolAndEosHardwareService {

	@Autowired
	private EolAndEosHardwareRepository eolAndEosHardwareRepository;

	public EolAndEosHardwareModel saveData(EolAndEosHardwareModel model) {
		try {

			return eolAndEosHardwareRepository.save(model);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return model;

	}

	public EolAndEosHardwareModel getData(String vendor) {
		return eolAndEosHardwareRepository.findById(vendor).orElse(null);

	}

	public EolAndEosHardwareModel update(EolAndEosHardwareModel model) {

		EolAndEosHardwareModel existing = eolAndEosHardwareRepository.findById(model.getVendor()).orElse(null);
		existing.setVendor(model.getVendor());
		existing.setModel(model.getModel());
		existing.setEndOfLifeCycle(model.getEndOfLifeCycle());
		existing.setEndOfExtendedSupport(model.getEndOfExtendedSupport());
		existing.setSourceLink(model.getSourceLink());
		existing.setEolEosHwId(model.getEolEosHwId());
		existing.setUserId(model.getUserId());

		return eolAndEosHardwareRepository.save(existing);

	}

}
