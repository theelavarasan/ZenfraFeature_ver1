package com.zenfra.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.model.FavouriteOrder_v2;
import com.zenfra.model.FavouriteView_v2;



@Component
public class FavouriteDao_v2 {

	
	@Autowired
	JdbcTemplate jdbc;
	
	@Autowired
	NamedParameterJdbcTemplate namedJdbc;
	
	public List<FavouriteView_v2> getFavouriteView(String query){
		
		List<FavouriteView_v2> view =new ArrayList<FavouriteView_v2>();
		try {
			
			JSONObject obj=new JSONObject();
			view= jdbc.query(query,  (rs, rowNum) ->(
					
					new FavouriteView_v2(
							rs.getString("updated_time"), rs.getString("updated_by"), rs.getString("report_name"),
							rs.getString("favourite_id"), rs.getObject("filter_property"),rs.getBoolean("is_active"),
							rs.getString("user_access_list"), rs.getString("group_by_period"), rs.getString("site_key"),
							rs.getString("grouped_columns"), rs.getString("created_by"), rs.getString("category_list"), 
							rs.getString("created_time"),rs.getString("favourite_name"), rs.getString("site_access_list"),
							rs.getString("project_id"),rs.getString("user_remove_list"))
					));
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		
		return view;
	}
	
	
	
public List<FavouriteOrder_v2> getFavouriteOrder(String query){
		
		List<FavouriteOrder_v2> view =new ArrayList<FavouriteOrder_v2>();
		try {
			
			view= jdbc.query(query,  (rs, rowNum) ->(
						
					new FavouriteOrder_v2(rs.getString("data_id"), 
							rs.getString("updated_time"), rs.getString("site_key"), rs.getString("updated_by"),
							rs.getString("report_name"), rs.getString("created_by"),rs.getString("order_id"),
							rs.getString("created_time"), rs.getBoolean("is_active"), rs.getString("project_id"),
							rs.getString("orders"))
					));
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		
		return view;
	}
	
	
	public JSONArray getJsonarray(String query) {
		
		JSONArray arr=new JSONArray();
		try {
			ObjectMapper map=new ObjectMapper();
			//map.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			List<Map<String, Object>> rows = jdbc.queryForList(query);

			
			rows.forEach(row->{
				if(row.get("user_access_list")!=null) {
					row.put("user_access_list", row.get("user_access_list").toString().replace("{", "[").replace("}", "]"));
				}
				arr.add(map.convertValue(row, JSONObject.class));
			});
	        
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return arr;
	}
	
	public Integer updateQuery(SqlParameterSource parameter,String query) {
		
		int responce=0;
		try {			
			 KeyHolder holder = new GeneratedKeyHolder();
			responce=namedJdbc.update(query,parameter);
		} catch (Exception e) {
			e.printStackTrace();			
		}
		
		return responce;
		
	}
	
	public Integer getCount(String query) {
		
		try {
			
			return  jdbc.queryForObject(query, Integer.class);
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return 0;
	}



	public int updateQuery(String query) {
		int responce=0;
		try {			
			 
			responce=jdbc.update(query);
		} catch (Exception e) {
			e.printStackTrace();			
		}
		
		return responce;
	}



}
