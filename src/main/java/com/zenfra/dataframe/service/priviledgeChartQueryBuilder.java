package com.zenfra.dataframe.service;

import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

@Service
public class priviledgeChartQueryBuilder {

	@SuppressWarnings("unchecked")
	public String priviledgeUserSummaryChartQueries(JSONObject chartConfig, JSONObject filterModel, String siteKey,
			String chartType, String reportLabel) throws ParseException {

		JSONParser jsonParser = new JSONParser();
		JSONArray chartTypes = new JSONArray();
		chartTypes.add("bar");
		chartTypes.add("line");
		chartTypes.add("table");
		chartTypes.add("scatter");

		System.out.println("ChartTypes : " + chartTypes + " : " + chartType);

		JSONArray xaxisColumnAry = (JSONArray) chartConfig.get("xaxis");
		JSONArray yaxisColumnAry = (JSONArray) chartConfig.get("yaxis");
		JSONArray breakDownAry = (JSONArray) chartConfig.get("breakdown");

		JSONArray pieChartCols = new JSONArray();
		JSONObject pieChartObject = new JSONObject();
		String pieChartColName = "";
		String pieChartClassName = "";
		String pieChartField = "";

		// yaxis column names
		JSONObject yaxisColumn = new JSONObject();
		String yaxisColumnName = "";
		JSONArray yaxisNames = new JSONArray();
		String className = "";
		JSONArray classNameArray = new JSONArray();

		JSONArray yaxisColumnField = new JSONArray();
		String yaxisColumnFieldName = "";
		boolean yaxisServerCheck = false;

		// xaxis column names
		JSONObject xaxisColumn = new JSONObject();
		String xaxisColumnNameField = "";
		String xaxisColumnName = "";
		String xaxisColumnClassName = "";

		// breakdown names
		JSONObject breakDown = new JSONObject();
		String breakDownName = "";
		String breakDownField = "";

		String query = "WITH SDDATA AS\r\n" + "    (SELECT PRIMARY_KEY_VALUE,\r\n"
				+ "        JSON_COLLECT(DATA::JSON) AS SDJSONDATA\r\n" 
				+ "        FROM SOURCE_DATA AS SD\r\n"
				+ "        INNER JOIN SOURCE AS SR ON SD.SOURCE_ID = SR.SOURCE_ID\r\n"
				+ "        WHERE SD.SITE_KEY = '"+ siteKey + "'\r\n"
				+ "            AND SR.IS_ACTIVE = TRUE\r\n"
				+ "            AND (SR.LINK_TO = 'All'\r\n"
				+ "                                OR SR.LINK_TO = 'None')\r\n"
				+ "        GROUP BY SD.PRIMARY_KEY_VALUE),\r\n" + "   "
				+ "USRDetails AS\r\n"
				+ "    (SELECT USRD.USER_NAME,\r\n" 
				+ "            USRD.USER_ID,\r\n" 
				+ "            USRD.GROUP_ID,\r\n"
				+ "            USRD.SERVERS_COUNT,\r\n" 
				+ "            USRD.PRIMARY_GROUP_NAME,\r\n"
				+ "            USRD.SECONDARY_GROUP_NAME,\r\n" 
				+ "            USRD.SUDO_PRIVILEGES_BY_USER,\r\n"
				+ "            USRD.SUDO_PRIVILEGES_BY_PRIMARY_GROUP,\r\n"
				+ "            USRD.SUDO_PRIVILEGES_BY_SECONDARY_GROUP,\r\n"
				+ "            USRD.MEMBER_OF_USER_ALIAS,\r\n" 
				+ "            USRD.SUDO_PRIVILEGES_BY_USER_ALIAS,\r\n"
				+ "            SDT.SDJSONDATA AS SR_DATA\r\n" 
				+ "        FROM USER_SUMMARY_REPORT_DETAILS AS USRD\r\n"
				+ "        LEFT JOIN SDDATA AS SDT ON USRD.USER_NAME = SDT.PRIMARY_KEY_VALUE\r\n"
				+ "        WHERE SITE_KEY = '" + siteKey + "')";

		if (chartType.equalsIgnoreCase("pie")) {

			pieChartCols = (JSONArray) chartConfig.get("column");
			pieChartObject = (JSONObject) pieChartCols.get(0);
			pieChartColName = (String) pieChartObject.get("value");
			pieChartClassName = (String) pieChartObject.get("className");
			pieChartField = (String) pieChartObject.get("field");

			if (pieChartField.startsWith("User Summary~")) {
				query = query.concat("select " + pieChartField.substring(13) + " as \"colName\"");
			} else {
				query = query.concat("SELECT SR_DATA::JSON ->> '" + pieChartField + "' AS \"colName\"");
			}

			if (pieChartField.startsWith("User Summary~")) {
				if (pieChartClassName.contains("count")) {
					query = query.concat(", count(" + pieChartField.substring(13) + ") as \"colValue\"");
				} else if (pieChartClassName.contains("sum")) {
					query = query.concat(", sum(" + pieChartField.substring(13) + "::int) as \"colValue\"");
				}
			} else {
				if (pieChartClassName.contains("count")) {
					query = query.concat(", count(SR_DATA::JSON ->> '" + pieChartField + "') as \"colValue\"");
				} else if (pieChartClassName.contains("sum")) {
					query = query.concat(", sum((SR_DATA::JSON ->> '" + pieChartField + "')::int) as \"colValue\"");
				}
			}

		} else if (chartTypes.contains(chartType)) {
			xaxisColumn = (JSONObject) xaxisColumnAry.get(0);
			xaxisColumnNameField = (String) xaxisColumn.get("field");
			xaxisColumnName = (String) xaxisColumn.get("value");
			xaxisColumnClassName = (String) xaxisColumn.get("className");

			breakDown = breakDownAry.isEmpty() ? new JSONObject() : (JSONObject) breakDownAry.get(0);
			breakDownName = (String) breakDown.get("value");
			breakDownField = (String) breakDown.get("field");

			for (int i = 0; i < yaxisColumnAry.size(); i++) {
				yaxisColumn = (JSONObject) yaxisColumnAry.get(i);
				yaxisColumnName = (String) yaxisColumn.get("value");
				yaxisNames.add(yaxisColumnName);
				className = (String) yaxisColumn.get("className");
				classNameArray.add(className);
				yaxisColumnFieldName = (String) yaxisColumn.get("field");
				yaxisColumnField.add(yaxisColumnFieldName);
			}

			if (xaxisColumnNameField.startsWith("User Summary~")) {
				query = query.concat("select " + xaxisColumnNameField.substring(13) + " as \"colName\"");
			} else {
				query = query.concat("select SR_DATA::JSON ->> '" + xaxisColumnNameField + "' as \"colName\"");
			}

			if (breakDownName != null && !breakDownName.isEmpty()) {
				if (breakDownField.startsWith("User Summary~")) {
					query = query.concat(", " + breakDownField.substring(13) + " as \"colBreakdown\"");
				} else {
					query = query.concat(", SR_DATA::JSON ->> '" + breakDownField + "' as \"colBreakdown\"");
				}
			}

			for (int i = 0; i < yaxisColumnField.size(); i++) {
				String operater = (String) classNameArray.get(i);

				String yFieldCheck = (String) yaxisColumnField.get(i);
				if (yFieldCheck.startsWith("User Summary~")) {
					if (operater.contains("count")) {
						query = query.concat(", count(" + yFieldCheck.substring(13) + ") as \"colValue" + i + "\"");
					} else if (operater.contains("sum")) {
						query = query.concat(", sum(" + yFieldCheck.substring(13) + "::int) as \"colValue" + i + "\"");
					}
				} else {
					if (operater.contains("count")) {
						query = query
								.concat(", count(SR_DATA::JSON ->> '" + yFieldCheck + "') as \"colValue" + i + "\"");
					} else if (operater.contains("sum")) {
						query = query.concat(
								", sum((SR_DATA::JSON ->> '" + yFieldCheck + "')::int) as \"colValue" + i + "\"");
					}
				}
			}
		}

		query = query.concat(" FROM USRDetails");

		if (chartType.equalsIgnoreCase("pie")) {
			if (pieChartField.startsWith("User Summary~")) {
				query = query.concat(" where " + pieChartField.substring(13) + " is not null");
			} else {
				query = query.concat(" WHERE SR_DATA::JSON ->> '" + pieChartField + "' is not null");
			}
		} else if (chartTypes.contains(chartType)) {
			if (xaxisColumnNameField.startsWith("User Summary~")) {
				query = query.concat(" where " + xaxisColumnNameField.substring(13) + " is not null");
			} else {
				query = query.concat(" WHERE SR_DATA::JSON ->> '" + xaxisColumnNameField + "' is not null");
			}
		}

//		filtering property
		if (!filterModel.isEmpty() && filterModel != null) {
			query = query.concat(ChartFilters(filterModel, reportLabel));
		}
//		filtering property

		query = query.concat(" group by ");
		if (chartType.equalsIgnoreCase("pie")) {
			if (pieChartField.startsWith("User Summary~")) {
				query = query.concat(" " + pieChartField.substring(13) + "");
			} else {
				query = query.concat(" SR_DATA::JSON ->> '" + pieChartField + "' ");
			}
		} else if (chartTypes.contains(chartType)) {
			if (xaxisColumnNameField.startsWith("User Summary~")) {
				query = query.concat(" " + xaxisColumnNameField.substring(13) + " ");
			} else {
				query = query.concat(" SR_DATA::JSON ->> '" + xaxisColumnNameField + "' ");
			}

			if (breakDownName != null && !breakDownName.isEmpty()) {
				if (breakDownField.startsWith("User Summary~")) {
					query = query.concat(", " + breakDownField.substring(13) + " ");
				} else {
					query = query.concat(", SR_DATA::JSON ->> '" + breakDownField + "' ");
				}
			}
		}

		return query;
	}

