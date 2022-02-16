package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.OktaLoginModel;

@Repository
public interface OktaLoginRepository extends JpaRepository<OktaLoginModel, String>  {

}
