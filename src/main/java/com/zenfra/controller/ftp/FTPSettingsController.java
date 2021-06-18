package com.zenfra.controller.ftp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.configuration.AESEncryptionDecryption;
import com.zenfra.ftp.service.FTPClientService;
import com.zenfra.ftp.service.FileNameSettingsService;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.utils.CommonFunctions;




@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/ftpSetting")
@Validated
public class FTPSettingsController {

	
	@Autowired
	FTPClientService service;
	
	@Autowired
	CommonFunctions functions;
	
	@Autowired
	AESEncryptionDecryption encrypt;
	
	@Autowired
	FileNameSettingsService fileService;
	
	public static final Logger logger = LoggerFactory.getLogger(FTPSettingsController.class);

	@SuppressWarnings({ "unchecked", "rawtypes", "static-access" })
	@PostMapping("/saveConnection")
	public ResponseModel_v2 saveFtbServer(
			@Valid @RequestBody FTPServerModel ftpServer) {
		
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			ftpServer.setServerPassword(encrypt.encrypt(ftpServer.getServerPassword()));
			ftpServer.setActive(true);
			ftpServer.setCreate_by(ftpServer.getUserId());
			ftpServer.setCreate_time(functions.getCurrentDateWithTime());
			String serverId = UUID.randomUUID().toString();
			ftpServer.setServerId(serverId);		
			
			ftpServer.setServerPath(ftpServer.getServerPath().startsWith("/") ? ftpServer.getServerPath() : "/"+ftpServer.getServerPath() );
			service.saveFtpServer(ftpServer);			
			response.setResponseCode(HttpStatus.OK);
			response.setjData(functions.convertEntityToJsonObject(ftpServer));
			response.setResponseDescription("Saved!");
			
			FileNameSettingsModel fileName=new FileNameSettingsModel();
			
			String deafultString="[{\"namePattern\":\"*SunOS*\",\"logType\":\"AUTO\"},{\"namePattern\":\"*EMCRPT*\",\"logType\":\"AUTO\"},{\"namePattern\":\"*_Linux_*\",\"logType\":\"AUTO\"},{\"namePattern\":\"*RVTools*\",\"logType\":\"VMWARE\"},{\"namePattern\":\"*3PAR*\",\"logType\":\"3PAR\"},{\"namePattern\":\"*Support*\",\"logType\":\"BROCADE\"},{\"namePattern\":\"*PURE*\",\"logType\":\"PURE\"},{\"namePattern\":\"*ntnx*\",\"logType\":\"NUTANIX\"},{\"namePattern\":\"*treme*\",\"logType\":\"XTREMIO\"},{\"namePattern\":\"*max*\",\"logType\":\"VMAX\"},{\"namePattern\":\"*Splore*\",\"logType\":\"3PAR\"},{\"namePattern\":\"*AIX*\",\"logType\":\"AUTO\"},{\"namePattern\":\"*Linux*\",\"logType\":\"AUTO\"},{\"namePattern\":\"*HPUX*\",\"logType\":\"AUTO\"},{\"namePattern\":\"*VNXCellera*\",\"logType\":\"VNXFILE\"},{\"namePattern\":\"*vplex*\",\"logType\":\"VPLEX\"},{\"namePattern\":\"*HDS*\",\"logType\":\"HDS\"},{\"namePattern\":\"*netapp*\",\"logType\":\"NETAPP\"},{\"namePattern\":\"*vnx*\",\"logType\":\"VNX\"},{\"namePattern\":\"*isilon*\",\"logType\":\"ISILON\"},{\"namePattern\":\"*cisco*\",\"logType\":\"CISCO\"},{\"namePattern\":\"*test*\",\"logType\":\"AUTO\"}]";
				FileNameSettingsModel model=fileService.getFileNameSettingsById("dc01e099-e8a5-413a-be30-f86f5ad9b474-default");
					if(model!=null) {
						System.out.println("Get default file name settings");
						fileName.setToPath(model.getToPath().replace(":site_key_value", ftpServer.getSiteKey()));
						deafultString=model.getPatternString();
					}
			ObjectMapper map=new ObjectMapper();
			
			JSONArray arr=map.readValue(deafultString, JSONArray.class);
			System.out.println(arr);
				String fileNameId = UUID.randomUUID().toString();
					fileName.setFileNameSettingId(fileNameId);
					fileName.setActive(true);
					fileName.setFtpName(ftpServer.getFtpName());
					fileName.setIpAddress(ftpServer.getIpAddress());
					fileName.setSiteKey(ftpServer.getSiteKey());
					fileName.setUserId(ftpServer.getUserId());
					//fileName.setToPath("/opt/ZENfra/ZenfraFiles/"+ftpServer.getSiteKey()+"/UploadedLogs");
					fileName.setPattern(arr);
					fileName.setPatternString(arr.toJSONString());
					fileService.saveEntity(FileNameSettingsModel.class, fileName);
			
		} catch (Exception e) {
			e.printStackTrace();
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseDescription(e.getMessage());			
	
		}
		
