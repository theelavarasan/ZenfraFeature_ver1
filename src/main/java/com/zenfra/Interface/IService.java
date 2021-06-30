package com.zenfra.Interface;

import java.io.Serializable;
import java.util.List;

public interface IService<T extends Serializable> {

	T findOne(final long id);

	T findOne(final String id);

	List<T> findAll();

	T save(T entity);

	T update(final T entity);

	void delete(final T entity);

	void deleteById(final long entityId);
	void deleteById(final String entityId);

}
