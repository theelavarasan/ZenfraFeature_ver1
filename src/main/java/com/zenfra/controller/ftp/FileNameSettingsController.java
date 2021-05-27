package com.zenfra.controller.ftp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.ftp.service.FTPClientService;
import com.zenfra.ftp.service.FileNameSettingsService;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileUploadStatus;
import com.zenfra.model.ftp.FileWithPath;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/fNSetting")
public class FileNameSettingsController {
	public static final Logger logger = LoggerFactory.getLogger(FileNameSettingsController.class);
	
	
	@Autowired
	FileNameSettingsService service;
	
	@Autowired
	FTPClientService ftpservice;
	
	@SuppressWarnings({ "unchecked", "static-access" })
	@PostMapping("/save")
	public ResponseModel_v2 saveFNSettings(
			@RequestBody FileNameSettingsModel fileNameSettings) {
		ResponseModel_v2 response = new ResponseModel_v2();
		try {

			fileNameSettings.setActive(true);
			// server.setFilePath(path.getFileName().toString());

			// if (FtbConfiguration.connectToServer(serverUsername, ipAddress, port,
			// Constants.filePath + uploadfile[0].getOriginalFilename(), serverPassword, "")
			// != null) {
			String fileNameSettingId = UUID.randomUUID().toString();
			fileNameSettings.setFileNameSettingId(fileNameSettingId);
			service.saveFileNameSettings(fileNameSettings);
			
			response.setResponseCode(HttpStatus.OK);
			 response.setResponseMessage("Saved FileName Settings");
			// service.saveFtpServer(server);
			return response;
			// }
			// return new ResponseEntity("Unable to login using this credentials!",
			// HttpStatus.OK);
		} catch (Exception e) {
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage("Getting exception in Saving File name Settings: "+e.getMessage());
			// service.saveFtpServer(server);
			return response;
		}		
	}
	@PostMapping("/get")
	public ResponseModel_v2 getFNSettings(@RequestParam String userId,
			@RequestParam String siteKey, @RequestParam String ftpName) {
		System.out.println("Get into FN Settings");
		ResponseModel_v2 response = new ResponseModel_v2();
		try {
			List<FileNameSettingsModel> list=service.getFileNameSettingsByFtpName(siteKey,ftpName);
			response.setjData(list);
			response.setResponseCode(HttpStatus.OK);
			response.setResponseMessage("File Name Settings get call executed Successfully");

		} catch(Exception e) {
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage("Error in get File Name API");
			logger.error("ERROR in GET CALL: "+e.getMessage());
		}	
		return response;
	}
	
	@PostMapping("/delete")
	public ResponseModel_v2 deleteFNSettings(@RequestParam(name = "authUserId", required = false) String userId,
			String siteKey, String serverUsername) {
		ResponseModel_v2 response = new ResponseModel_v2();
		
		try {
			FileNameSettingsModel model=service.getsaveFileNameSettings(siteKey, serverUsername);
				model.setActive(false);
			service.saveFileNameSettings(model);
			response.setResponseCode(HttpStatus.OK);
			response.setResponseMessage("File Name Settings got Successfully");

		} catch(Exception e) {
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage("Error in get File Name API");
			logger.error("ERROR in GET CALL: "+e.getMessage());
		}	
		return response;
	}
	
	
	@PostMapping("/getFromPattern")
	public ResponseModel_v2 getFilesFromPattern(@RequestParam(name = "authUserId", required = false) String userId,
			String siteKey, String ftpName) { 
		ResponseModel_v2 response = new ResponseModel_v2();
		try {			
			List<FileWithPath> filesFillter = service.getFilesByPattern(siteKey, ftpName,userId);
			System.out.println("filesFillter:: "+filesFillter);
			response.setjData(filesFillter);
			response.setResponseMessage("Files From pattern executed...");
		} catch (Exception e) {
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage(e.getMessage());		
		}
		return response;
	}
	
	@PostMapping("/moveFilesByPattern")
	public ResponseModel_v2 moveFilesByPattern(@RequestParam(name = "authUserId", required = false) String userId,
			String siteKey, String serverUsername) { 
		ResponseModel_v2 response = new ResponseModel_v2();
		
		try {
			List<FileWithPath> files = ftpservice.getFiles(siteKey, null, serverUsername);
			
			List<FileUploadStatus> status = service.moveFilesByPattern(siteKey, serverUsername,files);
			response.setjData(status);
			response.setResponseMessage("Files From pattern executed...");		
		} catch (Exception e) {
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage(e.getMessage());		
		
		}
		return response;
	}
	
}
