package com.zenfra.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.Interface.IDao;
import com.zenfra.Interface.IGenericDao;
import com.zenfra.ftp.repo.LogModelsRepo;
import com.zenfra.model.LogModels;

@Component
public class LogModelsDao implements IDao<LogModels>{

	@Autowired
	LogModelsRepo modelRepo;

	IGenericDao<LogModels> dao;
	@Autowired
	public void setDao(IGenericDao<LogModels> daoToSet) {
		dao = daoToSet;
		dao.setClazz(LogModels.class);
	}
	
	@Override
	public LogModels findOne(long id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public LogModels findOne(String id) {
		// TODO Auto-generated method stub
		return  modelRepo.findById(id).orElse(null);
	}

	@Override
	public List<LogModels> findAll() {
		// TODO Auto-generated method stub
		return modelRepo.findAll();
	}

	@Override
	public LogModels save(LogModels entity) {
		// TODO Auto-generated method stub
		return modelRepo.save(entity);
	}

	@Override
	public LogModels update(LogModels entity) {
		// TODO Auto-generated method stub
		return modelRepo.save(entity);
	}

	@Override
	public void delete(LogModels entity) {
		// TODO Auto-generated method stub
		modelRepo.delete(entity);
	}

	@Override
	public void deleteById(long entityId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteById(String entityId) {
		// TODO Auto-generated method stub
		modelRepo.deleteById(entityId);
	}

	public List<LogModels> findByActive() {
		
		return modelRepo.findByActive(true);
	}
	
	
	
	
	
	
}
