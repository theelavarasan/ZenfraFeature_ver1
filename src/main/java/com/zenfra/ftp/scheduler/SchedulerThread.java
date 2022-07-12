package com.zenfra.ftp.scheduler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import com.zenfra.ftp.service.FtpSchedulerService;
import com.zenfra.model.ftp.FtpScheduler;
import com.zenfra.utils.ExceptionHandlerMail;

public class SchedulerThread implements Runnable {

	FtpScheduler s;

	public SchedulerThread(FtpScheduler s) {
		this.s = s;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("------corn------Thread start-----------------");
		System.out.println(s.toString());
		FtpSchedulerService schedulerService = new FtpSchedulerService();
		System.out.println("----FtpScheduler---" + s.getFileNameSettingsId());
		try {
			if (s.getIsNas()) {
				System.out.println("-------Nas-Scheduler---" + s.getIsNas());
				schedulerService.parseNasFiles(s);
			} else {
				schedulerService.runFtpSchedulerFiles(s);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
	}

}
