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

public class TaniumGroupReportQueryBuilder {
	
	@Autowired
	CommonFunctions utilities;

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

    public String createSql(ServerSideGetRowsRequest request, String tableName, Map<String, List<String>> pivotValues) {
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

        //return selectSql() + fromSql(tableName) + whereSql() + groupBySql() + orderBySql() + limitSql();
        return getTaniumGroupReportQuery(request.getSiteKey(), request.getProjectId(), request.getStartRow(), request.getEndRow(), request.getFilterModel(), request.getSortModel());
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
            Integer filterValue = filter.getFilter();
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
    
    private String getTaniumGroupReportQuery(String siteKey, String projectId, int startRow, int endRow, Map<String, ColumnFilter> filters, List<SortModel> sortModel) {
		
		JSONParser parser = new JSONParser();
		

		String tasklistQuery = "select serverName,groupName, json_agg(data) as data from\r\n"
				+ "(select serverName, groupName,json_build_object('Server Name', serverName, 'Group Name', groupName, 'Member Of Group', memberOfGroup,\r\n"
				+ "'Sudoers Access', sudoPrivileges, 'Is Sudoers', isSudoers, 'Group Id', groupId, 'Os Version', osVersion) as data from\r\n"
				+ "(\r\n"
				+ "select\r\n"
				+ "ugi.server_name serverName\r\n"
				+ ",ugi.group_name groupName\r\n"
				+ ",ugi.gid groupId\r\n"
				+ ",ugi.member_of_group memberOfGroup\r\n"
				+ ",case when usi.user_name is null then 'No' else 'Yes' end as isSudoers\r\n"
				+ ",usi.sudo_privileges as sudoPrivileges\r\n"
				+ ",replace(hd.operating_system,'null','') as osVersion\r\n"
				+ "from linux_users_group_info ugi\r\n"
				+ "left join linux_user_sudo_info usi on ugi.server_name = usi.server_name and ugi.site_key = usi.site_key\r\n"
				+ "and ugi.group_name = usi.user_name and usi.is_group_user = 'true'\r\n"
				+ "join linux_host_details hd on hd.server_name = ugi.server_name and ugi.site_key = hd.site_key\r\n"
				+ "where ugi.site_key = '" + siteKey + "' " + getTasklistFilters(filters, siteKey) + " " + getOrderBy(sortModel) + " limit " + ((endRow - startRow) + 1) + " offset " + (startRow -1) + "\r\n"
				+ ") as d) as e\r\n"
				+ "group by e.serverName,e.groupName";

		System.out.println("!!!!! trackerQuery: " + tasklistQuery);

		return tasklistQuery;
	} 
    
    private String getTasklistFilters(Map<String, ColumnFilter> filters, String siteKey) {
    	
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
    					
						column = column.substring(column.indexOf("~") + 1, column.length());
						if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name = ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name = ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid = ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group = ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) = ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges = ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') = ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name <> ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name <> ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid <> ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group <> ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) <> ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges <> ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') <> ''" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
    						if(column.equalsIgnoreCase("Server Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.server_name not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Name")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.group_name not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Member Of Group")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.gid not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Sudoers Access")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " ugi.member_of_group not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Is Sudoers")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (case when usi.user_name is null then 'No' else 'Yes' end) not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Group Id")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " usi.sudo_privileges not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} else if(column.equalsIgnoreCase("Os Version")) {
    	    					filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " replace(hd.operating_system,'null','') not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + (columnArray.size() > 1 ? ")": ""));
    	    				} 
    					}
    					 
    				} /*else if(columnFilter instanceof NumberColumnFilter) {
    					
    						if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> '' and (case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numeric end) = " + ((NumberColumnFilter) columnFilter).getFilter() + "" + (columnArray.size() > 1 ? ")": ""));
    	  				
    						} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') = ''" + (columnArray.size() > 1 ? ")": ""));
    	 	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> ''" + (columnArray.size() > 1 ? ")": ""));
    	 	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> '' and (case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numerics end) <> " + ((NumberColumnFilter) columnFilter).getFilter() + "" + (columnArray.size() > 1 ? ")": ""));
    	      				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> '' and (case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numeric end) > " + ((NumberColumnFilter) columnFilter).getFilter() + "" + (columnArray.size() > 1 ? ")": ""));
    	    					        					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> '' and (case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numeric end) < " + ((NumberColumnFilter) columnFilter).getFilter() + "" + (columnArray.size() > 1 ? ")": ""));
    	    					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> '' and (case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilter() + "" + (columnArray.size() > 1 ? ")": ""));
    	   					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> '' and (case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + "" + (columnArray.size() > 1 ? ")": ""));
    	  				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + (columnArray.size() > 1 ? "(": "") + " (select json_array_elements(data::json) ->> '" + column + "') <> '' and ((case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + "");
        						filterQuery = filterQuery.append(" and (case when (select json_array_elements(data::json) ->> '" + column + "') = '' then 0 else (select json_array_elements(data::json) ->> '" + column + "')::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilterTo() + ")" + (columnArray.size() > 1 ? ")": ""));
    	  				
        					}
    					
    					
    				}*/ else if(columnFilter instanceof SetColumnFilter) {
    					
    					JSONArray valueArray = new JSONArray();
    					valueArray.addAll(((SetColumnFilter) columnFilter).getValues());
    					if(valueArray.contains(null)) {
    						valueArray.remove(null);
    						valueArray.add("");
    					} else if(valueArray.contains("null")) {
    						valueArray.remove("null");
    						valueArray.add("");
    					}
	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + " (select json_array_elements(data::json) ->> '" + column + "') in (select json_array_elements_text('" + valueArray + "'::json))" + (columnArray.size() > 1 ? ")": ""));
	    				
    					
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
    			if(!s.getColId().contains("~")) {
    				if(s.getColId().equalsIgnoreCase("Server Name")) {
    					orderBy = " order by ugi.server_name " + s.getSort();
    				} else if(s.getColId().equalsIgnoreCase("Group Name")) {
    					orderBy = " order by ugi.group_name " + s.getSort();
    				} else if(s.getColId().equalsIgnoreCase("Member Of Group")) {
    					orderBy = " order by ugi.gid " + s.getSort();
    				} else if(s.getColId().equalsIgnoreCase("Sudoers Access")) {
    					orderBy = " order by ugi.member_of_group " + s.getSort();
    				} else if(s.getColId().equalsIgnoreCase("Group Id")) {
    					orderBy = " order by (case when usi.user_name is null then 'No' else 'Yes' end) " + s.getSort();
    				} else if(s.getColId().equalsIgnoreCase("Server Name")) {
    					orderBy = " order by usi.sudo_privileges " + s.getSort();
    				} else if(s.getColId().equalsIgnoreCase("Os Version")) {
    					orderBy = " order by replace(hd.operating_system,'null','') " + s.getSort();
    				}
    			} 
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return orderBy;
    }


}
