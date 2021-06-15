package com.zenfra.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.spark.sql.functions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.model.ftp.CheckSumDetails;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileUploadStatus;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.utils.CommonFunctions;

@Component
public class FTPClientConfiguration extends CommonEntityManager {

		@Autowired
		CommonFunctions functions;
	
	public static FTPClient loginClient(FTPServerModel server) {

		try {
			FTPClient ftpClient = new FTPClient();
			int port = Integer.parseInt(server.getPort());
			ftpClient.connect(server.getIpAddress(), port);

			// System.out.println(ftpClient.getReplyString());

			ftpClient.sendCommand(FTPCommand.USER, server.getServerUsername());

			// System.out.println(ftpClient.getReplyString());
			AESEncryptionDecryption encryption = new AESEncryptionDecryption();

			ftpClient.sendCommand(FTPCommand.PASS, encryption.decrypt(server.getServerPassword()));
			// System.out.println(ftpClient.getReplyString());

			return ftpClient;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String testConenction(FTPServerModel server) {
		try {
			System.out.println(server.getIpAddress() + ":" + Integer.parseInt(server.getPort()));
			FTPClient ftpClient = new FTPClient();
			ftpClient.connect(server.getIpAddress(), Integer.parseInt(server.getPort()));

			boolean ftpChk = ftpClient.login(server.getServerUsername(), server.getServerPassword());

			System.out.println("FTP client Code:: " + ftpChk);
			if (!ftpChk) {
				return "Test Connection Failed!";
			}
			if (!ftpClient.changeWorkingDirectory(server.getServerPath())) {
				return "The given path is invalid!";
			}
			ftpClient.logout();
			ftpClient.disconnect();
			return "Test Connection Success!";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage().toString();
		}
	}

	public static FTPClient getConnection(FTPServerModel server) {
		try {

			return loginClient(server);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FileWithPath> getFilesFromPath(FTPServerModel server, String path) {

		try {

			List<FileWithPath> fileList = new ArrayList<FileWithPath>();

			List<String> existCheckSums = getCheckSumDetails(server.getSiteKey());

			System.out.println("Start iStream FTP" + path);
			FTPClient ftpClient = getConnection(server);
			fileList = getAllFilesFromPath(server, path, existCheckSums);

			System.out.println("file read END");
			
			ftpClient.logout();
			ftpClient.disconnect();
			System.out.println("End iStream FTP" + path);
			return fileList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getFileFromFtp(FTPServerModel server, String path, String toPath, String fileName) {

		try {

			System.out.println("path::" + path);
			System.out.println("toPath::" + toPath);
			System.out.println("fileName::" + fileName);

			FTPClient ftpClient = getConnection(server);
			ftpClient.changeWorkingDirectory(path);
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.enterLocalPassiveMode();
			File f = new File(toPath);
			if (!(f.exists() && f.isDirectory())) {
				f.mkdir();
			}

			toPath = toPath + "/" + fileName;
			System.out.println("toPath::" + toPath);
			try (FileOutputStream fos = new FileOutputStream(toPath)) {
				ftpClient.retrieveFile(fileName, fos);
			} catch (IOException e) {
				e.printStackTrace();
				getFileFromFtp(server, path, toPath, fileName);
				return e.getMessage().toString();
			}

			String str = ftpClient.getReplyString();
			ftpClient.logout();

			ftpClient.disconnect();
			return str;
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage().toString();
		}

	}

	public static List<FileUploadStatus> getFileFromFtpChange(FTPServerModel server, String fromPath, String toPath) {
		List<FileUploadStatus> statusList = new ArrayList<FileUploadStatus>();
		try {
			FTPClient ftpClient = getConnection(server);
			String[] files = fromPath.split(",");

			for (String s : files) {
				/// home/FTP-Logs/PureFlashArrayVTGNew.log
				String[] file = s.split("/");
				String fileName = file[file.length - 1];
				String path = s.replace(fileName, "");
				FileUploadStatus status = new FileUploadStatus();
				ftpClient.changeWorkingDirectory(path);
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				ftpClient.enterLocalPassiveMode();

				String copyPath = toPath + "/" + fileName;
				try (FileOutputStream fos = new FileOutputStream(copyPath)) {
					ftpClient.retrieveFile(fileName, fos);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}

				String str = ftpClient.getReplyString();

				status.setStatus(str);
				status.setFileName(s);
				statusList.add(status);

			}
			ftpClient.logout();

			ftpClient.disconnect();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return statusList;
	}

	public List<FileWithPath> getAllFilesFromPath(FTPServerModel server, String parentDir,
			List<String> existChecksums) {

		List<FileWithPath> files = new ArrayList<FileWithPath>();
		try {
			FTPClient ftpClient = getConnection(server);

			ftpClient.enterLocalPassiveMode();
			files.addAll(getAllFilesFromPath(ftpClient, parentDir, "", 0, server, files, existChecksums, false));
			ftpClient.logout();
			ftpClient.disconnect();
			return files;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FileWithPath> getAllFilesFromPath(FTPClient ftpClient, String parentDir, String currentDir, int level,
			FTPServerModel server, List<FileWithPath> fileList, List<String> existCheckSums, boolean subFolder)
			throws IOException {

		try {
			String dirToList = parentDir;
			if (!currentDir.equals("")) {
				dirToList += "/" + currentDir;
			}
			ftpClient.changeWorkingDirectory(dirToList);
			FTPFile[] files = ftpClient.listFiles();

			for (FTPFile aFile : files) {
				String currentFileName = aFile.getName();
				if (currentFileName.equals(".") || currentFileName.equals("..")) {
					// skip parent directory and directory itself
					continue;
				}
				for (int i = 0; i < level; i++) {
					System.out.print("\t");
				}
				if (aFile.isDirectory()) {
					subFolder = true;
					System.out.println("[" + currentFileName + "]");
					getAllFilesFromPath(ftpClient, parentDir, currentFileName, level, server, fileList, existCheckSums,
							subFolder);
					subFolder = false;
				} else {

					String chkSumFTP = "";
					System.out.println(currentFileName);

					if (aFile.getName().equalsIgnoreCase(".") || aFile.getName().equalsIgnoreCase("..")) {
						continue;
					}
					String details = aFile.getName();
					System.out.println("Stream path::" + dirToList + "/" + aFile.getName());
					InputStream iStream = ftpClient.retrieveFileStream(dirToList + "/" + aFile.getName());
					if (iStream != null) {
						File file1 = File.createTempFile("tmp", null);
						FileUtils.copyInputStreamToFile(iStream, file1);
						iStream.close();
						chkSumFTP = getFileChecksum(file1);
						file1.delete();
						ftpClient.completePendingCommand();
						if (copyStatus(existCheckSums, chkSumFTP, server.getServerId(), aFile.getName(),server.getSiteKey())) {
							continue;
						}

					}
					FileWithPath path1 = new FileWithPath();
					path1.setPath(dirToList + "/" + aFile.getName());
					path1.setName(aFile.getName());
					if (subFolder) {
						path1.setSubFolderPath(dirToList);
					}
					path1.setCheckSum(chkSumFTP);
					path1.setSubFolder(subFolder);
					fileList.add(path1);
					

				}
			}

			return fileList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getFileChecksum(File file) throws IOException, NoSuchAlgorithmException {

		System.out.println("Checksum start.........." + file.getName());
		MessageDigest digest = MessageDigest.getInstance("MD5");
		// Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		// Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		// Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		}
		;

		// close the stream; We don't need it now.
		fis.close();

		// Get the hash's bytes
		byte[] bytes = digest.digest();

		// This bytes[] has bytes in decimal format;
		// Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		System.out.println("Checksum exist.........." + file.getName());
		// return complete hash
		return sb.toString();
	}

	public boolean copyStatus(List<String> existCheckSums, String checkSum, String serverId, String fileName,String sitekey) {

		try {

			System.out.println("checkSum::" + checkSum);
			if (existCheckSums != null && existCheckSums.contains(checkSum)) {
				return true;
			}
	
			
			String query="INSERT INTO check_sum_details(check_sum_id, check_sum, client_ftp_server_id, file_name, site_key) VALUES (':check_sum_id', ':check_sum', ':client_ftp_server_id', ':file_name', ':site_key');";
				query=query.replace(":check_sum_id", functions.generateRandomId()).replace(":check_sum", checkSum).replace(":client_ftp_server_id", serverId).replace(":file_name", fileName).replace(":site_key", sitekey);
			System.out.println("CheckSum query::"+query);
				updateQuery(query);			
			return false;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public List<String> getCheckSumDetails(String sitekey) {
		List<String> list = new ArrayList<String>();
		try {
			List<Object> objList = getEntityListByColumn("select * from check_sum_details where site_key='"+sitekey+"'", CheckSumDetails.class);
			for (Object obj : objList) {
				CheckSumDetails check = (CheckSumDetails) obj;
				list.add(check.getCheckSum());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

}
