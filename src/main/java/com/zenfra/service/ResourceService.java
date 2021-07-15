package com.zenfra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.Interface.IService;
import com.zenfra.dao.ResourceDao;
import com.zenfra.model.ResourceModel;

@Service
public class ResourceService implements IService<ResourceModel>{

	
	@Autowired
	ResourceDao dao;
	
	@Override
	public ResourceModel findOne(long id) {
		// TODO Auto-generated method stub
		return dao.findOne(id);
	}

	@Override
	public ResourceModel findOne(String id) {
		// TODO Auto-generated method stub
		return dao.findOne(id);
	}

	@Override
	public List<ResourceModel> findAll() {
		// TODO Auto-generated method stub
		return dao.findAll();
	}

	@Override
	public ResourceModel save(ResourceModel entity) {
		// TODO Auto-generated method stub
		return dao.save(entity);
	}

	@Override
	public ResourceModel update(ResourceModel entity) {
		// TODO Auto-generated method stub
		return dao.update(entity);
	}

	@Override
	public void delete(ResourceModel entity) {
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
