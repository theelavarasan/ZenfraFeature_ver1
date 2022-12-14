package com.zenfra.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.zenfra.dao.UserDao;
import com.zenfra.service.UserCreateService;

@Service
public class ZenfraFeaturesRestApis {

	/*
	 * @Autowired TrippleDes keyGen;
	 */

	// RestTemplate restTemplate = new RestTemplate();

	@Autowired
	CommonFunctions functions;

	@Autowired
	UserCreateService service;

	@Autowired
	UserDao userDao;

	public JSONObject login(String email, String password) throws ParseException {

		JSONObject json = new JSONObject();

		try {
			json = loginToken(email, password);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return json;
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

	@SuppressWarnings({ "deprecation", "unchecked" })
	public JSONObject login(String username) throws ParseException {

		JSONObject jsonObject = new JSONObject();
		Object obj = service.getUserByEmail(username);
		jsonObject = functions.convertEntityToJsonObject(obj);

		return jsonObject;

	}

	@SuppressWarnings("unchecked")
	public JSONObject loginToken(String username, String password) throws ParseException {
		JSONObject jsonObject = new JSONObject();
		Object object = userDao.login(username, password);
		jsonObject = functions.convertEntityToJsonObject(object);

		return jsonObject;

	}

	/*
	 * public static void main(String[] args) throws ParseException {
	 * ZenfraFeaturesRestApis api=new ZenfraFeaturesRestApis();
	 * System.out.println(api.login("s")); }
	 */

}
