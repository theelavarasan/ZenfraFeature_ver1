package com.zenfra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.Interface.IService;
import com.zenfra.dao.SiteDao;
import com.zenfra.model.SiteModel;
import com.zenfra.model.SiteModel;

@Service
public class SiteService implements IService<SiteModel>{

	
	@Autowired
	SiteDao dao;
	
	@Override
	public SiteModel findOne(long id) {	
	 try {
		return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

	@Override
	public SiteModel findOne(String id) {
		try {
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

	@Override
	public List<SiteModel> findAll() {
		// TODO Auto-generated method stub
		try {
			return dao.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

	@Override
	public SiteModel save(SiteModel entity) {
		// TODO Auto-generated method stub
		try {
			return dao.save(entity);
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
		
	}

	@Override
	public SiteModel update(SiteModel entity) {
		// TODO Auto-generated method stub
		try {
			return dao.update(entity);
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
		
	}

	@Override
	public void delete(SiteModel entity) {
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
	public void deleteById(String entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
