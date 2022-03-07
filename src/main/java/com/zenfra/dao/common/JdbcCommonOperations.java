package com.zenfra.dao.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public abstract class JdbcCommonOperations {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	NamedParameterJdbcTemplate namedJdbc;

	public Integer updateQuery(SqlParameterSource parameter, String query) {

		int responce = 0;
		try {
			KeyHolder holder = new GeneratedKeyHolder();
			responce = namedJdbc.update(query, parameter);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return responce;

	}

	public Integer updateQuery(Map<String, Object> parameter, String query) {

		int responce = 0;
		try {
			responce = namedJdbc.update(query, parameter);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return responce;

	}

	public Integer getCount(String query) {

		try {

			return jdbc.queryForObject(query, Integer.class);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}
		return 0;
	}

	public int updateQuery(String query) {
		int responce = 0;
		try {

			responce = jdbc.update(query);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return responce;
	}

	public int queryWithParams(Map<String, Object> params, String query) {

		int responce = 0;
		try {

			return namedJdbc.update(query, params);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}

		return responce;
	}

	public List<Map<String, Object>> getListMapObjectById(String query) {
		List<Map<String, Object>> obj = new ArrayList<>();
		try {

			obj = jdbc.queryForList(query);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	public Object getObjectByQuery(String query, Class c) {
		Object obj = null;
		try {

			obj = jdbc.queryForObject(query, c);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	public Map<String, Object> getObjectByQueryNew(String query) throws SQLException {
		Map<String, Object> obj = new HashMap<String, Object>();
		List<Map<String, Object>> obj1 = new ArrayList<>();
		DataSource d = null;
		JdbcTemplate jdbc = new JdbcTemplate();
		try {
			System.out.println("query::" + query);
			Map<String, String> data = DBUtils.getPostgres();
			d = DataSourceBuilder.create().url(data.get("url")).username(data.get("userName"))
					.password(data.get("password")).driverClassName("org.postgresql.Driver").build();
			jdbc.setDataSource(d);
			obj1 = jdbc.queryForList(query);

			System.out.println(obj1);
			obj = obj1.get(0) != null ? obj1.get(0) : new HashMap<String, Object>();

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		} finally {
			jdbc.getDataSource().getConnection().close();
		}
		return obj;
	}

	public int excuteByUpdateQueryNew(String query) throws SQLException {
		int obj1 = 0;
		JdbcTemplate jdbc = new JdbcTemplate();

		try {
			System.out.println("query::" + query);
			Map<String, String> data = DBUtils.getPostgres();
			DataSource d = DataSourceBuilder.create().url(data.get("url")).username(data.get("userName"))
					.password(data.get("password")).driverClassName("org.postgresql.Driver").build();
			jdbc.setDataSource(d);
			obj1 = jdbc.update(query);
			jdbc.getDataSource().getConnection().close();

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		} finally {
			jdbc.getDataSource().getConnection().close();
		}
		return obj1;
	}

	public List<Map<String, Object>> getListObjectsByQueryNew(String query) {
		List<Map<String, Object>> obj1 = new ArrayList<>();
		try {
			System.out.println("query::" + query);
			Map<String, String> data = DBUtils.getPostgres();
			DataSource d = DataSourceBuilder.create().url(data.get("url")).username(data.get("userName"))
					.password(data.get("password")).driverClassName("org.postgresql.Driver").build();
			JdbcTemplate jdbc = new JdbcTemplate();
			jdbc.setDataSource(d);
			obj1 = jdbc.queryForList(query);
			jdbc.getDataSource().getConnection().close();

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj1;
	}

	public List<Map<String, Object>> getObjectFromQuery(String query) {
		List<Map<String, Object>> obj = new ArrayList<>();
		try {

			obj = jdbc.queryForList(query);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	public Map<String, Object> getResultAsMap(String query) {
		Map<String, Object> obj = new HashMap<>();
		try {

			obj = jdbc.queryForMap(query);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}
}
