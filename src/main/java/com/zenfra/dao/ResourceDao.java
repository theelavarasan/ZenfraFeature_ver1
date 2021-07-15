package com.zenfra.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.Interface.IDao;
import com.zenfra.Interface.IGenericDao;
import com.zenfra.model.ResourceModel;

@Component
public class ResourceDao implements IDao<ResourceModel>{

	
	IGenericDao<ResourceModel> dao;
	
	@Autowired
	public void setDao(IGenericDao<ResourceModel> daoToSet) {
		dao = daoToSet;
		dao.setClazz(ResourceModel.class);
	}
	
	
	@Override
	public ResourceModel findOne(long id) {
		try {
			
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ResourceModel findOne(String id) {
		try {
			
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<ResourceModel> findAll() {
		try {			
			return dao.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ResourceModel save(ResourceModel entity) {
		try {			
			return dao.save(entity);
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public ResourceModel update(ResourceModel entity) {
		try {
			
			return dao.update(entity);
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public void delete(ResourceModel entity) {
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


