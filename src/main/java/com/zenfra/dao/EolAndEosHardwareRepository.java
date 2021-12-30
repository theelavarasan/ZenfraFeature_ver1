package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zenfra.model.EolAndEosHardwareIdentityModel;
import com.zenfra.model.EolAndEosHardwareModel;

@Repository
public interface EolAndEosHardwareRepository extends JpaRepository<EolAndEosHardwareModel, EolAndEosHardwareIdentityModel>{

	@Query("select e from EolAndEosHardwareModel e where e.eol_eos_hw_id=:eol_eos_hw_id")
	EolAndEosHardwareModel findByHwId(@Param("eol_eos_hw_id") String eol_eos_hw_id);

}
