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

    public String createSql(ServerSideGetRowsRequest request, String tableName, Map<String, List<String>> pivotValues, String validationFilterQuery) {
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
        return getUserAccessReport(request.getSiteKey(), request.getProjectId(), request.getStartRow(), request.getEndRow(), request.getFilterModel(), request.getSortModel(),
        		request.getHealthCheckId(), request.getRuleList(), validationFilterQuery, request.getReportBy());
        
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
    
    private String getUserAccessReport(String siteKey, String projectId, int startRow, int endRow, Map<String, ColumnFilter> filters, List<SortModel> sortModel,
    		String healthCheckId, List<String> ruleList, String validationFilterQuery, String reportBy) {
		
		JSONParser parser = new JSONParser();
		
		String tasklistQuery = "";
		if(reportBy.equalsIgnoreCase("User")) {
			tasklistQuery = "WITH SDDATA AS\r\n"
					+ "    (SELECT PRIMARY_KEY_VALUE,\r\n"
					+ "            JSON_collect(DATA::JSON) AS SDJSONDATA\r\n"
					+ "        FROM SOURCE_DATA AS SD\r\n"
					+ "        INNER JOIN SOURCE AS SR ON SD.SOURCE_ID = SR.SOURCE_ID\r\n"
					+ "        WHERE SD.SITE_KEY = '" + siteKey + "'\r\n"
					+ "            AND SR.IS_ACTIVE = true \r\n"
					+ "            AND (SR.LINK_TO = 'All' \r\n"
					+ "                                OR SR.LINK_TO = 'None')\r\n"
					+ "        GROUP BY SD.PRIMARY_KEY_VALUE)\r\n"
					+ "SELECT count(1) over() as row_count, USRD.USER_NAME,\r\n"
					+ "    USRD.USER_ID,\r\n"
					+ "    USRD.GROUP_ID,\r\n"
					+ "    USRD.SERVERS_COUNT,\r\n"
					+ "    USRD.PRIMARY_GROUP_NAME,\r\n"
					+ "    USRD.SECONDARY_GROUP_NAME,\r\n"
					+ "    USRD.SUDO_PRIVILEGES_BY_USER,\r\n"
					+ "    USRD.SUDO_PRIVILEGES_BY_PRIMARY_GROUP,\r\n"
					+ "    USRD.SUDO_PRIVILEGES_BY_SECONDARY_GROUP,\r\n"
					+ "    USRD.MEMBER_OF_USER_ALIAS,\r\n"
					+ "    USRD.SUDO_PRIVILEGES_BY_USER_ALIAS,\r\n"
					+ "    SDT.SDJSONDATA AS source_data, \r\n"
					+ "    '{}' as source_data1"
					+ "FROM USER_SUMMARY_REPORT_DETAILS AS USRD \r\n"
					+ "LEFT JOIN SDDATA AS SDT ON USRD.USER_NAME = SDT.PRIMARY_KEY_VALUE \r\n"
					+ "WHERE SITE_KEY = '" + siteKey + "' " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " + getTasklistFilters(filters, siteKey, projectId, reportBy) + " "
					+ getSourceDataFilters(filters, siteKey, projectId, reportBy) + " " + getOrderBy(sortModel, reportBy) + " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + "";
			
			System.out.println("!!!!! user summary report query: " + tasklistQuery);
		} else if(reportBy.equalsIgnoreCase("Sudoers")) {
			
			tasklistQuery = "WITH SSDDATA AS\r\n"
					+ "(\r\n"
					+ "    SELECT count(1) over() as row_count, SITE_KEY,\r\n"
					+ "       SERVER_NAME,\r\n"
					+ "       USER_NAME,\r\n"
					+ "       USER_ID,\r\n"
					+ "       COALESCE(GROUP_ID, '') AS GROUP_ID,\r\n"
					+ "       COALESCE(PRIMARY_GROUP_NAME, '') AS PRIMARY_GROUP_NAME,\r\n"
					+ "       COALESCE(SECONDARY_GROUP_NAME, '') AS SECONDARY_GROUP_NAME,\r\n"
					+ "       COALESCE(SUDO_PRIVILEGES_BY_USER, '') AS SUDO_PRIVILEGES_BY_USER,\r\n"
					+ "       COALESCE(SUDO_PRIVILEGES_BY_PRIMARY_GROUP, '') AS SUDO_PRIVILEGES_BY_PRIMARY_GROUP,\r\n"
					+ "       COALESCE(SUDO_PRIVILEGES_BY_SECONDARY_GROUP, '') AS SUDO_PRIVILEGES_BY_SECONDARY_GROUP,\r\n"
					+ "       COALESCE(MEMBER_OF_USER_ALIAS, '') AS USER_ALIAS_NAME,\r\n"
					+ "       COALESCE(SUDO_PRIVILEGES_BY_USER_ALIAS, '') AS SUDO_PRIVILEGES_BY_USER_ALIAS,\r\n"
					+ "       PROCESSEDDATE,\r\n"
					+ "       OPERATING_SYSTEM AS OS\r\n"
					+ "    FROM SUDOERS_SUMMARY_DETAILS\r\n"
					+ "    WHERE SITE_KEY = '" + siteKey + "' " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " + getTasklistFilters(filters, siteKey, projectId, reportBy) + " "
							+ getSourceDataFilters(filters, siteKey, projectId, reportBy) + " " + getOrderBy(sortModel, reportBy) + " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + " "
					+ "),\r\n"
					+ "SDDATA AS\r\n"
					+ "(    \r\n"
					+ "    SELECT SD.PRIMARY_KEY_VALUE,\r\n"
					+ "           JSON_collect(SD.DATA::JSON) AS SDJSONDATA\r\n"
					+ "    FROM SOURCE_DATA SD\r\n"
					+ "       INNER JOIN SOURCE AS SR ON SD.SOURCE_ID = SR.SOURCE_ID\r\n"
					+ "        WHERE SD.SITE_KEY = '" + siteKey + "'\r\n"
					+ "            AND SR.IS_ACTIVE = 'True'\r\n"
					+ "            AND (SR.LINK_TO = 'All'\r\n"
					+ "                                OR SR.LINK_TO = 'None')\r\n"
					+ "             GROUP BY SD.PRIMARY_KEY_VALUE\r\n"
					+ ")\r\n"
					+ "SELECT SSD.row_count, SSD.SERVER_NAME,\r\n"
					+ "       SSD.USER_NAME,\r\n"
					+ "       SSD.USER_ID,\r\n"
					+ "       SSD.GROUP_ID,\r\n"
					+ "       SSD.PRIMARY_GROUP_NAME,\r\n"
					+ "       SSD.SECONDARY_GROUP_NAME,\r\n"
					+ "       SSD.SUDO_PRIVILEGES_BY_USER,\r\n"
					+ "       SSD.SUDO_PRIVILEGES_BY_PRIMARY_GROUP,\r\n"
					+ "       SSD.SUDO_PRIVILEGES_BY_SECONDARY_GROUP,\r\n"
					+ "       SSD.USER_ALIAS_NAME,\r\n"
					+ "       SSD.SUDO_PRIVILEGES_BY_USER_ALIAS,\r\n"
					+ "       SSD.PROCESSEDDATE,\r\n"
					+ "       SSD.OS,\r\n"
					+ "       SDT1.SDJSONDATA AS source_data,\r\n"
					+ "       SDT2.SDJSONDATA AS source_data1\r\n"
					+ "FROM SSDDATA AS SSD\r\n"
					+ "LEFT JOIN SDDATA AS SDT1\r\n"
					+ "ON SSD.SERVER_NAME = SDT1.PRIMARY_KEY_VALUE\r\n"
					+ "LEFT JOIN SDDATA AS SDT2\r\n"
					+ "ON SSD.USER_NAME = SDT2.PRIMARY_KEY_VALUE\r\n"
					+ "WHERE SSD.SITE_KEY = '" + siteKey + "'";
		}
		

		return tasklistQuery;
	} 
    
    private String getTasklistFilters(Map<String, ColumnFilter> filters, String siteKey, String projectId, String reportBy) {
    	
    	StringBuilder filterQuery = new StringBuilder();
    	ObjectMapper mapper = new ObjectMapper();
    	
    	String prefix = "";
		
		if(reportBy.equalsIgnoreCase("User")) {
			prefix = "User Summary~";
		} else if(reportBy.equalsIgnoreCase("Sudoers")) {
			prefix = "Sudoers Summary~";
		}
		
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
    					
    					if(column.contains(prefix)) {
    						String column1 = column.substring(column.indexOf("~") + 1, column.length());
    						
    						if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
    							filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " = '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    							
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
    	    					
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        						
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
    	    					
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") +  " " + column1 + " = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        						
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
    	    					
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        						
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
    	    					
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        						
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
    	    					
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        						
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    	    					
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        						
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
    	    					
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        						
        					}
    					}
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(column.contains(prefix)) {
    						String column1 = column.substring(column.indexOf("~") + 1, column.length());
    					
    						if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
    							filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '' and (case when " + column + " = '' then 0 else " + column1 + "::numeric end) = " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	  				
    						} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	 	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	 	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '' and (case when " + column1 + " = '' then 0 else " + column1 + "::numerics end) <> " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	      				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '' and (case when " + column1 + " = '' then 0 else " + column1 + "::numeric end) > " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	    					        					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '' and (case when " + column1 + " = '' then 0 else " + column1 + "::numeric end) < " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	    					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '' and (case when " + column1 + " = '' then 0 else " + column1 + "::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	   					
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '' and (case when " + column1 + " = '' then 0 else " + column1 + "::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + "" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	  				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " " + column1 + " <> '' and ((case when " + column1 + " = '' then 0 else " + column1 + "::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + "");
        						filterQuery = filterQuery.append(" and (case when " + column1 + " = '' then 0 else" + column1 + "::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilterTo() + ")" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	  				
        					}
    					}
    					
    					
    				} else if(columnFilter instanceof SetColumnFilter) {
    					
    					String column1 = column.substring(column.indexOf("~") + 1, column.length());
    					
    					JSONArray valueArray = new JSONArray();
    					valueArray.addAll(((SetColumnFilter) columnFilter).getValues());
    					if(valueArray.contains(null)) {
    						valueArray.remove(null);
    						valueArray.add("");
    					} else if(valueArray.contains("null")) {
    						valueArray.remove("null");
    						valueArray.add("");
    					}
	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + " " + column1 + " in (select json_array_elements_text('" + valueArray + "'::json))" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
	    				
    					
    				}
    			}
    				
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return filterQuery.toString();
    	
    } 
    
    private String getSourceDataFilters(Map<String, ColumnFilter> filters, String siteKey, String projectId, String reportBy) {
    	
    	StringBuilder filterQuery = new StringBuilder();
    	ObjectMapper mapper = new ObjectMapper();
    	
    	String prefix = "";
		
		if(reportBy.equalsIgnoreCase("User")) {
			prefix = "User Summary~";
		} else if(reportBy.equalsIgnoreCase("Sudoers")) {
			prefix = "Sudoers Summary~";
		}
		
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
    					
    					if(!column.contains(prefix)) {
   						
							if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " lower(data::json ->> '" + column + "') = lower('" + ((TextColumnFilter) columnFilter).getFilter() + "')" +  ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	        				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') = '' " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " lower(data::json ->> '" + column + "') <> lower('" + ((TextColumnFilter) columnFilter).getFilter() + "')" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					}
    					
    					}
    					
    					
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(!column.contains(prefix)) {
							
							if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "'::numeric end) = " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " and data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "')::numeric end) <> " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "')::numeric end) > " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "')::numeric end) < " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "')::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "')::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " (data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "')::numeric end) >= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + "  data::json ->> '" + column + "' <> '' and (case when data::json ->> '" + column + "' = '' then 0 else (data::json ->> '" + column + "')::numeric end) <= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    
        					
        					}
    					}
    					
    				} 
    			}
    				
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	String cedQuery = "and user_name in (select distinct primary_key_value from source_data where site_key = '" + siteKey + "' " + filterQuery.toString() + ") ";
    	
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
    
    private String getOrderBy(List<SortModel> sortModel, String reportBy) {
    	
    	String orderBy = "";
    	
    	String prefix = "";
		
		if(reportBy.equalsIgnoreCase("User")) {
			prefix = "User Summary~";
		} else if(reportBy.equalsIgnoreCase("Sudoers")) {
			prefix = "Sudoers Summary~";
		}
    	
    	try {
    		for(SortModel s: sortModel) {
    			if(s.getActualColId().startsWith(prefix)) {
    				String columnName = s.getActualColId().substring(s.getActualColId().indexOf("~") + 1, s.getActualColId().length());
        			System.out.println("!!!!! colId: " + s.getActualColId());
        			orderBy = " order by " + columnName +  " " + s.getSort();
    			} else {
    				orderBy = " order by SDT.SDJSONDATA::json ->> '" + s.getActualColId()+ "' " + " " + s.getSort();
    			}
    			
    			
    			
    			
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
