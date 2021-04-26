package com.zenfra.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

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

			if (row.get("filterProperty") != null) {
				row.put("filterProperty", (JSONArray) parser
						.parse(row.get("filterProperty").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("filterProperty", new JSONArray());
			}

			if (row.get("categoryList") != null) {
				row.put("categoryList", (JSONArray) parser
						.parse(row.get("categoryList").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("categoryList", new JSONArray());
			}
			if (row.get("siteAccessList") != null) {
				row.put("siteAccessList", (JSONArray) parser
						.parse(row.get("siteAccessList").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("siteAccessList", new JSONArray());
			}
			if (row.get("groupedColumns") != null) {
				row.put("groupedColumns", (JSONArray) parser
						.parse(row.get("groupedColumns").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("groupedColumns", new JSONArray());
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
		return row;
	}

	public JSONObject convertEntityToJsonObject(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		JSONObject json = new JSONObject();
		try {
			if (obj != null) {
				json = (JSONObject) new JSONParser().parse(mapper.writeValueAsString(obj));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return json;
	}

	public String getCurrentDateWithTime() {
		String currentTime = "";
		try {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			currentTime = dtf.format(now);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return currentTime;
	}

	public String generateRandomId() {

		String randomUUIDString = "";
		try {

			UUID uuid = UUID.randomUUID();
			randomUUIDString = uuid.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return randomUUIDString;
	}
	
	public String ConvertQueryWithMap(Map<String,Object> params,String query ) {
		
		try {
			
			for(String param:params.keySet()) {
				query=query.replace(param,params.get(param).toString());
			}		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query;
		
	}
	
}
