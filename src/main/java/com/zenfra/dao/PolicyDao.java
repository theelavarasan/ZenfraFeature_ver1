package com.zenfra.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.Interface.IDao;
import com.zenfra.Interface.IGenericDao;
import com.zenfra.model.PolicyModel;

@Component
public class PolicyDao implements IDao<PolicyModel>{

	
	IGenericDao<PolicyModel> dao;
	
	@Autowired
	public void setDao(IGenericDao<PolicyModel> daoToSet) {
		dao = daoToSet;
		dao.setClazz(PolicyModel.class);
	}
	
	
	@Override
	public PolicyModel findOne(long id) {
		try {
			
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public PolicyModel findOne(String id) {
		try {
			
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<PolicyModel> findAll() {
		try {			
			return dao.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public PolicyModel save(PolicyModel entity) {
		try {			
			return dao.save(entity);
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public PolicyModel update(PolicyModel entity) {
		try {
			
			return dao.update(entity);
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public void delete(PolicyModel entity) {
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