	@SuppressWarnings("unchecked")
	public String priviledgeServerSummaryChartQueries(JSONObject chartConfig, JSONObject filterModel, String siteKey, String chartType, String reportLabel)
			throws ParseException {

		JSONParser jsonParser = new JSONParser();
		JSONArray chartTypes = new JSONArray();
		chartTypes.add("bar");
		chartTypes.add("line");
		chartTypes.add("table");
		chartTypes.add("scatter");

		System.out.println("ChartTypes : " + chartTypes + " : " + chartType);

		JSONArray xaxisColumnAry = (JSONArray) chartConfig.get("xaxis");
		JSONArray yaxisColumnAry = (JSONArray) chartConfig.get("yaxis");
		JSONArray breakDownAry = (JSONArray) chartConfig.get("breakdown");

		JSONArray pieChartCols = new JSONArray();
		JSONObject pieChartObject = new JSONObject();
		String pieChartColName = "";
		String pieChartClassName = "";
		String pieChartField = "";
		String pieChartFieldSubString = "";
		
		// yaxis column names
		JSONObject yaxisColumn = new JSONObject();
		String yaxisColumnName = "";
		JSONArray yaxisNames = new JSONArray();
		String className = "";
		JSONArray classNameArray = new JSONArray();

		JSONArray yaxisColumnField = new JSONArray();
		String yaxisColumnFieldName = "";
		boolean yaxisServerCheck = false;
		String yaxisFieldSubString = "";

		// xaxis column names
		JSONObject xaxisColumn = new JSONObject();
		String xaxisColumnNameField = "";
		String xaxisColumnName = "";
		String xaxisColumnClassName = "";
		String xaxisFieldSubString = "";

		// breakdown names
		JSONObject breakDown = new JSONObject();
		String breakDownName = "";
		String breakDownField = "";
		String breakdownFieldSubString = "";

		String query = "WITH SDDATA AS\r\n"
				+ "(\r\n"
				+ "    SELECT SD.PRIMARY_KEY_VALUE,\r\n"
				+ "           JSON_collect(SD.DATA::JSON) AS SDJSONDATA\r\n"
				+ "    FROM SOURCE_DATA SD\r\n"
				+ "       INNER JOIN SOURCE AS SR ON SD.SOURCE_ID = SR.SOURCE_ID\r\n"
				+ "        WHERE SD.SITE_KEY = '" + siteKey + "'\r\n"
				+ "            AND SR.IS_ACTIVE = true\r\n"
				+ "            AND (SR.LINK_TO = 'All'\r\n"
				+ "                                OR SR.LINK_TO = 'None')\r\n"
				+ "             GROUP BY SD.PRIMARY_KEY_VALUE),\r\n"
				+ "PDD AS\r\n"
				+ "(\r\n"
				+ "    SELECT count(1) over() as row_count, SSRD.SERVER_NAME,\r\n"
				+ "           SSRD.USER_NAME,\r\n"
				+ "           SSRD.USER_ID,\r\n"
				+ "           COALESCE(SSRD.GROUP_ID, '') AS GROUP_ID,\r\n"
				+ "           COALESCE(SSRD.PRIMARY_GROUP_NAME, '') AS PRIMARY_GROUP_NAME,\r\n"
				+ "           COALESCE(SSRD.SECONDARY_GROUP_NAME, '') AS SECONDARY_GROUP_NAME,\r\n"
				+ "           COALESCE(SSRD.SUDO_PRIVILEGES_BY_USER, '') AS SUDO_PRIVILEGES_BY_USER,\r\n"
				+ "           COALESCE(SSRD.SUDO_PRIVILEGES_BY_PRIMARY_GROUP, '') AS SUDO_PRIVILEGES_BY_PRIMARY_GROUP,\r\n"
				+ "           COALESCE(SSRD.SUDO_PRIVILEGES_BY_SECONDARY_GROUP, '') AS SUDO_PRIVILEGES_BY_SECONDARY_GROUP,\r\n"
				+ "           COALESCE(SSRD.MEMBER_OF_USER_ALIAS, '') AS USER_ALIAS_NAME,\r\n"
				+ "           COALESCE(SSRD.SUDO_PRIVILEGES_BY_USER_ALIAS, '') AS SUDO_PRIVILEGES_BY_USER_ALIAS,\r\n"
				+ "           SSRD.PROCESSEDDATE AS PROCESSEDDATE,\r\n"
				+ "           SSRD.OPERATING_SYSTEM AS OS,\r\n"
				+ "           COALESCE(SDT.SDJSONDATA::TEXT, '{}') AS source_data\r\n"
				+ "    FROM privillege_data_details AS SSRD\r\n"
				+ "    LEFT JOIN SDDATA AS SDT\r\n"
				+ "    ON (SSRD.SERVER_NAME = SDT.PRIMARY_KEY_VALUE OR SSRD.USER_NAME = SDT.PRIMARY_KEY_VALUE)\r\n"
				+ "    WHERE SSRD.SITE_KEY = '" + siteKey + "'\r\n"
				+ ")";

		if (chartType.equalsIgnoreCase("pie")) {

			pieChartCols = (JSONArray) chartConfig.get("column");
			pieChartObject = (JSONObject) pieChartCols.get(0);
			pieChartColName = (String) pieChartObject.get("value");
			pieChartClassName = (String) pieChartObject.get("className");
			pieChartField = (String) pieChartObject.get("field");

			
			 if(pieChartField.startsWith("Server Summary~")) {
				 pieChartFieldSubString = pieChartField.substring(15);
			 } else if(pieChartField.startsWith("Server Data~")) {
				 pieChartFieldSubString = pieChartField.substring(12);
			 }
			
			if (pieChartField.startsWith("Server Summary~") || pieChartField.startsWith("Server Data~")) {
				query = query.concat("select " + pieChartFieldSubString + " as \"colName\"");
			} else {
				query = query.concat("SELECT source_data::JSON ->> '" + pieChartField + "' AS \"colName\"");
			}

			if (pieChartField.startsWith("Server Summary~") || pieChartField.startsWith("Server Data~")) {
				if (pieChartClassName.contains("count")) {
					query = query.concat(", count(" + pieChartFieldSubString + ") as \"colValue\"");
				} else if (pieChartClassName.contains("sum")) {
					query = query.concat(", sum(" + pieChartFieldSubString + "::int) as \"colValue\"");
				}
			} else {
				if (pieChartClassName.contains("count")) {
					query = query.concat(", count(source_data::JSON ->> '" + pieChartField + "') as \"colValue\"");
				} else if (pieChartClassName.contains("sum")) {
					query = query
							.concat(", sum((source_data::JSON ->> '" + pieChartField + "')::int) as \"colValue\"");
				}
			}

		} else if (chartTypes.contains(chartType)) {
			xaxisColumn = (JSONObject) xaxisColumnAry.get(0);
			xaxisColumnNameField = (String) xaxisColumn.get("field");
			xaxisColumnName = (String) xaxisColumn.get("value");
			xaxisColumnClassName = (String) xaxisColumn.get("className");

			breakDown = breakDownAry.isEmpty() ? new JSONObject() : (JSONObject) breakDownAry.get(0);
			breakDownName = (String) breakDown.get("value");
			breakDownField = (String) breakDown.get("field");

			for (int i = 0; i < yaxisColumnAry.size(); i++) {
				yaxisColumn = (JSONObject) yaxisColumnAry.get(i);
				yaxisColumnName = (String) yaxisColumn.get("value");
				yaxisNames.add(yaxisColumnName);
				className = (String) yaxisColumn.get("className");
				classNameArray.add(className);
				yaxisColumnFieldName = (String) yaxisColumn.get("field");
				yaxisColumnField.add(yaxisColumnFieldName);
			}

			if(xaxisColumnNameField.startsWith("Server Summary~")) {
				xaxisFieldSubString = xaxisColumnNameField.substring(15);
			} else if(xaxisColumnNameField.startsWith("Server Data~")) {
				xaxisFieldSubString = xaxisColumnNameField.substring(12);
			}
			
			
			if (xaxisColumnNameField.startsWith("Server Summary~") || xaxisColumnNameField.startsWith("Server Data~")) {
				query = query.concat("select " + xaxisFieldSubString + " as \"colName\"");
			} else {
				query = query.concat("select source_data::JSON ->> '" + xaxisColumnNameField + "' as \"colName\"");
			}

			if (breakDownName != null && !breakDownName.isEmpty()) {
				if(breakDownField.startsWith("Server Summary~")) {
					breakdownFieldSubString = breakDownField.substring(15);
				} else if(breakDownField.startsWith("Server Data~")) {
					breakdownFieldSubString = breakDownField.substring(12);
				}
				if (breakDownField.startsWith("Server Summary~") || breakDownField.startsWith("Server Data~")) {
					query = query.concat(", " + breakdownFieldSubString + " as \"colBreakdown\"");
				} else {
					query = query.concat(", source_data::JSON ->> '" + breakDownField + "' as \"colBreakdown\"");
				}
			}

			for (int i = 0; i < yaxisColumnField.size(); i++) {
				String operater = (String) classNameArray.get(i);

				String yFieldCheck = (String) yaxisColumnField.get(i);
				
				if(yFieldCheck.startsWith("Server Summary~")) {
					yaxisFieldSubString = yFieldCheck.substring(15);
				} else if(yFieldCheck.startsWith("Server Data~")) {
					yaxisFieldSubString = yFieldCheck.substring(12);
				}
				
				if (yFieldCheck.startsWith("Server Summary~") || yFieldCheck.startsWith("Server Data~")) {
					if (operater.contains("count")) {
						query = query.concat(", count(" + yaxisFieldSubString + ") as \"colValue" + i + "\"");
					} else if (operater.contains("sum")) {
						query = query.concat(", sum(" + yaxisFieldSubString + "::int) as \"colValue" + i + "\"");
					}
				} else {
					if (operater.contains("count")) {
						query = query.concat(", count(source_data::JSON ->> '" + yFieldCheck + "') as \"colValue" + i + "\"");
					} else if (operater.contains("sum")) {
						query = query.concat(", sum((source_data::JSON ->> '" + yFieldCheck + "')::int) as \"colValue" + i + "\"");
					}
				}
			}
		}

			query = query.concat(" FROM PDD");

			if (chartType.equalsIgnoreCase("pie")) {
				if (pieChartField.startsWith("Server Summary~") || pieChartField.startsWith("Server Data~")) {
					query = query.concat(" where " + pieChartFieldSubString + " is not null");
				} else {
					query = query.concat(" WHERE source_data::JSON ->> '" + pieChartField + "' is not null");
				}
			} else if(chartTypes.contains(chartType)) {
				if (xaxisColumnNameField.startsWith("Server Summary~") || xaxisColumnNameField.startsWith("Server Data~")) {
					query = query.concat(" where " + xaxisFieldSubString + " is not null");
				} else {
					query = query.concat(" WHERE source_data::JSON ->> '" + xaxisColumnNameField + "' is not null");
				}
			}

//			filtering property
			if (!filterModel.isEmpty() && filterModel != null) {
				query = query.concat(ChartFilters(filterModel, reportLabel));
			}
//			filtering property

		query = query.concat(" group by ");
		if (chartType.equalsIgnoreCase("pie")) {
			if (pieChartField.startsWith("Server Summary~") || pieChartField.startsWith("Server Data~")) {
				query = query.concat(" " + pieChartFieldSubString + "");
			} else {
				query = query.concat(" source_data::JSON ->> '" + pieChartField + "' ");
			}
		} else if (chartTypes.contains(chartType)) {
			if (xaxisColumnNameField.startsWith("Server Summary~") || xaxisColumnNameField.startsWith("Server Data~")) {
				query = query.concat(" " + xaxisFieldSubString + " ");
			} else {
				query = query.concat(" source_data::JSON ->> '" + xaxisColumnNameField + "' ");
			}

			if (breakDownName != null && !breakDownName.isEmpty()) {
				if (breakDownField.startsWith("Server Summary~") || breakDownField.startsWith("Server Data~")) {
					query = query.concat(", " + breakdownFieldSubString + " ");
				} else {
					query = query.concat(", source_data::JSON ->> '" + breakDownField + "' ");
				}
			}
		}

		return query;
	}
	
