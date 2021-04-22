package com.zenfra.dao;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.CategoryView;

@Component
@Transactional
public abstract class CommonEntityManager{

	@PersistenceContext
	EntityManager entityManager;

	public Object findById(Class c, String id) {
		try {

			return entityManager.find(c, id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void save(Class c, Object obj) {

		try {

			// entityManager.refresh(c);
			 entityManager.persist(obj);
		} catch (Exception e) {
			e.printStackTrace();		
		}
	}
}
