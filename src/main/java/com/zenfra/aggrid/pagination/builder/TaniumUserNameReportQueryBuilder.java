package com.zenfra.aggrid.pagination.builder;

import static com.google.common.collect.Streams.zip;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.zenfra.dataframe.filter.ColumnFilter;
import com.zenfra.dataframe.filter.NumberColumnFilter;
import com.zenfra.dataframe.filter.SetColumnFilter;
import com.zenfra.dataframe.filter.TextColumnFilter;
import com.zenfra.dataframe.request.ColumnVO;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.request.SortModel;
import com.zenfra.utils.CommonFunctions;

public class TaniumUserNameReportQueryBuilder {
	
	@Autowired
	CommonFunctions utilities;
	
	@Autowired
	JdbcTemplate jdbc;

    private List<String> groupKeys;
    private List<String> rowGroups;
    private List<String> rowGroupsToInclude;
    private boolean isGrouping;
    private List<ColumnVO> valueColumns;
    private List<ColumnVO> pivotColumns;
    private Map<String, ColumnFilter> filterModel;
    private List<SortModel> sortModel;
    private int startRow, endRow;
    private List<ColumnVO> rowGroupCols;
    private Map<String, List<String>> pivotValues;
    private boolean isPivotMode;
    private List<String> ruleList;

    public String createSql(ServerSideGetRowsRequest request) {
        this.valueColumns = request.getValueCols();
        this.pivotColumns = request.getPivotCols();
        this.groupKeys = request.getGroupKeys();
        this.rowGroupCols = request.getRowGroupCols();
        this.pivotValues = pivotValues;
        this.isPivotMode = request.isPivotMode();
        this.rowGroups = getRowGroups();
        this.rowGroupsToInclude = getRowGroupsToInclude();
        this.isGrouping = rowGroups.size() > groupKeys.size();
        this.filterModel = request.getFilterModel();
        this.sortModel = request.getSortModel();
        this.startRow = request.getStartRow();
        this.endRow = request.getEndRow();

        System.out.println("creating sql query");
        //return selectSql() + fromSql(tableName) + whereSql() + groupBySql() + orderBySql() + limitSql();
        return getPrivillegeAccessReport(request.getSiteKey());
        
    }

    private String selectSql() {
        List<String> selectCols;
        if (isPivotMode && !pivotColumns.isEmpty()) {
            selectCols = concat(rowGroupsToInclude.stream(), extractPivotStatements()).collect(toList());
        } else {
            Stream<String> valueCols = valueColumns.stream()
                    .map(valueCol -> valueCol.getAggFunc() + '(' + valueCol.getField() + ") as " + valueCol.getField());

            selectCols = concat(rowGroupsToInclude.stream(), valueCols).collect(toList());
        }

        return isGrouping ? "SELECT " + join(", ", selectCols) : "SELECT *";
    }

    private String fromSql(String tableName) {
        return format(" FROM %s", tableName);
    }

    private String whereSql() {
        String whereFilters =
                concat(getGroupColumns(), getFilters())
                        .collect(joining(" AND "));

        return whereFilters.isEmpty() ? "" : format(" WHERE %s", whereFilters);
    }

    private String groupBySql() {
        return isGrouping ? " GROUP BY " + join(", ", rowGroupsToInclude) : "";
    }

    private String orderBySql() {
        Function<SortModel, String> orderByMapper = model -> model.getColId() + " " + model.getSort();

        boolean isDoingGrouping = rowGroups.size() > groupKeys.size();
        int num = isDoingGrouping ? groupKeys.size() + 1 : MAX_VALUE;

        List<String> orderByCols = sortModel.stream()
                .filter(model -> !isDoingGrouping || rowGroups.contains(model.getColId()))
                .map(orderByMapper)
                .limit(num)
                .collect(toList());

        return orderByCols.isEmpty() ? "" : " ORDER BY " + join(",", orderByCols);
    }

