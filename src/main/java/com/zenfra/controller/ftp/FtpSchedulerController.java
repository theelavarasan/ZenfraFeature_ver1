package com.zenfra.controller.ftp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zenfra.ftp.scheduler.Demo;
import com.zenfra.ftp.scheduler.ScheduleTaskService;
import com.zenfra.ftp.scheduler.SchedulerThread;
import com.zenfra.ftp.service.FtpSchedulerService;
import com.zenfra.model.ftp.FtpScheduler;

@Controller
public class FtpSchedulerController {

	@Autowired
	FtpSchedulerService schedulerService;

	@Autowired
	ScheduleTaskService scheduleTaskService;

	@PostMapping("/ftp-scheduler")
	public @ResponseBody String runScheduler(@RequestBody FtpScheduler ftpScheduler) {

		try {
			// https://www.freeformatter.com/cron-expression-generator-quartz.html
			if (ftpScheduler.getType().equalsIgnoreCase("hour")) {

				String corn = "0 0 */hour ? * *";				
				
				ftpScheduler.setSchedulerCorn(corn.replace("hour", ftpScheduler.getSchedulerCorn()));

			} else if (ftpScheduler.getType().equalsIgnoreCase("month")) {

				String corn = "0 0 0 month/1 * ?";
				ftpScheduler.setSchedulerCorn(corn.replace("month", ftpScheduler.getSchedulerCorn()));

			} else if (ftpScheduler.getType().equalsIgnoreCase("weekly")) {

				String corn = "0 0 0 ? * weekly"; // 0 0 0 ? * MON,WED,THU *

				ftpScheduler.setSchedulerCorn(corn.replace("weekly", ftpScheduler.getSchedulerCorn()));

			}

			long id=schedulerService.saveFtpScheduler(ftpScheduler);

			SchedulerThread r = new SchedulerThread(ftpScheduler);
			
			scheduleTaskService.addTaskToScheduler(id, r, ftpScheduler.getSchedulerCorn());

			
			return "saved! & Cron add to thread";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	@GetMapping("/ftp-scheduler")
	public @ResponseBody FtpScheduler getFtpScheduler(@RequestParam("id") Long id) {

		try {

			return schedulerService.getFtpScheduler(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@GetMapping("/ftp-scheduler-all")
	public @ResponseBody List<FtpScheduler> getFtpSchedulerAll() {

		try {

			return schedulerService.getFtpSchedulerAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	@PostMapping("/run-ftp")
	public @ResponseBody String runFtp(@RequestParam String corn,
			@RequestParam String name) {
		try {

			System.out.println("---------------------------enter-------------");
			Demo demo = new Demo(name);
				scheduleTaskService.addTaskToScheduler(1, demo, corn);
			
			return "done";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return "done";
	}

}
