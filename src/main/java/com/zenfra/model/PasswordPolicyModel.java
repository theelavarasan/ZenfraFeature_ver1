package com.zenfra.model;

public class PasswordPolicyModel {

	private String pwdPolicyId;
	private int minLength;
	private int maxLength;
	private int minUpperCase;
	private int minLowerCase;
	private int minNumbers;
	private int minSpecial;
	private int prevPwdAllowed;
	private boolean firstLastName;
	private int noOfpwdAttempt;
	private int pwdExpiryDays;
	private String updatedBy;
	private String updatedTime;
	private String tenantId;
	private int combination;

	public String getPwdPolicyId() {
		return pwdPolicyId;
	}

	public void setPwdPolicyId(String pwdPolicyId) {
		this.pwdPolicyId = pwdPolicyId;
	}

	public int getMinLength() {
		return minLength;
	}

	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public int getMinUpperCase() {
		return minUpperCase;
	}

	public void setMinUpperCase(int minUpperCase) {
		this.minUpperCase = minUpperCase;
	}

	public int getMinLowerCase() {
		return minLowerCase;
	}

	public void setMinLowerCase(int minLowerCase) {
		this.minLowerCase = minLowerCase;
	}

	public int getMinNumbers() {
		return minNumbers;
	}

	public void setMinNumbers(int minNumbers) {
		this.minNumbers = minNumbers;
	}

	public int getMinSpecial() {
		return minSpecial;
	}

	public void setMinSpecial(int minSpecial) {
		this.minSpecial = minSpecial;
	}

	public int getPrevPwdAllowed() {
		return prevPwdAllowed;
	}

	public void setPrevPwdAllowed(int prevPwdAllowed) {
		this.prevPwdAllowed = prevPwdAllowed;
	}

	public boolean isFirstLastName() {
		return firstLastName;
	}

	public void setFirstLastName(boolean firstLastName) {
		this.firstLastName = firstLastName;
	}

	public int getNoOfpwdAttempt() {
		return noOfpwdAttempt;
	}

	public void setNoOfpwdAttempt(int noOfpwdAttempt) {
		this.noOfpwdAttempt = noOfpwdAttempt;
	}

	public int getPwdExpiryDays() {
		return pwdExpiryDays;
	}

	public void setPwdExpiryDays(int pwdExpiryDays) {
		this.pwdExpiryDays = pwdExpiryDays;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public int getCombination() {
		return combination;
	}

	public void setCombination(int combination) {
		this.combination = combination;
	}

}
