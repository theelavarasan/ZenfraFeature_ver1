package com.zenfra.utils;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CommonFunctions {

	
	public Map<String, Object> getFavViewCheckNull(Map<String, Object> row) {
		
		try {
			ObjectMapper map = new ObjectMapper();
			JSONArray viewArr = new JSONArray();
			JSONParser parser = new JSONParser();
			
			if(row.get("filterProperty")!=null) {
				row.put("filterProperty", (JSONArray) parser.parse(row.get("filterProperty").toString().replace("\\[", "").replace("\\]", "")));
			}else {
				row.put("filterProperty", new JSONArray());
			}
			
			if(row.get("categoryList")!=null) {
				row.put("categoryList", (JSONArray) parser.parse(row.get("categoryList").toString().replace("\\[", "").replace("\\]", "")));
			}else {
				row.put("categoryList", new JSONArray());
			}
		if(row.get("siteAccessList")!=null) {
				row.put("siteAccessList", (JSONArray) parser.parse(row.get("siteAccessList").toString().replace("\\[", "").replace("\\]", "")));
			}else {
				row.put("siteAccessList", new JSONArray());
			}
		
		} catch (Exception e) {
			e.printStackTrace();
				
		}
		return row;
	}
}
