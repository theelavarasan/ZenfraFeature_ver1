package com.zenfra.dao;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import com.zenfra.model.CategoryView;

@Component
 abstract class CommonEntityManager {

	@PersistenceContext
	private EntityManager entityManager;
	
	
	public Object findById(Class c,String id){		
		try {
			return entityManager.find(c, id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
