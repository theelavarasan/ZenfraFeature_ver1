package com.zenfra.ftp.repo;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zenfra.model.ftp.FTPServerModel;

@Repository
public interface FTPServerRepo extends JpaRepository<FTPServerModel, String> {

	@Transactional
	@Modifying
	@Query("select s from FTPServerModel s where s.isActive=true and s.ftpName=:ftpName and s.siteKey=:siteKey")
	List<FTPServerModel> checkName(@Param("ftpName") String ftpName, @Param("siteKey") String siteKey);

	@Transactional
	@Modifying
	@Query("select s from FTPServerModel s where s.siteKey=:siteKey and s.isNas=false")
	List<FTPServerModel> findConnectionsBySiteKey(@Param("siteKey") String siteKey);

	@Transactional
	@Modifying
	@Query("select s from FTPServerModel s where s.siteKey=:siteKey and s.isNas=:isNas")
	List<FTPServerModel> findNasBySiteKeyAndIsNas(@Param("siteKey") String siteKey, @Param("isNas") boolean isNas);

	@Transactional
	@Query("select s from FTPServerModel s where s.siteKey=:siteKey and s.ftpName=:ftpName")
	FTPServerModel findBySiteKey(@Param("siteKey") String siteKey, @Param("ftpName") String ftpName);

	@Transactional
	@Query("select s from FTPServerModel s where s.serverId=:serverId")
	FTPServerModel findByserverId(String serverId);

}
