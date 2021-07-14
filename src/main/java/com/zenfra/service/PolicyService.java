package com.zenfra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.Interface.IService;
import com.zenfra.dao.PolicyDao;
import com.zenfra.model.PolicyModel;

@Service
public class PolicyService implements IService<PolicyModel>{

	
	@Autowired
	PolicyDao dao;
	
	@Override
	public PolicyModel findOne(long id) {
		// TODO Auto-generated method stub
		return dao.findOne(id);
	}

	@Override
	public PolicyModel findOne(String id) {
		// TODO Auto-generated method stub
		return dao.findOne(id);
	}

	@Override
	public List<PolicyModel> findAll() {
		// TODO Auto-generated method stub
		return dao.findAll();
	}

	@Override
	public PolicyModel save(PolicyModel entity) {
		// TODO Auto-generated method stub
		return dao.save(entity);
	}

	@Override
	public PolicyModel update(PolicyModel entity) {
		// TODO Auto-generated method stub
		return dao.update(entity);
	}

	@Override
	public void delete(PolicyModel entity) {
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

}
