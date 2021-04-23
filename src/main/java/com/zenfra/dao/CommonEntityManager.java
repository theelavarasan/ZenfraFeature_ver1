package com.zenfra.dao;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.catalina.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.CategoryView;

@Component
@Transactional
public abstract class CommonEntityManager {

	@PersistenceContext
	EntityManager entityManager;

	public Object findEntityById(Class c, String id) {
		Object obj=new Object();
		try {
			obj=entityManager.find(c, id);
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
	
	public Object getEntityByColumn(String query,Class c) {		
		
		Object obj=new Object();
		try {				
			obj=entityManager.createNativeQuery(query,c).getSingleResult();
			System.out.println(obj);
		} catch (Exception e) {
			e.printStackTrace();			
		}
		return obj;
	}
	
	
	
}
