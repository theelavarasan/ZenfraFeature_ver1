package com.zenfra.ftp.repo;

import java.util.List;

import javax.persistence.NamedNativeQueries;
import javax.validation.constraints.NotEmpty;

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
	
	@Query(value="select * from log_file_details where is_active=:isActive and  site_key=:siteKey order by to_timestamp(updated_date_time, 'yyyy-mm-dd hh24:mi:ss') desc",nativeQuery = true)
	List<LogFileDetails> getBySiteKeyAndIsActive(@Param("siteKey") String siteKey,@Param("isActive") boolean isActive);
	
	@Modifying
	@Query("update LogFileDetails s set s.description=:description,s.logType=:logType  where s.logFileId in :logFileId")
	void saveLogtypeAndDescription(@Param("logFileId") List<String> logFileId,@Param("description") String description,@Param("logType") String logType);

	@Modifying
	@Query("update LogFileDetails s set s.isActive=false  where s.logFileId in :logFileIds")
	void updateLogFileIdsActive(List<String> logFileIds);

	long countBySiteKey(String siteKey);

	@Query(value="select log_type from log_file_details where is_active=:isActive and  site_key=:siteKey and status=:status and lower(log_type) not in ('custom excel data','aws','vmax','tanium')  group by log_type order by log_type" ,nativeQuery = true)
	List<String> getBySiteKeyAndStatusIsActive(@Param("siteKey") String siteKey,@Param("isActive") boolean isActive, @Param("status") String status);

	@Query(value="select distinct((case when log_type ilike 'tanium' then 'Linux'  when lower(log_type) ilike '%mode%' then 'netapp' when lower(log_type) ilike '%netapp%' then 'netapp' else log_type end)) from log_file_details where is_active=:isActive and  site_key=:siteKey and status=:status group by log_type order by log_type" ,nativeQuery = true)
	List<String> getDistinctLogTypeBySiteKeyAndStatusIsActive(@Param("siteKey") String siteKey, @Param("status") String status, @Param("isActive") boolean isActive);

}
