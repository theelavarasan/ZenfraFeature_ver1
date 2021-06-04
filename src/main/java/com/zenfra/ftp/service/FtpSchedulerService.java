package com.zenfra.ftp.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.AESEncryptionDecryption;
import com.zenfra.configuration.FTPClientConfiguration;
import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.ftp.repo.FtpSchedulerRepo;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.model.ftp.FtpScheduler;
import com.zenfra.model.ftp.ProcessingStatus;
import com.zenfra.service.ProcessService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Constants;

@Service
public class FtpSchedulerService extends CommonEntityManager{

	@Autowired
	FtpSchedulerRepo repo;

	@Autowired
	FileNameSettingsService settingsService;

	@Autowired
	FTPClientService clientService;
	
	@Autowired
	CommonFunctions functions;
	
	
	@Autowired
	ProcessService process;
	
	
	@Autowired
	AESEncryptionDecryption encryption;
	
	public long saveFtpScheduler(FtpScheduler ftpScheduler) {

		try {

			repo.save(ftpScheduler);
			repo.flush();
			
			System.out.println("----get id-----"+ftpScheduler.getId());
			return ftpScheduler.getId();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public FtpScheduler getFtpScheduler(String fileNameSettingsId) {

		try {

			return repo.findAllById(fileNameSettingsId);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FtpScheduler> getFtpSchedulerAll() {

		try {

			return repo.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Object runFtpSchedulerFiles(FtpScheduler s) {
		ProcessingStatus status=new ProcessingStatus();
		try {
			
				System.out.println("--------------eneter runFtpSchedulerFiles---------"+s.getFileNameSettingsId());
			FileNameSettingsModel settings = settingsService.getFileNameSettingsById(s.getFileNameSettingsId());
			
			FTPServerModel server = clientService.getFtpConnectionBySiteKey(settings.getSiteKey(), settings.getFtpName());
				
				status.setDataId(String.valueOf(server.getServerId()));
				status.setStartTime(functions.getCurrentDateWithTime());
				status.setId(functions.generateRandomId());
				status.setStatus("Processing");
			process.saveProcess(status);	
			
				
			
			List<FileWithPath> files=getFilesBased(server,settings);			
			System.out.println("FileWithPath size::"+files.size());
			List<String> existFiles=getFilesFromFolder(settings.getToPath());
			
			JSONArray fileList=new JSONArray();
			fileList.add("test");
			for(FileWithPath file:files) {
				System.out.println("settings.getToPath()::"+file.getPath());
				//file.setPath(settings.getToPath()+"/"+file.getName());
				String token=functions.getZenfraToken("aravind.krishnasamy@virtualtechgurus.com", "Aravind@123");
				System.out.println("Token::"+token);
				
				if(existFiles.contains(file.getName())) {
					System.out.println("path::"+settings.getToPath()+"/"+file.getName());
					 File file1 =new File(settings.getToPath()+"/"+file.getName());
					 String checkSum=FTPClientConfiguration.getFileChecksum(file1);
					System.out.println("Exist checkSum::"+file.getCheckSum());
					System.out.println("New checkSum::"+checkSum);
					 if(file.getCheckSum().equals(checkSum)) {
						 System.out.println("File Already parsed");
						 continue;
					 }
					 file1.delete();
				}
				System.out.println("Final::"+file.getPath());
					fileList.add(file.getPath()+"/"+file.getName());
				callParsing(file.getLogType(), settings.getUserId(),
						settings.getSiteKey(), s.getTenantId(), file.getName(), token,
						file.getPath(),s.getId());
			}		
			status.setStatus("Completed");
			status.setFile(fileList.toJSONString());
			status.setLogCount(String.valueOf(fileList.size()));
			status.setEndTime(functions.getCurrentDateWithTime());
			process.updateMerge(status);
			return files;
		} catch (Exception e) {
			status.setStatus(e.getMessage());
			process.updateMerge(status);
			return status;
		}
	}

	public List<FileWithPath> getFilesBased(FTPServerModel server,FileNameSettingsModel settings) {

		try {
			return settingsService.getFilesByPattern(server,settings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public Object callParsing(String logType,String userId,String siteKey,
		String tenantId,String fileName,String token,
		String folderPath,long schedulerId) {
		  Object responce=null;
		  ProcessingStatus status=new ProcessingStatus();
		try {			
			
			System.out.println("Enter Parsing.....");
					status.setProcessingType("FTP");
					status.setFile(folderPath+"/"+fileName);
					status.setLogType(logType);
					status.setUserId(userId);
					status.setSiteKey(siteKey);
					status.setTenantId(tenantId);
					status.setDataId(schedulerId!=0 ? String.valueOf(schedulerId) : "");
					
			MultiValueMap<String, Object> body= new LinkedMultiValueMap<>();
		      body.add("parseFilePath", folderPath);
		      body.add("parseFileName", fileName);
		      body.add("isFTP", true);
		      body.add("logType", logType);
		      body.add("description", "FTP file parsing");
		      body.add("siteKey", siteKey);
		      body.add("userId", userId);
		      body.add("tenantId", tenantId);
		      body.add("uploadAndProcess", true);
			  
		 RestTemplate restTemplate=new RestTemplate();
		 HttpEntity<Object> request = new HttpEntity<>(body,createHeaders("Bearer "+token));
		 ResponseEntity<String> response= restTemplate
                 //.exchange("http://localhost:8080/usermanagment/rest/ftpScheduler", HttpMethod.POST, request, String.class);
        		  .exchange(Constants.current_url+"/parsing/upload", HttpMethod.POST, request, String.class);
		 ObjectMapper mapper = new ObjectMapper();
         JsonNode root = mapper.readTree(response.getBody());	
         
         status.setResponse(root.toString());
        
		} catch (Exception e) {
			e.printStackTrace();
			 status.setResponse(e.getMessage());
		}
		
		saveEntity(ProcessingStatus.class, status);
		return responce;
	}
	
	
	 HttpHeaders createHeaders(String token){
	        return new HttpHeaders() {{
	              set( "Authorization", token );
	            setContentType(MediaType.MULTIPART_FORM_DATA);
	        }};
	    }

	 
	 
	
	 
	 public List<String> getFilesFromFolder(String path){
		 List<String> listFiles=new ArrayList<String>();
		 try {
			
				System.out.println("Set path:: "+path);
				File Folder = new File(path);
				System.out.println("Folder:: "+Folder);
				for(File filentry:Folder.listFiles()) {
					listFiles.add(filentry.getName());
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
		 return listFiles;
	 }
}