    private String limitSql() {
        return " OFFSET " + startRow + " ROWS FETCH NEXT " + (endRow - startRow + 1) + " ROWS ONLY";
    }

    private Stream<String> getFilters() {
        Function<Map.Entry<String, ColumnFilter>, String> applyFilters = entry -> {
            String columnName = entry.getKey();
            ColumnFilter filter = entry.getValue();

            if (filter instanceof SetColumnFilter) {
                return setFilter().apply(columnName, (SetColumnFilter) filter);
            }

            if (filter instanceof NumberColumnFilter) {
                return numberFilter().apply(columnName, (NumberColumnFilter) filter);
            }

            return "";
        };

        return filterModel.entrySet().stream().map(applyFilters);
    }

    private BiFunction<String, SetColumnFilter, String> setFilter() {
        return (String columnName, SetColumnFilter filter) ->
                columnName + (filter.getValues().isEmpty() ? " IN ('') " : " IN " + asString(filter.getValues()));
    }

    private BiFunction<String, NumberColumnFilter, String> numberFilter() {
        return (String columnName, NumberColumnFilter filter) -> {
            double filterValue = filter.getFilter();
            String filerType = filter.getType();
            String operator = operatorMap.get(filerType);

            return columnName + (filerType.equals("inRange") ?
                    " BETWEEN " + filterValue + " AND " + filter.getFilterTo() : " and " + filterValue);
        };
    }

    private Stream<String> extractPivotStatements() {

        // create pairs of pivot col and pivot value i.e. (DEALTYPE,Financial), (BIDTYPE,Sell)...
        List<Set<Pair<String, String>>> pivotPairs = pivotValues.entrySet().stream()
                .map(e -> e.getValue().stream()
                        .map(pivotValue -> Pair.of(e.getKey(), pivotValue))
                        .collect(toCollection(LinkedHashSet::new)))
                .collect(toList());

        // create a cartesian product of decode statements for all pivot and value columns combinations
        // i.e. sum(DECODE(DEALTYPE, 'Financial', DECODE(BIDTYPE, 'Sell', CURRENTVALUE)))
        return Sets.cartesianProduct(pivotPairs)
                .stream()
                .flatMap(pairs -> {
                    String pivotColStr = pairs.stream()
                            .map(Pair::getRight)
                            .collect(joining("_"));

                    String decodeStr = pairs.stream()
                            .map(pair -> "DECODE(" + pair.getLeft() + ", '" + pair.getRight() + "'")
                            .collect(joining(", "));

                    String closingBrackets = IntStream
                            .range(0, pairs.size() + 1)
                            .mapToObj(i -> ")")
                            .collect(joining(""));

                    return valueColumns.stream()
                            .map(valueCol -> valueCol.getAggFunc() + "(" + decodeStr + ", " + valueCol.getField() +
                                    closingBrackets + " \"" + pivotColStr + "_" + valueCol.getField() + "\"");
                });
    }

    private List<String> getRowGroupsToInclude() {
        return rowGroups.stream()
                .limit(groupKeys.size() + 1)
                .collect(toList());
    }

    private Stream<String> getGroupColumns() {
        return zip(groupKeys.stream(), rowGroups.stream(), (key, group) -> group + " = '" + key + "'");
    }

    private List<String> getRowGroups() {
        return rowGroupCols.stream()
                .map(ColumnVO::getField)
                .collect(toList());
    }

    private String asString(List<String> l) {
        return "(" + l.stream().map(s -> "\'" + s + "\'").collect(joining(", ")) + ")";
    }

    private Map<String, String> operatorMap = new HashMap<String, String>() {{
        put("equals", "=");
        put("notEqual", "<>");
        put("lessThan", "<");
        put("lessThanOrEqual", "<=");
        put("greaterThan", ">");
        put("greaterThanOrEqual", ">=");
    }};
    
