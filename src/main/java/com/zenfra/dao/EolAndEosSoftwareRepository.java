package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.EolAndEosSoftwareIdentityModel;
import com.zenfra.model.EolAndEosSoftwareModel;

@Repository
public interface EolAndEosSoftwareRepository extends JpaRepository<EolAndEosSoftwareModel, EolAndEosSoftwareIdentityModel>{

}