	@SuppressWarnings("unchecked")
	public String priviledgeSudoersChartQueries(JSONObject chartConfig, JSONObject filterModel, String siteKey, String chartType, String reportLabel)
			throws ParseException {

		JSONParser jsonParser = new JSONParser();
		JSONArray chartTypes = new JSONArray();
		chartTypes.add("bar");
		chartTypes.add("line");
		chartTypes.add("table");
		chartTypes.add("scatter");

		System.out.println("ChartTypes : " + chartTypes + " : " + chartType);

		JSONArray xaxisColumnAry = (JSONArray) chartConfig.get("xaxis");
		JSONArray yaxisColumnAry = (JSONArray) chartConfig.get("yaxis");
		JSONArray breakDownAry = (JSONArray) chartConfig.get("breakdown");

		JSONArray pieChartCols = new JSONArray();
		JSONObject pieChartObject = new JSONObject();
		String pieChartColName = "";
		String pieChartClassName = "";
		String pieChartField = "";

		// yaxis column names
		JSONObject yaxisColumn = new JSONObject();
		String yaxisColumnName = "";
		JSONArray yaxisNames = new JSONArray();
		String className = "";
		JSONArray classNameArray = new JSONArray();

		JSONArray yaxisColumnField = new JSONArray();
		String yaxisColumnFieldName = "";
		boolean yaxisServerCheck = false;

		// xaxis column names
		JSONObject xaxisColumn = new JSONObject();
		String xaxisColumnNameField = "";
		String xaxisColumnName = "";
		String xaxisColumnClassName = "";

		// breakdown names
		JSONObject breakDown = new JSONObject();
		String breakDownName = "";
		String breakDownField = "";

		String query = "";

		if (chartType.equalsIgnoreCase("pie")) {

			pieChartCols = (JSONArray) chartConfig.get("column");
			pieChartObject = (JSONObject) pieChartCols.get(0);
			pieChartColName = (String) pieChartObject.get("value");
			pieChartClassName = (String) pieChartObject.get("className");
			pieChartField = (String) pieChartObject.get("field");

			if (pieChartField.startsWith("Sudoers Summary~")) {
				query = query.concat("select " + pieChartField.substring(16) + " as \"colName\"");
			} else {
				query = query.concat("SELECT source_data1::JSON ->> '" + pieChartField + "' AS \"colName\"");
			}

			if (pieChartField.startsWith("Sudoers Summary~")) {
				if (pieChartClassName.contains("count")) {
					query = query.concat(", count(" + pieChartField.substring(16) + ") as \"colValue\"");
				} else if (pieChartClassName.contains("sum")) {
					query = query.concat(", sum(" + pieChartField.substring(16) + "::int) as \"colValue\"");
				}
			} else {
				if (pieChartClassName.contains("count")) {
					query = query.concat(", count(source_data1::JSON ->> '" + pieChartField + "') as \"colValue\"");
				} else if (pieChartClassName.contains("sum")) {
					query = query.concat(", sum((source_data1::JSON ->> '" + pieChartField + "')::int) as \"colValue\"");
				}
			}

		} else if (chartTypes.contains(chartType)) {
			xaxisColumn = (JSONObject) xaxisColumnAry.get(0);
			xaxisColumnNameField = (String) xaxisColumn.get("field");
			xaxisColumnName = (String) xaxisColumn.get("value");
			xaxisColumnClassName = (String) xaxisColumn.get("className");

			breakDown = breakDownAry.isEmpty() ? new JSONObject() : (JSONObject) breakDownAry.get(0);
			breakDownName = (String) breakDown.get("value");
			breakDownField = (String) breakDown.get("field");

			for (int i = 0; i < yaxisColumnAry.size(); i++) {
				yaxisColumn = (JSONObject) yaxisColumnAry.get(i);
				yaxisColumnName = (String) yaxisColumn.get("value");
				yaxisNames.add(yaxisColumnName);
				className = (String) yaxisColumn.get("className");
				classNameArray.add(className);
				yaxisColumnFieldName = (String) yaxisColumn.get("field");
				yaxisColumnField.add(yaxisColumnFieldName);
			}

			if (xaxisColumnNameField.startsWith("Sudoers Summary~")) {
				query = query.concat("select " + xaxisColumnNameField.substring(16) + " as \"colName\"");
			} else {
				query = query.concat("select source_data1::JSON ->> '" + xaxisColumnNameField + "' as \"colName\"");
			}

			if (breakDownName != null && !breakDownName.isEmpty()) {
				if (breakDownField.startsWith("Sudoers Summary~")) {
					query = query.concat(", " + breakDownField.substring(16) + " as \"colBreakdown\"");
				} else {
					query = query.concat(", source_data1::JSON ->> '" + breakDownField + "' as \"colBreakdown\"");
				}
			}

			for (int i = 0; i < yaxisColumnField.size(); i++) {
				String operater = (String) classNameArray.get(i);

				String yFieldCheck = (String) yaxisColumnField.get(i);
				if (yFieldCheck.startsWith("Sudoers Summary~")) {
					if (operater.contains("count")) {
						query = query.concat(", count(" + yFieldCheck.substring(16) + ") as \"colValue" + i + "\"");
					} else if (operater.contains("sum")) {
						query = query.concat(", sum(" + yFieldCheck.substring(16) + "::int) as \"colValue" + i + "\"");
					}
				} else {
					if (operater.contains("count")) {
						query = query.concat(", count(source_data1::JSON ->> '" + yFieldCheck + "') as \"colValue" + i + "\"");
					} else if (operater.contains("sum")) {
						query = query.concat(", sum((source_data1::JSON ->> '" + yFieldCheck + "')::int) as \"colValue" + i + "\"");
					}
				}
			}
		}

			query = query.concat("from (\r\n"
					+ "    select user_name,\r\n"
					+ "    user_id, group_id, primary_group_name, secondary_group_name, sudo_privileges_by_user, sudo_privileges_by_primary_group,\r\n"
					+ "    sudo_privileges_by_secondary_group, user_alias_name, sudo_privileges_by_user_alias, servers_count,\r\n"
					+ "    json_collect(sd.data::json) as source_data1 from user_sudoers_summary_details ud\r\n"
					+ "    LEFT JOIN source_data sd on sd.primary_key_value = ud.user_name and sd.site_key = '" + siteKey + "'\r\n"
					+ "    WHERE ud.site_key = '" + siteKey + "'\r\n"
					+ "    group by user_name, user_id, group_id, primary_group_name, secondary_group_name,\r\n"
					+ "    sudo_privileges_by_user, sudo_privileges_by_primary_group,\r\n"
					+ "    sudo_privileges_by_secondary_group, user_alias_name, sudo_privileges_by_user_alias, servers_count\r\n"
					+ ")a");

			if (chartType.equalsIgnoreCase("pie")) {
				if (pieChartField.startsWith("Sudoers Summary~")) {
					query = query.concat(" where " + pieChartField.substring(16) + " is not null");
				} else {
					query = query.concat(" WHERE source_data1::JSON ->> '" + pieChartField + "' is not null");
				}
			} else if(chartTypes.contains(chartType)) {
				if (xaxisColumnNameField.startsWith("Sudoers Summary~")) {
					query = query.concat(" where " + xaxisColumnNameField.substring(16) + " is not null");
				} else {
					query = query.concat(" WHERE source_data1::JSON ->> '" + xaxisColumnNameField + "' is not null");
				}
			}

//			filtering property
			if (!filterModel.isEmpty() && filterModel != null) {
				query = query.concat(ChartFilters(filterModel, reportLabel));
			}
//			filtering property

		query = query.concat(" group by ");
		if (chartType.equalsIgnoreCase("pie")) {
			if (pieChartField.startsWith("Sudoers Summary~")) {
				query = query.concat(" " + pieChartField.substring(16) + "");
			} else {
				query = query.concat(" source_data1::JSON ->> '" + pieChartField + "' ");
			}
		} else if (chartTypes.contains(chartType)) {
			if (xaxisColumnNameField.startsWith("Sudoers Summary~")) {
				query = query.concat(" " + xaxisColumnNameField.substring(16) + " ");
			} else {
				query = query.concat(" source_data1::JSON ->> '" + xaxisColumnNameField + "' ");
			}

			if (breakDownName != null && !breakDownName.isEmpty()) {
				if (breakDownField.startsWith("Sudoers Summary~")) {
					query = query.concat(", " + breakDownField.substring(16) + " ");
				} else {
					query = query.concat(", source_data1::JSON ->> '" + breakDownField + "' ");
				}
			}
		}

		return query;
	}
	
