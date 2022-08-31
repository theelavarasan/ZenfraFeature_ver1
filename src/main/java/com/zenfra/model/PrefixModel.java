package com.zenfra.model;

import java.util.HashMap;
import java.util.Map;

public class PrefixModel {

	static Map<String, String> prefix = new HashMap<>();
	
	public PrefixModel() {
		
		prefix.put("Privileged Access", "Server Data~");
		prefix.put("User", "User Summary~");
		prefix.put("Server", "Server Summary~");
		prefix.put("Sudoers", "Sudoers Summary~");
		prefix.put("thirdPartyData", "User Summary~");
		prefix.put("Sudoers Detail", "Sudoers Detail~");
		prefix.put("Summary", "AD Master~");
		prefix.put("Summary-User", "User Summary~");
	}
	
	public static String getPrefix(String key) {
		return prefix.get(key);
	}
}
