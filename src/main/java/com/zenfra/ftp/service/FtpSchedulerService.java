package com.zenfra.ftp.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;
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
import com.zenfra.ftp.repo.FtpSchedulerRepo;
import com.zenfra.model.BaseModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.model.ftp.FtpScheduler;

@Service
public class FtpSchedulerService {

	@Autowired
	FtpSchedulerRepo repo;

	@Autowired
	FileNameSettingsService settingsService;

	@Autowired
	FTPClientService clientService;

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

	public FtpScheduler getFtpScheduler(Long id) {

		try {

			return repo.findAllById(id);
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

	public List<FileWithPath> runFtpSchedulerFiles(FtpScheduler s) {
		try {

			
			System.out.println("--------------eneter runFtpSchedulerFiles---------");
			FileNameSettingsModel settings = settingsService.getFileNameSettingsById(s.getFileNameSettingsId());

			List<FileWithPath> files=getFilesBased(settings);
			System.out.println("files size::"+files.size());
			for(FileWithPath file:files) {
				System.out.println("settings.getToPath()::"+settings.getToPath());
				file.setPath(settings.getToPath()+"/"+file.getName());
				String token=token("aravind.krishnasamy@virtualtechgurus.com", "Aravind@123");
				System.out.println("Token::"+token);
				callParsing(file.getLogType(), settings.getUserId(),
						settings.getSiteKey(), s.getTenantId(), file.getPath(), token);
			}
			
			return files;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FileWithPath> getFilesBased(FileNameSettingsModel settings) {

		try {
			return settingsService.getFilesByPattern(settings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public Object callParsing(String logType,String userId,String siteKey,
		String tenantId,String path,String token) {
		  Object responce=null;
		try {
			        
		    File file = new File(path);
			   System.out.println("parsing file name::"+file.getAbsolutePath());
			MultiValueMap<String, Object> body= new LinkedMultiValueMap<>();
		      body.add("parseFile", file);
		      body.add("logType", logType);
		      body.add("description", "FTP file parsing");
		      body.add("siteKey", siteKey);
		      body.add("userId", userId);
		      body.add("tenantId", tenantId);
		      body.add("uploadAndProcess", false);
			      
		 RestTemplate restTemplate=new RestTemplate();
		 HttpEntity<Object> request = new HttpEntity<>(body,createHeaders("Bearer "+token));
		 ResponseEntity<String> response= restTemplate
                 //.exchange("http://localhost:8080/usermanagment/rest/ftpScheduler", HttpMethod.POST, request, String.class);
        		  .exchange("http://uat.zenfra.co:8080/parsing/upload", HttpMethod.POST, request, String.class);
		 ObjectMapper mapper = new ObjectMapper();
         JsonNode root = mapper.readTree(response.getBody());	
         
        System.out.println("root::"+root);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return responce;
	}
	
	
	 HttpHeaders createHeaders(String token){
	        return new HttpHeaders() {{
	              set( "Authorization", token );
	            setContentType(MediaType.MULTIPART_FORM_DATA);
	        }};
	    }

	 
	 
	 public String token(String username,String password) {
		 
		  Object token=null;
			try {
				        
				   
				MultiValueMap<String, Object> body= new LinkedMultiValueMap<>();
			      body.add("userName", username);
			      body.add("password", password);
			  	      
			 RestTemplate restTemplate=new RestTemplate();
			 HttpEntity<Object> request = new HttpEntity<>(body);
			 ResponseEntity<String> response= restTemplate
	                 //.exchange("http://localhost:8080/usermanagment/auth/login", HttpMethod.POST, request, String.class);
	        		  .exchange("http://uat.zenfra.co:8080/UserManagement/auth/login", HttpMethod.POST, request, String.class);
	         ObjectMapper mapper = new ObjectMapper();
	         JsonNode root = mapper.readTree(response.getBody());		
	         token=root.get("jData").get("AccessToken");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return token.toString().replace("\"", "");
	 }
}
