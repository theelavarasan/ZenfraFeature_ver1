package com.zenfra.model.ftp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.json.simple.JSONArray;

@Entity
public class FtpScheduler {

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	@Column(name = "sheduler_id")
	private long id;

	@Column
	@NotBlank(message = "fileNameSettingsId must not be empty")
	private String fileNameSettingsId;

	@Column
	private String type;

	@Column
	private String schedulerCorn;

	@Column
	@NotBlank(message = "timeSlot must not be empty")
	private String timeSlot;

	@Column
	private boolean isActive;

	@Column
	private String tenantId;

	@Column
	@NotBlank(message = "siteKey must not be empty")
	private String siteKey;

	@Column
	@NotBlank(message = "userId must not be empty")
	private String userId;

	@Column
	private JSONArray notificationEmail;

	@Column
	private JSONArray selectedDay;

	@Column
	private String selectedDate;

	@Column
	private String time;

	@Column
	private String timeZone;

	@Column
	private String emailString;

	@Column
	private Boolean isNas;

	@Transient
	private String[] logType;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getFileNameSettingsId() {
		return fileNameSettingsId;
	}

	public void setFileNameSettingsId(String fileNameSettingsId) {
		this.fileNameSettingsId = fileNameSettingsId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSchedulerCorn() {
		return schedulerCorn;
	}

	public void setSchedulerCorn(String schedulerCorn) {
		this.schedulerCorn = schedulerCorn;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTimeSlot() {
		return timeSlot;
	}

	public void setTimeSlot(String timeSlot) {
		this.timeSlot = timeSlot;
	}

	public String getSelectedDate() {
		return selectedDate;
	}

	public void setSelectedDate(String selectedDate) {
		this.selectedDate = selectedDate;
	}

	public JSONArray getSelectedDay() {
		return selectedDay;
	}

	public void setSelectedDay(JSONArray selectedDay) {
		this.selectedDay = selectedDay;
	}

	public JSONArray getNotificationEmail() {
		return notificationEmail;
	}

	public void setNotificationEmail(JSONArray notificationEmail) {
		this.notificationEmail = notificationEmail;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getEmailString() {
		return emailString;
	}

	public void setEmailString(String emailString) {
		this.emailString = emailString;
	}

	public Boolean getIsNas() {
		return isNas;
	}

	public void setIsNas(Boolean isNas) {
		this.isNas = isNas;
	}

	public String[] getLogType() {
		return logType;
	}

	public void setLogType(String[] logType) {
		this.logType = logType;
	}

}
