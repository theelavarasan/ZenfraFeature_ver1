
package com.zenfra.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.validation.constraints.NotEmpty;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.zenfra.Interface.IService;
import com.zenfra.configuration.RedisUtil;
import com.zenfra.dao.LogFileDetailsDao;
import com.zenfra.model.BaseModel;
import com.zenfra.model.LogFileDetails;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.Contants;

@Service
public class LogFileDetailsService implements IService<LogFileDetails> {

	Map<String, Map<String, Queue<BaseModel>>> siteQueueMap = new HashMap<String, Map<String, Queue<BaseModel>>>();
	Map<String, Queue<BaseModel>> logQueueMap = new HashMap<String, Queue<BaseModel>>();

	@Autowired
	LogFileDetailsDao logDao;

	@Autowired
	UserService userService;

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

	public List<LogFileDetails> getLogFileDetailsByLogids(List<String> logFileIds) {
		try {
			return logDao.getLogFileDetailsByLogids(logFileIds);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Object getLogFileDetailsBySiteKey(String siteKey) {
		try {

			Map<String, String> userList = userService.getUserNames();
			List<LogFileDetails> logFile = logDao.getLogFileDetailsBySiteKey(siteKey);
			List<LogFileDetails> logFileUpdate = new ArrayList<LogFileDetails>();
			for (LogFileDetails log : logFile) {
				if (userList.containsKey(log.getUploadedBy())) {
					log.setUploadedBy(userList.get(log.getUploadedBy()));
				}
				logFileUpdate.add(log);
			}

			return logFileUpdate;
		} catch (Exception e) {
			e.printStackTrace();
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
			}

		} catch (Exception e) {
			e.printStackTrace();
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
		}
		return logDao.save(logFile);
	}

	public boolean saveLogtypeAndDescription(List<String> logFileIds, String description, String logtype) {

		try {

			return logDao.saveLogtypeAndDescription(logFileIds, description, logtype);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean deleteLogfileProcessAction(List<String> logFileIds) {
		try {
			
			
			return logDao.deleteLogfileProcessAction(logFileIds);
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
