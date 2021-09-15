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
	
	List<LogFileDetails> findByLogFileIdIn(@Param("logId") List<String> logid);

	List<LogFileDetails> findBySiteKey(String siteKey);

	//@Query("select l from LogFileDetails l where order by cast(updatedDateTime as Date) DESC")
	List<LogFileDetails> findBySiteKeyAndIsActive(String siteKey, boolean isActive);
	
	@Modifying
	@Query("update LogFileDetails s set s.description=:description,s.logType=:logType  where s.logFileId in :logFileId")
	void saveLogtypeAndDescription(@Param("logFileId") List<String> logFileId,@Param("description") String description,@Param("logType") String logType);

	@Modifying
	@Query("update LogFileDetails s set s.isActive=false  where s.logFileId in :logFileIds")
	void updateLogFileIdsActive(List<String> logFileIds);

}
