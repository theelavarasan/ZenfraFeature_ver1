package com.zenfra.model;

import java.io.Serializable;
import java.util.Map;

public class ZKModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7866626096609066221L;
	public static Map<String, String> zkData;

	public static Map<String, String> getZkData() {
		return zkData;
	}
	
	public static String getProperty(String property) {
		return zkData.get(property);
	}
	
	public static boolean containsKey(String key) {
		return zkData.containsKey(key);
	}

}
