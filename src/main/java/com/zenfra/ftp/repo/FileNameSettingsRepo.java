package com.zenfra.ftp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.ftp.FileNameSettingsModel;

@Repository
@Transactional
public interface FileNameSettingsRepo extends JpaRepository<FileNameSettingsModel, String>{

	@Query("select s from FileNameSettingsModel s where s.siteKey=:siteKey and s.ftpName=:ftpName")
	FileNameSettingsModel getsaveFileNameSettings(@Param("siteKey")String siteKey,@Param("ftpName") String ftpName);

	@Query("delete from FileNameSettingsModel s where s.siteKey=:siteKey and s.ftpName=:ftpName")
	void deletesaveFileNameSettings(@Param("siteKey")String siteKey,@Param("ftpName") String ftpName);

	
	@Query("select s from FileNameSettingsModel s where s.siteKey=:siteKey and  s.ftpName=:ftpName")
	List<FileNameSettingsModel> getsaveFileNameSettingsByFtpName(@Param("siteKey") String siteKey,@Param("ftpName") String ftpName);

	@Query("select s from FileNameSettingsModel s where s.siteKey=:siteKey and s.ftpName=:ftpName")
	List<FileNameSettingsModel> getsaveFileNameSettingsList(String siteKey, String ftpName);

	@Query("select s from FileNameSettingsModel s where s.fileNameSettingId=:id")
	FileNameSettingsModel findByid(String id);

	@Query("select s from FileNameSettingsModel s where s.ftpName=:ftpName and s.siteKey=:siteKey")
	List<FileNameSettingsModel> getEntityListByColumn(String ftpName,String siteKey);

}
