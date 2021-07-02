package com.zenfra.configuration;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.DBUtils;


@Component
public class AwsInventoryPostgresConnection {	
	public static HikariDataSource dataSource = null;	
	static {
		Map<String, String> data=DBUtils.getPostgres();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(ZKModel.getProperty(ZKConstants.POSTGRES_DRIVER_CLASS_NAME));
        config.setJdbcUrl(data.get("aws_jdbc_url"));
        config.setUsername(data.get("userName"));
        config.setPassword(data.get("password"));
        config.addDataSourceProperty("minimumIdle", ZKModel.getProperty(ZKConstants.PG_MIN_IDLE_TIMEOUT));
        config.addDataSourceProperty("maximumPoolSize", ZKModel.getProperty(ZKConstants.PG_MAX_POOL_SIZE));
        dataSource = new HikariDataSource(config);
    }

}
