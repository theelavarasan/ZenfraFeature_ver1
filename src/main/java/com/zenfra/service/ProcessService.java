package com.zenfra.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.ProcessDao;
import com.zenfra.model.ftp.ProcessingStatus;
import com.zenfra.utils.DBUtils;

@Service
public class ProcessService {

	
	@Autowired
	ProcessDao dao;
	
	public void saveProcess(ProcessingStatus process) {
		
		try {
			
			dao.saveEntity(ProcessingStatus.class, process);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public ProcessingStatus getProcess(String id) {
		ProcessingStatus process=null;
		try {
			 process=(ProcessingStatus) dao.findEntityById(ProcessingStatus.class, id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return process;
	}

	public void updateMerge(ProcessingStatus status) {
	
		try {
			dao.updateEntity(ProcessingStatus.class, status);
		} catch (Exception e) {
				e.printStackTrace();
		}
	}

	public Object getFTPLogByServerId(String serverId) {
			try {
				
				String query="select * from processing_status where data_id=':data_id_value'";
					query=query.replace(":data_id_value", serverId);
				 return dao.getEntityListByColumn(query, ProcessingStatus.class);
			} catch (Exception e) {
				return e.getMessage();
			}
			
	}

	public void sentEmailFTP(JSONObject map) {
		try {
			
			Map<String,String> values=DBUtils.getEmailURL();
			JSONObject partObj = new JSONObject();
				partObj.put("templateName", values.get("ftp_template"));
				partObj.put("mailFrom", map.get("mailFrom"));
			List<String> mailToList = new ArrayList<>();
	                mailToList.addAll( (Collection<? extends String>) map.get("mailTo"));
	                partObj.put("mailTo", mailToList);
	                partObj.put("mailCc", new ArrayList<>());
	                partObj.put("mailBcc", new ArrayList<>());
	                partObj.put("mailSubject",map.get("subject"));
	                JSONObject modelJ = new JSONObject();
	                	modelJ.put("firstName", map.get("firstName"));
	                	modelJ.put("FTPname", map.get("FTPname"));
	                	modelJ.put("Time", map.get("Time"));
	                	modelJ.put("FileList", map.get("FileList"));
	            partObj.put("model", modelJ);
	                	
	              
				
				
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
