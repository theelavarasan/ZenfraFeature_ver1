package com.zenfra.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.postgresql.replication.fluent.CommonOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.Interface.IDao;
import com.zenfra.Interface.IGenericDao;
import com.zenfra.dao.common.JdbcCommonOperations;
import com.zenfra.ftp.repo.LogFileDetailsRepo;
import com.zenfra.model.LogFileDetails;

import lombok.extern.apachecommons.CommonsLog;

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
			return null;
		}
	}

	@Override
	public List<LogFileDetails> findAll() {
		try {			
			return dao.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public LogFileDetails save(LogFileDetails entity) {
		try {			
			return dao.save(entity);					
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public LogFileDetails update(LogFileDetails entity) {
		try {			
			return dao.update(entity);					
		} catch (Exception e) {
			e.printStackTrace();
			return entity;
		}
	}

	@Override
	public void delete(LogFileDetails entity) {
		try {
			dao.delete(entity);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void deleteById(long entityId) {
		try {
			dao.deleteById(entityId);
		} catch (Exception e) {
			e.printStackTrace();
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
		}
		
	}

	
	public List<LogFileDetails> getLogFileDetailsByLogids(List<String> logFileIds){				
		try {
			return logRepo.findByLogFileIdIn(logFileIds);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<LogFileDetails> getLogFileDetailsBySiteKey(String siteKey) {
		 List<LogFileDetails> log=new ArrayList<LogFileDetails>();
		try {
			
			log=logRepo.getBySiteKeyAndIsActive(siteKey,true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return log;
	}

	public boolean saveLogtypeAndDescription(List<String> logFileIds, String description, String logtype) {
		
		try {
			
			logRepo.saveLogtypeAndDescription(logFileIds,description,logtype);
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean deleteLogfileProcessAction(List<String> logFileIds) {
		try {
			
			logRepo.updateLogFileIdsActive(logFileIds);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public List<LogFileDetails> findAllByLogFileIds(List<String> logFileIds) {
		 List<LogFileDetails> log=new ArrayList<LogFileDetails>();
			try {
				
				log=logRepo.findByLogFileIdIn(logFileIds);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return log;
	}

	public Map<String, Object> getLogFileDetailedStatus(String logFileId) {
		try {
			String query="select cmd_status_parsing, json_agg(response) as response from (\r\n" + 
					"select cmd_status_parsing, json_build_object('serverName', parsing_server_name, 'fileName', file_name, 'parsedStatus', parsed_status, 'insertionStatus', insertion_status,\r\n" + 
					"'type','') as response from (\r\n" + 
					"select cmd_status_parsing, parsing_server_name, parsing_keys, coalesce(file_name, '') as file_name, coalesce(parsed_status, 'N/A') as parsed_status,\r\n" + 
					"coalesce(insertion_status, 'N/A') as insertion_status from (\r\n" + 
					"select cmd_status_parsing, parsing_server_name, parsing_keys, json_array_elements(parsing_details::json) ->> 'fileName' as file_name,\r\n" + 
					"json_array_elements(parsing_details::json) ->> 'parsedStatus' as parsed_status, ins.insertion_status from (\r\n" + 
					"select cmd_status_parsing, parsing_server_name, parsing_keys, json_array_elements(concat('[', parsing_details, ']')::json) ->> parsing_keys as parsing_details from (\r\n" + 
					"select cmd_status_parsing, parsing_server_name, parsing_details, json_object_keys(parsing_details::json) as parsing_keys from (\r\n" + 
					"select cmd_status_parsing, parsing_server_name, json_array_elements(concat('[',cmd_status_parsing,']')::json) ->> parsing_server_name as parsing_details from (\r\n" + 
					"select cmd_status_parsing, json_object_keys(cmd_status_parsing::json) as parsing_server_name\r\n" + 
					"from log_file_details where log_file_id = ':log_fil_id'\r\n" + 
					")a\r\n" + 
					")b\r\n" + 
					")c\r\n" + 
					")d\r\n" + 
					"LEFT JOIN (select cmd_status_insertion, insertion_server_name, insertion_keys,\r\n" + 
					"(case when insertion_status = 'failed' and insertion_message ilike '%duplicate%' then 'success' else insertion_status end) as insertion_status from (\r\n" + 
					"select cmd_status_insertion, insertion_server_name, insertion_keys,\r\n" + 
					"json_array_elements(concat('[', insertion_details, ']')::json) ->> 'insertionStatus' as insertion_status,\r\n" + 
					"json_array_elements(concat('[', insertion_details, ']')::json) ->> 'message' as insertion_message from (\r\n" + 
					"select cmd_status_insertion, insertion_server_name, insertion_keys,\r\n" + 
					"json_array_elements(concat('[', insertion_details, ']')::json) ->> insertion_keys as insertion_details from (\r\n" + 
					"select cmd_status_insertion, insertion_server_name, insertion_details, json_object_keys(insertion_details::json) as insertion_keys from (\r\n" + 
					"select cmd_status_insertion, insertion_server_name, json_array_elements(concat('[',cmd_status_insertion,']')::json) ->> insertion_server_name as insertion_details from (\r\n" + 
					"select cmd_status_insertion, json_object_keys(cmd_status_insertion::json) as insertion_server_name\r\n" + 
					"from log_file_details where log_file_id = ':log_fil_id'\r\n" + 
					")a\r\n" + 
					")b\r\n" + 
					")c\r\n" + 
					")d\r\n" + 
					")e) ins on ins.insertion_keys = d.parsing_keys\r\n" + 
					")f\r\n" + 
					")g\r\n" + 
					")h group by cmd_status_parsing";
			
			query=query.replace(":log_fil_id", logFileId);
			
			System.out.println("I information query::"+query);
			List<Map<String,Object>> response=getObjectFromQuery(query);
			
			if(response!=null && response.size()>0) {
				return response.get(0);
			}
					
			
			
		} catch (Exception e) {
			e.printStackTrace();			
		}
		return null;
	}


	
	
	
	
}
