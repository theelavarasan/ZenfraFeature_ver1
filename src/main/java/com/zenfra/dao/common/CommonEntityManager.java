package com.zenfra.dao.common;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public abstract class CommonEntityManager extends JdbcCommonOperations {

	@PersistenceContext
	EntityManager entityManager;

	public Object findEntityById(Class c, String id) {
		Object obj = new Object();
		try {
			obj = entityManager.find(c, id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	

	public Boolean saveEntity(Class c, Object obj) {

		try {
			
			entityManager.persist(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Boolean updateEntity(Class c, Object obj) {

		try {
			
			entityManager.merge(obj);
		} catch (Exception e) {
			e.printStackTrace();
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
		}
		return obj;
	}
	
	public boolean deleteByEntity(Object obj) {

		try {
			entityManager.remove(obj);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}
	
	public Boolean eveitEntity(Object obj) {

		try {
			
			entityManager.detach(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
