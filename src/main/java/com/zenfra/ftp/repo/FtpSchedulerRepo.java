package com.zenfra.ftp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.ftp.FtpScheduler;

@Repository
public interface FtpSchedulerRepo extends JpaRepository<FtpScheduler, Long>{

	
	@Query("select f from FtpScheduler f where f.fileNameSettingsId=:fileNameSettingsId")
	FtpScheduler findAllById(@Param("fileNameSettingsId")String fileNameSettingsId);
	
	@Modifying
	 @Transactional
	@Query("delete from FtpScheduler f where f.fileNameSettingsId in :fileNameSettingsId")
	void deleteFtpSchedulerByFileNameSettingsId(@Param("fileNameSettingsId")List<String> fileNameSettingsId);
	

}
