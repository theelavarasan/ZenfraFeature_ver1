package com.zenfra.ftp.scheduler;

public class Demo implements Runnable{

	String name;
	
	public Demo(String name) {
		
		this.name=name;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("-------hi-demoiooo--------"+name);
		
		
		
	}

}