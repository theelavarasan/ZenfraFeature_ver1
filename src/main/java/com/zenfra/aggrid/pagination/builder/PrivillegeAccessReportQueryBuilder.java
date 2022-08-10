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
import java.util.HashSet;
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
import com.zenfra.model.OperatorModel;

public class PrivillegeAccessReportQueryBuilder {
	
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

        //return selectSql() + fromSql(tableName) + whereSql() + groupBySql() + orderBySql() + limitSql();
        return getPrivillegeAccessReport(request.getSiteKey(), request.getProjectId(), request.getStartRow(), request.getEndRow(), request.getFilterModel(), request.getSortModel(),
        		request.getHealthCheckId(), request.getRuleList(), validationFilterQuery);
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
        put("contains", "ilike");
		put("notContains", "not ilike");
		put("startsWith", "ilike");
		put("endsWith", "ilike");
        put("Blanks", "=");
        put("Not Blanks", "<>");
        put("notEqual", "<>");
        put("lessThan", "<");
        put("lessThanOrEqual", "<=");
        put("greaterThan", ">");
        put("greaterThanOrEqual", ">=");
		put("inRange", "between");
    }};
    
    private String getPrivillegeAccessReport(String siteKey, String projectId, int startRow, int endRow, Map<String, ColumnFilter> filters, List<SortModel> sortModel,
    		String healthCheckId, List<String> ruleList, String validationFilterQuery) {
		
		JSONParser parser = new JSONParser();
		
		/*String tasklistQuery = "select * from ("
				+ "select row_count, source_id, server_name, privillege_data, json_agg(source_data1) as source_data1, json_agg(source_data2) as source_data2,"
				+ "json_agg(source_data3) as source_data3, json_agg(source_data4) as source_data4 from ( \r\n" 
				+ "select * from ( \r\n"
				+ "select row_count, a.source_id, server_name, a.data as privillege_data, (case when s1.source_name is null then null else json_build_object(s1.source_name,sd.data::json) end) as source_data1, \r\n"
				+ "(case when s2.source_name is null then null else json_build_object(s2.source_name, sd1.data::json) end) as source_data2,"
				+ "(case when sdls1.source_name is null then null else json_build_object(sdls1.source_name, sdl1.data::json) end) as source_data3,\r\n"
				+ "(case when sdls2.source_name is null then null else json_build_object(sdls2.source_name, sdl2.data::json) end) as source_data4 from (\r\n"
				+ "select count(1) over() as row_count,source_id, server_name, replace(replace(replace(replace(data, '.0\"', '\"'),'null', ''),':,',':\"\",'),': ,',':\"\",') as data from privillege_data\r\n"
				+ "where site_key = '" + siteKey + "' " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " + getTasklistFilters(filters, siteKey, projectId) + " " + getSourceDataFilters(filters, siteKey, projectId) + " " + " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + "\r\n"
				+ ") a\r\n"
				+ "LEFT JOIN source_data sd on sd.site_key = '" + siteKey + "' and lower(sd.primary_key_value) = lower(a.source_id) \r\n"
				+ "LEFT JOIN source s1 on s1.source_id = sd.source_id\r\n"
				+ "LEFT JOIN source_data sd1 on sd1.site_key = '" + siteKey + "' and lower(sd1.primary_key_value) = lower(a.server_name)\r\n"
				+ "LEFT JOIN source s2 on s2.source_id = sd1.source_id \r\n" 
				+ "LEFT JOIN source sdls1 on sdls1.link_to not in ('All', 'None') and sdls1.link_to = s1.source_id\r\n"
				+ "LEFT JOIN source_data sdl1 on sdl1.site_key = '" + siteKey + "' and sdl1.source_id = sdls1.source_id \r\n"
				+ "and lower(sdl1.primary_key_value) = lower(sd.data::json ->> sdls1.relationship) \r\n"
				+ "LEFT JOIN source sdls2 on sdls2.link_to not in ('All', 'None') and sdls2.link_to = s2.source_id\r\n"
				+ "LEFT JOIN source_data sdl2 on sdl2.site_key = '" + siteKey + "' and sdl2.source_id = sdls2.source_id \r\n"
				+ "and lower(sdl2.primary_key_value) = lower(sd1.data::json ->> sdls2.relationship) \r\n"
				+ ") b \r\n"
				+ ") b1 group by row_count, source_id, server_name, privillege_data \r\n" 
				+ ") a1 " + getOrderBy(sortModel) + getOrderBy1(sortModel) + "\r\n";*/
		
		/*String privillegeAccessReportQuery = "select * from ( select row_count, pd.source_id, pd.server_name, pd.privillege_data, json_collect(sd.data::json) as source_data1, "
				+ "json_collect(sd1.data::json) as source_data2 from (\r\n" + 
				"select count(1) over() as row_count, source_id, server_name, replace(data, 'null,', '\"\",') as privillege_data  from privillege_data \r\n" + 
				"where site_key = '" + siteKey + "' " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + getTasklistFilters(filters, siteKey, projectId) 
				+ getSourceDataFilters(filters, siteKey, projectId) + " order by server_name, source_id limit " 
				+ (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + " \r\n" + 
				") pd\r\n" + 
				"LEFT JOIN source_data sd on sd.site_key = '" + siteKey + "' and sd.primary_key_value = pd.source_id\r\n" + 
				"LEFT JOIN source_data sd1 on sd1.site_key = '" + siteKey + "' and sd1.primary_key_value = pd.server_name \r\n" +
				"group by row_count, pd.source_id, pd.server_name, pd.privillege_data \r\n" 
				+ ") a\r\n"
				+ getOrderBy(sortModel) + getOrderBy1(sortModel);*/
		
		String privillegeAccessReportQuery = "WITH PDDATA AS\r\n" + 
				"(\r\n" + 
				"    SELECT COUNT(1) over() AS row_count, * \r\n" + 
				"    FROM privillege_data_details \r\n" + 
				"    WHERE site_key = '" + siteKey + "' " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + getTasklistFilters(filters, siteKey, projectId) 
				+ getSourceDataFilters(filters, siteKey, projectId) + " \r\n" + 
				"),\r\n" + 
				"SDDATA AS\r\n" + 
				"(\r\n" + 
				"    SELECT primary_key_value, json_collect(data::json) AS sdjsondata\r\n" + 
				"    FROM source_data\r\n" + 
				"    WHERE site_key = '" + siteKey + "'\r\n" + 
				"    GROUP BY primary_key_value\r\n" + 
				")\r\n" + 
				"SELECT pdt.*, sdt.sdjsondata as source_data1, sdt1.sdjsondata as source_data2 \r\n" + 
				"FROM PDDATA AS pdt\r\n" + 
				"LEFT JOIN SDDATA AS sdt\r\n" + 
				"ON pdt.user_name = sdt.primary_key_value\r\n" + 
				"LEFT JOIN SDDATA AS sdt1\r\n" + 
				"ON pdt.server_name = sdt1.primary_key_value\r\n" 
				+ getOrderBy(sortModel) + getOrderBy1(sortModel) 
				+ " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + " \r\n";
				

		System.out.println("!!!!! trackerQuery: " + privillegeAccessReportQuery);

		return privillegeAccessReportQuery;
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
    						String column1 = column.substring(column.indexOf("~") + 1, column.length());
    						String value = ((TextColumnFilter) columnFilter).getFilter();
    						if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
    							value = "%" + ((TextColumnFilter) columnFilter).getFilter() + "%";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
    							value = "%" + ((TextColumnFilter) columnFilter).getFilter() + "%";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
    							value = ((TextColumnFilter) columnFilter).getFilter() + "%";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
    							value = "%" + ((TextColumnFilter) columnFilter).getFilter();
    						}
    						
    						System.out.println("filter type: " + OperatorModel.getOperator(((TextColumnFilter) columnFilter).getType()));
    						
    						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") +  column1 + " " + OperatorModel.getOperator(((TextColumnFilter) columnFilter).getType()) + " '" + value + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    					}  
    					
    					
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(column.contains("Server Data~")) {
    						String column1 = column.substring(column.indexOf("~") + 1, column.length());
    						String value = ((TextColumnFilter) columnFilter).getFilter();
    						
    						if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
    							value =  ((NumberColumnFilter) columnFilter).getFilter() + " and " + ((NumberColumnFilter) columnFilter).getFilterTo();
    						}
    						
    						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + column1 + "' <> '' and " + column1 + "::numeric " + OperatorModel.getOperator(((NumberColumnFilter) columnFilter).getType()) + " " + value + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    						
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
    	Set<String> sourceSet = new HashSet<String>();
    	JSONArray sourceArray = new JSONArray();
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
    						
							String column1 = column;
							String columnPrefix = column.substring(0, column.indexOf("~"));
							sourceSet.add(columnPrefix);
							if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " lower(coalesce(data::json ->> '" + column + "','')) = lower('" + ((TextColumnFilter) columnFilter).getFilter() + "')" +  ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    	        				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','')  = '' " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','')  <> '' " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') ilike '" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coaalesce(data::json ->> '" + column + "','') ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " lower(coalesce(data::json ->> '" + column + "','')) <> lower('" + ((TextColumnFilter) columnFilter).getFilter() + "')" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((TextColumnFilter) columnFilter).getType() != null && ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') not ilike '%" + ((TextColumnFilter) columnFilter).getFilter() + "%'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					}
    					
    					}
    					
    					
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(!column.contains("Server Data~")) {
    						
							String column1 = column;
							String columnPrefix = column.substring(0, column.indexOf("~"));
							sourceSet.add(columnPrefix);
							if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric = " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') = ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
        						
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') <> ''" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("notEqual")) {
    							
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric <> " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric > " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThan")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric < " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("lessThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric <= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("greaterThanOrEqual")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric >= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    			
        					} else if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
        						
        						filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + " (coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric >= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    				filterQuery = filterQuery.append(((i == 1) ? (" " + operator) : " and ") + ((columnArray.size() > 1 && i == 1) ? "(": "") + "  coalesce(data::json ->> '" + column + "','') <> '' and coalesce(data ->> '" + column + "','0')::numeric <= " + ((NumberColumnFilter) columnFilter).getFilter() + ((columnArray.size() > 1 && i == 1) ? ")": ""));
        	    
        					
        					}
    					}
    					
    				} 
    			}
    				
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	if(!sourceSet.isEmpty()) {
    		sourceArray.addAll(sourceSet);
    	}
    	String cedQuery = "and (source_id in (select distinct primary_key_value from source_data where site_key = '" + siteKey + "' and source_id in (select source_id from source where site_key = '" + siteKey + "' and source_name in (select json_array_elements_text('" + sourceArray + "')) union all select link_to from source where source_name in (select json_array_elements_text('" + sourceArray + "'))) " + filterQuery.toString() + ") or \r\n"
				+ "server_name in (select distinct primary_key_value from source_data where site_key = '" + siteKey + "' and source_id in (select source_id from source where site_key = '" + siteKey + "' and source_name in (select json_array_elements_text('" + sourceArray + "')) union all select source_id from source where source_name in (select json_array_elements_text('" + sourceArray + "'))) " + filterQuery.toString() + ")) ";
    	
    	return filterQuery.toString().isEmpty() ? "" : cedQuery;
    	
    	//return filterQuery.toString();
    	
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
    				orderBy = "order by (coalesce(sdt.sdjsondata,'{}'::json)::jsonb||coalesce(sdt1.sdjsondata,'{}'::json)::jsonb) ->> '" + s.getActualColId() + "' " + s.getSort();
    			} 
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return orderBy;
    }
    
    
}
