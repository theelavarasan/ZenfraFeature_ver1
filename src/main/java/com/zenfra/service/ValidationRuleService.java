package com.zenfra.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.parse.model.ZKConstants;
import com.parse.model.ZKModel;
import com.parse.serviceImpl.QueryExecutor;
import com.parse.util.ZenfraJSONObject;

@Component
public class ValidationRuleService {

	@Autowired
	SparkSession sparkSession;

	@SuppressWarnings("unchecked")
	public JSONArray getDiscoveryReportValues(String siteKey, String reportBy, String columnName, String category,
			String deviceType, String reportList) {
		System.out.println("---------------Inside Method");

		JSONArray resultArray = new JSONArray();
		String viewName = siteKey + "_" + deviceType.toLowerCase();

		try {
			Dataset<Row> dataset = sparkSession.sql("select * from global_temp." + viewName);
			dataset = dataset.sqlContext().sql("select distinct(columnName) from global_temp." + viewName);
			dataset = (Dataset<Row>) dataset.collectAsList();
			System.out.println("--------------Dataset" + dataset);
			resultArray.add(dataset);

		} catch (Exception e) {
			System.out.println("---------View Not exists--------");
		}
		String path = ZKModel.getProperty(ZKConstants.DISCOVERY_REPORT_PROPERTIES);
		System.out.println("!!!!! path: " + path);
		InputStream inputFile = null;
		JSONArray resultArray = new JSONArray();
		JSONParser parser = new JSONParser();
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			URL resources = classLoader.getResource(path);
			inputFile = new FileInputStream(resources.getFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Properties prop = new Properties();
		try {
			prop.load(inputFile);
		} catch (IOException e1) {

			e1.printStackTrace();
		}

		String reportQuery = "";

		System.out.println("===========Condition Values" + reportList + category);

		if ((reportList.equalsIgnoreCase("local") || reportList.equalsIgnoreCase("End-To-End-Basic"))
				&& !category.equalsIgnoreCase("Third Party Data") && !category.equalsIgnoreCase("Project")) {

			try {
				String key = reportList + "_" + deviceType + "_" + reportBy.replace(" ", "");
				System.out.println("!!!!! query key: " + key);
				System.out.println("---------------------" + prop);
				if (prop.containsKey(key)) {
					reportQuery = prop.getProperty(key);
					reportQuery = reportQuery.replace("[%s]", siteKey);
					reportQuery = reportQuery.replace("[%c]", deviceType.toLowerCase());
					// System.out.println("--------------Report Query: "+ reportQuery);

					JSONArray reportArray = new JSONArray();
					// System.out.println("!!!!! reportQuery: " + reportQuery);
					if (reportQuery.length() > 0) {
						// System.out.println("!!!!! deviceType: " + deviceType);
						if (deviceType.equalsIgnoreCase("AWS")) {
							reportArray = filterServiceImpl.queryExecutorPostgressql(deviceType, reportQuery);
							System.out.println("!!!!!**** SIZE: " + reportArray.size());
						} else {
							reportArray = QueryExecutor.orientDBQueryExecution(reportQuery);
						}

						Set<String> checkValue = new HashSet<String>();
						// System.out.println("----------reportArray"+ reportArray);
						for (int i = 0; i < reportArray.size(); i++) {
							ZenfraJSONObject valueObject = (ZenfraJSONObject) reportArray.get(i);
							JSONArray dataArray = (JSONArray) parser.parse(valueObject.get("data").toString());
							if (dataArray != null && !dataArray.isEmpty()) {
								JSONObject dataObject = (JSONObject) dataArray.get(0);
								if (dataObject.containsKey(columnName)) {
									if (dataObject.get(columnName) != null
											&& !dataObject.get(columnName).toString().trim().equalsIgnoreCase("")) {
										checkValue.add(dataObject.get(columnName).toString());
									}

								}
							}

						}

						if (!checkValue.isEmpty()) {
							resultArray.addAll(checkValue);
							System.out.println("------------resultArray: " + resultArray);
						}
					}
				}
			}

			catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("------------resultArray: " + resultArray);
		return resultArray;
	}

}
