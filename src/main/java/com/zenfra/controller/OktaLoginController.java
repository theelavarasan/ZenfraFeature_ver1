
package com.zenfra.controller;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zenfra.dao.OktaLoginRepository;
import com.zenfra.model.OktaLoginModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.service.OktaLoginService;

@RestController
@RequestMapping("/rest/LoginOkta")
public class OktaLoginController {

	@Autowired
	OktaLoginService oktaLoginService;

	@Autowired
	private OktaLoginRepository oktaLoginRepository;

	@PostMapping("/insert")
	public ResponseEntity<?> insertController(@RequestBody OktaLoginModel oktaLoginModel) {
		ResponseModel_v2 rmodel = new ResponseModel_v2();
		JSONObject resultObject = new JSONObject();
		resultObject = oktaLoginService.saveData(oktaLoginModel);

		rmodel.setjData(resultObject);
		return ResponseEntity.ok(rmodel);

	}

	@GetMapping("/get")
	public ResponseEntity<?> selectController(@RequestParam String id) {
		ResponseModel_v2 rmodel = new ResponseModel_v2();

		try {
			JSONObject resultObject = new JSONObject();

			resultObject = oktaLoginService.getData(id);
			if (resultObject != null) {
				rmodel.setjData(resultObject);
				rmodel.setResponseDescription("Successfully Retrieved ");
				rmodel.setStatusCode(200);
			} else {
				rmodel.setjData(new JSONObject());
				rmodel.setResponseDescription("No data found");
				rmodel.setStatusCode(500);
			}
		} catch (Exception e) {
			e.printStackTrace();
			rmodel.setResponseMessage("Failed");
			rmodel.setStatusCode(500);
			rmodel.setResponseDescription(e.getMessage());
		}

		return ResponseEntity.ok(rmodel);
	}

	@PutMapping("/update")
	public ResponseEntity<?> updateController(@RequestBody OktaLoginModel oktaLoginModel) {
		oktaLoginModel.setActive(true);
		return ResponseEntity.ok(oktaLoginService.updateData(oktaLoginModel));
	}

	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteController(@RequestParam String id) {
		return ResponseEntity.ok(oktaLoginService.deleteData(id));

	}

}