    private String getPrivillegeAccessReport(String siteKey) {
		
		JSONParser parser = new JSONParser();
		
		String tasklistQuery = "select up.server_name\r\n"
				+ ", up.user_name\r\n"
				+ ", up.uid as user_id\r\n"
				+ ",coalesce(up.gid, '') as group_id\r\n"
				+ ",coalesce(ugi.group_name, '') as Primary_Group_Name\r\n"
				+ ",coalesce(mg.member_of_groups,'') as  Secondary_Group_Name\r\n"
				+ ",coalesce(si.sudo_privileges, '') as sudo_privileges_by_user\r\n"
				+ ",concat(case when length(coalesce(sgi.sudo_privileges, '')) > 0\r\n"
				+ "then concat(coalesce(sgi.sudo_privileges, ''), ', ',mg.sudo_privileges_by_group) else\r\n"
				+ "mg.sudo_privileges_by_group end) as sudo_privileges_by_group\r\n"
				+ ",coalesce(ua.user_alias,'') as member_of_user_alias\r\n"
				+ ",coalesce(ua.user_alias_sudo_privileges,'') as sudo_privileges_by_user_alias\r\n"
				+ ",ud.log_date as processeddate\r\n"
				+ ",hd.operating_system as os from (\r\n"
				+ "select site_key, server_name, user_name, uid, coalesce(gid, '') as gid, coalesce(default_login_shell, '') as default_login_shell,\r\n"
				+ "coalesce(home_dir, '') as home_dir\r\n"
				+ "from linux_user_profile where site_key = '" + siteKey + "' ) up\r\n"
				+ "left join linux_user_pwdsettings pwd on pwd.server_name = up.server_name and up.user_name = pwd.user_name and up.site_key = pwd.site_key\r\n"
				+ "LEFT JOIN linux_user_details ud on up.server_name = ud.server_name and up.user_name = ud.user_name and ud.site_key = up.site_key\r\n"
				+ "LEFT JOIN linux_user_sudo_info si on si.server_name = up.server_name and si.user_name = up.user_name and ud.site_key = si.site_key and si.is_group_user = 'false'\r\n"
				+ "LEFT JOIN linux_users_group_info ugi on ugi.server_name = up.server_name and ugi.gid = up.gid and ugi.site_key = ud.site_key\r\n"
				+ "LEFT JOIN linux_user_sudo_info sgi on sgi.server_name = up.server_name and sgi.user_name = up.user_name and ud.site_key = sgi.site_key and sgi.is_group_user = 'true'\r\n"
				+ "LEFT JOIN linux_host_details hd on hd.server_name = up.server_name and hd.site_key = up.site_key\r\n"
				+ "left join view_linux_user_alias ua on ua.server_name = up.server_name and ua.user_name = up.user_name and ua.site_key = up.site_key\r\n"
				+ "LEFT JOIN (\r\n"
				+ "SELECT ud2.site_key,ud2.server_name,ud2.user_name\r\n"
				+ ",string_agg(ugmi.group_name::text, ',') member_of_groups\r\n"
				+ ",string_agg(CASE when si2.user_name is not null then si2.sudo_privileges::text else null END, ',') sudo_privileges_by_group\r\n"
				+ ",sum(case when length(coalesce(si2.sudo_privileges, '')) > 0 then 1 else 0 end) as is_sudoers_by_group\r\n"
				+ "FROM linux_user_profile ud2\r\n"
				+ "JOIN linux_user_group_member_info ugmi on ugmi.server_name = ud2.server_name\r\n"
				+ "and ugmi.member = ud2.user_name and ugmi.site_key = '" + siteKey + "'\r\n"
				+ "LEFT JOIN linux_user_sudo_info si2 on si2.server_name = ud2.server_name and si2.site_key ='" + siteKey + "'\r\n"
				+ "and si2.user_name = ugmi.group_name and ud2.site_key = si2.site_key and si2.is_group_user ='true'\r\n"
				+ "where ud2.site_key ='" + siteKey + "' \r\n"
				+ "and ugmi.group_name is not null\r\n"
				+ "group by ud2.server_name,ud2.user_name ,ud2.site_key\r\n"
				+ ") mg on mg.site_key = up.site_key and mg.server_name = up.server_name and mg.user_name = up.user_name";

		System.out.println("!!!!! trackerQuery: " + tasklistQuery);

		return tasklistQuery;
	} 
    
