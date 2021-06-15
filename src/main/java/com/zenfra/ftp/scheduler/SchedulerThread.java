package com.zenfra.ftp.scheduler;

import org.springframework.stereotype.Component;

import com.zenfra.ftp.service.FtpSchedulerService;
import com.zenfra.model.ftp.FtpScheduler;

import lombok.extern.apachecommons.CommonsLog;


public class  SchedulerThread implements Runnable{

	
	
	FtpScheduler s;
	
	public  SchedulerThread(FtpScheduler s) {		
		this.s=s;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("------corn------Thread start-----------------");
		System.out.println(s.toString());
		FtpSchedulerService schedulerService=new FtpSchedulerService();
		System.out.println("----FtpScheduler---"+s.getFileNameSettingsId());
		schedulerService.runFtpSchedulerFiles(s);
	}

}
