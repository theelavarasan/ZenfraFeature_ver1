package com.zenfra.service;

import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import com.zenfra.utils.ZenfraFeaturesRestApis;


@Service
public class UserServiceImpl implements UserDetailsService {

	@Autowired
	ZenfraFeaturesRestApis featureApi;

	@SuppressWarnings("unchecked")
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
			JSONObject userObject=new JSONObject();
		try {
			

			    userObject=featureApi.login(username);
				if (userObject == null) {
					throw new UsernameNotFoundException("Invalid username or password.");
				}
				
				
				return new org.springframework.security.core.userdetails.User(userObject.get("email").toString(), userObject.get("password").toString(),
						getAuthority());
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());			
			return null;
		} 
		
	}

	private List<SimpleGrantedAuthority> getAuthority() {
		return Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

}
