package com.zenfra.service;

import java.util.List;

import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.zenfra.Interface.IService;
import com.zenfra.dao.LogModelsDao;
import com.zenfra.model.LogModels;

@Service
public class LogModelsService implements IService<LogModels>{

	@Autowired
	LogModelsDao dao;
	
	
	@Override
	public LogModels findOne(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LogModels findOne(String id) {
		// TODO Auto-generated method stub
		return dao.findOne(id);
	}

	@Override
	public List<LogModels> findAll() {
		// TODO Auto-generated method stub
		return dao.findAll();
	}

	@Override
	public LogModels save(LogModels entity) {
		// TODO Auto-generated method stub
		return dao.save(entity);
	}

	@Override
	public LogModels update(LogModels entity) {
		// TODO Auto-generated method stub
		return dao.update(entity);
	}

	@Override
	public void delete(LogModels entity) {
		// TODO Auto-generated method stub
		dao.delete(entity);
	}

	@Override
	public void deleteById(long entityId) {
		// TODO Auto-generated method stub
		dao.deleteById(entityId);
	}

	@Override
	public void deleteById(String entityId) {
		// TODO Auto-generated method stub
		dao.deleteById(entityId);
	}

	public List<LogModels> findByActive() {
		// TODO Auto-generated method stub
		return dao.findByActive();
	}
	
	
	

}
