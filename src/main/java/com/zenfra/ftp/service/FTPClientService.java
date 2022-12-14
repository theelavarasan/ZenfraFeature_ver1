package com.zenfra.ftp.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.zenfra.configuration.FTPClientConfiguration;
import com.zenfra.ftp.repo.FTPServerRepo;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileUploadStatus;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class FTPClientService {

	@Autowired
	FTPServerRepo repo;

	@Autowired
	FTPClientConfiguration fTPClientConfiguration;

	@Autowired
	FileNameSettingsService fileNameService;

	public boolean saveFtpServer(FTPServerModel server) {

		try {

			repo.save(server);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public ResponseModel_v2 nameValidate(String siteKey, String userId, String ftpName) {

		ResponseModel_v2 response = new ResponseModel_v2();

		try {

			List<FTPServerModel> list = repo.checkName(ftpName, siteKey);
			if (list.size() > 0) {
				response.setResponseCode(HttpStatus.CONFLICT);
				response.setResponseMessage("Provided " + ftpName + " already Available");
			} else {
				response.setResponseCode(HttpStatus.ACCEPTED);
				response.setResponseMessage("Provided " + ftpName + " not Available");
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			response.setResponseMessage("Failed");
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);

		} finally {
			return response;
		}
	}

	public String deleteConncection(String serverId) {

		try {

			FTPServerModel server = getServerByServerId(serverId);
			if (server != null) {
				fileNameService.deleteFileNameSettingsByFtpName(server.getFtpName(), server.getSiteKey());
				repo.deleteById(serverId);
				return "deleted";
			} else {
				return "Please sent valid server id";
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return e.getMessage();
		}
	}

	public String testConnection(FTPServerModel server) {

		try {

			return FTPClientConfiguration.testConenction(server);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return e.getMessage().toString();
		}
	}

	public List<FTPServerModel> getFtpConnectionBySiteKey(String siteKey) {
		try {
			return repo.findConnectionsBySiteKey(siteKey);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}

	}

	public Object getNasBySiteKeyAndIsNas(String siteKey, boolean isNas) {
		try {
			return repo.findNasBySiteKeyAndIsNas(siteKey, isNas);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}

	}

	public List<FileWithPath> getFiles(String siteKey, String path, String connectionName, FTPServerModel server) {

		List<FileWithPath> listFilesFromPath = new ArrayList<FileWithPath>();
		try {
			FTPClientConfiguration fTPClientConfiguration = new FTPClientConfiguration();
			// FTPServerModel server = repo.findBySiteKey(siteKey, connectionName);
			if (server.getServerPath() != null) {
				listFilesFromPath = fTPClientConfiguration.getFilesFromPath(server, server.getServerPath());
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return listFilesFromPath;
	}

	public ResponseModel_v2 getFilesdFromServer(String siteKey, String connectionName, String fromPath, String toPath) {

		ResponseModel_v2 response = new ResponseModel_v2();
		try {

			FTPServerModel server = repo.findBySiteKey(siteKey, connectionName);

			List<FileUploadStatus> statusList = new ArrayList<FileUploadStatus>();

			String[] files = fromPath.split(",");

			for (String s : files) {
				/// home/FTP-Logs/home/PureFlashArrayVTGNew.log,home/FTP-Logs/PureFlashArrayVTGNew.log

				String[] file = s.split("/");
				String fileName = file[file.length - 1];
				String path = s.replace(fileName, "");

				FileUploadStatus status = new FileUploadStatus();
				status.setStatus(FTPClientConfiguration.getFileFromFtp(server, path, toPath, fileName));
				status.setFileName(s);
				statusList.add(status);
			}
			response.setResponseCode(HttpStatus.OK);
			response.setResponseMessage("Log files moved Successfully");
			response.setjData(statusList);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage("Error while fetching the data");
		}
		return response;
	}

	public List<FileUploadStatus> getFilesdFromServerPattern(FTPServerModel server, FileNameSettingsModel settings,
			List<FileWithPath> files) {

		try {
			System.out.println("Get files from FTP start");
			System.out.println("---files--"+files);
			List<FileUploadStatus> statusList = new ArrayList<FileUploadStatus>();
			for (FileWithPath s : files) {
				FileUploadStatus status = new FileUploadStatus();
				if (s.isSubFolder()) {
					System.out.println("--sub folder---");
					System.out.println("s.getSubFolderPath()"+s.getSubFolderPath()+" s.getPath()"+s.getPath());
					status.setStatus(FTPClientConfiguration.getFileFromFtp(server, s.getSubFolderPath(), s.getPath(),
							s.getName()));
				} else {
					System.out.println("server.getServerPath()"+server.getServerPath()+" s.getPath()"+s.getPath());
					status.setStatus(FTPClientConfiguration.getFileFromFtp(server, server.getServerPath(), s.getPath(),
							s.getName()));				
				}
				status.setFileName(s.getName());
				statusList.add(status);
			}
			System.out.println("Get files from FTP end");
			return statusList;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	public FTPServerModel getFtpConnectionBySiteKey(String siteKey, String connectionName) {
		FTPServerModel model = new FTPServerModel();
		try {
			model = repo.findBySiteKey(siteKey, connectionName);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return model;
	}

	public FTPServerModel getServerByServerId(String id) {
		try {

			return repo.findByserverId(id);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return null;
	}

}
