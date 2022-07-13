package com.zenfra.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileUploadStatus;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Component
public class FTPClientConfiguration extends CommonEntityManager {

	@Autowired
	CommonFunctions functions;

	public static Channel loginClient(FTPServerModel server) {

		try {
			FTPClient ftpClient = new FTPClient();
			int port = Integer.parseInt(server.getPort());
//			ftpClient.connect(server.getIpAddress(), port);

			// System.out.println(ftpClient.getReplyString());

//		ftpClient.sendCommand(FTPCommand.USER, server.getServerUsername());

			// System.out.println(ftpClient.getReplyString());
			AESEncryptionDecryption encryption = new AESEncryptionDecryption();

//			ftpClient.sendCommand(FTPCommand.PASS, encryption.decrypt(server.getServerPassword()));
			// System.out.println(ftpClient.getReplyString());

			JSch jsch = new JSch();

			Session session = jsch.getSession(server.getServerUsername(), server.getIpAddress(),
					Integer.parseInt(server.getPort()));
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(encryption.decrypt(server.getServerPassword()));
			System.out.println("ip: " + server.getIpAddress() + " pass: " + server.getServerPassword());
			session.connect();
			System.out.println("Connection established.");
			System.out.println("Creating SFTP Channel.");
			Channel sftp = session.openChannel("sftp");
			sftp.connect();
			return sftp;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	public static String testConenction(FTPServerModel server) {
		try {
			System.out.println(server.getIpAddress() + ":" + Integer.parseInt(server.getPort()));
//			FTPClient ftpClient = new FTPClient();
//			FTPSClient ftpClient = new FTPSClient();
//			ftpClient.connect(server.getIpAddress(), Integer.parseInt(server.getPort()));

			JSch jsch = new JSch();
			ChannelSftp sftp = null;
			Session session = jsch.getSession(server.getServerUsername(), server.getIpAddress(),
					Integer.parseInt(server.getPort()));
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(server.getServerPassword());
			session.connect();
			if (session.isConnected()) {
				System.out.println("Connection established.");
				System.out.println("Creatiftng SFTP Channel.");
				sftp = (ChannelSftp) session.openChannel("sftp");
				System.out.println("-----sftp-----"+sftp);
				sftp.connect();
				System.out.println("sftp connected "+ sftp.isConnected());
			}  
			if (sftp.isConnected()) {
				sftp.cd(server.getServerPath());
				System.out.println("--sftp-ls-"+sftp.ls(server.getServerPath()));
			}else {
				return "The given path is invalid!";
			}
				
			sftp.disconnect();
			session.disconnect();
			return "Test Connection Successfull!";	
//			boolean ftpChk = ftpClient.login(server.getServerUsername(), server.getServerPassword());

//			System.out.println("FTP client Code:: " + ftpChk);
//			if (!ftpChk) {
//				return "Test Connection Failed!";
//			}
//			if (!ftpClient.changeWorkingDirectory(server.getServerPath())) {
//				return "The given path is invalid!";
//			}
//			ftpClient.logout();
//			ftpClient.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return e.getMessage().toString();
		}
	}

	public static Channel getConnection(FTPServerModel server) {
		try {

			return loginClient(server);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	public List<FileWithPath> getFilesFromPath(FTPServerModel server, String path) {

		try {

			List<FileWithPath> fileList = new ArrayList<FileWithPath>();

			Map<String, List<String>> existCheckSums = getCheckSumDetails(server.getSiteKey());

			System.out.println("Start iStream FTP" + path);
			System.out.println("ip: " + server.getIpAddress() + "pass: " + server.getServerPassword());
			Channel sftpChannel = getConnection(server);
			fileList = getAllFilesFromPath(server, path, existCheckSums);
			System.out.println("file read END");
//			sshClient.logout();
			sftpChannel.disconnect();
			System.out.println("End iStream FTP" + path);
			return fileList;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static String getFileFromFtp(FTPServerModel server, String path, String toPath, String fileName) {

		int grabCount = 0;
		try {

			System.out.println("path::" + path);
			System.out.println("toPath::" + toPath);
			System.out.println("fileName::" + fileName);

			File f = new File(toPath);
			if (!(f.exists() && f.isDirectory())) {
				f.mkdir();
			}

			ChannelSftp sftpChannel = (ChannelSftp) getConnection(server);
			sftpChannel.cd(path);
			System.out.println("!!!!! lcd: " + sftpChannel.lpwd());
			Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(path);
			System.out.println("---ls ----"+sftpChannel.ls("."));
			for (ChannelSftp.LsEntry oListItem : list) {
				// output each item from directory listing for logs
				System.out.println(oListItem.toString());
				sftpChannel.put(path, toPath+ "/" +fileName);
				// If it is a file (not a directory)
				if (!oListItem.getAttrs().isDir()) {
					// Grab the remote file ([remote filename], [local path/filename to write file
					// to])

					System.out.println("get " + oListItem.getFilename());
					sftpChannel.put(path+ "/" +fileName, toPath  + "/" + fileName);
//					sftpChannel.get(oListItem.getFilename(), toPath + "/" + fileName); // while testing, disable this or
																						// all of your test files will																// be grabbed
					System.out.println("----listed---"+sftpChannel.ls(toPath));
					grabCount++;

					// Delete remote file
					// c.rm(oListItem.getFilename()); // Note for SFTP grabs from this remote host,
					// deleting the file is unnecessary,
					// as their system automatically moves an item to the 'downloaded' subfolder
					// after it has been grabbed. For other target hosts, un comment this line to
					// remove any downloaded files from the inbox.
				}
			}

			String str = sftpChannel.getHome();
			sftpChannel.disconnect();

			return str;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return e.getMessage().toString();
		}

	}

	public static List<FileUploadStatus> getFileFromFtpChange(FTPServerModel server, String fromPath, String toPath) {
		List<FileUploadStatus> statusList = new ArrayList<FileUploadStatus>();
		try {
			ChannelSftp sftpChannel = (ChannelSftp) getConnection(server);
			String[] files = fromPath.split(",");

			for (String s : files) {
				/// home/FTP-Logs/PureFlashArrayVTGNew.log
				String[] file = s.split("/");
				String fileName = file[file.length - 1];
				String path = s.replace(fileName, "");
				FileUploadStatus status = new FileUploadStatus();
				sftpChannel.cd(path);
//				sftpClient.setFileType(FTP.BINARY_FILE_TYPE);
//				sftpClient.enterLocalPassiveMode();

				String copyPath = toPath + "/" + fileName;
				try (FileOutputStream fos = new FileOutputStream(copyPath)) {
					sftpChannel.get(fileName, fos);
				} catch (IOException e) {
					e.printStackTrace();
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					String ex = errors.toString();
					ExceptionHandlerMail.errorTriggerMail(ex);
					return null;
				}

				String str = sftpChannel.getHome();

				status.setStatus(str);
				status.setFileName(s);
				statusList.add(status);

			}
//			sftpClient.logout();

			sftpChannel.disconnect();

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
		return statusList;
	}

	public List<FileWithPath> getAllFilesFromPath(FTPServerModel server, String parentDir,
			Map<String, List<String>> existChecksums) {

		List<FileWithPath> files = new ArrayList<FileWithPath>();
		try {
			ChannelSftp sftpChannel = (ChannelSftp) getConnection(server);

//			ftpClient.enterLocalPassiveMode();
			files.addAll(getAllFilesFromPath(sftpChannel, parentDir, "", 0, server, files, existChecksums, false));
//			ftpClient.logout();
			sftpChannel.disconnect();
			return files;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	public List<FileWithPath> getAllFilesFromPath(ChannelSftp sftpChanel, String parentDir, String currentDir,
			int level, FTPServerModel server, List<FileWithPath> fileList, Map<String, List<String>> existChecksums,
			boolean subFolder) throws IOException {

		try {
			String dirToList = parentDir;
			if (!currentDir.equals("")) {
				dirToList += "/" + currentDir;
			}
			sftpChanel.cd(dirToList);
			Vector<ChannelSftp.LsEntry> files = sftpChanel.ls(dirToList);

			for (ChannelSftp.LsEntry entry : files) {
				String currentFileName = entry.getFilename();
				if (currentFileName.equals(".") || currentFileName.equals("..")) {
					// skip parent directory and directory itself
					continue;
				}
				for (int i = 0; i < level; i++) {
					System.out.print("\t");
				}
				if (entry.getAttrs().isDir()) {
					subFolder = true;
					System.out.println("[" + currentFileName + "]");
					getAllFilesFromPath(sftpChanel, parentDir, currentFileName, level, server, fileList, existChecksums,
							subFolder);
					subFolder = false;
				} else {

					System.out.println(currentFileName);

					if (entry.getFilename().equalsIgnoreCase(".") || entry.getFilename().equalsIgnoreCase("..")) {
						continue;
					}
					String details = entry.getFilename();
					System.out.println("Stream path::" + dirToList + "/" + entry.getFilename());
					// InputStream iStream = ftpClient.retrieveFileStream(dirToList + "/" +
					// aFile.getName());
					// if (iStream != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("serverId", server.getServerId());
					map.put("fileName", entry.getFilename());
					map.put("siteKey", server.getSiteKey());
					map.put("fileSize", String.valueOf(entry.getAttrs().getSize()));
					map.put("createDate", entry.getAttrs().getATime());
					System.out.println("map::" + map);
					// File file1 = File.createTempFile("tmp", null);
					// FileUtils.copyInputStreamToFile(iStream, file1);
					// iStream.close();
					// chkSumFTP = getFileChecksum(file1);
					// file1.delete();
					System.out.println("start start check sum");
					// ftpClient.completePendingCommand();
					System.out.println("start end check sum");
					if (copyStatus(map, existChecksums)) {
						continue;
					}

					// }
					FileWithPath path1 = new FileWithPath();
					path1.setPath(dirToList + "/" + entry.getFilename());
					path1.setName(entry.getFilename());
					if (subFolder) {
						path1.setSubFolderPath(dirToList);
					}
					path1.setSubFolder(subFolder);
					fileList.add(path1);
					System.out.println("complelete ftp");

				}
			}

			return fileList;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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

	public boolean copyStatus(Map<String, Object> currentMap, Map<String, List<String>> existMap) {

		try {

			System.out.println("start check sum function");
			CommonFunctions functions = new CommonFunctions();
			if (currentMap != null && existMap.containsKey(currentMap.get("fileName"))
					&& (existMap.get(currentMap.get("fileName")).contains(currentMap.get("fileSize"))
							&& existMap.get(currentMap.get("fileName")).contains(currentMap.get("createDate")))) {
				System.out.println("check test");
				return true;
			}

			System.out.println("start check sum end");

			String query = "INSERT INTO check_sum_details(check_sum_id, create_date, client_ftp_server_id, file_name, site_key,file_size) VALUES (':check_sum_id', ':create_date', ':client_ftp_server_id', ':file_name', ':site_key',':file_size');";
			query = query.replace(":check_sum_id", functions.generateRandomId())
					.replace(":file_size", currentMap.get("fileSize").toString())
					.replace(":create_date", currentMap.get("createDate").toString())
					.replace(":client_ftp_server_id", currentMap.get("serverId").toString())
					.replace(":file_name", currentMap.get("fileName").toString())
					.replace(":site_key", currentMap.get("siteKey").toString());
			System.out.println("CheckSum query::" + query);
			excuteByUpdateQueryNew(query);
			return false;

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}

	}

	public boolean copyStatusNas(Map<String, Object> currentMap, Map<String, List<String>> existMap) {

		try {

			System.out.println("start check sum function");
			CommonFunctions functions = new CommonFunctions();
			System.out.println("--file map size--" + currentMap.get("fileSize").toString());
			if (currentMap != null && existMap.containsKey(currentMap.get("fileName"))) {
				System.out.println("Nas check test");
				return true;
			} else {
				System.out.println("start check sum end");

				String query = "INSERT INTO check_sum_details(check_sum_id, create_date, client_ftp_server_id, file_name, site_key,file_size) VALUES (':check_sum_id', ':create_date', ':client_ftp_server_id', ':file_name', ':site_key',':file_size');";
				query = query.replace(":check_sum_id", functions.generateRandomId())
						.replace(":file_size", currentMap.get("fileSize").toString()).replace(":create_date", "")
						.replace(":client_ftp_server_id", currentMap.get("serverId").toString())
						.replace(":file_name", currentMap.get("fileName").toString())
						.replace(":site_key", currentMap.get("siteKey").toString());
				System.out.println("CheckSum query::" + query);
				excuteByUpdateQueryNew(query);
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}

	}

	public Map<String, List<String>> getCheckSumDetails(String sitekey) {
		Map<String, List<String>> list = new HashMap<String, List<String>>();
		try {
			String query = "select * from check_sum_details where site_key='" + sitekey + "'";

			List<Map<String, Object>> map = getListObjectsByQueryNew(query);
			// List<Object> objList = getEntityListByColumn("select * from check_sum_details
			// where site_key='"+sitekey+"'", CheckSumDetails.class);

			for (Map<String, Object> obj : map) {
				// list.add(obj.get("check_sum").toString());
				List<String> temList = new ArrayList<String>();
				temList.add(obj.get("file_size") != null ? obj.get("file_size").toString() : "");
				temList.add(obj.get("create_date") != null ? obj.get("create_date").toString() : "");
				list.put(obj.get("file_name") != null ? obj.get("file_name").toString() : "", temList);
			}

			System.out.println("Exist checksum::" + list);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return list;
	}

}
