package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.zenfra.dao.ResourceCrudRepository;
import com.zenfra.model.ResourceCrudModel;
import com.zenfra.model.ResponseModel_v2;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
@Component
public class ResourceCrudService {

	@Autowired
	ResourceCrudRepository resourceCrudRepository;

	ResponseModel_v2 responseModel = new ResponseModel_v2();

	public ResponseEntity<?> insert(ResourceCrudModel resourceCrudModel) {
		try {
			resourceCrudModel.setResourceDataId(UUID.randomUUID().toString());
			resourceCrudRepository.save(resourceCrudModel);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(resourceCrudModel);
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}
	}

	public ResponseEntity<?> get(String resourceDataId) {
		if (resourceDataId == null) {
			try {
				List<ResourceCrudModel> resourceCrudModel = resourceCrudRepository.findAll();

				System.out.println("-------------------" + resourceCrudModel);

				responseModel.setResponseMessage("Success");
				responseModel.setStatusCode(200);
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData(resourceCrudModel);
				return ResponseEntity.ok(responseModel);

			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
				responseModel.setStatusCode(500);
				responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
				return (ResponseEntity<?>) ResponseEntity.badRequest();
			}

		} else {
			try {
				ResourceCrudModel resourceCrudModel = resourceCrudRepository.findById(resourceDataId).orElse(null);

				responseModel.setResponseMessage("Success");
				responseModel.setStatusCode(200);
				responseModel.setResponseCode(HttpStatus.OK);
				responseModel.setjData(resourceCrudModel);
				return ResponseEntity.ok(responseModel);

			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
				responseModel.setStatusCode(500);
				responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
				return (ResponseEntity<?>) ResponseEntity.badRequest();
			}

		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ResponseEntity<?> update(ResourceCrudModel resourceCrudModel) {
		List<ResourceCrudModel> massUpdate = new ArrayList();

		try {
			ResourceCrudModel existing = resourceCrudRepository.findById(resourceCrudModel.getResourceDataId())
					.orElse(null);

			if (existing != null) {
				existing.setParent(resourceCrudModel.getParent());
				existing.setUpdatedTime(resourceCrudModel.getUpdatedTime());
				existing.setResourceId(resourceCrudModel.getResourceId());
				existing.setIcon(resourceCrudModel.getIcon());
				existing.setDescription(resourceCrudModel.getDescription());
				existing.setClassName(resourceCrudModel.getClassName());
				existing.setLabel(resourceCrudModel.getLabel());
				existing.setPriority(resourceCrudModel.getPriority());
				existing.setActive(resourceCrudModel.isActive());
				existing.setMenu(resourceCrudModel.isMenu());
				existing.setMenuClassName(resourceCrudModel.getMenuClassName());
				existing.setChildren(resourceCrudModel.getChildren());
				existing.setCreatedBy(resourceCrudModel.getCreatedBy());
				existing.setCreatedTime(resourceCrudModel.getCreatedTime());
				existing.setHref(resourceCrudModel.getHref());
				existing.setValue(resourceCrudModel.getValue());
				existing.setSeq(resourceCrudModel.getSeq());
				existing.setCreate_by(resourceCrudModel.getCreate_by());
				existing.setCreatedDateTime(resourceCrudModel.getCreatedDateTime());
				existing.setParentId(resourceCrudModel.getParentId());
				existing.setUpdatedDateTime(resourceCrudModel.getUpdatedDateTime());
				existing.setHref(resourceCrudModel.getHref());
				existing.getResourceDataId();

				massUpdate.add(existing);

			}

			resourceCrudRepository.saveAll(massUpdate);
			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(existing);
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}

	}

	public ResponseEntity<?> delete(String resourceDataId) {
		try {
			ResourceCrudModel resourceCrudModel = resourceCrudRepository.findById(resourceDataId).orElse(null);
			resourceCrudRepository.delete(resourceCrudModel);
			System.out.println("-------------------" + resourceCrudModel);

			responseModel.setResponseMessage("Success");
			responseModel.setStatusCode(200);
			responseModel.setResponseCode(HttpStatus.OK);
			responseModel.setjData(resourceCrudModel);
			return ResponseEntity.ok(responseModel);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			responseModel.setStatusCode(500);
			responseModel.setResponseCode(HttpStatus.EXPECTATION_FAILED);
			return (ResponseEntity<?>) ResponseEntity.badRequest();
		}

	}
}
