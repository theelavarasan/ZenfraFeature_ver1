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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

	
	public Object callParsing(String siteKey,
		String tenantId,List<FileWithPath> files,String token) {
		  Object responce=null;
		try {
			        
		 	
		MultiValueMap<String, Object> body= new LinkedMultiValueMap<>();
			      body.add("files", files);
			      body.add("siteKey", siteKey);
			      body.add("tenantId", tenantId);
		
			      
		 RestTemplate restTemplate=new RestTemplate();
		 HttpEntity<Object> request = new HttpEntity<>(body,createHeaders(token));
          responce= restTemplate
                 .exchange("http://uat.zenfra.co:8080/usermanagment/rest/ftpScheduler", HttpMethod.POST, request, String.class);	
			
        
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return responce;
	}
	
	
	 HttpHeaders createHeaders(String token){
	        return new HttpHeaders() {{
	              set( "Authorization", token );
	            setContentType(MediaType.APPLICATION_JSON);
	        }};
	    }

}
