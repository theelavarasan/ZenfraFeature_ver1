package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.EolAndEosHardwareIdentityModel;
import com.zenfra.model.EolAndEosHardwareModel;

@Repository
public interface EolAndEosHardwareRepository extends JpaRepository<EolAndEosHardwareModel, EolAndEosHardwareIdentityModel>{

}
