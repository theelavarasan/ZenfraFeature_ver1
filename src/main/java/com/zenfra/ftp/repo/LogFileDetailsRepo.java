package com.zenfra.ftp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.LogFileDetails;

@Repository
@Transactional
public interface LogFileDetailsRepo extends JpaRepository<LogFileDetails, String>{
	
	List<LogFileDetails> findByLogIdIn(@Param("logId") List<String> logid);

	List<LogFileDetails> findBySiteKey(String siteKey);

	List<LogFileDetails> findBySiteKeyAndIsActive(String siteKey, boolean isActive);
	
	@Modifying
	@Query("update LogFileDetails s set s.description=:description,s.logType=:logType  where s.logFileId in :logFileId")
	void saveLogtypeAndDescription(@Param("logFileId") List<String> logFileId,@Param("description") String description,@Param("logType") String logType);

}
