package com.zenfra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.ProcessDao;
import com.zenfra.model.ftp.ProcessingStatus;

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
	
}
