package com.zenfra.utils;

import java.util.Arrays;

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

		System.err.println("--------In Java Exception Handler---------");

		RestTemplate restTemplate = new RestTemplate();
		Response response = new Response();

		// HttpServletRequest request;
		// String url = request.getRequestURL().toString();
		// System.out.println("------------url---------" + url);
		String fromAddress = "zenfra.alerts@zenfra.co";
		String[] toMail = { ZKModel.getProperty(ZKConstants.To_ERROR_MAIL_ADDRESS) };

		JSONObject errorObj = new JSONObject();

		errorObj.put("category", "Java");
		errorObj.put("repoName", "Zenfra-Features");
		errorObj.put("stackTrace", stackTrace);
		errorObj.put("fromAddress", fromAddress);
		errorObj.put("toEmail", toMail);

		HttpHeaders headers1 = new HttpHeaders();
		headers1.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers1.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<JSONObject> requestEntity1 = new HttpEntity<JSONObject>(errorObj, headers1);
		String sendMailUrl = "http://localhost:8081/mailservice/mail/send-error-mail";
		System.out.println("----------Send Mail Url---" + sendMailUrl);
		ResponseEntity<String> uri = restTemplate.exchange(sendMailUrl, HttpMethod.POST, requestEntity1, String.class);

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

	}

}
