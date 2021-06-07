package com.zenfra.ftp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.AESEncryptionDecryption;
import com.zenfra.ftp.repo.FileNameSettingsRepo;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileUploadStatus;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.utils.CommonFunctions;


@Service
public class FileNameSettingsService {

	@Autowired
	FileNameSettingsRepo repo;

	@Autowired
	FTPClientService clientService;
	
	@Autowired
	AESEncryptionDecryption encryption;

	@Autowired
	CommonFunctions functions;
	
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

	
	
	public List<FileWithPath> getFilesByPattern(FTPServerModel server,FileNameSettingsModel settings) {

		List<FileWithPath> filesFillter = new ArrayList<FileWithPath>();
		try {
			
			List<FileWithPath> files = clientService.getFiles(settings.getSiteKey(), server.getServerPath(), settings.getFtpName());
			String toPath=functions.getDate();
			ObjectMapper map=new ObjectMapper();
			for (FileWithPath f : files) {
			
				 String patternVal = null;
				 String logType = null;		
				 for(int j=0; j < settings.getPattern().size();j++) {
					 
					 JSONObject patJson = map.convertValue(settings.getPattern().get(j), JSONObject.class);
					 patternVal =patJson.get("namePattern").toString().replace("*", ".*");
					 logType = patJson.get("logType").toString().replace("*", ".*");
					 System.out.println("patternVal::"+patternVal);
					 System.out.println("logType::"+logType);
					 //patternVal=".*sun.*";logType="";
					if (isValidMatch(patternVal,f.getName()) || isValidMatch(logType, f.getName()) ) {
					// if (f.getName().contains(patternVal) || f.getName().contains(logType) ) {
							System.out.println("Find Match");
							f.setLogType(logType);
							System.out.println("Path::check::"+settings.getToPath()+"/"+logType+"_"+toPath);
							f.setPath(settings.getToPath()+"/"+logType+"_"+toPath);
							filesFillter.add(f);
							
					}
				
				 }
				
		
			}
			if(filesFillter.size()>0) {
				clientService.getFilesdFromServerPattern(server, settings, filesFillter);
			}
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

	public List<FileNameSettingsModel> getFileNameSettingsByFtpName(String siteKey,String ftpName) {
		List<FileNameSettingsModel> list=new ArrayList<FileNameSettingsModel>();
		try {
			List<FileNameSettingsModel> temp=list=repo.getsaveFileNameSettingsByFtpName(siteKey,ftpName);
				 for(FileNameSettingsModel t:temp) {
					 t.setToPath("");
					 list.add(t);
				 }
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list;
	}

	static boolean isValidMatch(String patternRegex, String content) {
	    boolean isMatched = false;
	    try {
	        Pattern pattern = Pattern.compile(patternRegex, Pattern.CASE_INSENSITIVE);
	        Matcher matcher = pattern.matcher(content);
	        return matcher.find();              
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    return isMatched;
	            
	}

	
}

	
	

