package com.zenfra.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.zenfra.model.Response;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;

public class ExceptionHandlerMail {

	@SuppressWarnings("unchecked")
	public static ResponseEntity<String> errorTriggerMail(String stackTrace) {

		String path = "properties/AvoidedExceptions/AvoidedExceptions.properties";
		System.out.println("!!!!! path: " + path);
		InputStream inputFile = null;
		try {
			ClassLoader classLoader = ExceptionHandlerMail.class.getClassLoader();
			URL resources = classLoader.getResource(path);
			System.out.println("!!!!! resources.getFile(): " + resources.getFile());
			inputFile = new FileInputStream(resources.getFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<String> skippedExceptions = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new InputStreamReader(inputFile));

		String line;
		try {
			while ((line = br.readLine()) != null) {
				skippedExceptions.add(line);
				if (line.contains(stackTrace)) {
					System.out.println("----------Exception  Avoided--------");
				} else {
					try {

						System.out.println("--------In Java Exception Handler---------");

						RestTemplate restTemplate = new RestTemplate();
						Response response = new Response();

						JSONObject errorObj = new JSONObject();

						errorObj.put("category", "Java");
						errorObj.put("repoName", "Zenfra-Features");
						errorObj.put("description", "Java Exception");
						errorObj.put("stackTrace", stackTrace);

						HttpHeaders headers1 = new HttpHeaders();
						headers1.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
						headers1.setContentType(MediaType.APPLICATION_JSON);
						HttpEntity<JSONObject> requestEntity1 = new HttpEntity<JSONObject>(errorObj, headers1);
						String sendMailUrl = ZKModel.getProperty(ZKConstants.SEND_ERROR_MAIL_URL)
								.replaceAll("<HOSTNAME>", ZKModel.getProperty(ZKConstants.APP_SERVER_IP));
						System.out.println("----------Zenfra Features---");
						System.out.println("----------Send Mail Url---" + sendMailUrl);
						ResponseEntity<String> uri = restTemplate.exchange(sendMailUrl, HttpMethod.POST, requestEntity1,
								String.class);

						if (uri != null && uri.getBody() != null) {
							if (uri.getBody().equalsIgnoreCase("ACCEPTED")) {

								response.setData(uri.getBody());
								response.setResponseCode(200);
								response.setResponseMsg("Success!!!");
							} else {
								response.setData(uri.getBody());
								response.setResponseCode(500);
								response.setResponseMsg("Failed!!!");
							}
						} else {
							response.setData("Mail send successfully");
							response.setResponseCode(200);
							response.setResponseMsg("Success!!!");
						}
						return new ResponseEntity<String>(response.toString(), HttpStatus.OK);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<String>(HttpStatus.OK);
	}
}
