
package com.zenfra.controller;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dao.OktaLoginRepository;
import com.zenfra.model.OktaLoginModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.OktaLoginService;

@RestController
@RequestMapping("/rest/LoginOkta")
public class OktaLoginController {

	@Autowired
	OktaLoginService OktaLoginService;

	@Autowired
	private OktaLoginRepository OktaLoginRepository;
	
	@PostMapping("/insert")
	public ResponseEntity<?> insertController(@RequestBody OktaLoginModel OktaLoginModel) {
		return ResponseEntity.ok(OktaLoginService.saveData(OktaLoginModel));

	}

	@GetMapping("/get")
	public ResponseEntity<?> selectController(@RequestParam String id) {
		ResponseModel_v2 rmodel=new ResponseModel_v2();

		try {
		OktaLoginModel olmodelObject=OktaLoginService.getData(id);
		
		ObjectMapper mapper= new ObjectMapper();
		JSONObject jsondata=mapper.convertValue(olmodelObject, JSONObject.class);
		if(olmodelObject != null) {
			rmodel.setjData(jsondata);
			rmodel.setResponseDescription("Successfully Retrieved ");
			rmodel.setStatusCode(200);
		}
		else {
			rmodel.setjData(new JSONObject());
			rmodel.setResponseDescription("No data found");
			rmodel.setResponseCode(HttpStatus.OK);
		}
		}
		catch(Exception e){
			e.printStackTrace();
			rmodel.setResponseMessage("Failed");
			rmodel.setStatusCode(500);
			rmodel.setResponseDescription(e.getMessage());
		}
		
		return ResponseEntity.ok(rmodel);
	}

	@PutMapping("/update")
	public ResponseEntity<?> updateController(@RequestBody OktaLoginModel OktaLoginModel) {

		return ResponseEntity.ok(OktaLoginService.updateData(OktaLoginModel));
	}

	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteController(@RequestParam String id) {

		return ResponseEntity.ok(OktaLoginService.deleteData(id));

	}
}
