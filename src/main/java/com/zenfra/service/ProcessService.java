package com.zenfra.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

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
	
}
