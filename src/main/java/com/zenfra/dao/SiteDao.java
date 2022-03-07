package com.zenfra.dao;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.Interface.IDao;
import com.zenfra.Interface.IGenericDao;
import com.zenfra.model.SiteModel;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class SiteDao implements IDao<SiteModel> {

	IGenericDao<SiteModel> dao;

	@Autowired
	public void setDao(IGenericDao<SiteModel> daoToSet) {
		dao = daoToSet;
		dao.setClazz(SiteModel.class);
	}

	@Override
	public SiteModel findOne(long id) {
		try {
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}

	}

	@Override
	public SiteModel findOne(String id) {
		try {
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return entity;
		}

	}

	@Override
	public void delete(SiteModel entity) {
		try {

			dao.delete(entity);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	@Override
	public void deleteById(long entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	@Override
	public void deleteById(String entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
	}

}
