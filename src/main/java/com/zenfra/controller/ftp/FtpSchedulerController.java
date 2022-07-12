package com.zenfra.controller.ftp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.json.simple.JSONObject;
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
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rest/ftpScheduler")
@Validated
public class FtpSchedulerController {

	@Autowired
	FtpSchedulerService schedulerService;

	@Autowired
	ScheduleTaskService scheduleTaskService;

	@Autowired
	CommonFunctions functions;

	@PostMapping("/runScheduler")
	public @ResponseBody String runScheduler(@Valid @RequestBody FtpScheduler ftpScheduler,
			@RequestParam(name = "isNas", required = false) boolean isNas) {

		try {

			if (ftpScheduler.getId() == 0) {
				FtpScheduler temp = schedulerService.getFtpScheduler(ftpScheduler.getFileNameSettingsId());
				if (temp != null) {
					return "Scheduler already agains the given filename settings";
				}
			}

			if (ftpScheduler.getType().equalsIgnoreCase("hour")) {
//				String corn = "0 minutes current/hour * * ?";
//				ftpScheduler.setSchedulerCorn(
//						corn.replace("hour", ftpScheduler.getTime()).replace("current", functions.getCurrentHour())
//								.replace("minutes", functions.getCurrentMinutes()));
				ftpScheduler.setSchedulerCorn("0 0/4 * * * ?");

			} else if (ftpScheduler.getType().equalsIgnoreCase("daily")) {
				String timseslot = functions.convertTimeZone(ftpScheduler.getTimeZone(), ftpScheduler.getTimeSlot());
				String corn = "0 0 from today/everyday * ?";
				corn = corn.replace("from", timseslot).replace("everyday", ftpScheduler.getTime()).replace("today",
						String.valueOf(new Date().getDate()));
				ftpScheduler.setSchedulerCorn(corn);
			} else if (ftpScheduler.getType().equalsIgnoreCase("month")) {
				String corn = "0 0 hour month/1 * ?";
				ftpScheduler.setSchedulerCorn(
						corn.replace("month", ftpScheduler.getTime()).replace("hour", functions.getCurrentHour()));
			} else if (ftpScheduler.getType().equalsIgnoreCase("weekly")) {
				String days = "";
				for (int i = 0; i < ftpScheduler.getSelectedDay().size(); i++) {
					days += ftpScheduler.getSelectedDay().get(i).toString().substring(0, 3).toUpperCase() + ",";
				}
				days = days.substring(0, days.length() - 1);
				String corn = "0 0 hour ? * weekly"; // 0 0 0 ? * MON,WED,THU *
				ftpScheduler.setSchedulerCorn(corn.replace("weekly", days).replace("hour", ftpScheduler.getTime()));
			}
			System.out.println(ftpScheduler.getSchedulerCorn());
			ftpScheduler.setActive(true);
			if (isNas) {
				ftpScheduler.setIsNas(true);
				ftpScheduler.setLogType(ftpScheduler.getLogType());
				ftpScheduler.setSiteKey(ftpScheduler.getSiteKey());
				ftpScheduler.setTenantId(ftpScheduler.getTenantId());
				ftpScheduler.setUserId(ftpScheduler.getUserId());
				ftpScheduler.setId(ftpScheduler.getId());
			}else {
				ftpScheduler.setIsNas(false);
			}		
			ftpScheduler.setEmailString(ftpScheduler.getNotificationEmail().toJSONString());
			System.out.println(ftpScheduler.toString());
			long id = schedulerService.saveFtpScheduler(ftpScheduler);

			SchedulerThread r = new SchedulerThread(ftpScheduler);

			scheduleTaskService.addTaskToScheduler(id, r, ftpScheduler.getSchedulerCorn());

			return "saved! & Cron add to thread";
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return e.getMessage();
		}

	}

	@GetMapping("/ftp-scheduler")
	public ResponseModel_v2 getFtpScheduler(
			@NotEmpty(message = "Please provide valid fileNameSettingsId") @RequestParam("fileNameSettingsId") String fileNameSettingsId) {

		ResponseModel_v2 response = new ResponseModel_v2();
		JSONObject dataObj = new JSONObject();
		try {
			response.setjData(schedulerService.getFtpScheduler(fileNameSettingsId));
			response.setResponseCode(HttpStatus.OK);
			response.setResponseMessage("Got the schedular Details Successfully");

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return null;
	}

	@PostMapping("/run-ftp")
	public @ResponseBody String runFtp(@RequestBody FtpScheduler ftpScheduler, @RequestParam String name,
			@RequestParam String secound) {
		try {

			System.out.println("---------------------------enter-------------");
			Demo demo = new Demo(name);

			if (ftpScheduler.getType().equalsIgnoreCase("hour")) {
				String corn = "0 0 current/hour * * ?";
				ftpScheduler.setSchedulerCorn(
						corn.replace("hour", ftpScheduler.getTime()).replace("current", functions.getCurrentHour()));
			} else if (ftpScheduler.getType().equalsIgnoreCase("daily")) {
				String timseslot = functions.convertTimeZone(ftpScheduler.getTimeZone(), ftpScheduler.getTimeSlot());
				String corn = "0 0 from today/everyday * ?";
				corn = corn.replace("from", timseslot).replace("everyday", ftpScheduler.getTime()).replace("today",
						String.valueOf(new Date().getDate()));
				ftpScheduler.setSchedulerCorn(corn);
			} else if (ftpScheduler.getType().equalsIgnoreCase("month")) {
				String corn = "0 0 hour month/1 * ?";
				ftpScheduler.setSchedulerCorn(
						corn.replace("month", ftpScheduler.getTime()).replace("hour", functions.getCurrentHour()));
			} else if (ftpScheduler.getType().equalsIgnoreCase("weekly")) {
				String days = "";
				for (int i = 0; i < ftpScheduler.getSelectedDay().size(); i++) {
					days += ftpScheduler.getSelectedDay().get(i).toString().substring(0, 3).toUpperCase() + ",";
				}
				days = days.substring(0, days.length() - 1);
				String corn = "0 0 hour ? * weekly"; // 0 0 0 ? * MON,WED,THU *
				ftpScheduler.setSchedulerCorn(corn.replace("weekly", days).replace("hour", ftpScheduler.getTime()));
			} else if (ftpScheduler.getType().equalsIgnoreCase("secounds")) {
				String corn = "0/secound * * ? * * *";
				ftpScheduler.setSchedulerCorn(corn.replace("secound", ftpScheduler.getTime()));
			} else if (ftpScheduler.getType().equalsIgnoreCase("minutes")) {
				String corn = "* 0/minutes * ? * * *";
				ftpScheduler.setSchedulerCorn(corn.replace("minutes", ftpScheduler.getTime()));
			}
			scheduleTaskService.addTaskToScheduler(1, demo, ftpScheduler.getSchedulerCorn());

			return "done";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return "done";
	}

}
