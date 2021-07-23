package com.zenfra.configuration;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.sun.istack.NotNull;
import com.zenfra.utils.DBUtils;

@Configuration
@Component
///@Profile(value={"dev", "prod","uat","demo","premigration","local","ai"})
public class PostgresqlConfiguration {


	
	/*
	@NotNull
	@Value("${db.username}")
    private String username;
	
    @NotNull
    @Value("${db.password}")
    private String password;

    @NotNull
    @Value("${db.url}")
    private String url;
    
    @Value("${db.driver}")
    private String driver;   
    
    */
    @Bean
    DataSource loadDataSource() throws SQLException {   
    	Map<String, String> data=DBUtils.zookeeperConfig();
    	 String username=data.get("userName");
    	 String password=data.get("password");
         String url=data.get("url");
         String driver="org.postgresql.Driver";
    	
    	//System.out.println("zookeeper data::"+data);
         return DataSourceBuilder
                 .create()
                 .url(url)
                 .username(username)
                 .password(password)
                 .driverClassName(driver)
                 .build();
    }
   
    @Bean
    @Autowired
    public JdbcTemplate loadJdbcTemplate(@Qualifier("loadDataSource")DataSource dataSource) {   	
    	
            return new JdbcTemplate(dataSource);
        }
   

	@Bean
	@Autowired
	public NamedParameterJdbcTemplate loadNamedParameterJdbcTemplate(@Qualifier("loadDataSource")DataSource dataSource) {   	
		
	        return new NamedParameterJdbcTemplate(dataSource);
	    }
	}

