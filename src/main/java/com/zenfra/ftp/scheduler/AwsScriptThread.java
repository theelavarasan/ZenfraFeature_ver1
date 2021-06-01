package com.zenfra.ftp.scheduler;

import com.zenfra.controller.AwsInventoryController;
import com.zenfra.payload.model.CallAwsScript;

public class AwsScriptThread implements Runnable{

	
	CallAwsScript script;
	
	public AwsScriptThread(CallAwsScript script) {		
		this.script=script;
	}
	
	@Override
	public void run() {
		System.out.println("Start AwsScriptThread");
		AwsInventoryController control=new AwsInventoryController();
		control.callAwsScript(script.getSecurityKey(), script.getAccessKey(),
				script.getSiteKey(), script.getUserId(), script.getToken(), script.getProcessingStatus(),script.getRid());
	}

}