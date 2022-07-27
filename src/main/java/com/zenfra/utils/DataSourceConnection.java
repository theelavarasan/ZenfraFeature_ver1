package com.zenfra.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;

public class DataSourceConnection {
	
	public static HikariDataSource dataSource = null;
	
	public static HikariDataSource dataSourceForAwsInventory = null;
	public static HikariDataSource dataSourceForAwsInventoryUAT = null;
	
	static {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(ZKModel.getProperty(ZKConstants.POSTGRES_DRIVER_CLASS_NAME));
        config.setJdbcUrl(ZKModel.getProperty(ZKConstants.POSTGRES_URL));
        config.setUsername(ZKModel.getProperty(ZKConstants.POSTGRES_USER));
        config.setPassword(ZKModel.getProperty(ZKConstants.POSTGRES_PWD));
        config.addDataSourceProperty("minimumIdle", ZKModel.getProperty(ZKConstants.PG_MIN_IDLE_TIMEOUT));
        config.addDataSourceProperty("maximumPoolSize", ZKModel.getProperty(ZKConstants.PG_MAX_POOL_SIZE));
        config.setConnectionTimeout(3000000);
        config.setLeakDetectionThreshold(3000000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        dataSource = new HikariDataSource(config);
        
       /* HikariConfig config1 = new HikariConfig();
        config1.setDriverClassName(ZKModel.getProperty(ZKConstants.POSTGRES_DRIVER_CLASS_NAME));
        config1.setJdbcUrl(ZKModel.getProperty(ZKConstants.POSTGRES_URL_AWS_DB));
        config1.setUsername(ZKModel.getProperty(ZKConstants.POSTGRES_USER));
        config1.setPassword(ZKModel.getProperty(ZKConstants.POSTGRES_PWD));
        config1.addDataSourceProperty("minimumIdle", ZKModel.getProperty(ZKConstants.PG_MIN_IDLE_TIMEOUT));
        config1.addDataSourceProperty("maximumPoolSize", ZKModel.getProperty(ZKConstants.PG_MAX_POOL_SIZE));
        config1.setConnectionTimeout(300000);
        config1.setLeakDetectionThreshold(3000000);
        config1.setIdleTimeout(600000);
        config1.setMaxLifetime(1800000);
        dataSourceForAwsInventory = new HikariDataSource(config1);*/

    }
	
	
}
