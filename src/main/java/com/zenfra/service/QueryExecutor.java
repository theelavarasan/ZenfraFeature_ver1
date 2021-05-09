package com.zenfra.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.model.ZenfraJSONObject;

public class QueryExecutor {

	static DateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-MM-dd");
	public static LinkedHashMap<String, String> mymap = null;
	public static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);
	static JSONArray Columns = new JSONArray();
	static JSONArray MetricsColumn = new JSONArray();
	static LinkedHashSet<String> ColumnGroup = new LinkedHashSet<String>();

	@SuppressWarnings("unchecked")
	public static JSONArray orientDBQueryExecution(String query) throws SQLException {

		// System.out.println("*************^^^^^^^^^^^^^^^^orientDBQueryExecution^^^^^^^^^^^^^^**************************************");
		JSONArray resultSetArr = new JSONArray();
		Connection connection = null;
		try {
			Class.forName(ZKModel.getProperty(ZKConstants.ORIENTDB_DRIVER_CLASS));

			String url = ZKModel.getProperty(ZKConstants.ORIENTDBJDBC) + ZKModel.getProperty(ZKConstants.ORIENTDBNAME);
			connection = (OrientJdbcConnection) DriverManager.getConnection(url,
					ZKModel.getProperty(ZKConstants.ORIENTDBUSER), ZKModel.getProperty(ZKConstants.ORIENTDBPWD));
			// logger.info("***********Connection estabilshed**********");
			Statement statement = connection.createStatement();
			// logger.info("!!!!!!!!!! query: " + query);
			ResultSet resultSet = statement.executeQuery(query);
			ResultSetMetaData rsmd;
			int columnsNumber = 0;
			if (resultSet == null) {
				rsmd = null;
				columnsNumber = 0;
			} else {
				rsmd = resultSet.getMetaData();
				columnsNumber = rsmd.getColumnCount();
			}

			if (resultSet != null) {
				while (resultSet.next()) {
					String rid = resultSet.getString("rid");
					ZenfraJSONObject json = new ZenfraJSONObject();
					for (int i = 1; i <= columnsNumber; i++) {

						// logger.info(rsmd.getColumnName(i) + "\t" + resultSet.getString(i));
						json.put(rsmd.getColumnName(i), resultSet.getObject(i));

					}
					if (rid != null) {
						json.put("rid", rid.split("#")[1].split("\\{")[0]);
					}
					if (json.size() > 0) {
						resultSetArr.add(json);
					}
				}

				resultSet.close();
			}

			statement.close();
		} catch (SQLException | ClassNotFoundException e) {
			logger.info("Exception @orientDBQueryExecution : " + e);
			e.printStackTrace();
		} finally {
			if(connection != null)
			{
				connection.close();
			}
		}

		return resultSetArr;
	}
	

}
