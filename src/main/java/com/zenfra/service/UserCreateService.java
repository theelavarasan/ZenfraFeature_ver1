package com.zenfra.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.configuration.CommonQueriesData;
import com.zenfra.dao.UserDao;
import com.zenfra.model.Users;

@Service
public class UserCreateService{

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
	
	public Map<String,String> getUserNames() {
		Map<String,String> names=new HashMap<String, String>();
		try {			
			List<Object> obj=userDao.getEntityListByColumn("select * from user_temp", Users.class);
			
			for(Object c:obj) {
				Users user=(Users)c;
				if(user.getFirst_name()!=null && user.getLast_name()!=null) {
					names.put(user.getUser_id(),(user.getFirst_name()+" "+user.getLast_name()));
				}
					
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return names;
	}
	
	public List<Users> getAllUsers() {
		List<Object> usersObj = new ArrayList<Object>();
		List<Users> users = new ArrayList<Users>();
		try {			
			usersObj = userDao.getEntityListByColumn("select * from user_temp u where u.is_active='true'", Users.class);	
			for(Object obj : usersObj) {
				if(obj instanceof Users) {
					users.add((Users) obj);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return users;
	}
	
	public Users getUserByEmail(String username) {
		Users user=new Users();
		try {			
			user=(Users)userDao.getEntityByColumn(queries.userTable().getGetUserByEmail().replace(":email", username),Users.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return user;
	}
	
}