	public String ChartFilters(JSONObject filterModel, String reportLabel) {

		String filters = "";
		JSONObject filterModelObject = (JSONObject) filterModel;
		Set<String> filterKeys = new HashSet<>();
		for (int i = 0; i < filterModelObject.size(); i++) {
			JSONObject jsonObj = filterModelObject;
			filterKeys.addAll(jsonObj.keySet());
		}

		filters = filters.concat(" and ");
		JSONObject conditionObject = new JSONObject();

		for (String key : filterKeys) {
			JSONObject filterColumnName = (JSONObject) filterModelObject.get(key);

			for (int i = 0; i < (filterModelObject.size() >= 2 ? filterModelObject.size() / 2
					: filterModelObject.size()); i++) {

				if ((filterColumnName.containsKey("type") && filterColumnName.containsKey("filter")
						&& filterColumnName.containsKey("filterType")) || (filterColumnName.containsKey("type") && filterColumnName.containsKey("filterType"))) {
					if(reportLabel.startsWith("User-Tanium-User")) {
						if (key.startsWith("User Summary~")) {
							filters = filters.concat(key.substring(13));
						} else {
							filters = filters.concat(" SR_DATA::JSON ->> '" + key + "'");
						}
					} else if(reportLabel.startsWith("User-Tanium-Privileged Access") || reportLabel.startsWith("User-Tanium-Server")) {
						String keySubstring = "";
						if(key.startsWith("Server Summary~")) {
							keySubstring = key.substring(15);
						 } else if(key.startsWith("Server Data~")) {
							 keySubstring = key.substring(12);
						 }
						
						if (key.startsWith("Server Summary~") || key.startsWith("Server Data~")) {
							filters = filters.concat(keySubstring);
						} else {
							filters = filters.concat(" source_data::JSON ->> '" + key + "'");
						}
					} else if(reportLabel.startsWith("User-Tanium-Sudoers")) {
						if (key.startsWith("Sudoers Summary~")) {
							filters = filters.concat(key.substring(16));
						} else {
							filters = filters.concat(" source_data1::JSON ->> '" + key + "'");
						}
					}
					
					if(filterColumnName.containsKey("filterType") && filterColumnName.get("filterType").toString().equalsIgnoreCase("text")) {
						if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("contains")) {
							filters = filters.concat(" ilike '%" + filterColumnName.get("filter") + "%'");
						} else if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("notContains")) {
							filters = filters.concat(" not ilike '%" + filterColumnName.get("filter") + "%'");
						} else if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("equals")) {
							filters = filters.concat(" = '" + filterColumnName.get("filter") + "'");
						} else if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("notEqual")) {
							filters = filters.concat(" <> '" + filterColumnName.get("filter") + "'");
						} else if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("Blanks")) {
							filters = filters.concat(" = ''");
						} else if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("Not Blanks")) {
							filters = filters.concat(" <> ''");
						} else if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("startsWith")) {
							filters = filters.concat(" ilike '" + filterColumnName.get("filter") + "%'");
						} else if (filterColumnName.containsKey("type")
								&& filterColumnName.get("type").toString().equalsIgnoreCase("endsWith")) {
							filters = filters.concat(" ilike '%" + filterColumnName.get("filter") + "'");
						}
					} else if(filterColumnName.containsKey("filterType") && filterColumnName.get("filterType").toString().equalsIgnoreCase("number")) {
						System.out.println("number:");
					}
					
					filters = filters.concat(" and ");
				} else if (filterColumnName.containsKey("filterType") && filterColumnName.containsKey("operator")) {
					Set<String> Keys = new HashSet<>();
					for (int j = 0; j < filterModelObject.size(); j++) {
						JSONObject jsonObj = filterColumnName;
						Keys.addAll(jsonObj.keySet());
					}
					for (int j = 0; j < Keys.size() - 2; j++) {
						conditionObject = filterColumnName;
						if (conditionObject.containsKey("condition" + (j + 1))) {
							JSONObject object = (JSONObject) conditionObject.get("condition" + (j + 1));
							if ((object.containsKey("type") && object.containsKey("filter")
									&& object.containsKey("filterType")) || (object.containsKey("type") && object.containsKey("filterType"))) {
								if(reportLabel.startsWith("User-Tanium-User")) {
									if (key.startsWith("User Summary~")) {
										filters = filters.concat(key.substring(13));
									} else {
										filters = filters.concat(" SR_DATA::JSON ->> '" + key + "'");
									}
								} else if(reportLabel.startsWith("User-Tanium-Privileged Access") || reportLabel.startsWith("User-Tanium-Server")) {
									String keySubstring = "";
									if(key.startsWith("Server Summary~")) {
										keySubstring = key.substring(15);
									 } else if(key.startsWith("Server Data~")) {
										 keySubstring = key.substring(12);
									 }
									
									if (key.startsWith("Server Summary~") || key.startsWith("Server Data~")) {
										filters = filters.concat(keySubstring);
									} else {
										filters = filters.concat(" source_data::JSON ->> '" + key + "'");
									}
								} else if(reportLabel.startsWith("User-Tanium-Sudoers")) {
									if (key.startsWith("Sudoers Summary~")) {
										filters = filters.concat(key.substring(16));
									} else {
										filters = filters.concat(" source_data1::JSON ->> '" + key + "'");
									}
								}

								if(object.containsKey("filterType") && object.get("filterType").toString().equalsIgnoreCase("text")) {
									if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("contains")) {
										filters = filters.concat(" ilike '%" + object.get("filter") + "%'");
									} else if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("notContains")) {
										filters = filters.concat(" not ilike '%" + object.get("filter") + "%'");
									} else if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("equals")) {
										filters = filters.concat(" = '" + object.get("filter") + "'");
									} else if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("notEqual")) {
										filters = filters.concat(" <> '" + object.get("filter") + "'");
									} else if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("Blanks")) {
										filters = filters.concat(" = ''");
									} else if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("Not Blanks")) {
										filters = filters.concat(" <> ''");
									} else if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("startsWith")) {
										filters = filters.concat(" ilike '" + object.get("filter") + "%'");
									} else if (object.containsKey("type")
											&& object.get("type").toString().equalsIgnoreCase("endsWith")) {
										filters = filters.concat(" ilike '%" + object.get("filter") + "'");
									}
								} else if(object.containsKey("filterType") && object.get("filterType").toString().equalsIgnoreCase("number")) {
									System.out.println("Number 1 :");
								}
							}

							if (conditionObject.containsKey("operator")
									&& conditionObject.get("operator").toString().equalsIgnoreCase("and")) {
								filters = filters.concat(" and ");
							} else if (conditionObject.containsKey("operator")
									&& conditionObject.get("operator").toString().equalsIgnoreCase("or")) {
								filters = filters.concat(" or ");
							}
						}
					}

				}

			}
		}
		
		System.out.println("filters : " + filters);
		if (conditionObject.containsKey("operator")
				&& conditionObject.get("operator").toString().equalsIgnoreCase("or")) {
			filters = filters.substring(0, filters.length() - 4);
		} else {
			filters = filters.substring(0, filters.length() - 5);
		}
		return filters;
	}

}