package com.zenfra.dao.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import com.zenfra.utils.ExceptionHandlerMail;

@Component
public abstract class CommonEntityManager extends JdbcCommonOperations {

	@PersistenceContext
	private EntityManager entityManager;

	public Object findEntityById(Class c, String id) {
		Object obj = new Object();
		try {
			obj = entityManager.find(c, id);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	@Transactional
	public Boolean saveEntity(Class c, Object obj) {

		try {

			entityManager.persist(obj);
			entityManager.flush();
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
		return true;
	}

	@Transactional
	public Boolean updateEntity(Class c, Object obj) {

		try {

			entityManager.merge(obj);
			entityManager.flush();
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
		return true;
	}

	public Object getEntityByColumn(String query, Class c) {

		Object obj = null;
		try {
			obj = entityManager.createNativeQuery(query, c).getSingleResult();

		} catch (NoResultException e) {

		}
		return obj;
	}

	public List<Object> getEntityListByColumn(String query, Class c) {

		List<Object> obj = new ArrayList<Object>();
		try {
			obj = entityManager.createNativeQuery(query, c).getResultList();
			System.out.println(obj);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	@Transactional
	public boolean deleteByEntity(Object obj) {

		try {
			this.entityManager.remove(obj);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}

	}

	public Boolean eveitEntity(Object obj) {

		try {

			entityManager.detach(obj);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
		return true;
	}

}
