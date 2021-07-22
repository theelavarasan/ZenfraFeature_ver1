package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.HealthCheckDao;
import com.zenfra.model.HealthCheck;
import com.zenfra.utils.CommonFunctions;

@Service
public class HealthCheckService {
	
	@Autowired
	HealthCheckDao healthCheckDao;

	@Autowired
	CommonFunctions commonFunctions;
	
	public HealthCheck saveHealthCheck(HealthCheck healthCheck) {
		healthCheck.setHealthCheckId(commonFunctions.generateRandomId());
		healthCheckDao.saveEntity(HealthCheck.class, healthCheck);
		return (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
	}

	public HealthCheck getHealthCheck(HealthCheck healthCheck) {
		// TODO Auto-generated method stub
		return (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
	}

	public HealthCheck updateHealthCheck(HealthCheck healthCheck) {
		healthCheckDao.updateEntity(HealthCheck.class, healthCheck);
		return (HealthCheck) healthCheckDao.findEntityById(HealthCheck.class, healthCheck.getHealthCheckId());
	}

	public boolean deleteHealthCheck(HealthCheck healthCheck) {
		// TODO Auto-generated method stub
		return healthCheckDao.deleteByEntity(healthCheck);
	}

}
