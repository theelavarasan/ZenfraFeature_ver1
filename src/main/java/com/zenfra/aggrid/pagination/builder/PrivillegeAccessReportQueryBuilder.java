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
import com.zenfra.model.PrefixModel;

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
    
    OperatorModel operator = new OperatorModel();
    OperatorModel prefixModel = new OperatorModel();

    public String createSql(ServerSideGetRowsRequest request, String tableName, Map<String, List<String>> pivotValues, String validationFilterQuery, String reportBy, Map<String, String> sourceMap) {
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
        		request.getHealthCheckId(), request.getRuleList(), validationFilterQuery, reportBy, sourceMap, request.getCategory(), request.getThirdPartyId());
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
    		String healthCheckId, List<String> ruleList, String validationFilterQuery, String reportBy, Map<String, String> sourceMap, String category, String thirdPartyId) {
		
		JSONParser parser = new JSONParser();
		
		String taniumReportQuery = ""; 
		
		if(category.equalsIgnoreCase("User")) {
		if(reportBy.equalsIgnoreCase("Privileged Access") || reportBy.equalsIgnoreCase("Server")) {
			/*taniumReportQuery = "select * from ( WITH PDDATA AS\r\n" + 
					"(\r\n" + 
					"    SELECT * \r\n" + 
					"    FROM privillege_data_details \r\n" + 
					"    WHERE site_key = '" + siteKey + "' \r\n" 
					+ " \r\n " +
					"),\r\n" + 
					"SDDATA AS\r\n" + 
					"(\r\n" + 
					"    SELECT primary_key_value, json_collect(data::json) AS sdjsondata\r\n" + 
					"    FROM source_data\r\n" + 
					"    WHERE site_key = '" + siteKey + "'\r\n" + 
					"    GROUP BY primary_key_value\r\n" + 
					")\r\n" + 
					"SELECT pdt.*, count(1) over() as row_count,coalesce(sdt.sdjsondata,'{}') as source_data1, coalesce(sdt1.sdjsondata,'{}') as source_data2 \r\n" + 
					"FROM PDDATA AS pdt\r\n" + 
					"LEFT JOIN SDDATA AS sdt\r\n" + 
					"ON pdt.user_name = sdt.primary_key_value\r\n" + 
					"LEFT JOIN SDDATA AS sdt1\r\n" + 
					"ON pdt.server_name = sdt1.primary_key_value \r\n" +
					"where pdt.site_key = '" + siteKey + "' " 
					+ (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + getTasklistFilters(filters, siteKey, projectId, reportBy) 
					+ getSourceDataFilters(filters, siteKey, projectId, reportBy, sourceMap) + " \r\n" 
					+ getOrderBy(sortModel, reportBy) + getOrderBy1(sortModel, reportBy) +
					" limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0)
					+ ") a";*/
			
			taniumReportQuery = "select * from (select count(1) over() as row_count, server_name,user_name, user_id, is_sudoers_by_user, sudo_privileges_by_user, group_id, primary_group_name, secondary_group_name,\r\n"
					+ "is_sudoers_by_group, sudo_privileges_by_primary_group, sudo_privileges_by_secondary_group, member_of_user_alias, \r\n"
					+ "is_sudoers_by_user_alias, sudo_privileges_by_user_alias, processeddate, default_login_shell, home_dir, account_expiration_date, \r\n"
					+ "date_of_last_pwd_change, num_of_days_after_pwd_exp_to_disable_the_account, num_of_days_in_advance_to_dis_pwd_exp_msg, \r\n"
					+ "max_required_days_btw_pwd_changes, min_required_days_btw_pwd_changes, operating_system,  \r\n"
					+ "json_collect(sd.data::json) as source_data1, json_collect(sd1.data::json) as source_data2 from privillege_data_details pd \r\n"
					+ "LEFT JOIN source_data sd on sd.primary_key_value = pd.user_name and sd.site_key = '" + siteKey + "' \r\n"
					+ "LEFT JOIN source_data sd1 on sd1.primary_key_value = pd.server_name and sd1.site_key = '" + siteKey + "' \r\n"
					+ "where pd.site_key = '" + siteKey + "' \r\n" 
					+ (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + getTasklistFilters(filters, siteKey, projectId, reportBy) 
					+ getSourceDataFilters(filters, siteKey, projectId, reportBy, sourceMap) + " \r\n" 
					+ "group by server_name,user_name, user_id, is_sudoers_by_user, sudo_privileges_by_user, group_id, primary_group_name, secondary_group_name,\r\n"
					+ "is_sudoers_by_group, sudo_privileges_by_primary_group, sudo_privileges_by_secondary_group, member_of_user_alias, \r\n"
					+ "is_sudoers_by_user_alias, sudo_privileges_by_user_alias, processeddate, default_login_shell, home_dir, account_expiration_date, \r\n"
					+ "date_of_last_pwd_change, num_of_days_after_pwd_exp_to_disable_the_account, num_of_days_in_advance_to_dis_pwd_exp_msg, \r\n"
					+ "max_required_days_btw_pwd_changes, min_required_days_btw_pwd_changes, operating_system\r\n" 
					+ getOrderBy(sortModel, reportBy) + getOrderBy1(sortModel, reportBy) + " \r\n"
					+ "limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) 
					+ ") a \r\n";
					
					
			
		} else if(reportBy.equalsIgnoreCase("User")) {
			
			taniumReportQuery = "select * from ( WITH SDDATA AS\r\n"
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
					+ "    coalesce(SDT.SDJSONDATA, '{}') AS source_data1,"
					+ " '{}' as source_data2, \r\n"
					+ " USRD.processeddate "
					+ "FROM USER_SUMMARY_REPORT_DETAILS AS USRD \r\n"
					+ "LEFT JOIN SDDATA AS SDT ON USRD.USER_NAME = SDT.PRIMARY_KEY_VALUE \r\n"
					+ "WHERE SITE_KEY = '" + siteKey + "' " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " + getTasklistFilters(filters, siteKey, projectId, reportBy) + " "
					+ getSourceDataFilters(filters, siteKey, projectId, reportBy, sourceMap) + " " + getOrderBy(sortModel, reportBy) + getOrderBy1(sortModel, reportBy) 
					+ " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + ") a ";
					
			
		} else if(reportBy.equalsIgnoreCase("Sudoers")) {
			
			/*taniumReportQuery = "select * from ( WITH SSDDATA AS\r\n"
					+ "(\r\n"
					+ "    SELECT SITE_KEY,\r\n"
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
					+ "       OPERATING_SYSTEM \r\n"
					+ "    FROM SUDOERS_SUMMARY_DETAILS\r\n"
					+ "    WHERE SITE_KEY = '" + siteKey + "' " 
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
					+ "SELECT count(1) over() as row_count, SSD.SERVER_NAME,\r\n"
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
					+ "       SSD.OPERATING_SYSTEM,\r\n"
					+ "       coalesce(SDT1.SDJSONDATA,'{}') AS source_data1,\r\n"
					+ "       coalesce(SDT2.SDJSONDATA, '{}') AS source_data2 \r\n"
					+ "FROM SSDDATA AS SSD\r\n"
					+ "LEFT JOIN SDDATA AS SDT1\r\n"
					+ "ON SSD.SERVER_NAME = SDT1.PRIMARY_KEY_VALUE\r\n"
					+ "LEFT JOIN SDDATA AS SDT2\r\n"
					+ "ON SSD.USER_NAME = SDT2.PRIMARY_KEY_VALUE\r\n"
					+ "WHERE SSD.SITE_KEY = '" + siteKey + "' " 
					+ (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " + getTasklistFilters(filters, siteKey, projectId, reportBy) + " "
					+ getSourceDataFilters(filters, siteKey, projectId, reportBy, sourceMap) + " " 
							+ getOrderBy(sortModel, reportBy) + getOrderBy1(sortModel, reportBy) 
					+ " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + " \r\n"
					+ ")a ";*/
			
			taniumReportQuery = "select * from (select count(1) over() as row_count,user_name, processeddate, user_id, group_id, primary_group_name, secondary_group_name, sudo_privileges_by_user, sudo_privileges_by_primary_group, \r\n"
					+ "sudo_privileges_by_secondary_group, user_alias_name, sudo_privileges_by_user_alias, servers_count, json_collect(sd.data::json) as source_data1 "
					+ "from user_sudoers_summary_details ud \r\n"
					+ "LEFT JOIN source_data sd on sd.primary_key_value = ud.user_name and sd.site_key = '" + siteKey + "'\r\n"
					+ "WHERE ud.site_key = '" + siteKey + "'  \r\n " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " 
					+ getTasklistFilters(filters, siteKey, projectId, reportBy) + " "
					+ getSourceDataFilters(filters, siteKey, projectId, reportBy, sourceMap) + " "
					+ "group by user_name, processeddate, user_id, group_id, primary_group_name, secondary_group_name, sudo_privileges_by_user, sudo_privileges_by_primary_group, \r\n"
					+ "sudo_privileges_by_secondary_group, user_alias_name, sudo_privileges_by_user_alias, servers_count \r\n" 
					+ getOrderBy(sortModel, reportBy) + getOrderBy1(sortModel, reportBy)
					+ " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + " \r\n"
					+ ")a \r\n";
					
					
			
		} else if(reportBy.equalsIgnoreCase("Sudoers Detail")) {
			
			taniumReportQuery = "select server_name, user_name, user_id, group_id, primary_group_name, secondary_group_name, sudo_privileges_by_user, \r\n"
					+ "sudo_privileges_by_primary_group, sudo_privileges_by_secondary_group, member_of_user_alias, sudo_privileges_by_user_alias, \r\n"
					+ "processeddate, operating_system, is_group_user, json_collect(coalesce(sd.data, '{}')::json) as source_data from sudoers_summary_details ssd \r\n"
					+ "LEFT JOIN source_data sd on sd.primary_key_value = ssd.user_name and sd.site_key = '" + siteKey + "' \r\n"
					+ "where ssd.site_key = '" + siteKey + "' \r\n " + (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " 
					+ getTasklistFilters(filters, siteKey, projectId, reportBy) + " "
					+ getSourceDataFilters(filters, siteKey, projectId, reportBy, sourceMap) + " "
					+ "group by server_name, user_name, user_id, group_id, primary_group_name, secondary_group_name, sudo_privileges_by_user, \r\n"
					+ "sudo_privileges_by_primary_group, sudo_privileges_by_secondary_group, member_of_user_alias, sudo_privileges_by_user_alias, \r\n"
					+ "processeddate, operating_system, is_group_user\r\n"
					+ getOrderBy(sortModel, reportBy) + getOrderBy1(sortModel, reportBy)
					+ " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + " \r\n";
		}
		} else if(category.equalsIgnoreCase("Third Party Data")) {
			
			String[] sourceId = thirdPartyId.split("~"); 
			String secondaryCondition = "";
			if(thirdPartyId.contains("~link_true~")) {
				secondaryCondition = " and data::json ->> '" + sourceId[1] + "~" + sourceId[4] + "' in (select primary_key_value from source_data where source_id = '" + sourceId[5] + "')";
			}
			taniumReportQuery = "select count(1) over() as row_count, sd.data as source_data, usr.* from source_data sd \r\n"
					+ "LEFT JOIN user_summary_report_details usr on usr.user_name = sd.primary_key_value \r\n"
					+ "where sd.site_key = '" + siteKey + "' and sd.source_id = '" + sourceId[2] + "' " + secondaryCondition + " \r\n" 
					+ (!validationFilterQuery.isEmpty() ? validationFilterQuery: "") + " " 
					+ getTasklistFilters(filters, siteKey, projectId, "thirdPartyData") + " "
					+ getSourceDataFilters(filters, siteKey, projectId, "thirdPartyData", sourceMap) + " "
					+ getOrderBy(sortModel, "thirdPartyData") + getOrderBy1(sortModel, "thirdPartyData") + " \r\n"
					+ " limit " + (startRow > 0 ? ((endRow - startRow) + 1) : endRow) + " offset " + (startRow > 0 ? (startRow - 1) : 0) + " \r\n";
		}
		
		/*if(!getOrderBy1(sortModel, reportBy).isEmpty()) {
			
			taniumReportQuery = taniumReportQuery.replace("{<%dummyvalue%>}", setDummyValue(sortModel, taniumReportQuery).toJSONString());
		} else {
			taniumReportQuery = taniumReportQuery.replace("{<%dummyvalue%>}", "{}");
		}*/

		System.out.println("!!!!! trackerQuery: " + taniumReportQuery);

		return taniumReportQuery;
	} 
    
    private String getTasklistFilters(Map<String, ColumnFilter> filters, String siteKey, String projectId, String reportBy) {
    	
    	StringBuilder filterQuery = new StringBuilder();
    	ObjectMapper mapper = new ObjectMapper();
    	
    	System.out.println("!!!!! reportBy: " + reportBy);
    	String prefix = PrefixModel.getPrefix(reportBy);
    	
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
    					
    					if(reportBy.equalsIgnoreCase("Sudoers Detail")) {
    						column = column.replace("Server Summary~", "Sudoers Detail~");
    			    	}
    					if(column.contains(prefix)) {
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
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("blank") || ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
    							value = "";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notBlank") || ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
    							value = "";
    						}
    						
    						System.out.println("filter type: " + ((TextColumnFilter) columnFilter).getType());
    						System.out.println("filter type: " + OperatorModel.getOperator(((TextColumnFilter) columnFilter).getType()));
    						
    						filterQuery = filterQuery.append(((i == 1) ? (" " + operator + " ") : " and ") + ((columnArray.size() > 1 && i == 0) ? "(": "") +  column1 + " " + OperatorModel.getOperator(((TextColumnFilter) columnFilter).getType()) + " '" + value + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    					}  
    					
    					
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(column.contains(prefix)) {
    						String column1 = column.substring(column.indexOf("~") + 1, column.length());
    						String value = "";
    						double value1 = ((NumberColumnFilter) columnFilter).getFilter();
    						double value2 = 0;
    						
    						if(((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
    							value2 = ((NumberColumnFilter) columnFilter).getFilterTo();
    						}
    						
    						if(((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
    							filterQuery = filterQuery.append(((i == 1) ? (" " + operator + " ") : " and ") + ((columnArray.size() > 1 && i == 0) ? "(": "") + column1 + " <> '' and " + column1 + "::numeric " + OperatorModel.getOperator(((NumberColumnFilter) columnFilter).getType()) + " " + value1 + " and " + value2 + " " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    						} else {
    							filterQuery = filterQuery.append(((i == 1) ? (" " + operator+ " ") : " and ") + ((columnArray.size() > 1 && i == 0) ? "(": "") + column1 + " <> '' and " + column1 + "::numeric " + OperatorModel.getOperator(((NumberColumnFilter) columnFilter).getType()) + " " + value1 + ((columnArray.size() > 1 && i == 1) ? ")": ""));
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
    
    private String getSourceDataFilters(Map<String, ColumnFilter> filters, String siteKey, String projectId, String reportBy, Map<String, String> sourceMap) {
    	
    	StringBuilder filterQuery = new StringBuilder();
    	ObjectMapper mapper = new ObjectMapper();
    	JSONArray sourceArray = new JSONArray();
    	
    	String prefix = PrefixModel.getPrefix(reportBy);
    	
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
    				System.out.println("!!!!! columnObject: " + columnObject);
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
    					
    					if(reportBy.equalsIgnoreCase("Sudoers Detail")) {
    						column = column.replace("Server Summary~", "Sudoers Detail~");
    			    	}
    					
    					if(!column.contains(prefix)) {
    						
							String column1 = column;
							String columnPrefix = column.substring(0, column.indexOf("~"));
							
							if(!sourceArray.contains(sourceMap.get(columnPrefix))) {
								sourceArray.add(sourceMap.get(columnPrefix));
							}
							
							//String column1 = column.substring(column.indexOf("~") + 1, column.length());
    						String value = ((TextColumnFilter) columnFilter).getFilter();
    						if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("contains")) {
    							value = "%" + ((TextColumnFilter) columnFilter).getFilter() + "%";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notContains")) {
    							value = "%" + ((TextColumnFilter) columnFilter).getFilter() + "%";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("startsWith")) {
    							value = ((TextColumnFilter) columnFilter).getFilter() + "%";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("endsWith")) {
    							value = "%" + ((TextColumnFilter) columnFilter).getFilter();
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("blank") || ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Blanks")) {
    							value = "";
    						} else if(((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("notBlank") || ((TextColumnFilter) columnFilter).getType().equalsIgnoreCase("Not Blanks")) {
    							value = "";
    						}
    						
    						if(reportBy.equalsIgnoreCase("User")) {
    							column1 = "coalesce(coalesce(SDT.SDJSONDATA,'{}')::jsonb ->> '" + column + "','') ";
    						} else if(reportBy.equalsIgnoreCase("Sudoers") || reportBy.equalsIgnoreCase("Sudoers Detail")) {
    							column1 = "coalesce(coalesce(sd.data, '{}')::json ->> '" + column + "','') ";
    						} else if(reportBy.equalsIgnoreCase("thirdPartyData")) {
    							column1 = "coalesce(coalesce(sd.data, '{}')::json ->> '" + column + "','') ";
    						} else {
    							column1 = "coalesce(coalesce(sd.data,'{}')::jsonb || coalesce(sd1.data,'{}')::jsonb ->> '" + column + "','') ";
    						}
    						
    						//System.out.println("filter type: " + ((TextColumnFilter) columnFilter).getType());
    						//System.out.println("filter type: " + OperatorModel.getOperator(((TextColumnFilter) columnFilter).getType()));
    						System.out.println("!!!!! columnArray size: " + columnArray.size() + " ---- i == " + i);
    						filterQuery = filterQuery.append(((i == 1) ? (" " + operator + " ") : " and ") + ((columnArray.size() > 1 && i == 0) ? "(": "") +  column1 + " " + OperatorModel.getOperator(((TextColumnFilter) columnFilter).getType()) + " '" + value + "'" + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    					
    					}
    					
    					
    				} else if(columnFilter instanceof NumberColumnFilter) {
    					
    					if(!column.contains(prefix)) {
    						
							String column1 = column;
							String columnPrefix = column.substring(0, column.indexOf("~"));
							if(!sourceArray.contains(sourceMap.get(columnPrefix))) {
								sourceArray.add(sourceMap.get(columnPrefix));
							}
							
							if(reportBy.equalsIgnoreCase("User")) {
    							column1 = "coalesce(coalesce(SDT.SDJSONDATA,'{}')::jsonb ->> '" + column + "','') <> '' and coalesce(coalesce(SDT.SDJSONDATA,'{}')::jsonb ->> '" + column + "','0')::numeric ";
    						} else if(reportBy.equalsIgnoreCase("Sudoers") || reportBy.equalsIgnoreCase("Sudoers Detail")) {
    							column1 = "coalesce(coalesce(sd.data, '{}')::json ->> '" + column + "','') <> '' and coalesce(coalesce(sd.data, '{}')::json ->> '" + column + "','0')::numeric ";
    						} else if(reportBy.equalsIgnoreCase("thirdPartyData")) {
    							column1 = "coalesce(coalesce(sd.data, '{}')::json ->> '" + column + "','') <> '' and coalesce(coalesce(sd.data, '{}')::json ->> '" + column + "','0')::numeric ";
    						} else {
    							column1 = "coalesce((coalesce(sd.data,'{}')::jsonb || coalesce(sd1.data,'{}')::jsonb) ->> '" + column + "','') <> '' and coalesce((coalesce(sd.data,'{}')::jsonb || coalesce(sd1.data,'{}')::jsonb) ->> '" + column + "','')::numeric ";
    						}
							
    						String value = "";
    						double value1 = ((NumberColumnFilter) columnFilter).getFilter();
    						double value2 = 0;
    						
    						if(((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
    							value2 = ((NumberColumnFilter) columnFilter).getFilterTo();
    						}
    						
    						if(((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("inRange")) {
    							filterQuery = filterQuery.append(((i == 1) ? (" " + operator + " ") : " and ") + ((columnArray.size() > 1 && i == 0) ? "(": "") + column1 + " <> '' and " + column1 + "::numeric " + OperatorModel.getOperator(((NumberColumnFilter) columnFilter).getType()) + " " + value1 + " and " + value2 + " " + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    						} else {
    							filterQuery = filterQuery.append(((i == 1) ? (" " + operator + " ") : " and ") + ((columnArray.size() > 1 && i == 0) ? "(": "") + column1 + " <> '' and " + column1 + "::numeric " + OperatorModel.getOperator(((NumberColumnFilter) columnFilter).getType()) + " " + value1 + ((columnArray.size() > 1 && i == 1) ? ")": ""));
    						}
    						
							/*if(((NumberColumnFilter) columnFilter).getType() != null && ((NumberColumnFilter) columnFilter).getType().equalsIgnoreCase("equals")) {
    							
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
        	    
        					
        					}*/
    					}
    					
    				} 
    			}
    				
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	String cedQuery = "and (user_name in (select distinct primary_key_value from source_data where site_key = '" + siteKey + "' and source_id in (select json_array_elements_text('" + sourceArray + "'::json)) " + filterQuery.toString() + ") or \r\n"
				+ "server_name in (select distinct primary_key_value from source_data where site_key = '" + siteKey + "' and source_id in (select json_array_elements_text('" + sourceArray + "'::json))  " + filterQuery.toString() + ")) ";
    	if(reportBy.equalsIgnoreCase("User")) {
    		cedQuery = "and user_name in (select distinct primary_key_value from source_data where site_key = '" + siteKey + "' and source_id in (select json_array_elements_text('" + sourceArray + "'::json)) " + filterQuery.toString() + ") \r\n";
    				
    	}
    	
    	//return filterQuery.toString().isEmpty() ? "" : cedQuery;
    	
    	return filterQuery.toString();
    	
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
    	
    	String prefix = PrefixModel.getPrefix(reportBy);
    	try {
    		for(SortModel s: sortModel) {
    			System.out.println("!!!!! colId: " + s.getActualColId());
    			if(s.getActualColId().startsWith(prefix)) {
    				String column_name = s.getActualColId().substring(s.getActualColId().indexOf("~") + 1, s.getActualColId().length());
    				System.out.println("!!!!! column_name: " + column_name);
    				if(column_name.equalsIgnoreCase("servers_count")) {
    					orderBy = " order by (case when servers_count is null or servers_count = '' then 0 else " + column_name + "::int end)" + s.getSort();
    				} else {
    					orderBy = " order by " + column_name + " " + s.getSort();
    				}
    				
    				
    			} 
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return orderBy;
    }
    
    private String getOrderBy1(List<SortModel> sortModel, String reportBy) {
    	
    	String orderBy = "";
    	
    	String prefix = PrefixModel.getPrefix(reportBy);
    	try {
    		for(SortModel s: sortModel) {
    			System.out.println("!!!!! colId: " + s.getActualColId());
    			if(!s.getActualColId().startsWith(prefix)) {
    				if(reportBy.equalsIgnoreCase("User")) {
    					orderBy = " order by coalesce(SDT.SDJSONDATA::jsonb ->> '" + s.getActualColId() + "','') " + s.getSort();
    				} else if(reportBy.equalsIgnoreCase("Sudoers") || reportBy.equalsIgnoreCase("Sudoers Detail")) {
    					orderBy = " order by coalesce(json_collect(coalesce(sd.data::json, '{}'::json))::json ->> '" + s.getActualColId() + "','') " + s.getSort();
    				} else if(reportBy.equalsIgnoreCase("thirdPartyData")) {
    					orderBy = " order by coalesce(coalesce(sd.data::json, '{}'::json)::json ->> '" + s.getActualColId() + "','') " + s.getSort();
    				} else {
    					orderBy = " order by coalesce(json_collect((coalesce(sd.data,'{}')::jsonb||coalesce(sd1.data,'{}')::jsonb)::json) ->> '" + s.getActualColId() + "','') " + s.getSort();
    				}
    				
    				
    			} 
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return orderBy;
    }
    
}
