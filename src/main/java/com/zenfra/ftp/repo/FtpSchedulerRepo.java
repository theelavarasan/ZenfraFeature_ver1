package com.zenfra.ftp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zenfra.model.ftp.FtpScheduler;

@Repository
public interface FtpSchedulerRepo extends JpaRepository<FtpScheduler, Long>{

	
	@Query("select f from FtpScheduler f where f.id=:id")
	FtpScheduler findAllById(@Param("id")Long id);
	
	

}
