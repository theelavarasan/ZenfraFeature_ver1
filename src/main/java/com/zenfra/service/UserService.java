package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.UserDao;
import com.zenfra.model.Users;
import com.zenfra.queries.UserTableQueries;

@Service
public class UserService {

	@Autowired
	CommonQueriesData queries;
	
	@Autowired
	UserDao userDao;
	
	public Users getUserByUserId(String userId) {
		Users user=new Users();
		try {			
			user=(Users)userDao.getEntityByColumn(queries.userTable().getGetUserByUserid().replace(":user_id", userId),Users.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return user;
	}
	
	
	
}
