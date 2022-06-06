
package com.zenfra.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.Interface.IService;
import com.zenfra.configuration.RedisUtil;
import com.zenfra.dao.LogFileDetailsDao;
import com.zenfra.model.BaseModel;
import com.zenfra.model.LogFileDetails;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Contants;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class LogFileDetailsService implements IService<LogFileDetails> {

	Map<String, Map<String, Queue<BaseModel>>> siteQueueMap = new HashMap<String, Map<String, Queue<BaseModel>>>();
	Map<String, Queue<BaseModel>> logQueueMap = new HashMap<String, Queue<BaseModel>>();

	@Autowired
	LogFileDetailsDao logDao;

	@Autowired
	UserCreateService userCreateService;

	@Autowired
	CommonFunctions common;

	@Autowired
	RedisUtil redisUtil;

	@Override
	public LogFileDetails findOne(long id) {
		// TODO Auto-generated method stub
		return logDao.findOne(id);
	}

	@Override
	public List<LogFileDetails> findAll() {
		return logDao.findAll();
	}

	@Override
	public LogFileDetails save(LogFileDetails entity) {
		// TODO Auto-generated method stub
		return logDao.save(entity);
	}

	@Override
	public LogFileDetails update(LogFileDetails entity) {
		// TODO Auto-generated method stub
		return logDao.update(entity);
	}

	@Override
	public void delete(LogFileDetails entity) {
		logDao.delete(entity);

	}

	@Override
	public void deleteById(long entityId) {
		logDao.deleteById(entityId);

	}

	@Override
	public void deleteById(String entityId) {
		logDao.deleteById(entityId);

	}

	@Override
	public LogFileDetails findOne(String id) {
		// TODO Auto-generated method stub
		return logDao.findOne(id);
	}

	public JSONArray getLogFileDetailsByLogids(List<String> logFileIds) {
		JSONArray resultArray = new JSONArray();
		try {
			List<LogFileDetails> logFile = logDao.getLogFileDetailsByLogids(logFileIds);
			List<LogFileDetails> logFileUpdate = new ArrayList<LogFileDetails>();

			for (LogFileDetails log : logFile) {						
				if(log.getLogType() != null && !log.getLogType().trim().isEmpty() && (log.getLogType().equalsIgnoreCase("CUSTOM EXCEL DATA"))) {

					if(log.getStatus() != null && log.getStatus().equalsIgnoreCase("success")) {
						log.setStatus("import_success");
					}
				}
				JSONObject json = new JSONObject();
				json.put("status", log.getStatus());
				json.put("logFileId", log.getLogFileId());
				if(log.getStatus() != null && log.getStatus().equalsIgnoreCase("success")) {
					json.put("parsedDateTime", log.getParsedDateTime() != null? common.convertToUtc(TimeZone.getDefault(), log.getParsedDateTime()) : "");
				}
				resultArray.add(json);
			}

			return resultArray;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	public Object getLogFileDetailsBySiteKey(String siteKey, boolean fromZenfraCollector) {
		try {

			Map<String, String> userList = userCreateService.getUserNames();
			List<LogFileDetails> logFile = logDao.getLogFileDetailsBySiteKey(siteKey, fromZenfraCollector);
			List<LogFileDetails> logFileUpdate = new ArrayList<LogFileDetails>();
			for (LogFileDetails log : logFile) {
				if (userList.containsKey(log.getUploadedBy())) {
					log.setUploadedBy(userList.get(log.getUploadedBy()));
				}
				log.setCreatedDateTime(common.convertToUtc(TimeZone.getDefault(), log.getCreatedDateTime()));
				log.setUpdatedDateTime(common.convertToUtc(TimeZone.getDefault(), log.getUpdatedDateTime()));
				log.setParsedDateTime(common.convertToUtc(TimeZone.getDefault(), log.getParsedDateTime()));
				log.setParsingStartTime(common.convertToUtc(TimeZone.getDefault(), log.getParsingStartTime()));
				if (log.getLogType() != null && !log.getLogType().trim().isEmpty()
						&& (log.getLogType().equalsIgnoreCase("CUSTOM EXCEL DATA"))) {
					log.setCreatedDateTime(log.getCreatedDateTime());
					log.setUpdatedDateTime(log.getCreatedDateTime());
					log.setParsedDateTime(log.getCreatedDateTime());
					log.setParsingStartTime(log.getCreatedDateTime());
					if (log.getStatus() != null && log.getStatus().equalsIgnoreCase("success")) {
						log.setStatus("import_success");
					}
				}
				logFileUpdate.add(log);
			}

			return logFileUpdate;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	/******************************************
	 * parisng functions
	 **************************/

	public File getFilePath(String siteKey, String type, MultipartFile file) {
		try {
			String inputPath = ZKModel.getProperty(ZKConstants.INPUT_FOLDER);
			File inputFile = new File(inputPath);
			if (!inputFile.exists()) {
				inputFile.mkdirs();
			}

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
			LocalDateTime now = LocalDateTime.now();
			String finalFormattedDate = dtf.format(now).toString();

			String inputFilepath = inputPath + siteKey + "/" + ZKModel.getProperty(ZKConstants.UPLOADEDLOGS_PATH) + "/"
					+ type + "_" + finalFormattedDate + "_" + System.currentTimeMillis() + "/";
			File inputFolder = new File(inputFilepath);
			if (!inputFolder.exists()) {
				inputFolder.mkdirs();
			}
			System.out.println("!!!!! inputFilePath: " + inputFilepath);
			System.out.println("!!!!! file.getOriginalFilename(): " + file.getOriginalFilename());
			System.out.println("Log file path - " + inputFilepath + file.getOriginalFilename().replaceAll("\\s+", "_"));
			File convFile = new File(inputFilepath + file.getOriginalFilename().replaceAll("\\s+", "_"));
			convFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(convFile);
			fos.write(file.getBytes());
			fos.close();
			return convFile;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return null;
	}

	public String unZipFiles(String path, String siteKey) {
		System.out.println("@unZipFiles");
		String uncompressPath = ZKModel.getProperty(ZKConstants.UNCOMPRESS_PATH);
		System.out.println("uncompressPath::" + uncompressPath);
		JSONObject extractedData = new JSONObject();
		JSONParser parser = new JSONParser();
		String toReturnPathFromFile = path;
		try {
			if (path.endsWith(".zip") || path.endsWith(".tar") || path.endsWith(".tar.gz") || path.endsWith(".tar.Z")
					|| path.endsWith(".tgz") || path.endsWith(".gz") || path.endsWith(".rar") || path.endsWith(".7z")) {
				String uncompressExec = "sh " + uncompressPath + " " + path + " " + siteKey;
				System.out.println("UnCompress - Path - " + uncompressExec);
				String phpResultPath = common.execPHP(uncompressExec);
				extractedData = (JSONObject) parser.parse(phpResultPath);
				String status = extractedData.get("exit").toString();
				if (status.contains("0")) {
					toReturnPathFromFile = extractedData.get("file").toString();
					return toReturnPathFromFile;
				} else {
					throw new Exception("File not found");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return toReturnPathFromFile;
	}

	public JSONObject predictModel(String path, String siteKey) {
		JSONObject toRet = new JSONObject();
		System.out.println("@predictModel - " + path);
		String phpPredictPath = ZKModel.getProperty(ZKConstants.PREDICTION_PATH);
		try {
			String predictQuery = "php " + phpPredictPath + " -f " + path + " -s " + siteKey;
			System.out.println("Predict engin query - " + predictQuery);
			String predictResponse = execPHP(predictQuery);
			System.out.println("predictResponse - " + predictResponse);
			JSONParser parse = new JSONParser();

			try {
				toRet = (JSONObject) parse.parse(predictResponse);
				System.out.println("toRet - " + toRet);

			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return toRet;
	}

	public String execPHP(String scriptName) throws Exception {
		String FinOut = null;

		System.out.println("!!!!! ScriptName: " + scriptName);
		try {
			System.out.println("!!!!! execPHP 1 ");
			String line;
			System.out.println("!!!!! execPHP 1 ");
			StringBuilder output = new StringBuilder();
			System.out.println("!!!!! execPHP 1 ");
			Process p = Runtime.getRuntime().exec(scriptName);
			System.out.println("!!!!! execPHP 1 ");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			System.out.println("!!!!! execPHP 1 ");
			while ((line = input.readLine()) != null) {
				System.out.println("!!!!! line: " + line);
				output.append(line);
			}
			FinOut = output.toString();
			input.close();
			System.out.println("PHP file is working : ");
		} catch (Exception err) {
			err.printStackTrace();
			throw err;

		}
		return FinOut;
	}

	public List<LogFileDetails> parseExce(JSONObject responseJsonobject, JSONObject filePaths, String uploadAndProcess,
			LogFileDetails logFile, File convFile) {
		List<LogFileDetails> logFileIdList = new ArrayList<LogFileDetails>();
		try {

			JSONParser parser = new JSONParser();

			for (Object key : responseJsonobject.keySet()) {
				String type = key.toString().replace("-", "");
				String pathValue = responseJsonobject.get(key).toString();

				JSONArray pathList = (JSONArray) parser.parse(pathValue);
				System.out.println("pathList:: " + pathList);
				if (pathList.size() > 0) {
					for (int i = 0; i < pathList.size(); i++) {
						JSONObject jsonObject = (JSONObject) pathList.get(i);
						String pathFromObj = jsonObject.containsKey("predicted")
								? jsonObject.get("predicted").toString()
								: "";
						String compressedPath = jsonObject.containsKey("compressed")
								? jsonObject.get("compressed").toString()
								: convFile.getAbsolutePath();

						filePaths.put("compressedPath", compressedPath);
						filePaths.put("pathFromObj", pathFromObj);
						LogFileDetails temp = saveLogFileDetails(filePaths, logFile, convFile, uploadAndProcess,
								Contants.LOG_FILE_STATUS_DRAFT, Contants.LOG_FILE_STATUS_DRAFT, "File in draft");
						logFileIdList.add(temp);

					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return logFileIdList;
	}

	public LogFileDetails saveLogFileDetails(JSONObject filePaths, LogFileDetails logFile, File convFile,
			String uploadAndProcess, String status, String parsingStatus, String msg) {

		try {
			logFile.setFilePaths(filePaths.toJSONString());
			logFile.setMasterLogs(convFile.getAbsolutePath());
			logFile.setParsingStatus(uploadAndProcess.equalsIgnoreCase("true") ? Contants.LOG_FILE_STATUS_QUEUE
					: Contants.LOG_FILE_STATUS_DRAFT);
			logFile.setLogFileId(common.generateRandomId());
			logFile.setStatus(status);
			logFile.setParsingStatus(parsingStatus);
			logFile.setMessage(msg);
			logFile.setFileName(convFile.getName());
			logFile.setFileSize(String.valueOf(convFile.length()));

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return logDao.save(logFile);
	}

	public boolean saveLogtypeAndDescription(List<String> logFileIds, String description, String logtype) {

		try {

			return logDao.saveLogtypeAndDescription(logFileIds, description, logtype);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}

	}

	public boolean deleteLogfileProcessAction(List<String> logFileIds) {
		try {

			return logDao.deleteLogfileProcessAction(logFileIds);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
	}

	public Object getLogFileDetailedStatus(String logFileId) {
		try {

			Map<String, Object> map = logDao.getLogFileDetailedStatus(logFileId);

			if (map != null && map.containsKey("response")) {

				return new ObjectMapper().readTree(map.get("response").toString());

			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return null;
	}

	public int zipMultipleFile(List<String> path, String zipFile) throws FileNotFoundException {
		int fileLength = 0;
		try {
			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			// create byte buffer
			byte[] buffer = new byte[1024];

			for (int i = 0; i < path.size(); i++) {
				File srcFile = new File(path.get(i));

				if (!srcFile.exists()) {
					System.out.println("File Not Found::" + path.get(i));
					continue;
				}

				fileLength += srcFile.length();
				FileInputStream fis = new FileInputStream(srcFile);
				// begin writing a new ZIP entry, positions the stream to the start of the entry
				// data
				zos.putNextEntry(new ZipEntry(srcFile.getName()));
				int length;
				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}
				zos.closeEntry();
				// close the InputStream
				fis.close();
			}
			// close the ZipOutputStream
			zos.close();
		} catch (IOException ioe) {
			System.out.println("Error creating zip file: " + ioe);
		}
		return fileLength;
	}

	public List<LogFileDetails> findAllByLogFileIds(List<String> modelName) {
		try {

			return logDao.findAllByLogFileIds(modelName);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	public Object getFileLogCount(String siteKey) {
		try {

			return logDao.getFileLogCount(siteKey);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return 0;
		}
	}

	public JSONArray getParsedLogFileDetailsBySiteKey(String siteKey) {
		JSONArray jsonArray = new JSONArray();
		try {
			List<String> logFile = logDao.getLogFileDetailsBySiteKeyAndStatusIsActive(siteKey);
			for (String log : logFile) {
				JSONObject json = new JSONObject();
				json.put("id", log.toLowerCase());
				json.put("name", log.toUpperCase());
				jsonArray.add(json);
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return jsonArray;
	}
}
