package com.zenfra.dao;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.Interface.IDao;
import com.zenfra.Interface.IGenericDao;
import com.zenfra.dao.common.JdbcCommonOperations;
import com.zenfra.ftp.repo.LogFileDetailsRepo;
import com.zenfra.model.LogFileDetails;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class LogFileDetailsDao extends JdbcCommonOperations implements IDao<LogFileDetails> {

	@Autowired
	LogFileDetailsRepo logRepo;

	IGenericDao<LogFileDetails> dao;

	@Autowired
	public void setDao(IGenericDao<LogFileDetails> daoToSet) {
		dao = daoToSet;
		dao.setClazz(LogFileDetails.class);
	}

	@Override
	public LogFileDetails findOne(long id) {
		try {
			return dao.findOne(id);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	@Override
	public List<LogFileDetails> findAll() {
		try {
			return dao.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	@Override
	public LogFileDetails save(LogFileDetails entity) {
		try {
			return dao.save(entity);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return entity;
		}
	}

	@Override
	public LogFileDetails update(LogFileDetails entity) {
		try {
			return dao.update(entity);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return entity;
		}
	}

	@Override
	public void delete(LogFileDetails entity) {
		try {
			dao.delete(entity);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	@Override
	public void deleteById(long entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	@Override
	public LogFileDetails findOne(String id) {
		// TODO Auto-generated method stub
		return dao.findOne(id);
	}

	@Override
	public void deleteById(String entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	public List<LogFileDetails> getLogFileDetailsByLogids(List<String> logFileIds) {
		try {
			return logRepo.findByLogFileIdIn(logFileIds);
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
	public List<LogFileDetails> getLogFileDetailsBySiteKey(String siteKey, boolean fromZenfraCollector) {
		List<LogFileDetails> log = new ArrayList<LogFileDetails>();
		try {
			JSONArray dataArray = new JSONArray();
			if (fromZenfraCollector) {

				log = logRepo.getByFromZenfraCollector(siteKey, true);

				System.out.println("-------log-----" + log);
			} else {
				log = getFileDetails(siteKey, true);
				//log = logRepo.getBySiteKeyAndIsActive(siteKey, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return log;
	}

	@SuppressWarnings("unchecked")
	public List<LogFileDetails> getFileDetails(String siteKey, boolean isActive) {
		
		
		List<LogFileDetails> log = new ArrayList<LogFileDetails>();
		
		Map<String, String> data = new HashMap<>();
		data = DBUtils.getPostgres();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {

			String selectQuery = "select *, \r\n" + "(case when log_type ilike 'zoom' then concat(file_name, '-', \r\n"
					+ "to_char(to_timestamp(updated_date_time, 'yyyy-mm-dd hh24:mi:ss')::timestamp, 'mm-dd-yyyy hh24:mi:ss')) \r\n"
					+ "else file_name end) as file_name2 from log_file_details \r\n" + "where is_active= " + isActive
					+ " and  site_key= '" + siteKey + "'\r\n"
					+ "order by to_timestamp(updated_date_time, 'yyyy-mm-dd hh24:mi:ss') \r\n" + "desc";

			System.out.println("!!!SelectQuery: " + selectQuery);
			ResultSet rs = statement.executeQuery(selectQuery);

			while (rs.next()) {
				
				LogFileDetails logData = new LogFileDetails();

				logData.setCreatedDateTime(rs.getString("created_date_time"));
				logData.setDescription(rs.getString("description"));
				logData.setExtractedPath(rs.getString("extracted_path"));
				logData.setFileName(rs.getString("file_name"));
				logData.setFileSize(rs.getString("file_size"));
				logData.setLogType(rs.getString("log_type"));
				logData.setMasterLogs(rs.getString("master_logs"));
				logData.setResponse(rs.getString("response"));
				logData.setSiteKey(rs.getString("site_key"));
				logData.setStatus(rs.getString("status"));
				logData.setTenantId(rs.getString("tenant_id"));
				logData.setUpdatedDateTime(rs.getString("updated_date_time"));
				logData.setUploadedBy(rs.getString("uploaded_by"));
				logData.setUploadedLogs(rs.getString("uploaded_logs"));
				logData.setCmdStatusInsertion(rs.getString("Cmd_status_insertion"));
				logData.setCmdStatusParsing(rs.getString("Cmd_status_parsing"));
				logData.setMessage(rs.getString("message"));
				logData.setParsedDateTime(rs.getString("parsed_date_time"));
				logData.setParsingStartTime(rs.getString("parsing_start_time"));
				logData.setParsingStatus(rs.getString("parsing_status"));
				logData.setTempStatus(rs.getString("temp_status"));
				logData.setLogId(rs.getString("log_id"));
				logData.setLogFileId(rs.getString("log_file_id"));
				logData.setFilePaths(rs.getString("file_paths"));
				logData.setFileName(rs.getString("file_name"));
				
				
//				dataObj.put("createdDateTime", rs.getString("created_date_time"));
//				dataObj.put("description", rs.getString("description"));
//				dataObj.put("extractedPath", rs.getString("extracted_path"));
//				dataObj.put("fileName", rs.getString("file_name"));
//				dataObj.put("fileSize", rs.getString("file_size"));
//				dataObj.put("isActive", rs.getBoolean("is_active"));
//				dataObj.put("logType", rs.getString("log_type"));
//				dataObj.put("masterLogs", rs.getString("master_logs"));
//				dataObj.put("response", rs.getString("response"));
//				dataObj.put("siteKey", rs.getString("site_key"));
//				dataObj.put("status", rs.getString("status"));
//				dataObj.put("tenantId", rs.getString("tenant_id"));
//				dataObj.put("updatedDateTime", rs.getString("updated_date_time"));
//				dataObj.put("uploadedBy", rs.getString("uploaded_by"));
//				dataObj.put("uploadedLogs", rs.getString("uploaded_logs"));
//				dataObj.put("CmdStatusInsertion", rs.getString("Cmd_status_insertion"));
//				dataObj.put("CmdStatusParsing", rs.getString("Cmd_status_parsing"));
//				dataObj.put("message", rs.getString("message"));
//				dataObj.put("parsedDateTime", rs.getString("parsed_date_time"));
//				dataObj.put("parsingStartTime", rs.getString("parsing_start_time"));
//				dataObj.put("parsing_status", rs.getString("parsing_status"));
//				dataObj.put("tempStatus", rs.getString("temp_status"));
//				dataObj.put("logId", rs.getString("log_id"));
//				dataObj.put("filePaths", rs.getString("file_paths"));
//				dataObj.put("fileName2", rs.getString("file_name2"));
//				response.add(rs.getString("file_name2"));
//				System.out.println("---res---"+response);
				
				log.add(logData);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return log;
	}

	public boolean saveLogtypeAndDescription(List<String> logFileIds, String description, String logtype) {

		try {

			logRepo.saveLogtypeAndDescription(logFileIds, description, logtype);

			return true;
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

			logRepo.updateLogFileIdsActive(logFileIds);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return false;
		}
	}

	public List<LogFileDetails> findAllByLogFileIds(List<String> logFileIds) {
		List<LogFileDetails> log = new ArrayList<LogFileDetails>();
		try {

			log = logRepo.findByLogFileIdIn(logFileIds);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return log;
	}

	public Map<String, Object> getLogFileDetailedStatus(String logFileId) {
		try {

			String query = "select cmd_status_parsing, json_agg(response) as response from (\r\n"
					+ "select cmd_status_parsing, json_build_object('serverName', parsing_server_name, 'fileName', file_name, 'parsedStatus', parsed_status, 'insertionStatus', insertion_status,\r\n"
					+ "'type','') as response from (\r\n"
					+ "select cmd_status_parsing, parsing_server_name, parsing_keys, file_name,\r\n"
					+ "(case when insertion_status = 'N/A' then 'No data available in file' else parsed_status end) as parsed_status, insertion_status from (\r\n"
					+ "select cmd_status_parsing, parsing_server_name, parsing_keys, coalesce(file_name, '') as file_name, coalesce(parsed_status, 'N/A') as parsed_status,\r\n"
					+ "coalesce(insertion_status, 'N/A') as insertion_status from (\r\n"
					+ "select cmd_status_parsing, parsing_server_name, parsing_keys, json_array_elements(parsing_details::json) ->> 'fileName' as file_name,\r\n"
					+ "json_array_elements(parsing_details::json) ->> 'parsedStatus' as parsed_status, ins.insertion_status from (\r\n"
					+ "select cmd_status_parsing, parsing_server_name, parsing_keys, json_array_elements(concat('[', parsing_details, ']')::json) ->> parsing_keys as parsing_details from (\r\n"
					+ "select cmd_status_parsing, parsing_server_name, parsing_details, json_object_keys(parsing_details::json) as parsing_keys from (\r\n"
					+ "select cmd_status_parsing, parsing_server_name, json_array_elements(concat('[',cmd_status_parsing,']')::json) ->> parsing_server_name as parsing_details from (\r\n"
					+ "select cmd_status_parsing, json_object_keys(cmd_status_parsing::json) as parsing_server_name\r\n"
					+ "from log_file_details where log_file_id =':log_file_id' \r\n" + ")a\r\n" + ")b\r\n" + ")c\r\n"
					+ ")d\r\n" + "LEFT JOIN (select cmd_status_insertion, insertion_server_name, insertion_keys,\r\n"
					+ "(case when insertion_status = 'failed' and insertion_message ilike '%duplicate%' then 'success' else insertion_status end) as insertion_status from (\r\n"
					+ "select cmd_status_insertion, insertion_server_name, insertion_keys,\r\n"
					+ "json_array_elements(concat('[', insertion_details, ']')::json) ->> 'insertionStatus' as insertion_status,\r\n"
					+ "json_array_elements(concat('[', insertion_details, ']')::json) ->> 'message' as insertion_message from (\r\n"
					+ "select cmd_status_insertion, insertion_server_name, insertion_keys,\r\n"
					+ "json_array_elements(concat('[', insertion_details, ']')::json) ->> insertion_keys as insertion_details from (\r\n"
					+ "select cmd_status_insertion, insertion_server_name, insertion_details, json_object_keys(insertion_details::json) as insertion_keys from (\r\n"
					+ "select cmd_status_insertion, insertion_server_name, json_array_elements(concat('[',cmd_status_insertion,']')::json) ->> insertion_server_name as insertion_details from (\r\n"
					+ "select cmd_status_insertion, json_object_keys(cmd_status_insertion::json) as insertion_server_name\r\n"
					+ "from log_file_details where log_file_id = ':log_file_id' \r\n" + ")a\r\n" + ")b\r\n" + ")c\r\n"
					+ ")d\r\n" + ")e) ins on ins.insertion_keys = d.parsing_keys\r\n" + ")f\r\n" + ")g\r\n" + ")g1\r\n"
					+ ")h group by cmd_status_parsing";

			query = query.replace(":log_file_id", logFileId);

			System.out.println("I information query::" + query);
			List<Map<String, Object>> response = getObjectFromQuery(query);

			if (response != null && response.size() > 0) {
				return response.get(0);
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

	public Object getFileLogCount(String siteKey) {
		try {

			return logRepo.countBySiteKey(siteKey);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return 0;
		}
	}

	public List<String> getLogFileDetailsBySiteKeyAndStatusIsActive(String siteKey) {
		List<String> log = new ArrayList<String>();
		try {

			log = logRepo.getBySiteKeyAndStatusIsActive(siteKey, true, "success");
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return log;
	}

}
