package com.zenfra.dao;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.CategoryView;

@Component
@Transactional
public abstract class CommonEntityManager {

	@PersistenceContext
	EntityManager entityManager;

	public Object findEntityById(Class c, String id) {
		try {
			return entityManager.find(c, id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Boolean saveEntity(Class c, Object obj) {

		try {

			entityManager.persist(obj);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
