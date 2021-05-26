package com.zenfra.ftp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zenfra.model.ftp.FtpServer;

@Repository
public interface FtbRepo extends JpaRepository<FtpServer, Long>{

	
	
	@Query("select s from FtpServer s where s.siteKey=:siteKey and s.connectionName=:connectionName")
	FtpServer findBySiteKey(@Param("siteKey") String siteKey,@Param("connectionName") String connectionName);

	@Query("select s from FtpServer s where s.siteKey=:siteKey")
	List<FtpServer> findConnectionsBySiteKey(@Param("siteKey") String siteKey);

	@Query("delete from FtpServer f where f.connectionName=:connectionName and f.siteKey=:siteKey")
	void deleteConnection(@Param("connectionName") String connectionName, @Param("siteKey")String siteKey);

	

	
	

}
