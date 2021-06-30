package com.zenfra.Interface;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;

public interface IGenericDao<T extends Serializable> {

	T findOne(final long id);

	T findOne(final String id);

	List<T> findAll();

	T save(T entity);

	T update(final T entity);

	void delete(final T entity);
	

	void deleteById(final long entityId);
	void deleteById(final String entityId);

	void setClazz(Class<T> class1);

	EntityManager getEntityManger();
}
