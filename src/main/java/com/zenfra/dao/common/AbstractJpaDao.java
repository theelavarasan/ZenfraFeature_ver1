package com.zenfra.dao.common;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

@Component
@Transactional
public abstract class AbstractJpaDao<T extends Serializable> {
	private Class<T> clazz;

	@PersistenceContext
	EntityManager entityManager;

	public void setClazz(Class<T> clazzToSet) {
		this.clazz = clazzToSet;
	}

	public T findOne(final long id) {
		return entityManager.find(clazz, id);
	}
	
	public T findOne(final String id) {
		return entityManager.find(clazz, id);
	}

	public List<T> findAll() {
		return entityManager.createQuery("from " + clazz.getName()).getResultList();
	}

	public T save(T entity) {
		entityManager.persist(entity);
		entityManager.flush();
		return entity;
	}

	public T update(T entity) {
		return entityManager.merge(entity);
	}

	public void delete(T entity) {
		entityManager.remove(entity);
	}

	public void deleteById(final long entityId) {
		T entity = findOne(entityId);
		delete(entity);
	}

	public void deleteById(final String entityId) {
		T entity = findOne(entityId);
		delete(entity);
	}
	
	public T find(T entity) {
		return entityManager.merge(entity);
	}

	public EntityManager getEntityManger() {
		return entityManager;

	}

}