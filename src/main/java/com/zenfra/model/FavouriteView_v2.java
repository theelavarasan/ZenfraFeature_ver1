package com.zenfra.model;

import org.json.simple.JSONArray;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FavouriteView_v2 {

	ObjectMapper map = new ObjectMapper();
	
	public FavouriteView_v2(String updated_time, String updated_by, String report_name, String favourite_id,
			Object filter_property, boolean is_active, String user_access_list, String group_by_period, String site_key,
			String grouped_columns, String created_by, String category_list, String created_time, String favourite_name,
			String site_access_list, String project_id, String user_remove_list) {

		this.updated_time = updated_time;
		this.updated_by = updated_by;
		this.favourite_id = favourite_id;	
		if (filter_property != null) {
			map.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);			
			this.filter_property = map.convertValue(filter_property, JSONArray.class);
		} else {
			this.filter_property = new JSONArray();
		}
		this.is_active = is_active;

		this.user_access_list = user_access_list;

		this.group_by_period = group_by_period;
		this.site_key = site_key;
		this.grouped_columns = grouped_columns;
		this.created_by = created_by;
		this.category_list = category_list;
		this.created_time = created_time;
		this.favourite_name = favourite_name;
		this.site_access_list = site_access_list;
		this.project_id = project_id;
		this.user_remove_list = user_remove_list;
	}

	
	private String updated_time;
	private String updated_by;
	private String report_name;
	private String favourite_id;
	private JSONArray filter_property;

	private boolean is_active;
	private String user_access_list;
	private String group_by_period;
	private String site_key;
	private String grouped_columns;
	private String created_by;

	private String category_list;
	private String created_time;
	private String favourite_name;
	private String site_access_list;
	private String project_id;
	private String user_remove_list;

	public ObjectMapper getMap() {
		return map;
	}

	public void setMap(ObjectMapper map) {
		this.map = map;
	}


	public String getUpdated_time() {
		return updated_time;
	}

	public void setUpdated_time(String updated_time) {
		this.updated_time = updated_time;
	}

	public String getUpdated_by() {
		return updated_by;
	}

	public void setUpdated_by(String updated_by) {
		this.updated_by = updated_by;
	}

	public String getReport_name() {
		return report_name;
	}

	public void setReport_name(String report_name) {
		this.report_name = report_name;
	}

	public String getFavourite_id() {
		return favourite_id;
	}

	public void setFavourite_id(String favourite_id) {
		this.favourite_id = favourite_id;
	}

	public JSONArray getFilter_property() {
		return filter_property;
	}

	public void setFilter_property(JSONArray filter_property) {
		this.filter_property = filter_property;
	}

	public boolean isIs_active() {
		return is_active;
	}

	public void setIs_active(boolean is_active) {
		this.is_active = is_active;
	}

	public String getUser_access_list() {
		return user_access_list;
	}

	public void setUser_access_list(String user_access_list) {
		this.user_access_list = user_access_list;
	}

	public String getGroup_by_period() {
		return group_by_period;
	}

	public void setGroup_by_period(String group_by_period) {
		this.group_by_period = group_by_period;
	}

	public String getSite_key() {
		return site_key;
	}

	public void setSite_key(String site_key) {
		this.site_key = site_key;
	}

	public String getGrouped_columns() {
		return grouped_columns;
	}

	public void setGrouped_columns(String grouped_columns) {
		this.grouped_columns = grouped_columns;
	}

	public String getCreated_by() {
		return created_by;
	}

	public void setCreated_by(String created_by) {
		this.created_by = created_by;
	}

	public String getCategory_list() {
		return category_list;
	}

	public void setCategory_list(String category_list) {
		this.category_list = category_list;
	}

	public String getCreated_time() {
		return created_time;
	}

	public void setCreated_time(String created_time) {
		this.created_time = created_time;
	}

	public String getFavourite_name() {
		return favourite_name;
	}

	public void setFavourite_name(String favourite_name) {
		this.favourite_name = favourite_name;
	}

	public String getSite_access_list() {
		return site_access_list;
	}

	public void setSite_access_list(String site_access_list) {
		this.site_access_list = site_access_list;
	}

	public String getProject_id() {
		return project_id;
	}

	public void setProject_id(String project_id) {
		this.project_id = project_id;
	}

	public String getUser_remove_list() {
		return user_remove_list;
	}

	public void setUser_remove_list(String user_remove_list) {
		this.user_remove_list = user_remove_list;
	}

}
