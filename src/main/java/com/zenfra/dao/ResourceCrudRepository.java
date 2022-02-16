package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.ResourceCrudModel;

@Repository
public interface ResourceCrudRepository extends JpaRepository<ResourceCrudModel, String>{
	

}
