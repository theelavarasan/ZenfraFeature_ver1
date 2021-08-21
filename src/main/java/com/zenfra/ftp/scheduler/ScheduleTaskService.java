package com.zenfra.ftp.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.zenfra.ftp.service.FtpSchedulerService;
import com.zenfra.model.ftp.FtpScheduler;
import com.zenfra.utils.CommonFunctions;

@Service
@Configuration 
public class ScheduleTaskService {

	
//	/https://thebackendguy.com/spring-schedule-tasks-or-cron-jobs-dynamically/#:~:text=Spring%20provides%20Task%20Scheduler%20API,different%20methods%20to%20schedule%20task.
	
	@Autowired
	CommonFunctions  functions;
	
	@Autowired
	FtpSchedulerService ftpSchedulerService;
	
	// Task Scheduler
		@Autowired
		TaskScheduler scheduler;
		
		
		
		// A map for keeping scheduled tasks
		Map<Long, ScheduledFuture<?>> jobsMap = new HashMap<>();
		
		public ScheduleTaskService(TaskScheduler scheduler) {
			
			this.scheduler = scheduler;
		}
		
		
		// Schedule Task to be executed every night at 00 or 12 am
		public void addTaskToScheduler(long id,Runnable task,String corn) {//"0 0 0 * * ?"
			ScheduledFuture<?> scheduledTask = scheduler.schedule(task, new CronTrigger(corn, TimeZone.getTimeZone(TimeZone.getDefault().getID())));//TimeZone.getTimeZone(TimeZone.getDefault().getID())
			jobsMap.put(id, scheduledTask);
		}
		
		// Remove scheduled task 
		public void removeTaskFromScheduler(long id) {
			ScheduledFuture<?> scheduledTask = jobsMap.get(id);
			if(scheduledTask != null) {
				scheduledTask.cancel(true);
				jobsMap.put(id, null);
			}
		}
		
		// A context refresh event listener
		//@EventListener({ ContextRefreshedEvent.class })
		void contextRefreshedEvent() {
			// Get all tasks from DB and reschedule them in case of context restarted
			
			System.out.println("-----------welcome----db-corn------");
			
			List<FtpScheduler> list=ftpSchedulerService.getFtpSchedulerAll();
			
				for(FtpScheduler s:list) {
					SchedulerThread r = new SchedulerThread(s);
					if (s.getType()!=null && s.getType().equalsIgnoreCase("hour")) {
						String corn = "0 minutes current/hour * * ?";
						s.setSchedulerCorn(corn.replace("hour", s.getTime()).replace("current", functions.getCurrentHour()).replace("minutes", functions.getCurrentMinutes()));
					}
					
					addTaskToScheduler(s.getId(), r, s.getSchedulerCorn());
					System.out.println("--------corn added----"+s.getType()+":"+s.getSchedulerCorn());
				}
			
			
		}
}
