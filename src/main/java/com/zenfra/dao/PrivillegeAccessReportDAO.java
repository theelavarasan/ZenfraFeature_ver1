package com.zenfra.dao;

import static com.zenfra.aggrid.pagination.builder.EnterpriseResponseBuilder.createResponseServerSide;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zenfra.aggrid.pagination.builder.PrivillegeAccessReportQueryBuilder;
import com.zenfra.aggrid.pagination.response.EnterpriseGetRowsResponse;
import com.zenfra.dataframe.request.ColumnVO;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.utils.CommonFunctions;

@Repository("privillegeAccessReportDAO")
public class PrivillegeAccessReportDAO {
	
	@Autowired
	CommonFunctions utilities;

    private JdbcTemplate template;
    private PrivillegeAccessReportQueryBuilder queryBuilder;
    
    

    @Autowired
    public PrivillegeAccessReportDAO(JdbcTemplate template) {
        this.template = template;
        queryBuilder = new PrivillegeAccessReportQueryBuilder();
    }

    public EnterpriseGetRowsResponse getData(ServerSideGetRowsRequest request) {
        String tableName = "privillege_data"; // could be supplied in request as a lookup key?
        
        // first obtain the pivot values from the DB for the requested pivot columns
        //Map<String, List<String>> pivotValues = getPivotValues(request.getPivotCols());
        Map<String, List<String>> pivotValues = new HashMap<String, List<String>>();

        // generate sql
        
        String validationFilter = "";
        if(request.getHealthCheckId() != null && !request.getHealthCheckId().isEmpty()) {
        	String validationFilterQuery = getValidationRuleCondition(request.getSiteKey(), request.getHealthCheckId(), request.getRuleList());
            List<Map<String, Object>> validationRows = utilities.getDBDatafromJdbcTemplate(validationFilterQuery);
            validationFilter = getValidationFilter(validationRows);
        }
        
        String sql = queryBuilder.createSql(request, tableName, pivotValues, validationFilter);
        
        List<Map<String, Object>> rows = utilities.getDBDatafromJdbcTemplate(sql); //template.queryForList(sql);
        JSONArray resultArray = dataNormalize(rows);
        //System.out.println("!!!!! pagination data: " + rows);
        // create response with our results
        int rowCount = rows.isEmpty() ? 0 : getRowCount(rows.get(0));
        return createResponseServerSide(request, resultArray, pivotValues, rowCount);
    }

    private Map<String, List<String>> getPivotValues(List<ColumnVO> pivotCols) {
        return pivotCols.stream()
                .map(ColumnVO::getField)
                .collect(toMap(pivotCol -> pivotCol, this::getPivotValues, (a, b) -> a, LinkedHashMap::new));
    }

    private List<String> getPivotValues(String pivotColumn) {
        String sql = "SELECT DISTINCT %s FROM trade";
        return template.queryForList(format(sql, pivotColumn), String.class);
    }
    
