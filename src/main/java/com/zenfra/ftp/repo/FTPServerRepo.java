package com.zenfra.ftp.repo;

import java.util.List;

import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zenfra.model.FTPServerModel;
import com.zenfra.model.ftp.FtpServer;

@Repository
public interface FTPServerRepo extends JpaRepository<FTPServerModel, String>{
	
	
	@Query("select * from FTPServerModel where isActive=true and ftpName=:ftpname")
	List<FTPServerModel> checkName(@Param("ftpName") String ftpName);
	
	@Query("select s from FTPServerModel s where s.siteKey=:siteKey")
	List<FTPServerModel> findConnectionsBySiteKey(@Param("siteKey") String siteKey);

	@Query("select s from FTPServerModel s where s.siteKey=:siteKey and s.ftpName=:ftpName")
	FTPServerModel findBySiteKey(@Param("siteKey") String siteKey,@Param("ftpName") String ftpName);


}
