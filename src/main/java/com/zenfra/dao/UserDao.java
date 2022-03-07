package com.zenfra.dao;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class UserDao extends CommonEntityManager {

	@Autowired
	CommonFunctions functions;

	public Map<String, Object> login(String email, String password) {
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			String query = "select updated_time as \"updatedTime\", updated_by as \"updatedBy\", available_sites as \"availableSites\", company_name as \"companyName\", last_name, last_accessed_project_list as \"lastAccessedProjectList\", pin_status as \"pinStatus\",header_pin_status as \"headerPinStatus\",\r\n"
					+ "is_active as \"isActive\", user_id as \"userId\", favorite_menus, is_tenant_admin as \"isTenantAdmin\", password, last_visted_siteKey as \"lastVistedSiteKey\",\r\n"
					+ "created_by as \"createdBy\", custom_policy as \"customPolicy\", tenant_id as \"tenantId\", is_okta_user as \"isOktaUser\", created_time \"createdTime\", first_name as \"first_name\", is_menu_pos as \"isMenuPos\", email,\r\n"
					+ "json_object_agg(site_details, policy_resources::json) as \"availableResources\" from (\r\n"
					+ "select b.updated_time, b.updated_by, available_sites, company_name, last_name, last_accessed_project_list, pin_status,header_pin_status,\r\n"
					+ "b.is_active, user_id, favorite_menus, is_tenant_admin, password, last_visted_siteKey,\r\n"
					+ "b.created_by, custom_policy, b.tenant_id, is_okta_user, b.created_time, first_name, is_menu_pos, email, site_details, pl.resources as policy_resources from (\r\n"
					+ "select updated_time, updated_by, available_sites, company_name, last_name, last_accessed_project_list, pin_status,header_pin_status,\r\n"
					+ "is_active, user_id, favorite_menus, is_tenant_admin, password, last_visted_siteKey,\r\n"
					+ "created_by, custom_policy, tenant_id, is_okta_user, created_time, first_name, is_menu_pos, email, site_details, json_array_elements_text(policy_details::json) as policy_ids from (\r\n"
					+ "select updated_time, updated_by, available_sites, company_name, last_name, last_accessed_project_list, pin_status,header_pin_status,\r\n"
					+ "is_active, user_id, favorite_menus, is_tenant_admin, password, last_visted_siteKey,\r\n"
					+ "created_by, custom_policy, tenant_id, is_okta_user, created_time, first_name, is_menu_pos, email,\r\n"
					+ "json_array_elements(custom_policy::json) ->> 'siteKey' as site_details,\r\n"
					+ "json_array_elements(custom_policy::json) ->> 'policset' as policy_details from user_temp\r\n"
					+ "where email = '" + email + "' \r\n" + "and password = '" + password
					+ "' and  is_active=true \r\n" + ") a ) b "
					+ "LEFT JOIN policy pl on pl.policy_id = b.policy_ids\r\n" + ") c\r\n"
					+ "group by updated_time, updated_by, available_sites, company_name, last_name, last_accessed_project_list, pin_status,header_pin_status,\r\n"
					+ "is_active, user_id, favorite_menus, is_tenant_admin, password, last_visted_siteKey,\r\n"
					+ "created_by, custom_policy, tenant_id, is_okta_user, created_time, first_name, is_menu_pos, email";
			System.out.println("query::" + query);
			List<Map<String, Object>> obj1 = getListMapObjectById(query);

			if (obj1 != null && !obj1.isEmpty()) {
				map = obj1 != null && !obj1.isEmpty() && obj1.get(0) != null ? obj1.get(0)
						: new HashMap<String, Object>();
			}
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return new HashMap<String, Object>();
		}

	}

}
