package com.zenfra.ftp.service;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
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
import com.zenfra.model.Users;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.model.ftp.FtpScheduler;
import com.zenfra.model.ftp.ProcessingStatus;
import com.zenfra.service.ProcessService;
import com.zenfra.service.UserService;
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
	
	@Autowired
	UserService userService;
	
	
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
		JSONObject email=new JSONObject();
		
		try {
			System.out.println("--------------eneter runFtpSchedulerFiles---------"+s.getFileNameSettingsId());
			
			JSONObject fileList=new JSONObject();
			/*List<String> l=new ArrayList<String>();
				l.add("aravind.krishnasamy@virtualtechgurus.com");
			Users user=userService.getUserByUserId(s.getUserId());
		
					fileList.add("test");
				email.put("mailFrom", user.getEmail());
				//email.put("mailTo", functions.convertJsonArrayToList(s.getNotificationEmail()));
				email.put("mailTo", l);
				email.put("subject", "FTP File Parsing trigger mail");
				email.put("firstName", user.getFirst_name());
				email.put("Time", functions.getCurrentDateWithTime());
				email.put("resetUrl", "uat.zenfra.co");
				email.put("FileList", fileList.toJSONString().replace("\"", "").replace("[", "").replace("]", ""));
			
				process.sentEmailFTP(email);*/
				
			FileNameSettingsModel settings = settingsService.getFileNameSettingsById(s.getFileNameSettingsId());
			
			FTPServerModel server = clientService.getFtpConnectionBySiteKey(settings.getSiteKey(), settings.getFtpName());
				email.put("FTPname", server.getFtpName());				
				status.setProcessingType("FTP");
				status.setDataId(String.valueOf(server.getServerId()));
				status.setStartTime(functions.getCurrentDateWithTime());
				status.setId(functions.generateRandomId());
				status.setStatus("Processing");
			process.saveProcess(status);	
			List<FileWithPath> files=getFilesBased(server,settings);			
			System.out.println("FileWithPath size::"+files.size());
			
			String token=functions.getZenfraToken(Constants.ftp_email, Constants.ftp_password);
			
			
			
			for(FileWithPath file:files) {
				System.out.println("settings.getToPath()::"+file.getPath());
				//file.setPath(settings.getToPath()+"/"+file.getName());
				System.out.println("Token::"+token);
			
				System.out.println("Final::"+file.getPath());
					
				String url=callParsing(file.getLogType(), settings.getUserId(),
						settings.getSiteKey(), s.getTenantId(), file.getName(), token,
						file.getPath(),s.getId());
				fileList.put(file.getPath()+"/"+file.getName(),url);
			}	
			
			if(fileList.isEmpty() || fileList==null) {
				fileList.put("file","No files");
			}
			//email.put("Time", functions.getCurrentDateWithTime());
			//email.put("FileList", fileList.toJSONString().replace("\"", "").replace("[", "").replace("]", ""));
			status.setStatus("Completed");
			status.setFile(fileList.toJSONString());
			status.setLogCount(String.valueOf(fileList.size()));
			status.setEndTime(functions.getCurrentDateWithTime());
			status.setPath(server.getServerPath());
			process.updateMerge(status);
			//process.sentEmailFTP(email);
			return files;
		} catch (Exception e) {
			//email.put("Notes", "Unable to parse file.Don't worry admin look in to this.");
			//process.sentEmailFTP(email);
			status.setStatus("Failed");
			status.setResponse(e.getMessage());
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

	
	public String callParsing(String logType,String userId,String siteKey,
		String tenantId,String fileName,String token,
		String folderPath,long schedulerId) {
		
		try {			
			
			RestTemplate restTemplate=new RestTemplate();
			System.out.println("Enter Parsing.....");
						
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
			  
		 
		 HttpEntity<Object> request = new HttpEntity<>(body,createHeaders("Bearer "+token));
		 ResponseEntity<String> response= restTemplate
                 //.exchange("http://localhost:8080/usermanagment/rest/ftpScheduler", HttpMethod.POST, request, String.class);
        		  .exchange(Constants.current_url+"/parsing/upload", HttpMethod.POST, request, String.class);
		 ObjectMapper mapper = new ObjectMapper();
         JsonNode root = mapper.readTree(response.getBody());
         
        //String rid, String logType, String description, boolean isReparse
		if(root==null && root.isEmpty()) {
			System.out.println("invalid response");
		}
		System.out.println("Upload response::"+response);
     	final String rid=root.get("jData").get("logFileDetails").get(0).get("rid").toString().replace("\"", "");
		
		
		if(rid==null && rid.isEmpty()) {
			return "invalid rid";
		}
		

		StringBuilder builder = new StringBuilder(Constants.current_url+"/parsing/parse");
         builder.append("?rid=");	
         builder.append(URLEncoder.encode(rid,StandardCharsets.UTF_8.toString()));
         builder.append("&logType=");	
         builder.append(URLEncoder.encode(logType,StandardCharsets.UTF_8.toString()));
         builder.append("&description=");	
         builder.append(URLEncoder.encode("",StandardCharsets.UTF_8.toString()));
         builder.append("&isReparse=");	
         builder.append(URLEncoder.encode("false",StandardCharsets.UTF_8.toString()));
         	
         this.sync(restTemplate, builder, token);
         
         return builder.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return "Unable to call Parse API";
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
	 
	 
	 
	 @Async
	 public void sync(RestTemplate restTemplate,StringBuilder builder,String token) {
		 
		 try {
			
			  URI uri = URI.create(builder.toString());
				System.out.println("Parisng call::"+uri);
				HttpEntity<Object> requestParse = new HttpEntity<>(createHeaders("Bearer "+token));
				ResponseEntity<String> responseParse= restTemplate
         //.exchange("http://localhost:8080/usermanagment/rest/ftpScheduler", HttpMethod.POST, request, String.class);
						.exchange(uri, HttpMethod.GET, requestParse, String.class);
	
		} catch (Exception e) {
			e.printStackTrace();
		}
	 }
}
