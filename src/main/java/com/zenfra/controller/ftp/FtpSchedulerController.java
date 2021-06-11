package com.zenfra.controller.ftp;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.ftp.scheduler.ScheduleTaskService;
import com.zenfra.ftp.scheduler.SchedulerThread;
import com.zenfra.ftp.service.FtpSchedulerService;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.model.ftp.FtpScheduler;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/ftpScheduler")
@Validated
public class FtpSchedulerController {

	@Autowired
	FtpSchedulerService schedulerService;

	@Autowired
	ScheduleTaskService scheduleTaskService;

	
	@PostMapping("/runScheduler")
	public @ResponseBody String runScheduler(@Valid @RequestBody FtpScheduler ftpScheduler) {

		try {
			
			if (ftpScheduler.getId()==0) {
				FtpScheduler temp = schedulerService.getFtpScheduler(ftpScheduler.getFileNameSettingsId());
				if(temp!=null) {
					return "Scheduler already agains the given filename settings";
				}
				
			}

			if (ftpScheduler.getType().equalsIgnoreCase("hour")) {
				String corn = "0 0 0/hour ? * * ";
				ftpScheduler.setSchedulerCorn(corn.replace("hour", ftpScheduler.getTime()));
			} else if (ftpScheduler.getType().equalsIgnoreCase("daily")) {
				String timseslot = ftpScheduler.getTimeSlot().replace(" ", "").replace("AM", "").replace("PM", "").replace("IST", "");
				String corn = "0 0 from-two today/everyday * ? ";
				corn = corn.replace("from-two", timseslot).replace("everyday", ftpScheduler.getTime()).replace("today",
						String.valueOf(new Date().getDate()));
				ftpScheduler.setSchedulerCorn(corn);
			} else if (ftpScheduler.getType().equalsIgnoreCase("month")) {
				String corn = "0 0 0 month/1 * ?";
				ftpScheduler.setSchedulerCorn(corn.replace("month", ftpScheduler.getTime()));
			} else if (ftpScheduler.getType().equalsIgnoreCase("weekly")) {
				String days = "";
				for (int i = 0; i < ftpScheduler.getSelectedDay().size(); i++) {
					days += ftpScheduler.getSelectedDay().get(i).toString().substring(0, 3).toUpperCase() + ",";
				}
				days = days.substring(0, days.length() - 1);
				String corn = "0 0 0 ? * weekly"; // 0 0 0 ? * MON,WED,THU *
				ftpScheduler.setSchedulerCorn(corn.replace("weekly", days));
			}
			System.out.println(ftpScheduler.getSchedulerCorn());
			ftpScheduler.setActive(true);
			long id = schedulerService.saveFtpScheduler(ftpScheduler);

			SchedulerThread r = new SchedulerThread(ftpScheduler);

			scheduleTaskService.addTaskToScheduler(id, r, ftpScheduler.getSchedulerCorn(),ftpScheduler.getTimeZone());

			return "saved! & Cron add to thread";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	@GetMapping("/ftp-scheduler")
	public ResponseModel_v2 getFtpScheduler(
			@NotEmpty(message = "Please provide valid fileNameSettingsId") @RequestParam("fileNameSettingsId") String fileNameSettingsId) {

		ResponseModel_v2 response = new ResponseModel_v2();
		try {

			response.setjData(schedulerService.getFtpScheduler(fileNameSettingsId));
			response.setResponseCode(HttpStatus.OK);
			response.setResponseMessage("Got the schedular Details Successfully");

		} catch (Exception e) {
			e.printStackTrace();
			response.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			response.setResponseMessage("Getting exception in Saving File name Settings: " + e.getMessage());

		}
		return response;
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

	@PostMapping("/run-sample")
	public Object run(@RequestParam String fileNameSettingsId) {

		try {
			FtpScheduler ftp = schedulerService.getFtpScheduler(fileNameSettingsId);

			return schedulerService.runFtpSchedulerFiles(ftp);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	
	@PostMapping("/run-ftp")
	public @ResponseBody String runFtp(@RequestParam String corn,
			@RequestParam String name) {
		try {

			System.out.println("---------------------------enter-------------");
			Demo demo = new Demo(name);
				scheduleTaskService.addTaskToScheduler(1, demo, corn,"UTC");
			
			return "done";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return "done";
	}

}
