package com.zenfra.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="user_temp")
public class Users {

	
	@Id
	@Column(name="user_id")
	private String user_id;
	
	@Column(name="password")
	private String password;
	
	@Column(name="user_name")
	private String user_name;
	
	
	@Column(name="tenant_id")
	private String tenant_id;
	
	@Column(name="last_name")
	private String last_name;
	
	@Column(name="first_name")
	private String first_name;
	
	@Column(name="is_menu_pos")
	private boolean is_menu_pos;
	
	@Column(name="email")
	private String email;
	
	
	@Column(name="favorite_menus")
	private String favorite_menus;
	
	
	@Column(name="is_encrypt")
	private String is_encrypt;
	
	
	@Column(name="updated_time")
	private String updated_time;
	
	@Column(name="updated_by")
	private String updated_by;
	
	@Column(name="available_sites")
	private String available_sites;
	
	@Column(name="company_name")
	private String company_name;
	
	@Column(name="is_active")
	private boolean is_active;
	
	
	@Column(name="is_tenant_admin")
	private boolean is_tenant_admin;
	
	
	@Column(name="created_by")
	private String created_by;
	
	@Column(name="custom_policy")
	private String custom_policy;
	
	@Column(name="created_time")
	private String created_time;
	
	
	@Column(name="pin_status")
	private boolean pin_status=false;
	
	
	@Column(name="last_visted_sitekey")
	private String last_visted_sitekey;
	
	@Column(name="is_okta_user")
	private boolean is_okta_user;

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUser_name() {
		return user_name;
	}

	public void setUser_name(String user_name) {
		this.user_name = user_name;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getLast_name() {
		return last_name;
	}

	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}

	public String getFirst_name() {
		return first_name;
	}

	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}

	public boolean isIs_menu_pos() {
		return is_menu_pos;
	}

	public void setIs_menu_pos(boolean is_menu_pos) {
		this.is_menu_pos = is_menu_pos;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFavorite_menus() {
		return favorite_menus;
	}

	public void setFavorite_menus(String favorite_menus) {
		this.favorite_menus = favorite_menus;
	}

	public String getIs_encrypt() {
		return is_encrypt;
	}

	public void setIs_encrypt(String is_encrypt) {
		this.is_encrypt = is_encrypt;
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

	public String getAvailable_sites() {
		return available_sites;
	}

	public void setAvailable_sites(String available_sites) {
		this.available_sites = available_sites;
	}

	public String getCompany_name() {
		return company_name;
	}

	public void setCompany_name(String company_name) {
		this.company_name = company_name;
	}

	public boolean isIs_active() {
		return is_active;
	}

	public void setIs_active(boolean is_active) {
		this.is_active = is_active;
	}

	public boolean isIs_tenant_admin() {
		return is_tenant_admin;
	}

	public void setIs_tenant_admin(boolean is_tenant_admin) {
		this.is_tenant_admin = is_tenant_admin;
	}

	public String getCreated_by() {
		return created_by;
	}

	public void setCreated_by(String created_by) {
		this.created_by = created_by;
	}

	public String getCustom_policy() {
		return custom_policy;
	}

	public void setCustom_policy(String custom_policy) {
		this.custom_policy = custom_policy;
	}

	public String getCreated_time() {
		return created_time;
	}

	public void setCreated_time(String created_time) {
		this.created_time = created_time;
	}

	public boolean isPin_status() {
		return pin_status;
	}

	public void setPin_status(boolean pin_status) {
		this.pin_status = pin_status;
	}

	public String getLast_visted_sitekey() {
		return last_visted_sitekey;
	}

	public void setLast_visted_sitekey(String last_visted_sitekey) {
		this.last_visted_sitekey = last_visted_sitekey;
	}

	public boolean isIs_okta_user() {
		return is_okta_user;
	}

	public void setIs_okta_user(boolean is_okta_user) {
		this.is_okta_user = is_okta_user;
	}
	
	
	
	
}
