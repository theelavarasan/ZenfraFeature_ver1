package com.zenfra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.Interface.IService;
import com.zenfra.dao.LogFileDetailsDao;
import com.zenfra.model.LogFileDetails;

@Service
public class LogFileDetailsService implements IService<LogFileDetails>{

	@Autowired
	LogFileDetailsDao logDao;
	
	@Override
	public LogFileDetails findOne(long id) {
		// TODO Auto-generated method stub
		return logDao.findOne(id);
	}

	@Override
	public List<LogFileDetails> findAll() {
		return logDao.findAll();
	}

	@Override
	public LogFileDetails save(LogFileDetails entity) {
		// TODO Auto-generated method stub
		return logDao.save(entity);
	}

	@Override
	public LogFileDetails update(LogFileDetails entity) {
		// TODO Auto-generated method stub
		return logDao.update(entity);
	}

	@Override
	public void delete(LogFileDetails entity) {
		logDao.delete(entity);
		
	}

	@Override
	public void deleteById(long entityId) {
		logDao.deleteById(entityId);
		
	}

	@Override
	public void deleteById(String entityId) {
		logDao.deleteById(entityId);
		
	}
	
	@Override
	public LogFileDetails findOne(String id) {
		// TODO Auto-generated method stub
		return logDao.findOne(id);
	}
	
	

}
