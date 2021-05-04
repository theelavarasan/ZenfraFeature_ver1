package com.zenfra.configuration;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.sun.istack.NotNull;

@Configuration
@Component
//@PropertySource("classpath:uat.properties")
@Profile(value={"dev", "prod","uat","demo"})
public class PostgresqlConfiguration {

	/*
	 * Map<String, String> data=DBUtils.getPostgres();
	@NotNull
	//@Value("${db.username}")
    private String username=data.get("userName");
	
    @NotNull
    //@Value("${db.password}")
    private String password=data.get("password");

    @NotNull
   // @Value("${db.url}")
    private String url=data.get("url");
    
   // @Value("${db.driver}")
    private String driver="org.postgresql.Driver";  

    @Bean
    DataSource loadDataSource() throws SQLException {   
    	System.out.println(this.url);
         return DataSourceBuilder
                 .create()
                 .url(this.url)
                 .username(this.username)
                 .password(this.password)
                 .driverClassName(this.driver)
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
*/
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

    @Bean
    DataSource loadDataSource() throws SQLException {   
    	System.out.println(this.url);
         return DataSourceBuilder
                 .create()
                 .url(this.url)
                 .username(this.username)
                 .password(this.password)
                 .driverClassName(this.driver)
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

 


