package com.zenfra.ftp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
