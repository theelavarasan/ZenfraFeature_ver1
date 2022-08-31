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
import com.zenfra.model.PrefixModel;
import com.zenfra.utils.CommonFunctions;

@Repository("privillegeAccessReportDAO")
public class PrivillegeAccessReportDAO {
	
	@Autowired
	CommonFunctions utilities;

    private JdbcTemplate template;
    private PrivillegeAccessReportQueryBuilder queryBuilder;
    
    PrefixModel prefixModel = new PrefixModel();
    
    

    @Autowired
    public PrivillegeAccessReportDAO(JdbcTemplate template) {
        this.template = template;
        queryBuilder = new PrivillegeAccessReportQueryBuilder();
    }

    public EnterpriseGetRowsResponse getData(ServerSideGetRowsRequest request) {
        String tableName = "privillege_data"; // could be supplied in request as a lookup key?
        
        // first obtain the pivot values from the DB for the requested pivot columns
        //Map<String, List<String>> pivotValues = getPivotValues(request.getPivotCols());
        if(request.getCategory().equalsIgnoreCase("Third Party Data")) {
        	request.setReportBy("thirdPartyData");
        }
        Map<String, List<String>> pivotValues = new HashMap<String, List<String>>();

        // generate sql
        
        String validationFilter = "";
        if(request.getHealthCheckId() != null && !request.getHealthCheckId().isEmpty()) {
        	String validationFilterQuery = getValidationRuleCondition(request.getSiteKey(), request.getHealthCheckId(), request.getRuleList(), request.getReportBy(), request.getOstype());
            List<Map<String, Object>> validationRows = utilities.getDBDatafromJdbcTemplate(validationFilterQuery);
            validationFilter = getValidationFilter(validationRows);
        }
        
        System.out.println("!!!!! reportBy DAO: " + request.getReportBy());
        String sql = queryBuilder.createSql(request, tableName, pivotValues, validationFilter, request.getReportBy(), getSourceMap(request.getSiteKey()));
        
        List<Map<String, Object>> rows = utilities.getDBDatafromJdbcTemplate(sql); //template.queryForList(sql);
        
        JSONArray resultArray = utilities.dataNormalize(rows, request.getReportBy());
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
    
    private String  getValidationRuleCondition(String siteKey, String healthCheckId, List<String> ruleList, String reportBy, String deviceType) {
    	
    	JSONArray ruleArray = new JSONArray();
		if(!ruleList.isEmpty()) {
			ruleArray.addAll(ruleList);
		}
		String prefix = PrefixModel.getPrefix(reportBy);
		
		String column1 = "";
		String mainColumn = "";
		
		if(reportBy.equalsIgnoreCase("User")) {
			column1 = " coalesce(coalesce(SDT.SDJSONDATA,''{}'')::jsonb ->> '''";
		} else if(reportBy.equalsIgnoreCase("Sudoers") || reportBy.equalsIgnoreCase("Sudoers Detail")) {
			column1 = "coalesce(coalesce(sd.data, ''{}'')::json ->> '''";
		} else if(reportBy.equalsIgnoreCase("Privileged Access") || reportBy.equalsIgnoreCase("Server")) {
			column1 = " coalesce(coalesce(sd.data,''{}'')::jsonb || coalesce(sd1.data,''{}'')::jsonb ->> '''";
		} else if(reportBy.equalsIgnoreCase("thirdPartyData")) {
			column1 = "coalesce(coalesce(sd.data, ''{}'')::json ->> '''";
		}
		
		if(deviceType.equalsIgnoreCase("activedirectory")) {
			if(reportBy.equalsIgnoreCase("Summary")) {
				column1 = " coalesce(coalesce(source_data1,''{}'')::jsonb || coalesce(source_data2,''{}'')::jsonb ->> '''";
			}
			
		}
		
		
		String validationRuleQuery = "select concat('(', string_agg(concat('(', condition_value, ')'), ' or '), ')') as condition_value from ( \r\n"
				+ "select string_agg(condition_value, (case when con_operator is null or trim(con_operator) = '' then ' AND ' else concat(' ', con_operator, ' ') \r\n"
				+ "end)) as condition_value from (\r\n"
				+ "select rule_id, con_operator, concat('(', string_agg( condition_value, (case when con_operator is null or trim(con_operator) = '' then ' AND ' else concat(' ', con_operator, ' ') \r\n"
				+ "end)) over(partition by rule_id, con_operator order by rule_id, con_id) , ')') \r\n"
				+ "as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, (case when con_operator is null or con_operator = '' then lead(con_operator) over(partition by rule_id \r\n"
				+ "order by con_id) else con_operator end) as con_operator, condition_field,\r\n"
				+ "(case when rule_row = 1 then concat(' ( ', condition_value, ' ) ') else condition_value end) as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, condition_field, string_agg(condition_value, ' ') \r\n"
				+ "over(partition by rule_id, con_operator) as condition_value,\r\n"
				+ "row_number() over(partition by rule_id, con_operator) as rule_row from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator,\r\n"
				+ " con_field_id as condition_field,\r\n"
				+ "concat((case when op_row = 1 then null else con_operator end), ' ', (case when con_field_id ilike '" + prefix + "%' then ' ' else '" + column1 + " end),  "
				+ "(case when con_field_id ilike '" + prefix + "%' then " + (!reportBy.equalsIgnoreCase("Summary") ? " replace(substring(con_field_id, position('~' in con_field_id) + 1, length(con_field_id)), 'servers_count', 'servers_count::numeric')" :   " \"con_field_id\" ") + " else concat(con_field_id,''','''')') end), ' ',\r\n"
				+ "(select con_value from tasklist_validation_conditions where con_name = con_condition),\r\n"
				+ "(case when con_condition = 'startsWith' then concat(' ''',con_value, '%''') else (case when con_condition = 'endsWith' then concat(' ''%',con_value, '''')\r\n"
				+ "else (case when con_condition = 'notBlank' then concat('''',con_value,'''') else (case when con_condition = 'blank' then concat(' ''',con_value,'''')\r\n"
				+ "else (case when con_condition = 'contains' or con_condition = 'notContains' then concat(' ''%',con_value, '%''') else concat(' ''',con_value, '''',(case when con_field_id ilike '%~servers_count' then '::numeric' else '' end)) end) end) end) end) end), "
				+ "(case when con_field_id ilike '" + prefix + "%' then '' else '' end)) as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value, op_row from (select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value, row_number() over(partition by rule_id, con_operator) as op_row from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, (case when con_operator is null or con_operator = '' then lead(con_operator) \r\n"
				+ "over(partition by rule_id order by rule_id, con_id) \r\n"
				+ "else con_operator end) as con_operator, con_condition, (case when con_field_id ilike '%~servers_count' then string_agg(con_value,'') \r\n"
				+ "else concat('(', string_agg(replace(replace(con_value,')', '\\)'),'(','\\('), '|'), ')') end) as con_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, (case when con_id is null then 0 else con_id::int end) as con_id,\r\n"
				+ "con_operator,\r\n"
				+ "con_condition,\r\n"
				+ "(case when con_condition = 'notBlank' or con_condition = 'blank' then ''\r\n"
				+ "else conditions::json ->> 'value' end) as con_value  from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, json_array_elements(conditions::json) as conditions, con_condition from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator,\r\n"
				+ "(case when conditions = '[]' then '[{\"value\":\"\", \"label\":\"\"}]' else coalesce(conditions, '[{\"value\":\"\", \"label\":\"\"}]') end) as conditions, con_condition from (\r\n"
				+ "select report_by, rule_id, json_array_elements(conditions::json) ->> 'field' as con_field_id,\r\n"
				+ "json_array_elements(conditions::json) ->> 'conditionId' as con_id,\r\n"
				+ "json_array_elements(conditions::json) ->> 'operator' as con_operator,\r\n"
				+ "json_array_elements(conditions::json) ->> 'value' as conditions, json_array_elements(conditions::json) ->> 'condition' as con_condition  from (\r\n"
				+ "select *, row_number() over(partition by rule_id) as row_number from (\r\n"
				+ "select report_by, json_array_elements(report_condition::json) ->> 'id' as rule_id,\r\n"
				+ "json_array_elements(report_condition::json) ->> 'conditions' as conditions\r\n"
				+ "from health_check where health_check_id = '" + healthCheckId + "'\r\n"
				+ ") a where rule_id in (select json_array_elements_text('" + ruleArray + "'))\r\n"
				+ ") a1 where row_number = 1\r\n"
				+ ") a2\r\n"
				+ ") a22\r\n"
				+ ") a3\r\n"
				+ ") b group by report_by, rule_id, con_field_id, con_id, con_operator, con_condition order by rule_id, con_id\r\n"
				+ ") c\r\n"
				+ ") d\r\n"
				+ ") e\r\n"
				+ ") e1\r\n"
				+ ") f order by con_id\r\n"
				+ ") d where rule_row = 1\r\n"
				+ ") g order by rule_id, con_id \r\n"
				+ ") f group by rule_id "
				+ ") h";
		
		/*String validationRuleQuery = "select string_agg(condition_value, (case when con_operator is null or trim(con_operator) = '' then ' AND ' else concat(' ', con_operator, ' ') \r\n"
				+ "end)) as condition_value from (\r\n"
				+ "select rule_id, con_operator, concat('(', string_agg( condition_value, (case when con_operator is null or trim(con_operator) = '' then ' AND ' else concat(' ', con_operator, ' ') \r\n"
				+ "end)) over(partition by con_operator order by con_id) , ')')  as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, condition_field,\r\n"
				+ "(case when rule_row = 1 then concat(' ( ', condition_value, ' ) ') else condition_value end) as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, condition_field, string_agg(condition_value, ' ') over(partition by rule_id, con_operator) as condition_value,\r\n"
				+ "row_number() over(partition by rule_id, con_operator) as rule_row from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator,\r\n"
				+ " con_field_id as condition_field,\r\n"
				+ "concat((case when op_row = 1 then null else con_operator end), ' ', (case when con_field_id ilike '" + prefix + "%' then ' ' else '" + column1 + " end),  "
				+ "(case when con_field_id ilike '" + prefix + "%' then replace(substring(con_field_id, position('~' in con_field_id) + 1, length(con_field_id)), 'servers_count', 'servers_count::numeric')  else concat(con_field_id,''','''')') end), ' ', \r\n"
				+ "(select con_value from tasklist_validation_conditions where con_name = con_condition),\r\n"
				+ "(case when con_condition = 'startsWith' then concat(' ''',con_value, '%''') else (case when con_condition = 'endsWith' then concat(' ''%',con_value, '''')\r\n"
				+ "else (case when con_condition = 'notBlank' then concat('''',con_value,'''') else (case when con_condition = 'blank' then concat(' ''',con_value,'''')\r\n"
				+ "else (case when con_condition = 'contains' or con_condition = 'notContains' then concat(' ''%',con_value, '%''') "
				+ "else concat(' ''',con_value, '''',(case when con_field_id ilike '%~servers_count' then '::numeric' else '' end)) end) end) end) end) end), (case when con_field_id ilike '" + prefix + "%' then '' else '' end)) as condition_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value, op_row from ("
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value, row_number() over(partition by rule_id, con_operator) as op_row from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, con_operator, con_condition, con_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, con_id, (case when con_operator is null then lead(con_operator) over(partition by rule_id order by rule_id, con_id) else con_operator end) as con_operator, con_condition, "
				+ "(case when con_field_id ilike '%~servers_count' then string_agg(con_value,'') else concat('(', string_agg(con_value, '|'), ')') end) as con_value from (\r\n"
				+ "select report_by, rule_id, con_field_id, (case when con_id is null then 0 else con_id::int end) as con_id,\r\n"
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
				+ ") b group by report_by, rule_id, con_field_id, con_id, con_operator, con_condition order by rule_id, con_id\r\n"
				+ ") c\r\n"
				+ ") d\r\n"
				+ ") e\r\n"
				+ ") e1\r\n"
				+ ") f order by con_id \r\n"
				+ ") d where rule_row = 1 \r\n"
				+ ") g order by con_id\r\n"
				+ ") f";*/
		
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
    	
    	return validationFilterQuery.isEmpty() ? "" : (" and (" + validationFilterQuery.trim() + ")");
    	
    }
    
    private Map<String, String> getSourceMap(String siteKey) {
    	
    	String query = "select (case when link_to not in ('All', 'None') then link_to else source_id end) as source_id, source_name from source where site_key = '" + siteKey + "'";
    	List<Map<String, Object>> rows = utilities.getDBDatafromJdbcTemplate(query);
    	
    	Map<String, String> sourceMap = new HashMap<>();
    	
    	try {
    		for(Map<String, Object> row : rows) {
    			sourceMap.put(row.get("source_name").toString(), row.get("source_id").toString());
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return sourceMap;
    	
    }

}
