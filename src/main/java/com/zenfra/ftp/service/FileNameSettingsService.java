package com.zenfra.ftp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.ftp.repo.FileNameSettingsRepo;
import com.zenfra.model.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettings;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileUploadStatus;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.model.ftp.FtpServer;

@Service
public class FileNameSettingsService {

	@Autowired
	FileNameSettingsRepo repo;

	@Autowired
	FTPClientService clientService;

	public String saveFileNameSettings(FileNameSettingsModel settings) {
		try {

			repo.save(settings);
			return "Saved!";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public FileNameSettingsModel getsaveFileNameSettings(String siteKey, String connectionName) {

		try {
			return repo.getsaveFileNameSettings(siteKey, connectionName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FileNameSettingsModel> getsaveFileNameSettingsList(String siteKey, String connectionName) {

		try {
			return repo.getsaveFileNameSettingsList(siteKey, connectionName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public FileNameSettingsModel getFileNameSettingsById(String id) {

		try {
			return repo.findByid(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public List<FileWithPath> getFilesByPattern(String siteKey, String connectionName,String userId) {

		List<FileWithPath> filesFillter = new ArrayList<FileWithPath>();
		try {

			List<FileNameSettingsModel>  settings= getsaveFileNameSettingsList(siteKey, connectionName);
			FTPServerModel server = clientService.getFtpConnectionBySiteKey(siteKey, connectionName);

			List<FileWithPath> files = clientService.getFiles(siteKey, server.getServerPath(), connectionName);

			for (FileWithPath f : files) {

			for(FileNameSettingsModel s:settings) {
				
				 String patternVal = null;
				 String logType = null;
				 for(int j=0; j < s.getPattern().size();j++) {
					 JSONObject patJson =  (JSONObject) s.getPattern().get(j);
					 patternVal = patJson.get("namePattern").toString();
					 logType = patJson.get("logType").toString();
				 }
				
				 if ( Pattern.matches(patternVal,f.getName()) || Pattern.matches(logType, f.getName()) ) {
						System.out.println("Find Match");
						f.setLogType(logType);
						filesFillter.add(f);
					}
			}
			}

			// clientService.getFilesdFromServerPattern(server, settings, files);
			return filesFillter;
		} catch (Exception e) {
			e.printStackTrace();
			return filesFillter;
		}
	}

	
	
	public List<FileWithPath> getFilesByPattern(FileNameSettingsModel settings) {

		List<FileWithPath> filesFillter = new ArrayList<FileWithPath>();
		try {

			FTPServerModel server = clientService.getFtpConnectionBySiteKey(settings.getSiteKey(), settings.getFtpName());

			List<FileWithPath> files = clientService.getFiles(settings.getSiteKey(), server.getServerPath(), settings.getFtpName());

			for (FileWithPath f : files) {

				 String patternVal = null;
				 String logType = null;
				 for(int j=0; j < settings.getPattern().size();j++) {
					 JSONObject patJson =  (JSONObject) settings.getPattern().get(j);
					 patternVal = patJson.get("namePattern").toString();
					 logType = patJson.get("logType").toString();
				 }
				
				 if ( Pattern.matches(patternVal,f.getName()) || Pattern.matches(logType, f.getName()) ) {
						System.out.println("Find Match");
						f.setLogType(logType);
						filesFillter.add(f);
					}
			
			}

			// clientService.getFilesdFromServerPattern(server, settings, files);
			return filesFillter;
		} catch (Exception e) {
			e.printStackTrace();
			return filesFillter;
		}
	}

	
	public List<FileUploadStatus> moveFilesByPattern(String siteKey, String connectionName,
			List<FileWithPath> filesFillter) {

		try {
			List<FileUploadStatus>  status=new ArrayList<FileUploadStatus>();
			
			List<FileNameSettingsModel> settings = getsaveFileNameSettingsList(siteKey, connectionName);
			FTPServerModel server = clientService.getFtpConnectionBySiteKey(siteKey, connectionName);

			for(FileNameSettingsModel s:settings) {
				status.addAll(clientService.getFilesdFromServerPattern(server, s, filesFillter));
			}
			 return status;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean deleteFileNameSettings(String siteKey, String connectionName) {
		try {
			repo.deletesaveFileNameSettings(siteKey, connectionName);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public List<FileNameSettingsModel> getFileNameSettingsByFtpName(String serverUsername) {
		List<FileNameSettingsModel> list=new ArrayList<FileNameSettingsModel>();
		try {
			
			list=repo.getsaveFileNameSettingsByFtpName(serverUsername);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list;
	}

	

	
	
}
