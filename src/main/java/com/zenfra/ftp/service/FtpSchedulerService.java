package com.zenfra.ftp.service;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
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
import org.springframework.web.client.HttpClientErrorException.Unauthorized;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.icu.util.TimeZone;
import com.zenfra.configuration.AESEncryptionDecryption;
import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.ftp.repo.FtpSchedulerRepo;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.model.ftp.FtpScheduler;
import com.zenfra.model.ftp.ProcessingStatus;
import com.zenfra.service.ProcessService;
import com.zenfra.service.UserService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Constants;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.TrippleDes;

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

	
	public Object runFtpSchedulerFiles(FtpScheduler s) throws SQLException {
		ProcessingStatus status=new ProcessingStatus();
		ProcessService process=new ProcessService();
		JSONObject email=new JSONObject();
		CommonFunctions functions=new CommonFunctions();
		ObjectMapper mapper=new ObjectMapper();
		Map<String,String> parseUrls=new HashMap<String, String>();
		String passFileList="";
		FTPServerModel server =new FTPServerModel();
		final List<FileWithPath> files=new ArrayList<>();
		try {
			System.out.println("--------------eneter runFtpSchedulerFiles---------"+s.toString());
			List<String> l=new ArrayList<String>();			
			if(s.getEmailString()!=null && s.getEmailString()!="[]" ) {
				String arr[]=s.getEmailString().replace("\"", "").replace("[", "").replace("]", "").split(",");
					if(arr.length>0) {
						Collections.addAll(l, arr); 
					}
			}
			Map<String,Object> userMap=getObjectByQueryNew("select * from user_temp where user_id='"+s.getUserId()+"'") ;
				email.put("mailFrom", userMap.get("email").toString());
				email.put("mailTo", l);
				email.put("firstName", userMap.get("first_name").toString());
				//email.put("Time", functions.getCurrentDateWithTime());
				email.put("Notes","File processing initiated");
				
			FileNameSettingsService settingsService=new FileNameSettingsService();
				System.out.println("s.getFileNameSettingsId()::"+s.getFileNameSettingsId());			
			
			String getFileNameSettings="select * from file_name_settings_model where file_name_setting_id='"+s.getFileNameSettingsId()+"'";
			FileNameSettingsModel settings=new FileNameSettingsModel();
			Map<String,Object> map=getObjectByQueryNew(getFileNameSettings) ;//settingsService.getFileNameSettingsById(s.getFileNameSettingsId());
				if(map!=null) {
					settings.setFileNameSettingId(map.get("file_name_setting_id").toString());
					settings.setFtpName(map.get("ftp_name").toString());
					settings.setIpAddress(map.get("ip_address").toString());
					System.out.println(map.get("pattern_string"));
					settings.setPattern(map.get("pattern_string")!=null && !map.get("pattern_string").toString().isEmpty()? mapper.readValue(map.get("pattern_string").toString(), JSONArray.class) : new JSONArray());
					settings.setSiteKey(map.get("site_key").toString());
					settings.setToPath(map.get("to_path").toString());
					settings.setUserId(map.get("user_id").toString());
				}
				
			System.out.println("settings::"+settings.toString());
			String serverQuery="select * from ftpserver_model  where site_key='"+settings.getSiteKey()+"' and ftp_name='"+settings.getFtpName()+"'";
			Map<String,Object> serverMap=getObjectByQueryNew(serverQuery) ;//settingsService.getFileNameSettingsById(s.getFileNameSettingsId());
			
				if(server!=null) {
					server.setFtpName(serverMap.get("ftp_name").toString());
					server.setIpAddress(serverMap.get("ip_address").toString());
					server.setPort(serverMap.get("port").toString());
					server.setServerId(serverMap.get("server_id").toString());
					server.setServerPassword(serverMap.get("server_password").toString());
					server.setServerPath(serverMap.get("server_path").toString());
					server.setServerUsername(serverMap.get("server_username").toString());
					server.setSiteKey(serverMap.get("site_key").toString());
					server.setUserId(serverMap.get("user_id").toString());
				}
				email.put("subject", "FTP -"+ server.getFtpName()+" Scheduler has ran Successfully");
				email.put("FTPname", server.getFtpName());				
				status.setProcessingType("FTP");
				status.setProcessing_id(functions.generateRandomId());
				status.setStartTime(functions.getCurrentDateWithTime());
				status.setProcessDataId(String.valueOf(server.getServerId()));
				status.setStatus("Scheduler start");
				status.setSiteKey(server.getSiteKey());				
				status.setPath(server.getServerPath());		
				status.setEndTime(functions.getCurrentDateWithTime());
			
			String processQuery="INSERT INTO processing_status(processing_id, end_time, log_count, path, process_data_id, processing_type,  site_key, start_time, status, tenant_id, user_id)	VALUES (':processing_id', ':end_time',  ':log_count', ':path', ':process_data_id', ':processing_type', ':site_key', ':start_time', ':status', ':tenant_id', ':user_id');";
			
			processQuery=processQuery.replace(":processing_id", status.getProcessing_id())
						.replace(":end_time", functions.getCurrentDateWithTime()).replace(":log_count", "0").replace(":path", server.getServerPath())
						.replace(":process_data_id", String.valueOf(server.getServerId())).replace(":processing_type", "FTP").replace(":site_key", server.getSiteKey())
						.replace(":start_time",functions.getCurrentDateWithTime()).replace(":status", "Scheduler started").replace(":tenant_id","").replace(":user_id", server.getUserId());
			excuteByUpdateQueryNew(processQuery);
			files.addAll(settingsService.getFilesByPattern(server,settings));
			String processUpdate="UPDATE processing_status SET log_count=':log_count',  status=':status' WHERE processing_id=':processing_id';";
				processUpdate=processUpdate.replace(":log_count", String.valueOf(files.size())).replace(":status", "Retrieving files").replace(":processing_id", status.getProcessing_id());
			excuteByUpdateQueryNew(processUpdate);	
						
			System.out.println("FileWithPath size::"+files.size());
			
			String token=functions.getZenfraToken(Constants.ftp_email, Constants.ftp_password);
			
			
			String emailFileList="";
			String updateFiles="";
			for(FileWithPath file:files) {
				System.out.println("Token::"+token);			
				System.out.println("Final::"+file.getPath());
				String url=callParsing(file.getLogType(), settings.getUserId(),
							settings.getSiteKey(), s.getTenantId(), file.getName(), token,
							file.getPath(),s.getId());		
				 parseUrls.put(file.getLogType()+":"+file.getName(),url);
				 emailFileList+="<li>"+file.getLogType()+":"+file.getName()+"</li>";
				 updateFiles+=updateFiles+","+file.getName();
			}	
			String statusFtp="File processing";
			if(emailFileList.isEmpty() || emailFileList==null) {
				emailFileList="No files";
				statusFtp="No file to process";
			}
			email.put("Time", functions.getCurrentDateWithTime()+" "+TimeZone.getDefault().getDisplayName());
			email.put("FileList", emailFileList);
			String processUpdateLast="UPDATE processing_status SET file=':file',end_time=':end_time',status=':status' WHERE processing_id=':processing_id';";
				processUpdateLast=processUpdateLast.replace(":file",updateFiles).replace(":end_time", functions.getCurrentDateWithTime())
								.replace(":status", statusFtp).replace(":processing_id", status.getProcessing_id());
				excuteByUpdateQueryNew(processUpdateLast);
			
				if(files.size()>0) {
					process.sentEmailFTP(email);
				}
			
				
			System.out.println("parseUrls::"+parseUrls);
			RestTemplate restTemplate=new RestTemplate();
			for(String parse:parseUrls.keySet()) {	
				  passFileList+="<li>"+parse+"</li>";
				  
				Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
				    @Override
				    public void uncaughtException(Thread th, Throwable ex) {
				    	email.put("FileList", "<li>"+parse+"</li>");
						email.put("subject", "FTP -"+server.getFtpName()+" Scheduler has Failed");
						email.put("Notes", "Unable to process the file. Don't worry, Admin will check. The above listed files are processing fail.");
						if(files.size()>0) {
							process.sentEmailFTP(email);
						}
				    }
				};
				Thread n = new Thread(){
			        public void run(){
			        	try {
			        		CallFTPParseAPI(restTemplate, parseUrls.get(parse), token);
						} catch (Exception e) {
							email.put("FileList", "<li>"+parse+"</li>");
							email.put("subject", "FTP -"+server.getFtpName()+" Scheduler has Failed");
							email.put("Notes", "Unable to process the file. Don't worry, Admin will check. The above listed files are processing fail.");
							if(files.size()>0) {
								process.sentEmailFTP(email);
							}	
						}				        	
			        }
				  };
				    
				    n.setUncaughtExceptionHandler(h);n.start();				    
				  
			}
			return files;
		} catch (Exception e) {
			e.printStackTrace();
			email.put("FileList", passFileList);
			email.put("subject", "FTP -"+server.getFtpName()+" Scheduler has Failed");
			email.put("Notes", "Unable to process the files. Don't worry, Admin will check. The above listed files are successfully processed.");
			if(files.size()>0) {
				process.sentEmailFTP(email);
			}
			String processUpdateLast="UPDATE processing_status SET response=':response',end_time=':end_time'  status=':status' WHERE processing_id=':processing_id';";
			processUpdateLast=processUpdateLast.replace(":response", e.getMessage()).replace(":end_time", functions.getCurrentDateWithTime())
							.replace(":status", "Failed").replace(":processing_id", status.getProcessing_id());
			status.setEndTime(functions.getCurrentDateWithTime());	
			status.setStatus("Failed");
			status.setResponse(e.getMessage());
			excuteByUpdateQueryNew(processUpdateLast);	
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
			String parsingURL=DBUtils.getParsingServerIP();
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
			  
		 System.out.println("Params::"+body);
		 HttpEntity<Object> request = new HttpEntity<>(body,createHeaders("Bearer "+token));
		 ResponseEntity<String> response= restTemplate
                 //.exchange("http://localhost:8080/usermanagment/rest/ftpScheduler", HttpMethod.POST, request, String.class);
        		  .exchange(parsingURL+"/parsing/upload", HttpMethod.POST, request, String.class);
		 ObjectMapper mapper = new ObjectMapper();
         JsonNode root = mapper.readTree(response.getBody());
         
        //String rid, String logType, String description, boolean isReparse
		if(root==null) {
			System.out.println("invalid response");
		}
		System.out.println("Upload response::"+response);
     	final String rid=root.get("jData").get("logFileDetails").get(0).get("rid").toString().replace("\"", "");
		
		
		if(rid==null && rid.isEmpty()) {
			return "invalid rid";
		}		

		StringBuilder builder = new StringBuilder(parsingURL+"/parsing/parse");
         builder.append("?rid=");	
         builder.append(URLEncoder.encode(rid,StandardCharsets.UTF_8.toString()));
         builder.append("&logType=");	
         builder.append(URLEncoder.encode(logType,StandardCharsets.UTF_8.toString()));
         builder.append("&description=");	
         builder.append(URLEncoder.encode("",StandardCharsets.UTF_8.toString()));
         builder.append("&isReparse=");	
         builder.append(URLEncoder.encode("false",StandardCharsets.UTF_8.toString()));
         	
       
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
	 
	 
	 
	
	 public void CallFTPParseAPI(RestTemplate restTemplate,String builder,String token) {
		 
		 try {
			
			  URI uri = URI.create(builder.toString());
				System.out.println("Parisng call::"+uri);
				HttpEntity<Object> requestParse = new HttpEntity<>(createHeaders("Bearer "+token));
				ResponseEntity<String> responseParse= restTemplate
         //.exchange("http://localhost:8080/usermanagment/rest/ftpScheduler", HttpMethod.POST, request, String.class);
						.exchange(uri, HttpMethod.GET, requestParse, String.class);
	
		} catch (Unauthorized e) {
			e.printStackTrace();
			token=functions.getZenfraToken(Constants.ftp_email, Constants.ftp_password);
			CallFTPParseAPI(restTemplate, builder, token);
		}catch (Exception e) {
			e.printStackTrace();
		}
	 }
}
