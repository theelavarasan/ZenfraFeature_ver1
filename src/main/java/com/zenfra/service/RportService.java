package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.ReportDao;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class RportService {

	@Autowired
	ReportDao dao;

	public void getMigarationMethod() {

		try {

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}
	}

}
