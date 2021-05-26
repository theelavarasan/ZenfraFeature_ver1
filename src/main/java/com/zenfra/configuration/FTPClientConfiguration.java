package com.zenfra.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Component;

import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileUploadStatus;
import com.zenfra.model.ftp.FileWithPath;

@Component
public class FTPClientConfiguration {


	public static FTPClient loginClient(FTPServerModel server) {

		try {
			FTPClient ftpClient = new FTPClient();			
			int port = Integer.parseInt(server.getPort());
			ftpClient.connect(server.getIpAddress(), port);

			System.out.println(ftpClient.getReplyString());

			ftpClient.sendCommand(FTPCommand.USER, server.getServerUsername());

			System.out.println(ftpClient.getReplyString());

			ftpClient.sendCommand(FTPCommand.PASS, server.getServerPassword());
			System.out.println(ftpClient.getReplyString());

			return ftpClient;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String testConenction(FTPServerModel server) {
		try {
			System.out.println(server.getIpAddress()+":"+Integer.parseInt(server.getPort()));
			FTPClient ftpClient = new FTPClient();
			ftpClient.connect(server.getIpAddress(), Integer.parseInt(server.getPort()));

			boolean ftpChk = ftpClient.login(server.getServerUsername(), server.getServerPassword());
			 
			 System.out.println("FTP client Code:: "+ftpChk);
			 if (!ftpChk) {
			     return "Test Connection Failed!";
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

	public static List<FileWithPath> getFilesFromPath(FTPServerModel server, String path) {

		try {

			List<FileWithPath> fileList = new ArrayList<FileWithPath>();

			
			FTPClient ftpClient = getConnection(server);
			
			ftpClient.enterLocalPassiveMode();
			ftpClient.changeWorkingDirectory(path);
			FTPFile[] files = ftpClient.listFiles();
			 MessageDigest md5Digest = MessageDigest.getInstance("MD5");
			 String chkSumFTP = null;
			// ftpClient.g
			for (FTPFile file : files) {
				String details = file.getName();
				 File fileForChkSum = new File(path + "/" + details);
				 ftpClient.enterLocalPassiveMode();
				 InputStream iStream=ftpClient.retrieveFileStream(path + "/" + details);
				 if(iStream!=null) {
				 //File file1 = File.createTempFile("tmp", path + "/" + details);
				 File file1 = File.createTempFile("tmp", null);
				 System.out.println("File1:: "+file1);
				 FileUtils.copyInputStreamToFile(iStream, file1);
				 chkSumFTP =getFileChecksum(md5Digest, file1);
				 file1.delete();
				 iStream.close();
				 ftpClient.completePendingCommand();
				 }			 
				/*
				 * if (file.isDirectory()) { details = "[" + details + "]"; }
				 */
				FileWithPath path1 = new FileWithPath();
				path1.setPath(path + "/" + details);
				path1.setName(details);
				path1.setCheckSum(chkSumFTP);
				fileList.add(path1);
			}
			 ftpClient.logout();
			 ftpClient.disconnect();
			return fileList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getFileFromFtp(FTPServerModel server, String path, String toPath, String fileName) {

		try {


			FTPClient	ftpClient = getConnection(server);
			

			ftpClient.changeWorkingDirectory(path);
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.enterLocalPassiveMode();

			/*FTPFile[] files = ftpClient.listFiles();

			// ftpClient.g
			for (FTPFile file : files) {
				String details = file.getName();
				if (file.isDirectory()) {
					details = "[" + details + "]";
				}
				System.out.println(details);
			}

			
			  try (FileOutputStream fos = new FileOutputStream(
			  "/home/vtg-admin/Desktop/aravind/Pure-Collection-Commands.txt")) {
			 ftpClient.retrieveFile("Pure-Collection-Commands.txt", fos); }
			 */
			toPath = toPath + "/" + fileName;
			try (FileOutputStream fos = new FileOutputStream(toPath)) {
				ftpClient.retrieveFile(fileName, fos);
			} catch (IOException e) {
				e.printStackTrace();
				return e.getMessage().toString();
			}

			String str=ftpClient.getReplyString();
			ftpClient.logout();
			
			ftpClient.disconnect();
			return str;
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage().toString();
		}

	}
	public static List<FileUploadStatus> getFileFromFtpChange(FTPServerModel server, String fromPath, String toPath) {
		List<FileUploadStatus> statusList=new ArrayList<FileUploadStatus>();
		try {
			FTPClient	ftpClient = getConnection(server);
			String[] files=fromPath.split(",");
			
			for(String s:files) {
				///home/FTP-Logs/PureFlashArrayVTGNew.log
				String[] file=s.split("/");
					String fileName=file[file.length-1];
						String path=s.replace(fileName,"" );
						FileUploadStatus status=new FileUploadStatus();
						
						

						ftpClient.changeWorkingDirectory(path);
						ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
						ftpClient.enterLocalPassiveMode();

						/*FTPFile[] files = ftpClient.listFiles();

						// ftpClient.g
						for (FTPFile file : files) {
							String details = file.getName();
							if (file.isDirectory()) {
								details = "[" + details + "]";
							}
							System.out.println(details);
						}

						
						  try (FileOutputStream fos = new FileOutputStream(
						  "/home/vtg-admin/Desktop/aravind/Pure-Collection-Commands.txt")) {
						 ftpClient.retrieveFile("Pure-Collection-Commands.txt", fos); }
						 */
						String copyPath = toPath + "/" + fileName;
						try (FileOutputStream fos = new FileOutputStream(copyPath)) {
							ftpClient.retrieveFile(fileName, fos);
						} catch (IOException e) {
							e.printStackTrace();
							return null;
						}

						String str=ftpClient.getReplyString();
						
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
	public static List<FileWithPath> getAllFilesFromPath(FTPServerModel server, String path){
		
		 List<FileWithPath> files=new ArrayList<FileWithPath>();
		try {
			FTPClient ftpClient = getConnection(server);

			ftpClient.enterLocalPassiveMode();
			files.addAll(getAllFilesFromPath(ftpClient, path, "", 0, server, files));
			ftpClient.logout();
			ftpClient.disconnect();
			return files;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static List<FileWithPath> getAllFilesFromPath(
			FTPClient ftpClient, String parentDir,
            String currentDir, int level,
            FTPServerModel server, 
			List<FileWithPath> fileList) throws IOException {

		try {
			 String dirToList = parentDir;
		        if (!currentDir.equals("")) {
		            dirToList += "/" + currentDir;
		        }
			ftpClient.changeWorkingDirectory(dirToList);
			FTPFile[] files = ftpClient.listFiles();

			// ftpClient.g
			/*for (FTPFile file : files) {
				String details = file.getName();
				FileWithPath path1 = new FileWithPath();
				path1.setPath(path + "/" + file.getName());
				path1.setName(file.getName());
				System.out.println("------file----"+file.getName());
				fileList.add(path1);
	            
				
				
			}*/
			
			for (FTPFile aFile : files) {
	            String currentFileName = aFile.getName();
	            if (currentFileName.equals(".")
	                    || currentFileName.equals("..")) {
	                // skip parent directory and directory itself
	                continue;
	            }
	            for (int i = 0; i < level; i++) {
	                System.out.print("\t");
	            }
	            if (aFile.isDirectory()) {
	                System.out.println("[" + currentFileName + "]");
	                getAllFilesFromPath(ftpClient, parentDir, currentFileName, level, server, fileList);
	            } else {
	                System.out.println(currentFileName);
	                String details = aFile.getName();
					FileWithPath path1 = new FileWithPath();
					path1.setPath(dirToList + "/" + aFile.getName());
					path1.setName(aFile.getName());
				
	                fileList.add(path1);
	            }
	        }

			//getFilesFromSubFolder(ftpClient)
			
			return fileList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public static void main(String[] args) {
		String server = "192.168.1.10";
		int port = 21;
		String user = "Test";
		String pass = "ZENfra@123$";

		FTPClient ftpClient = new FTPClient();

		try {
			ftpClient.connect(server, port);

			System.out.println(ftpClient.getReplyString());

			ftpClient.sendCommand(FTPCommand.USER, user);

			System.out.println(ftpClient.getReplyString());

			ftpClient.sendCommand(FTPCommand.PASS, pass);
			System.out.println(ftpClient.getReplyString());

			System.out.println(ftpClient.getReplyString());

			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.enterLocalPassiveMode();

			ftpClient.changeWorkingDirectory("/home/FTP-Logs");
			FTPFile[] files = ftpClient.listFiles();

			// ftpClient.g
			for (FTPFile file : files) {
				String details = file.getName();
				if (file.isDirectory()) {
					details = "[" + details + "]";
				}
				System.out.println(details);
			}

			/*
			 * ftpClient.changeWorkingDirectory("/home");
			 * System.out.println(ftpClient.getReplyString());
			 * 
			 * 
			 * FTPFile[] files = ftpClient.listFiles();
			 * 
			 * 
			 * //ftpClient.g for (FTPFile file : files) { String details = file.getName();
			 * if (file.isDirectory()) { details = "[" + details + "]"; }
			 * System.out.println(details); }
			 * 
			 * System.out.println(ftpClient.getReplyString());
			 * 
			 * ftpClient.sendCommand(FTPCmd.CWD, "Upload");
			 * 
			 * System.out.println(ftpClient.getReplyString());
			 * 
			 * ftpClient.sendCommand(FTPCmd.MKD, "CodeJava");
			 * 
			 * ftpClient.sendCommand(FTPCmd.MLSD, pass);
			 * 
			 * System.out.println(ftpClient.getReplyString());
			 * 
			 */
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}
	private static String getFileChecksum(MessageDigest digest, File file) throws IOException
	{
	    //Get file input stream for reading the file content
	    FileInputStream fis = new FileInputStream(file);
	     
	    //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0; 
	      
	    //Read file data and update in message digest
	    while ((bytesCount = fis.read(byteArray)) != -1) {
	        digest.update(byteArray, 0, bytesCount);
	    };
	     
	    //close the stream; We don't need it now.
	    fis.close();
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
	     
	    //This bytes[] has bytes in decimal format;
	    //Convert it to hexadecimal format
	    StringBuilder sb = new StringBuilder();
	    for(int i=0; i< bytes.length ;i++)
	    {
	        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
	     
	    //return complete hash
	   return sb.toString();
	}
}
