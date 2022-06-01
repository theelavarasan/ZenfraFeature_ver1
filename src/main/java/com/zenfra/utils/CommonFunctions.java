package com.zenfra.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.model.FavouriteModel;
import com.zenfra.model.ftp.FtpScheduler;

@Component
public class CommonFunctions {

	@Autowired
	RestTemplate restTemplate;

	private List<String> dateFormats = Arrays.asList("yyyy/MM/dd HH:mm:ss,yyyy/mm/dd HH:mm:ss".split(",")); // ,yyyy/mm/dd
																											// hh:mm:ss,yyyy-MM-dd
																											// HH:mm:ss,yyyy-mm-dd
																											// HH:mm:ss,yyyy-mm-dd
																											// hh:mm:ss

	public Map<String, Object> getFavViewCheckNull(Map<String, Object> row) {

		try {
			ObjectMapper map = new ObjectMapper();
			JSONArray viewArr = new JSONArray();
			JSONParser parser = new JSONParser();

			if (row.get("filterProperty") != null && !row.get("filterProperty").toString().trim().isEmpty() && !row.get("filterProperty").equals("[]")) {
				row.put("filterProperty", (JSONArray) parser.parse(row.get("filterProperty").toString()));
			} else {
				row.put("filterProperty", new JSONArray());
			}

			if (row.get("categoryList") != null && !row.get("categoryList").toString().trim().isEmpty() && !row.get("categoryList").equals("[]")) {
				row.put("categoryList", (JSONArray) parser
						.parse(row.get("categoryList").toString()));
			} else {
				row.put("categoryList", new JSONArray());
			}
			if (row.get("siteAccessList") != null && !row.get("siteAccessList").toString().trim().isEmpty() && !row.get("siteAccessList").equals("[]") && !row.get("userAccessList").equals("{}")) {
				row.put("siteAccessList", (JSONArray) parser
						.parse(row.get("siteAccessList").toString()));
			} else {
				row.put("siteAccessList", new JSONArray());
			}
			if (row.get("groupedColumns") != null && !row.get("groupedColumns").toString().trim().isEmpty()  && !row.get("groupedColumns").equals("[]")) {

				row.put("groupedColumns", (JSONArray) parser.parse(row.get("groupedColumns").toString()));
			} else {
				row.put("groupedColumns", new JSONArray());
			}
			
			
			if (row.get("userAccessList") != null && !row.get("userAccessList").toString().trim().isEmpty()  && !row.get("userAccessList").equals("[]") && !row.get("userAccessList").equals("{}")) {
				row.put("userAccessList", (JSONArray) parser
						.parse(row.get("userAccessList").toString()));
			}
			 

		} catch (Exception e) {
			e.printStackTrace();
			/*try {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			} catch (Exception e2) {
				// TODO: handle exception
			}
			*/

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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return query;

	}

	public JSONObject convertGetMigarationReport(Map<String, Object> map) {

		JSONObject obj = new JSONObject();
		ObjectMapper mapper = new ObjectMapper();
		JSONParser parser = new JSONParser();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		try {

			if (map.containsKey("userAccessList") && map.get("userAccessList") != null) {
				map.put("userAccessList",
						(Object) map.get("userAccessList").toString().replace("{", "").replace("}", "").split(","));
			}
			if (map.containsKey("siteAccessList") && map.get("siteAccessList") != null) {
				map.put("siteAccessList",
						map.get("siteAccessList").toString().replace("{", "").replace("}", "").split(","));
			}
			if (map.get("categoryList") != null && !map.get("categoryList").equals("[]")) {
				map.put("categoryList", (JSONArray) parser
						.parse(map.get("categoryList").toString().replace("\\[", "").replace("\\]", "")));
			} else {
				map.put("categoryList", new JSONArray());
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return obj;
		}
		System.out.println(obj);
		return obj;
	}

	public JSONArray formatJsonArrayr(Object object) {
		JSONArray jsonArray = new JSONArray();
		JSONParser jsonParser = new JSONParser();
		if (object != null) {
			String str = object.toString();
			str = str.replaceAll("\\\\", "");
			try {
				if (!str.isEmpty()) {
					jsonArray = (JSONArray) jsonParser.parse(str);
				}

			} catch (ParseException e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}

		}
		return jsonArray;
	}

	public JSONObject getValueFromString(JSONObject obj) {
		try {

			if (obj != null && obj.containsKey("value")) {
				obj.put("value", convertStringToJsonArray(obj.get("value")));
			} else {
				obj.put("value", new JSONArray());
			}
			System.out.println(obj);
			return obj;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
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
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return arr;
	}

	public JSONArray convertObjectToJsonArray(Object object) {
		JSONArray jsonArray = new JSONArray();
		if (object != null) {
			String str = object.toString();
			if (str.startsWith("[") && str.endsWith("]")) {
				str = str.substring(1, str.length() - 1);
				List<String> array = Arrays.asList(str.split(","));
				if (array != null && array.size() > 0) {
					for (String data : array) {
						if (data.startsWith("\"") && data.endsWith("\"")) {
							data = data.substring(1, data.length() - 1);
						}
						jsonArray.add(data.trim());
					}

				}

			}
		}
		return jsonArray;
	}

	public String getUpdateFavQuery(FavouriteModel favouriteModel) {
		String query = "";
		try {
			ObjectMapper map = new ObjectMapper();
//			String user = favouriteModel.getUserAccessList().toString().replace("[", "{").replace("]", "}");
			String user = map.convertValue(favouriteModel.getUserAccessList(), JSONArray.class).toJSONString();
			String site_access_list = map.convertValue(favouriteModel.getSiteAccessList(), JSONArray.class)
					.toJSONString();
			JSONArray category_list = map.convertValue(favouriteModel.getCategoryList(), JSONArray.class);

			if (favouriteModel.getCategoryColumns() != null) {
				query = query + ", category_list='" + category_list.toJSONString() + "'";
			}
			if (favouriteModel.getUserAccessList() != null && !favouriteModel.getUserAccessList().isEmpty()) {
				query = query + ", user_access_list='" + user + "'";
			}
			if (favouriteModel.getFavouriteName() != null) {
				query = query + ", favourite_name='" + favouriteModel.getFavouriteName() + "'";
			}

			if (favouriteModel.getSiteAccessList() != null && !favouriteModel.getSiteAccessList().isEmpty()) {
				query = query + ", site_access_list='" + site_access_list + "'";
			}

			if (favouriteModel.getReportName() != null) {
				query = query + ", report_name='" + favouriteModel.getReportName() + "'";
			}
			if (favouriteModel.getReportLabel() != null) {
				query = query + ", report_label='" + favouriteModel.getReportLabel() + "'";
			}
			if (favouriteModel.getGroupedColumns() != null && !favouriteModel.getGroupedColumns().isEmpty()) {
				query = query + ", grouped_columns='" + favouriteModel.getGroupedColumns().toJSONString() + "'";
			}
			if (favouriteModel.getFilterProperty() != null && !favouriteModel.getFilterProperty().isEmpty()) {
				query = query + ", filter_property='" + favouriteModel.getFilterProperty().toJSONString() + "'";
			}
			if (favouriteModel.getGroupByPeriod() != null && !favouriteModel.getGroupByPeriod().isEmpty()) {
				query = query + ", group_by_period='" + favouriteModel.getGroupByPeriod() + "'";
			}
			if (favouriteModel.getProjectId() != null && !favouriteModel.getProjectId().isEmpty()) {
				query = query + ", project_id='" + favouriteModel.getProjectId() + "'";
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return query;
	}

	public String getZenfraToken(String username, String password) {

		Object token = null;
		try {

			System.out.println("Start get token");
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("userName", username);
			body.add("password", password);

			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<Object> request = new HttpEntity<>(body); //, createHeaders(null)
			String url = Constants.current_url + "/UserManagement/auth/login";
			url = CommonUtils.checkPortNumberForWildCardCertificate(url);
		
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request,
							String.class);
		
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response.getBody());
			token = root.get("jData").get("AccessToken");
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return token.toString().replace("\"", "");
	}

	public Object updateLogFile(JSONObject json, String token) {
		ResponseEntity<String> response = null;
		System.out.println("-----------------URL"+DBUtils.getParsingServerIP());
		try {
			// String token="Bearer "+getZenfraToken(Constants.ftp_email,
			// Constants.ftp_password);
			HttpEntity<Object> request = new HttpEntity<>(json.toString(), createHeaders(token));
			String url = DBUtils.getParsingServerIP() +"/parsing/rest/api/excute-aws-call";
			System.out.println("-----------AWS ----------- URL1--------------:"+url);
			url = CommonUtils.checkPortNumberForWildCardCertificate(url);
			System.out.println("-----------AWS ----------- URL2--------------:"+url);
			response = restTemplate.exchange(url,
					HttpMethod.POST, request, String.class);

			return response.getBody();
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	HttpHeaders createHeaders(String token) {
		return new HttpHeaders() {
			{
				if (token != null) {
					set("Authorization", token);
				}
				setContentType(MediaType.APPLICATION_JSON);
			}
		};
	}

	public String getDate() {
		String formattedDate = "";
		try {
			LocalDateTime myDateObj = LocalDateTime.now();
			System.out.println("Before formatting: " + myDateObj);
			DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
			formattedDate = myDateObj.format(myFormatObj);
			System.out.println("After formatting: " + formattedDate);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return formattedDate;
	}

	public boolean sentEmail(JSONObject partObj, String hostName) {

		boolean isSuccess = false;
		try {
			RestTemplate restTemplate = new RestTemplate();
			System.out.println("email object" + partObj);

			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<JSONObject> requestEntity = new HttpEntity<JSONObject>(partObj, headers);
			String resetLink = hostName + "/mailservice/mail/send";
			resetLink = CommonUtils.checkPortNumberForWildCardCertificate(resetLink);
			ResponseEntity<String> uri = restTemplate.exchange(resetLink, HttpMethod.POST, requestEntity, String.class);
			if (uri != null && uri.getBody() != null) {
				if (uri.getBody().equalsIgnoreCase("ACCEPTED")) {
					isSuccess = true;
				} else {
					isSuccess = false;
				}
			} else {
				isSuccess = true;
			}
			System.out.println("Mail response::" + isSuccess);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return isSuccess;
	}

	public List<Object> convertJsonArrayToList(JSONArray arr) {
		List<Object> list = new ArrayList<Object>();
		try {
			for (int i = 0; i < arr.size(); i++) {
				list.add(arr.get(i));
			}
			
		} catch (Exception e) {
			return list;
		}
		System.out.println(list);
		return list;
	}

	public static String convertTimeZone(String inputTimeZone, String timeSlot) {
		String hour = "0";
		try {

			String[] arr = timeSlot.replace(" ", "").split("-");
			String[] split = arr[0].split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
			int time = Integer.valueOf(split[0]);
			String clock = split[1];
			String DATE_FORMAT = "dd-M-yyyy hh:mm:ss a";
			SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a");
			Date date = parseFormat.parse(time + ":00 " + clock);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
			// String inputTimeZone = "UTC";
			String outputTimeZone = TimeZone.getDefault().getID();
			// String dateInString = "10-06-2021 10:00:00 AM";
			LocalDateTime myDateObj = LocalDate.now().atTime(date.getHours(), 0);

			String formattedDate = myDateObj.format(formatter);
			// System.out.println(formattedDate);
			LocalDateTime ldt = LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern(DATE_FORMAT));
			TimeZone inputDateWithZone = TimeZone.getTimeZone(inputTimeZone);
			ZonedDateTime inputTimeWithTimeZone = ldt.atZone(inputDateWithZone.toZoneId());
			ZonedDateTime utcTime = inputTimeWithTimeZone.withZoneSameInstant(ZoneId.of(outputTimeZone));
			// System.out.println(formatter.format(inputTimeWithTimeZone));
			// System.out.println(formatter.format(utcTime));
			// System.out.println(" convert hour--------- " + utcTime.getHour());
			hour = String.valueOf(utcTime.getHour());

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return hour;
	}

	public String getCorn(FtpScheduler ftpScheduler) {
		String corn = "";
		try {
			String timseslot = ftpScheduler.getTimeSlot().replace(" ", "").replaceAll("[a-zA-Z]", "");

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return corn;
	}

	public static void main(String[] args) {
		System.out.println(convertTimeZone("UTC", "12 AM - 12 AM IST"));
		// CommonFunctions f=new CommonFunctions();
		// f.logout("Bearer
		// eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ6MEVqMlhRWGZSc3NranlMR2g3UXFkbkp2TVRaRkRVQXE2diswdTBBV1NMbjBXUUhHbmpXbkE9PSIsInNjb3BlcyI6W3siYXV0aG9yaXR5IjoiUk9MRV9BRE1JTiJ9XSwiaXNzIjoiaHR0cDovL3plbmZyYS5jb20iLCJpYXQiOjE2MjMzMjUxNzh9.xg33ygz9pAIwIAAK7iu96dS-vYekVlYWSQ_bgjGzPUo");
	}

	public String logout(String token) {
		try {
			RestTemplate restTemplate = new RestTemplate();
			// token=token.replace("Bearer ","");
			HttpEntity<Object> request = new HttpEntity<>(null, createHeaders(null));
			String url = Constants.current_url + "UserManagement/auth/logout?token="+ token;
			url = CommonUtils.checkPortNumberForWildCardCertificate(url);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request,
					String.class);

			System.out.println(response.getBody());

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return "logout";
	}

	public String getCurrentHour() {
		String hour = "*";
		try {
			Date dt = new Date();
			SimpleDateFormat dateFormat;
			dateFormat = new SimpleDateFormat("kk:mm:ss");
			hour = dateFormat.format(dt).split(":")[0];

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return hour;
	}

	public String getCurrentMinutes() {
		String minutes = "*";
		try {
			Date dt = new Date();
			SimpleDateFormat dateFormat;
			dateFormat = new SimpleDateFormat("kk:mm:ss");
			minutes = String.valueOf((Integer.valueOf(dateFormat.format(dt).split(":")[1]) + 1));

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return minutes;
	}

	public Object callAwsScriptAPI(String builder, String token) {
		ResponseEntity<String> response = null;
		System.out.println(DBUtils.getParsingServerIP());
		try {

			String url = DBUtils.getParsingServerIP() + "/parsing/rest/api/excute-aws-data-call" + builder;
			url = CommonUtils.checkPortNumberForWildCardCertificate(url);
			
			URI uri = URI.create(url);
			System.out.println("URl::" + uri);
			// String token="Bearer "+getZenfraToken(Constants.ftp_email,
			// Constants.ftp_password);
			HttpEntity<Object> request = new HttpEntity<>(createHeaders(token));
			
			response = restTemplate.exchange(uri, HttpMethod.GET, request, String.class);
			// DBUtils.getParsingServerIP()+
			return response.getBody();
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public String convertCamelCase(String key) {
		try {
			final char[] delimiters = { ' ', '_' };

			key = WordUtils.capitalizeFully(key, delimiters);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return key;
	}

	public String execPHP(String scriptName) throws Exception {
		String FinOut = null;

		System.out.println("!!!!! ScriptName: " + scriptName);
		try {
			System.out.println("!!!!! execPHP 1 ");
			String line;
			System.out.println("!!!!! execPHP 1 ");
			StringBuilder output = new StringBuilder();
			System.out.println("!!!!! execPHP 1 ");
			Process p = Runtime.getRuntime().exec(scriptName);
			System.out.println("!!!!! execPHP 1 ");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			System.out.println("!!!!! execPHP 1 ");
			while ((line = input.readLine()) != null) {
				System.out.println("!!!!! line: " + line);
				output.append(line);
			}
			FinOut = output.toString();
			input.close();
			System.out.println("PHP file is working : ");
		} catch (Exception err) {
			err.printStackTrace();
			throw err;

		}
		return FinOut;
	}

	public String convertToUtc(TimeZone timeZone, String dateString) {
		String utcTime = dateString;
		try {
			// for(String df : dateFormats) {
			try {
				String df = "yyyy/MM/dd HH:mm:ss";
				if (dateString != null && dateString.contains("-")) {
					df = "yyyy-MM-dd HH:mm:ss";
				}
				DateFormat formatterIST = new SimpleDateFormat(df);
				formatterIST.setTimeZone(TimeZone.getDefault()); // better than using IST
				if (dateString != null && !dateString.isEmpty()) {
					Date date = formatterIST.parse(dateString);

					DateFormat formatterUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					formatterUTC.setTimeZone(TimeZone.getTimeZone("UTC")); // UTC timezone
					return formatterUTC.format(date);
				}

			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}
			// }

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return utcTime;

	}

	public String getUtcDateTime() {
		Instant instant = Instant.now();
		return instant.toString();
	}

	public List<String> convertToArrayList(String value, String separator) {

		List<String> list = new ArrayList<String>();
		value = value.replace("[", "").replace("]", "").replace("\"", "");
		String[] valueArray = value.split(separator);

		for (int i = 0; i < valueArray.length; i++) {
			list.add(valueArray[i].trim());
		}

		return list;
	}
}
