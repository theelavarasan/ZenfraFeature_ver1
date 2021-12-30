package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zenfra.model.EolAndEosSoftwareIdentityModel;
import com.zenfra.model.EolAndEosSoftwareModel;

@Repository
public interface EolAndEosSoftwareRepository extends JpaRepository<EolAndEosSoftwareModel, EolAndEosSoftwareIdentityModel>{

	@Query("select e from EolAndEosSoftwareModel e where e.eol_eos_sw_id=:eol_eos_sw_id")
	EolAndEosSoftwareModel findBySwId(@Param("eol_eos_sw_id") String eol_eos_sw_id);

	

}
