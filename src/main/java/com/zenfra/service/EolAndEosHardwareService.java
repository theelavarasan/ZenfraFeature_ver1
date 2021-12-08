package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.EolAndEosHardwareRepository;
import com.zenfra.model.EolAndEosHardwareIdentityModel;
import com.zenfra.model.EolAndEosHardwareModel;

@Service
public class EolAndEosHardwareService {

	@Autowired
	private EolAndEosHardwareRepository eolAndEosHardwareRepository;

	public String saveData(EolAndEosHardwareModel model) {
		try {
			model.setEolAndEosHardwareIdentityModel(
					new EolAndEosHardwareIdentityModel(model.getVendor(), model.getModel()));
			eolAndEosHardwareRepository.save(model);
			return "Success";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Error";

	}

	public String update(EolAndEosHardwareModel model) {
		try {
			EolAndEosHardwareModel existing = eolAndEosHardwareRepository
					.findById(new EolAndEosHardwareIdentityModel(model.getVendor(), model.getModel())).orElse(null);
			existing.setEndOfLifeCycle(model.getEndOfLifeCycle());
			existing.setEndOfExtendedSupport(model.getEndOfExtendedSupport());
			existing.setSourceLink(model.getSourceLink());
			existing.setEolEosHwId(model.getEolEosHwId());
			existing.setUserId(model.getUserId());
			existing.setActive(model.isActive());

			eolAndEosHardwareRepository.save(existing);

			return "Sucess";

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return "Error";

	}

}
