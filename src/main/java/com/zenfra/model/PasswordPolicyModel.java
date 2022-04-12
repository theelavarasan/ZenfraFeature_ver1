package com.zenfra.model;

public class PasswordPolicyModel {

	private String pwdPolicyId;
	private int pwdLength;
	private int pwdExpire;
	private int pwdLock;
	private int noOfExistingPwd;
	private boolean alphaUpper;
	private boolean alphaLower;
	private boolean numbers;
	private boolean nonAlphaNumeric;
	private boolean isNonFnIn;

	public String getPwdPolicyId() {
		return pwdPolicyId;
	}

	public void setPwdPolicyId(String pwdPolicyId) {
		this.pwdPolicyId = pwdPolicyId;
	}

	public int getPwdLength() {
		return pwdLength;
	}

	public void setPwdLength(int pwdLength) {
		this.pwdLength = pwdLength;
	}

	public int getPwdExpire() {
		return pwdExpire;
	}

	public void setPwdExpire(int pwdExpire) {
		this.pwdExpire = pwdExpire;
	}

	public int getPwdLock() {
		return pwdLock;
	}

	public void setPwdLock(int pwdLock) {
		this.pwdLock = pwdLock;
	}

	public int getNoOfExistingPwd() {
		return noOfExistingPwd;
	}

	public void setNoOfExistingPwd(int noOfExistingPwd) {
		this.noOfExistingPwd = noOfExistingPwd;
	}

	public boolean isAlphaUpper() {
		return alphaUpper;
	}

	public void setAlphaUpper(boolean alphaUpper) {
		this.alphaUpper = alphaUpper;
	}

	public boolean isAlphaLower() {
		return alphaLower;
	}

	public void setAlphaLower(boolean alphaLower) {
		this.alphaLower = alphaLower;
	}

	public boolean isNumbers() {
		return numbers;
	}

	public void setNumbers(boolean numbers) {
		this.numbers = numbers;
	}

	public boolean isNonAlphaNumeric() {
		return nonAlphaNumeric;
	}

	public void setNonAlphaNumeric(boolean nonAlphaNumeric) {
		this.nonAlphaNumeric = nonAlphaNumeric;
	}

	public boolean isNonFnIn() {
		return isNonFnIn;
	}

	public void setNonFnIn(boolean isNonFnIn) {
		this.isNonFnIn = isNonFnIn;
	}

}
