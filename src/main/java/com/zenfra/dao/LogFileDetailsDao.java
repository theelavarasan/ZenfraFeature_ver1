package com.zenfra.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.Interface.IDao;
import com.zenfra.Interface.IGenericDao;
import com.zenfra.model.LogFileDetails;

@Component
public class LogFileDetailsDao implements IDao<LogFileDetails>{

	
	IGenericDao<LogFileDetails> dao;
	
	@Autowired
	public void setDao(IGenericDao<LogFileDetails> daoToSet) {
		dao = daoToSet;
		dao.setClazz(LogFileDetails.class);
	}
	
	@Override
	public LogFileDetails findOne(long id) {
		try {			
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<LogFileDetails> findAll() {
		try {			
			return dao.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public LogFileDetails save(LogFileDetails entity) {
		try {			
			return dao.save(entity);					
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public LogFileDetails update(LogFileDetails entity) {
		try {			
			return dao.update(entity);					
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public void delete(LogFileDetails entity) {
		try {
			dao.delete(entity);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void deleteById(long entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public LogFileDetails findOne(String id) {
		// TODO Auto-generated method stub
		return dao.findOne(id);
	}

	@Override
	public void deleteById(String entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	
	
}
