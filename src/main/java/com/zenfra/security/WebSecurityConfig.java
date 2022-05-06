package com.zenfra.security;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.zenfra.service.UserServiceImpl;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Autowired
    public void globalUserDetails(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(encoder());
    }

    @Bean
    public JwtAuthenticationFilter authenticationTokenFilterBean() throws Exception {
        return new JwtAuthenticationFilter();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	 // Enable CORS and disable CSRF
        http = http.cors().and().csrf().disable();
        // Set session management to stateless
        http = http
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and();

        // Set unauthorized requests exception handler
        http = http
            .exceptionHandling()
				.authenticationEntryPoint(unauthorizedHandler)
            .and();

        // Set permissions on endpoints
        http.authorizeRequests()                    
            .antMatchers(HttpMethod.POST, "/rest/df/getOdbReportData").permitAll()  
            .antMatchers(HttpMethod.GET, "/rest/df/createEolEodDf").permitAll()
            .antMatchers(HttpMethod.POST, "/rest/df/createDataframeOdbData").permitAll()
            .antMatchers(HttpMethod.POST, "/rest/df/saveDefaultFavView").permitAll()
            .antMatchers(HttpMethod.POST, "/rest/df/getReportData").permitAll() 
            .antMatchers(HttpMethod.POST, "/rest/df/getReportHeader").permitAll() 
            .antMatchers(HttpMethod.GET, "/rest/df/deleteCloudCostDf").permitAll() 
            .antMatchers(HttpMethod.POST, "/rest/df/getReportDataFromClickHouse").permitAll() 
            .antMatchers(HttpMethod.POST, "/rest/reports/health-check/get-field-values").permitAll() 
//            .antMatchers(HttpMethod.PUT, "/rest/password-migration-aes").permitAll() 
//            .antMatchers(HttpMethod.PUT, "/rest/password-migration-rsa").permitAll() 

            //.antMatchers(HttpMethod.DELETE, "/rest/api/log-file/**").permitAll()
            // Our private endpoints
            .anyRequest().authenticated();

        // Add JWT token filter
        http.addFilterBefore(
        		authenticationTokenFilterBean(),
            UsernamePasswordAuthenticationFilter.class
        );
    }

    @Bean
    public static BCryptPasswordEncoder encoder(){
        return new BCryptPasswordEncoder();
    }
    
    
}