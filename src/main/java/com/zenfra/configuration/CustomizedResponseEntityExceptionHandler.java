package com.zenfra.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.apache.zookeeper.proto.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.zenfra.model.ResponseModel_v2;

@ControllerAdvice
@RestController
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	
	 @ExceptionHandler(ConstraintViolationException.class)
	    public final List<String> handleConstraintViolation(
	                                            ConstraintViolationException ex,
	                                            WebRequest request)
	    {
	        List<String> details = ex.getConstraintViolations()
	                                    .parallelStream()
	                                    .map(e -> e.getMessage())
	                                    .collect(Collectors.toList());
	 
	        ResponseModel_v2 model=new ResponseModel_v2();
	        		model.setjData(details);
	        return details;
	    }
	 
	 @Override
	  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
	      HttpHeaders headers, HttpStatus status, WebRequest request) {
		 
		  List<String> errors = new ArrayList<String>();
		    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
		        errors.add(error.getField() + ": " + error.getDefaultMessage());
		    }
		   /* for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
		        errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
		    }*/
	        ResponseModel_v2 model=new ResponseModel_v2();
	        	model.setResponseCode(HttpStatus.BAD_REQUEST);
	        	model.setResponseDescription(ex.getBindingResult().toString());
	        	model.setResponseMessage("Please sent valid params");
	        	model.setjData(errors);
	        	
	    return new ResponseEntity(model, HttpStatus.BAD_REQUEST);
	  } 
}