		return response;
	}

	@PostMapping(value = "/validateFTPName")
	public ResponseEntity<?> getUniqueNames(@NotEmpty(message="authUserId is missing")
	@RequestParam(name = "authUserId", required = false) String userId,
	@RequestParam(name = "siteKey", required = false) String siteKey,
	@NotEmpty(message = "Please provide valid ftp name") @RequestParam(name = "ftpName", required = false) String ftpName) throws IOException,
	URISyntaxException, org.json.simple.parser.ParseException, ParseException, SQLException {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			response = service.nameValidate(siteKey, userId, ftpName);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResponseEntity.ok(response);
	}
	
	@GetMapping(value = "/deleteFTPSettings")
	public ResponseModel_v2 deleteFTPSettings(@RequestParam(name = "authUserId", required = false) String userId,
			@NotEmpty(message = "Please provide valid server id")	@RequestParam(name = "serverId") String serverId) {
		
		System.out.println("Delete option triggered");
		ResponseModel_v2 response = new ResponseModel_v2();
		
		try {			
			response.setResponseCode(HttpStatus.OK);
			response.setResponseMessage(service.deleteConncection(serverId));
		} catch(Exception e) {
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.setResponseMessage("Error in delete API");
			logger.error("ERROR in DELETE CALL: "+e.getMessage());
		}	
		return response;
	}
	
	@SuppressWarnings("static-access")
	@PostMapping(value = "/updateFTPSettings")
	public ResponseModel_v2 updateFTPSettings(@RequestParam(name = "authUserId", required = false) String userId,
			@Valid @RequestBody FTPServerModel ftpServer) {
		System.out.println("Update FTP settings call....................");
		ResponseModel_v2 response = new ResponseModel_v2();
		
		try {			
			if(ftpServer.getServerId()==null) {
				response.setResponseCode(HttpStatus.NOT_ACCEPTABLE);
				response.setResponseMessage("Sent valid object");
			
			}
			ftpServer.setUpdate_by(userId);
			ftpServer.setUpdated_time(functions.getCurrentDateWithTime());
			service.saveFtpServer(ftpServer);
			response.setResponseCode(HttpStatus.OK);
			response.setResponseMessage("Updated Successfully");
			
		} catch (Exception e) {
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.setResponseMessage("Error in delete API");
			logger.error("ERROR in DELETE CALL: "+e.getMessage());
		
			e.printStackTrace();
		} 
		return response;
	}
	
	@PostMapping("/testConnection")
	public ResponseModel_v2 testConnection(@RequestBody FTPServerModel ftpserver){
		ResponseModel_v2 response = new ResponseModel_v2();
		
		
		try {
			
			String connectionRes = service.testConnection(ftpserver);
			System.out.println("Connection Result........... "+connectionRes);
			if(connectionRes.contains("Success")) {
				response.setResponseCode(HttpStatus.OK);
				response.setResponseMessage("Connection Succeeded");
			}else {
				response.setResponseCode(HttpStatus.CREATED);
				response.setResponseMessage("Connection Failed, Please enter valid server or server path info");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.setResponseMessage("Error in delete API");
			logger.error("ERROR in DELETE CALL: "+e.getMessage());
		
		}
		
		return response;
		
			
	}
	
	@GetMapping("/get-ftp-connections-by-user")
	public ResponseModel_v2 getFtpServers(@RequestParam("siteKey") String siteKey) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			
			response.setjData(service.getFtpConnectionBySiteKey(siteKey));
			response.setResponseCode(HttpStatus.OK);			
			response.setResponseMessage("Successfully Executed");
		} catch(Exception e) {
			
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage("Error while fetching the data");
			logger.error("ERROR in FTP servers: "+e.getMessage());
		}	
		
		return response;
	}
	
	@GetMapping("/get-files-from-customer-server")
	public ResponseModel_v2 getFilesFromCustomerPath(@RequestParam(name = "authUserId", required = false) String userId,
			@RequestParam("fromPath") String fromPath,@RequestParam("siteKey") String siteKey,
			@RequestParam("connectionName") String connectionName, @RequestParam("toServer") String toPath){
		ResponseModel_v2 connectionRes = service.getFilesdFromServer(userId, connectionName, fromPath, toPath);
		return connectionRes;
	}
	
	@GetMapping("/ftbserver-getfiles")
	public List<FileWithPath> getFiles(@RequestParam("siteKey") String siteKey, @RequestParam("path") String path,
			@RequestParam("connectionName") String connectionName) {
		try {
		List<FileWithPath> connectionRes = null;//service.getFiles(siteKey, path, connectionName);

			return connectionRes;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	@GetMapping("/default-file-name-pattern")
	public ResponseModel_v2 getFilesNamePatterns(@RequestParam String fileNameSettingsId) {
		ResponseModel_v2 response=new ResponseModel_v2();
		try {
			FileNameSettingsModel model=fileService.getFileNameSettingsById("dc01e099-e8a5-413a-be30-f86f5ad9b474-default");
			response.setjData(model);
			response.setResponseCode(HttpStatus.OK);
			response.setResponseDescription("Data retived");
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
