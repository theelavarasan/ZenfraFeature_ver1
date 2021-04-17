package com.zenfra.model;

public class FavouriteOrder_v2 {

	
	
	public FavouriteOrder_v2(String data_id,String updated_time,String site_key,String updated_by,
			String report_name,String created_by,String order_id,String created_time,
			boolean is_active,String project_id,String orders){
		
		
		this.data_id=data_id;
		this.updated_time=updated_time;
		this.site_key=site_key;
		this.updated_by=updated_by;
		this.report_name=report_name;
		this.created_by=created_by;
		this.order_id=order_id;
		this.created_time=created_time;
		this.is_active=is_active;
		this.project_id=project_id;
		this.orders=orders;
		
	}
	
	private String data_id;
	private String updated_time;
	private String site_key;
	private String updated_by;
	private String report_name;
	private String created_by;
	private String order_id;
	private String created_time;
	private boolean is_active;
	private String project_id;
	private String orders;
	public String getData_id() {
		return data_id;
	}
	public void setData_id(String data_id) {
		this.data_id = data_id;
	}
	public String getUpdated_time() {
		return updated_time;
	}
	public void setUpdated_time(String updated_time) {
		this.updated_time = updated_time;
	}
	public String getSite_key() {
		return site_key;
	}
	public void setSite_key(String site_key) {
		this.site_key = site_key;
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
	public String getCreated_by() {
		return created_by;
	}
	public void setCreated_by(String created_by) {
		this.created_by = created_by;
	}
	public String getOrder_id() {
		return order_id;
	}
	public void setOrder_id(String order_id) {
		this.order_id = order_id;
	}
	public String getCreated_time() {
		return created_time;
	}
	public void setCreated_time(String created_time) {
		this.created_time = created_time;
	}
	public boolean isIs_active() {
		return is_active;
	}
	public void setIs_active(boolean is_active) {
		this.is_active = is_active;
	}
	public String getProject_id() {
		return project_id;
	}
	public void setProject_id(String project_id) {
		this.project_id = project_id;
	}
	public String getOrders() {
		return orders;
	}
	public void setOrders(String orders) {
		this.orders = orders;
	}
	
	
	
	
	
}