    private String getTasklistFilters(Map<String, ColumnFilter> filters, String siteKey, String projectId) {
    	
    	StringBuilder filterQuery = new StringBuilder();
    	ObjectMapper mapper = new ObjectMapper();
    	try {
    		if(!filters.isEmpty()) {
    			
    			JSONObject filterObject = mapper.convertValue(filters, JSONObject.class);
    			System.out.println("!!!!! filterObject: " + filterObject);
    			Set<String> columnSet = filterObject.keySet();
    			
    			for(String column : columnSet) {
    				
    				JSONObject columnObject = mapper.convertValue(filters.get(column), JSONObject.class);
    				System.out.println("!!!!! columnObject: " + columnObject);
    				
    				JSONArray columnArray = new JSONArray();
    				String operator = " and";
    				if(columnObject.containsKey("condition1") && columnObject.get("condition1") != null && columnObject.containsKey("condition2") && columnObject.get("condition2") != null) {
    					columnArray.add(columnObject.get("condition1"));
    					columnArray.add(columnObject.get("condition2"));
    					operator = columnObject.get("operator") == null ? "" : columnObject.get("operator").toString();
    				} else {
    					columnArray.add(filters.get(column));
    				}
    				
    				if(!columnArray.isEmpty()) {
    					columnArray.remove(null);
    					columnArray.remove("null");
    				}
    				
    				System.out.println("!!!!! columnArray: " + columnArray);
    				for(int i = 0; i < columnArray.size(); i++) {
    				
    				ColumnFilter columnFilter = mapper.convertValue(columnArray.get(i), ColumnFilter.class);
    				 					
    				
    				if(columnFilter instanceof TextColumnFilter) {
    					
    					if(column.contains("Server Data~")) {
    						column = column.substring(column.indexOf("~") + 1, column.length());
    						if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " server_name = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " source_id = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " concat(server_name, '~', source_id) = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " server_name ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " source_id ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) { 
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") +  " concat(server_name,'~',source_id) ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") +  " server_name = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " source_id = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) { 
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " concat(server_name, '~', source_id) = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " server_name <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " and source_id <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) { 
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " concat(server_name, '~', source_id) <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " server_name ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " source_id ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) { 
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " concat(server_name,'~',source_id) ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " server_name ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " source_id ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) { 
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " concat(server_name, '~', source_id) ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " server_name <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " source_id <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " concat(server_name, '~', source_id) <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
        						if(column.equalsIgnoreCase("Server Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " server_name not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("User Name")) {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " source_id not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else if(column.equalsIgnoreCase("Server & User Name")) { 
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " concat(server_name, '~', source_id) not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				} else {
        	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				}
        					}
    					} 
    					
    					
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(column.contains("Server Data~")) {
    						if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numeric end) = " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	  				
    						} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	 	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	 	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numerics end) <> " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	      				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numeric end) > " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	    					        					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numeric end) < " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	    					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	   					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	  				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and ((case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + "");
        						filterQuery = filterQuery.append(" and (case when data::json ->> '" + column + "' = '' then 0 else data::json ->> '" + column + "'::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilterTo() + ")" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	  				
        					}
    					} 
    					
    				} else if(columnFilter instanceof SetColumnFilter) {
    					
    					JSONArray valueArray = new JSONArray();
    					valueArray.addAll(((SetColumnFilter) columnFilter).getValues());
    					if(valueArray.contains(null)) {
    						valueArray.remove(null);
    						valueArray.add("");
    					} else if(valueArray.contains("null")) {
    						valueArray.remove("null");
    						valueArray.add("");
    					}
	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + " data::json ->> '" + column + "' in (select json_array_elements_text('" + valueArray + "'::json))" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
	    				
    					
    				}
    			}
    				
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return filterQuery.toString();
    	
    } 
    
    private String getSourceDataFilters(Map<String, ColumnFilter> filters, String siteKey, String projectId) {
    	
    	StringBuilder filterQuery = new StringBuilder();
    	ObjectMapper mapper = new ObjectMapper();
    	try {
    		if(!filters.isEmpty()) {
    			
    			JSONObject filterObject = mapper.convertValue(filters, JSONObject.class);
    			System.out.println("!!!!! filterObject: " + filterObject);
    			Set<String> columnSet = filterObject.keySet();
    			
    			for(String column : columnSet) {
    				
    				JSONObject columnObject = mapper.convertValue(filters.get(column), JSONObject.class);
    				System.out.println("!!!!! columnObject: " + columnObject);
    				
    				JSONArray columnArray = new JSONArray();
    				String operator = " and";
    				if(columnObject.containsKey("condition1") && columnObject.get("condition1") != null && columnObject.containsKey("condition2") && columnObject.get("condition2") != null) {
    					columnArray.add(columnObject.get("condition1"));
    					columnArray.add(columnObject.get("condition2"));
    					operator = columnObject.get("operator") == null ? "" : columnObject.get("operator").toString();
    				} else {
    					columnArray.add(filters.get(column));
    				}
    				
    				if(!columnArray.isEmpty()) {
    					columnArray.remove(null);
    					columnArray.remove("null");
    				}
    				
    				System.out.println("!!!!! columnArray: " + columnArray);
    				for(int i = 0; i < columnArray.size(); i++) {
    				
    				ColumnFilter columnFilter = mapper.convertValue(columnArray.get(i), ColumnFilter.class);
    				 					
    				
    				if(columnFilter instanceof TextColumnFilter) {
    					
    					if(!column.contains("Server Data~")) {
    						
							String column1 = column.substring(column.indexOf("~") + 1, column.length());
							if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " lower(data::json ->> '" + column1 + "') = lower('" + ((TextColumnFilter) columnFilter).getFilter() + "')" +  ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	        				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' = '' " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' <> '' " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " lower(data::json ->> '" + column1 + "') <> lower('" + ((TextColumnFilter) columnFilter).getFilter() + "')" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					}
    					
    					}
    					
    					
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(!column.contains("Server Data~")) {
    						
							String column1 = column.substring(column.indexOf("~") + 1, column.length());
							if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "'::numeric end) = " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " and data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "')::numeric end) <> " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "')::numeric end) > " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "')::numeric end) < " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "')::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "')::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " (data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "')::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + "  data::json ->> '" + column1 + "' <> '' and (case when data::json ->> '" + column1 + "' = '' then 0 else (data::json ->> '" + column1 + "')::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    
        					
        					}
    					}
    					
    				} 
    			}
    				
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	String cedQuery = "and (source_id in (select distinct primary_key_value from source_data where source_id in (select source_id from source where is_active = true \r\n"
				+ "and site_key = '" + siteKey + "'\r\n"
				+ "union all select link_to from source where is_active = true and site_key = '" + siteKey + "') " + filterQuery.toString() + ") or \r\n"
				+ "server_name in (select distinct primary_key_value from source_data where source_id in (select source_id from source where is_active = true \r\n"
				+ "and site_key = '" + siteKey + "'\r\n"
				+ "union all select link_to from source where is_active = true and site_key = '" + siteKey + "')) " + filterQuery.toString() + ") ";
    	
    	return filterQuery.toString().isEmpty() ? "" : cedQuery;
    	
    } 
    
    private String getCEDFilters(Map<String, ColumnFilter> filters, JSONArray tpIDArray) {
    	
    	StringBuilder filterQuery = new StringBuilder();
    	
    	try {
    		if(!filters.isEmpty()) {
    			
    			Set<String> columnsSet = filters.keySet(); 
    			for(String column : columnsSet) {
    				ColumnFilter columnFilter = filters.get(column);
    				if(columnFilter instanceof TextColumnFilter) {
    					for(int i = 0; i < tpIDArray.size(); i++) {
    						if(column.startsWith(tpIDArray.get(i).toString())) {
    							if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
        							
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "' = '" + ((TextColumnFilter) columnFilter).getFilter() + "'");
            	    				
            					} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
            						
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'");
            	    				
            					} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
            						
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "' = ''");
            	    				
            					} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
            						
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "' <> ''");;
            	    				
            					} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
            						
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "') ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'");
            	    				
            					} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
            						
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'");
            	    				
            					} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
        							
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "' <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'");
            	    				
            					} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
            						
            	    				filterQuery = filterQuery.append(" and data::json ->> '" + column + "' not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'");
            	    				
            					}
    						}
    					}
    					
    				}
    				
    			}
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return filterQuery.toString();
    	
    }
    
    private String getOrderBy(List<SortModel> sortModel) {
    	
    	String orderBy = "";
    	
    	try {
    		for(SortModel s: sortModel) {
    			System.out.println("!!!!! colId: " + s.getActualColId());
    			if(s.getActualColId().startsWith("Server Data~")) {
    				String column_name = s.getActualColId().substring(s.getActualColId().indexOf("~") + 1, s.getActualColId().length());
    				System.out.println("!!!!! column_name: " + column_name);
    				if(column_name.equalsIgnoreCase("Server Name")) {
    					orderBy = " order by server_name " + s.getSort();
    				} else if(column_name.equalsIgnoreCase("User Name")) {
    					orderBy = " order by source_id " + s.getSort();
    				} else if(column_name.equalsIgnoreCase("Server & User Name")) {
    					orderBy = " order by concat(server_name, '~', source_id) " + s.getSort();
    				} else {
    					orderBy = " order by privillege_data::json ->> '" + column_name + "' " + s.getSort();
    				}
    			} /*else {
    					String column_name = s.getActualColId().substring(s.getActualColId().indexOf("~") + 1, s.getActualColId().length());
    					String column_alias = s.getActualColId().substring(0, s.getActualColId().indexOf("~"));
    					orderBy = "order by \"sd~" + column_alias + "_data\".data::json ->> '" + column_name + "') " + s.getSort() ;
    			}*/
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return orderBy;
    }
    
    private String getOrderBy1(List<SortModel> sortModel) {
    	
    	String orderBy = "";
    	
    	try {
    		for(SortModel s: sortModel) {
    			if(!s.getActualColId().contains("Server Data~")) {
    				String columnPrefix = s.getActualColId().substring(0, s.getActualColId().indexOf("~"));
    				String columnName = s.getActualColId().substring(s.getActualColId().indexOf("~") + 1, s.getActualColId().length());
    				orderBy = "order by (case when source_data1::text ilike '%\"" + columnPrefix + "\"%' then (select json_array_elements(source_data1::json) ->> '" + columnPrefix + "')::json ->> '" + columnName + "'\r\n"
    					+ "else (case when source_data2::text ilike '%\"" + columnPrefix + "\"%' then (select json_array_elements(source_data2::json) ->> '" + columnPrefix + "')::json ->> '" + columnName + "' \r\n"
    					+ " else (case when source_data3::text ilike '%\"" + columnPrefix + "\"%' then (select json_array_elements(source_data3::json) ->> '" + columnPrefix + "')::json ->> '" + columnName + "' \r\n"
    					+ " else (select json_array_elements(source_data4::json) ->> '" + columnPrefix + "')::json ->> '" + columnName + "' end) end) end) " + s.getSort();
    			} 
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return orderBy;
    }

}
