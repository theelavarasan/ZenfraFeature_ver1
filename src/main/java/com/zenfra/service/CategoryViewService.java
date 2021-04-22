package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.model.CategoryView;
import com.zenfra.repo.CategoryViewRepo;

@Service
public class CategoryViewService {

	
	@Autowired
	CategoryViewRepo categoryRepo;
	
	
	
	
}
