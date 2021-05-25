package com.zenfra.ftp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zenfra.model.ftp.FileNameSettings;
import com.zenfra.model.ftp.FileNameSettingsModel;

@Repository
public interface FileNameSettingsRepo extends JpaRepository<FileNameSettingsModel, String>{

	@Query("select s from FileNameSettingsModel s where s.siteKey=:siteKey and s.ftpName=:ftpName")
	FileNameSettingsModel getsaveFileNameSettings(@Param("siteKey")String siteKey,@Param("ftpName") String ftpName);

	@Query("delete from FileNameSettings s where s.siteKey=:siteKey and s.connectionName=:connectionName")
	void deletesaveFileNameSettings(@Param("siteKey")String siteKey,@Param("connectionName") String connectionName);

	@Query("select s from FileNameSettings s where s.ftpName=:ftpName")
	List<FileNameSettingsModel> getsaveFileNameSettingsByFtpName(@Param("ftpName") String ftpName);

	@Query("select s from FileNameSettingsModel s where s.siteKey=:siteKey and s.ftpName=:ftpName")
	List<FileNameSettingsModel> getsaveFileNameSettingsList(String siteKey, String connectionName);

	@Query("select s from FileNameSettingsModel s where s.fileNameSettingId=:id")
	FileNameSettingsModel findByid(String id);

}