    @SuppressWarnings("unchecked")
	private JSONArray dataNormalize(List<Map<String, Object>> rows) {
    	
    	JSONArray resultArray = new JSONArray();
    	JSONParser parser = new JSONParser();
    	try {
    		for(Map<String, Object> row : rows) {
    			JSONObject resultObject = new JSONObject();
    			List<String> keys = new ArrayList<>(row.keySet());
    			for(int i = 0; i < keys.size(); i++) {
    				if(keys.get(i).equalsIgnoreCase("privillege_data")) {
    					JSONObject dataObject = (JSONObject) parser.parse(row.get(keys.get(i)) == null ? "{}" : row.get(keys.get(i)).toString());
    					if(!dataObject.isEmpty()) {
    						List<String> dataKeys = new ArrayList<>(dataObject == null ? new HashSet<>() : dataObject.keySet());
        					for(int j = 0; j < dataObject.size(); j++) {
        						
        						resultObject.put("Server Data~" + dataKeys.get(j), dataObject.get(dataKeys.get(j)));
        					}
    					}
    					
    				} else if(keys.get(i).contains("source_data")) {
    					JSONArray sourceDataArray = (JSONArray) parser.parse(row.get(keys.get(i)) == null ? "[]" : row.get(keys.get(i)).toString());
    					for(int j = 0; j < sourceDataArray.size(); j++) {
    						JSONObject sourceDataObject = (JSONObject) sourceDataArray.get(j);
    						Set<String> keySet = sourceDataObject == null ? new HashSet<>() : sourceDataObject.keySet();
    						for(String key : keySet) {
    							JSONObject jsonObject1 = (JSONObject) sourceDataObject.get(key);
    							Set<String> innerKeySet = jsonObject1 == null ? new HashSet<>() : jsonObject1.keySet();
    							for(String key1 : innerKeySet) {
    								if(key1 != null && key1.isEmpty() && (key1.equalsIgnoreCase("Processed Date"))) {
    									String value = jsonObject1.get(key1).toString();
    									value = formatDateStringToUtc(value);
    									resultObject.put(key + "~" + key1, value);
    								} else {
    									String value = jsonObject1.get(key1) != null && !jsonObject1.get(key1).toString().equalsIgnoreCase("null") ? jsonObject1.get(key1).toString() : "";
    									resultObject.put(key + "~" + key1, value);
    								}
    								String value = jsonObject1.get(key1) != null && !jsonObject1.get(key1).toString().equalsIgnoreCase("null") ? jsonObject1.get(key1).toString() : "";
    								resultObject.put(key + "~" + key1, value);
    							}
    						}
    					}
    					
    				} else {
    					if(!keys.get(i).equalsIgnoreCase("row_count")) {
    						if(keys.get(i).equalsIgnoreCase("server_name")) {
    							resultObject.put("Server Data~Server Name", row.get(keys.get(i)));
    						} else if(keys.get(i).equalsIgnoreCase("source_id")) {
    							resultObject.put("Server Data~User Name", row.get(keys.get(i)));
    						} else {
    							resultObject.put("Server Data~" + keys.get(i), row.get(keys.get(i)));
    						}
    						
    					} 
    				}
    			}
    			if(!resultObject.isEmpty()) {
    				resultArray.add(resultObject);
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return resultArray;
    }
    
    private int getRowCount(Map<String, Object> row) {
    	
    	int rowCount = 0;
    	try {
    		rowCount = Integer.parseInt(row.get("row_count").toString());
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return rowCount;
    	
    }
    
    private String formatDateStringToUtc(String value) {
		try {
			value = value.replaceAll("UTC", "").replaceAll("utc", "").trim();
			//System.out.println("!!!!! value: " + value);
			value = utilities.convertToUtc(value);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return value;
	}
    
    private String  getValidationRuleCondition(String siteKey, String healthCheckId, List<String> ruleList) {
    	
    	JSONArray ruleArray = new JSONArray();
		if(!ruleList.isEmpty()) {
			ruleArray.addAll(ruleList);
		}
		String validationRuleQuery = "select string_agg(condition_value, ' or ') as condition_value from (\r\n"
				+ "select rule_id, concat('(', string_agg(condition_value, ' '), ')') as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, condition_field,\r\n"
				+ "(case when con_id = 0 then concat(' ( ', condition_value, ' ) ') else condition_value end) as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, condition_field, string_agg(condition_value, ' or ') as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator,\r\n"
				+ " con_field_id as condition_field,\r\n"
				+ "concat(con_operator, (case when con_field_id ilike 'Server Data~%' then ' replace(data,'null,','\"\",')::json ->> ''' else ' source_id in (select distinct primary_key_value from \r\n"
				+ "source_data where site_key = '':site_key'' and data::json ->> ''' end),  substring(con_field_id, position('~' in con_field_id) + 1, length(con_field_id)), ''' ', \r\n"
				+ "(select con_value from tasklist_validation_conditions where con_name = con_condition),\r\n"
				+ "(case when con_condition = 'startsWith' then concat(' ''(',con_value, ')%''') else (case when con_condition = 'endsWith' then concat(' ''%(',con_value, ')''')\r\n"
				+ "else (case when con_condition = 'notBlank' then concat('''',con_value,'''') else (case when con_condition = 'blank' then concat('''',con_value,'''')\r\n"
				+ "else concat(' ''',con_value, '''') end) end) end) end), (case when con_field_id ilike 'Server Data~%' then '' else ')' end)) as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, coalesce(con_operator, '') as con_operator, con_condition, con_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value as con_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, (case when con_operator is null then 0 else 1 end) as con_id,\r\n"
				+ "con_operator,\r\n"
				+ "con_condition,\r\n"
				+ "(case when con_condition = 'notBlank' or con_condition = 'blank' then ''\r\n"
				+ "else conditions::json ->> 'value' end) as con_value  from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, json_array_elements(conditions::json) as conditions, con_condition from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, \r\n"
				+ "(case when conditions = '[]' then '[{\"value\":\"\", \"label\":\"\"}]' else coalesce(conditions, '[{\"value\":\"\", \"label\":\"\"}]') end) as conditions, con_condition from (\r\n"
				+ "select report_by, rule_id, json_array_elements(conditions::json) ->> 'field' as con_field_id,\r\n"
				+ "json_array_elements(conditions::json) ->> 'conditionId' as con_id,\r\n"
				+ "json_array_elements(conditions::json) ->> 'operator' as con_operator,\r\n"
				+ "json_array_elements(conditions::json) ->> 'value' as conditions, json_array_elements(conditions::json) ->> 'condition' as con_condition  from (\r\n"
				+ "select *, row_number() over(partition by rule_id) as row_number from (\r\n"
				+ "select report_by, json_array_elements(report_condition::json) ->> 'id' as rule_id,\r\n"
				+ "json_array_elements(report_condition::json) ->> 'conditions' as conditions \r\n"
				+ "from health_check where health_check_id = '" + healthCheckId + "'\r\n"
				+ ") a where rule_id in (select json_array_elements_text('" + ruleArray + "'))\r\n"
				+ ") a1 where row_number = 1\r\n"
				+ ") a2 \r\n"
				+ ") a22\r\n"
				+ ") a3\r\n"
				+ ") b order by con_id\r\n"
				+ ") c\r\n"
				+ ") d\r\n"
				+ ") e\r\n"
				+ ") f group by report_by, rule_id, con_field_id, con_id, con_operator, condition_field order by con_id\r\n"
				+ ") d\r\n"
				+ ") g group by rule_id\r\n"
				+ ") f";
		
		validationRuleQuery = validationRuleQuery.replace(":site_key", siteKey);
		System.out.println("!!!!! validation query: " + validationRuleQuery);
    	
    	return validationRuleQuery;
    }
    
    private String getValidationFilter(List<Map<String, Object>> rows) {
    	
    	String validationFilterQuery = "";
    	try {
    		for(Map<String, Object> row : rows) {
    			validationFilterQuery = row.get("condition_value") == null ? "" : row.get("condition_value").toString();
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return validationFilterQuery.isEmpty() ? "" : (" and " + validationFilterQuery.trim());
    	
    }

}
