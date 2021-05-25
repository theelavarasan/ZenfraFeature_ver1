package com.zenfra.ftp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.ftp.repo.FtpSchedulerRepo;
import com.zenfra.model.ftp.FileNameSettings;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.model.ftp.FtpScheduler;

@Service
public class FtpSchedulerService {

	@Autowired
	FtpSchedulerRepo repo;

	@Autowired
	FileNameSettingsService settingsService;

	@Autowired
	FTPClientService clientService;

	public long saveFtpScheduler(FtpScheduler ftpScheduler) {

		try {

			repo.save(ftpScheduler);
			repo.flush();
			
			System.out.println("----get id-----"+ftpScheduler.getId());
			return ftpScheduler.getId();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public FtpScheduler getFtpScheduler(Long id) {

		try {

			return repo.findAllById(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FtpScheduler> getFtpSchedulerAll() {

		try {

			return repo.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FileWithPath> runFtpSchedulerFiles(FtpScheduler s) {
		try {

			
			System.out.println("--------------eneter runFtpSchedulerFiles---------");
			List<FileWithPath> files = new ArrayList<FileWithPath>();

			List<FtpScheduler> scheduler = new ArrayList<FtpScheduler>();
			
			

			/*for (FtpScheduler s : scheduler) {

				jsonObject jsonObject = new JsonParser().parse(s.getSchedulerAttributes()).getAsJsonObject();
				
				if (s.getType().equalsIgnoreCase("Hour")) {			
					LocalTime now = LocalTime.now();
					LocalTime limit = LocalTime.parse(jsonObject.get("hour").getAsString());
					if(now.equals( limit )) {
						getFilesBased(s);
					}					
				}else if(s.getType().equalsIgnoreCase("Month")) {
					
					int day=jsonObject.get("day").getAsInt();
					Calendar cal = Calendar.getInstance();
					int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
					  
					if(day!=0 && day==dayOfMonth) {
						getFilesBased(s);
					}					
					
				}else if(s.getType().equalsIgnoreCase("Week")) {
					
					List<String> daysList=Arrays.asList(jsonObject.get("days").getAsString().split(",") );
					Calendar cal = Calendar.getInstance();
					int dayOfMonth = cal.get(Calendar.DAY_OF_WEEK);
					  
					String[] arr=new String[] {
							"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday","Saturday"};
					if(daysList.contains(arr[dayOfMonth])) {
						getFilesBased(s);
					}					
					
				}

			}*/

			return getFilesBased(s);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<FileWithPath> getFilesBased(FtpScheduler scheduler) {

		try {

			FileNameSettingsModel settings = settingsService.getFileNameSettingsById(scheduler.getFileNameSettingsId());

			
			return settingsService.getFilesByPattern(settings);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
}
