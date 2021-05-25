package com.zenfra.ftp.scheduler;

import org.springframework.beans.factory.annotation.Autowired;

import com.zenfra.ftp.service.FtpSchedulerService;
import com.zenfra.model.ftp.FtpScheduler;

public class  SchedulerThread implements Runnable{

	@Autowired
	FtpSchedulerService schedulerService;
	
	
	FtpScheduler s;
	
	public  SchedulerThread(FtpScheduler s) {		
		this.s=s;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("------corn------Thread start-----------------");
		FtpSchedulerService schedulerService=new FtpSchedulerService();
		System.out.println("----FtpScheduler---"+s.getFileNameSettingsId());
		schedulerService.runFtpSchedulerFiles(s);
	}

}
