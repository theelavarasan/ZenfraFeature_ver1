package com.zenfra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.UserDao;
import com.zenfra.model.Users;
import com.zenfra.queries.UserTableQueries;

@Service
public class UserService {

	
	@Autowired
	UserDao userDao;
	
	@Autowired
	UserTableQueries userQueries;
	
	public Users getUserByUserId(String userId) {
		Users user=new Users();
		try {		
			
			System.out.println(userQueries.getGetUserByUserid().replace(":user_id", userId));
			user=(Users)userDao.getEntityByColumn(userQueries.getGetUserByUserid().replace(":user_id", userId),Users.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return user;
	}
	
	
	
}
