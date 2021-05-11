package com.zenfra.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;


import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CommonFunctions {


	public Map<String, Object> getFavViewCheckNull(Map<String, Object> row) {

		try {
			ObjectMapper map = new ObjectMapper();
			JSONArray viewArr = new JSONArray();
			JSONParser parser = new JSONParser();

			if (row.get("filterProperty") != null &&  !row.get("filterProperty").equals("[]")) {
				row.put("filterProperty", (JSONArray) parser
						.parse(row.get("filterProperty").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("filterProperty", new JSONArray());
			}

			if (row.get("categoryList") != null &&  !row.get("categoryList").equals("[]")) {
				row.put("categoryList", (JSONArray) parser
						.parse(row.get("categoryList").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("categoryList", new JSONArray());
			}
			if (row.get("siteAccessList") != null && !row.get("siteAccessList").equals("[]")) {
				row.put("siteAccessList", (JSONArray) parser
						.parse(row.get("siteAccessList").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("siteAccessList", new JSONArray());
			}
			if (row.get("groupedColumns") != null && !row.get("groupedColumns").equals("[]") ) {

				row.put("groupedColumns", (JSONArray) parser
						.parse(row.get("groupedColumns").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				row.put("groupedColumns", new JSONArray());
			}

		} catch (Exception e) {
			//e.printStackTrace();

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

	public String ConvertQueryWithMap(Map<String, Object> params, String query) {

		try {

			for (String param : params.keySet()) {
				query = query.replace(param, params.get(param).toString());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return query;


	}

	public JSONObject convertGetMigarationReport(Map<String, Object> map) {

		JSONObject obj = new JSONObject();
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		try {

			if(map.containsKey("userAccessList")&&map.get("userAccessList")!=null) {
				map.put("userAccessList",
						(Object) map.get("userAccessList").toString().replace("{", "").replace("}", "").split(","));
			}
			if(map.containsKey("siteAccessList")&&map.get("siteAccessList")!=null) {
				map.put("siteAccessList",
						map.get("siteAccessList").toString().replace("{", "").replace("}", "").split(","));
			}
			if(map.containsKey("categoryList")&&map.get("categoryList")!=null) {
				map.put("categoryList",
						map.get("categoryList").toString().replace("{", "").replace("}", "").split(","));

			}
		
			obj = mapper.convertValue(map, JSONObject.class);
			
			JSONObject tempBreak = mapper.convertValue(map.get("breakdown"), JSONObject.class);
			obj.put("breakdown", getValueFromString(tempBreak).get("value"));
			JSONObject column = mapper.convertValue(map.get("column"), JSONObject.class);
			obj.put("column", getValueFromString(column).get("value"));
			JSONObject yaxis = mapper.convertValue(map.get("yaxis"), JSONObject.class);
			obj.put("yaxis", getValueFromString(yaxis).get("value"));
			JSONObject xaxis = mapper.convertValue(map.get("xaxis"), JSONObject.class);
			obj.put("xaxis", getValueFromString(xaxis).get("value"));
			JSONObject tablecolumns = mapper.convertValue(map.get("tablecolumns"), JSONObject.class);
			obj.put("tableColumns", getValueFromString(tablecolumns).get("value"));

		} catch (Exception e) {
			e.printStackTrace();
			return obj;
		}
		System.out.println(obj);
		return obj;
	}


	
	

	

	
	
	


	public JSONArray formatJsonArrayr(Object object) {
		JSONArray jsonArray = new JSONArray();
		JSONParser jsonParser = new JSONParser();
		if(object != null) {
			String str = object.toString();
			str = str.replaceAll("\\\\","");
			try {
				if(!str.isEmpty()) {
					jsonArray  = (JSONArray) jsonParser.parse(str);
				}
				
			} catch (ParseException e) {				
				e.printStackTrace();
			}
		
		}
		return jsonArray;
	}
	

	public JSONObject getValueFromString(JSONObject obj) {
		try {

			if (obj != null && obj.containsKey("value")) {
				obj.put("value", convertStringToJsonArray(obj.get("value")));
			}else {
				obj.put("value", new JSONArray());
			}
			System.out.println(obj);
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	

	public JSONArray convertStringToJsonArray(Object value) {
		JSONArray arr = new JSONArray();
		JSONParser jsonParser = new JSONParser();
		try {
			if (value != null && !value.toString().isEmpty() && !value.toString().equals("[]")) {
				arr = (JSONArray) jsonParser.parse(value.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return arr;
	}
	
	
	public JSONArray convertObjectToJsonArray(Object object) {
		JSONArray jsonArray = new JSONArray();
		if(object != null) {
			String str = object.toString();
			if(str.startsWith("[") && str.endsWith("]")){
				str = str.substring(1, str.length() - 1);
				List<String> array = Arrays.asList(str.split(","));
				if(array != null && array.size() > 0) {
					for(String data : array) {
						if(data.startsWith("\"") && data.endsWith("\"")){
							data = data.substring(1, data.length() - 1);
						}
						jsonArray.add(data.trim());
					}
					
				}
				
				
			}
		}
		return jsonArray;
	}



}