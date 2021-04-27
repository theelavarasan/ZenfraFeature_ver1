package com.zenfra.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.zenfra.queries.ChartQueries;
import com.zenfra.queries.DashBoardChartsDetailsQueries;
import com.zenfra.queries.DashBoardChartsQueries;
import com.zenfra.queries.FavouriteOrderQueries;
import com.zenfra.queries.FavouriteViewQueries;
import com.zenfra.queries.UserTableQueries;

@Component
@Configuration
@PropertySource("classpath:quries.properties")
public class CommonQueriesData {
	
	 @Bean
	 @ConfigurationProperties("chart.table")
	 public ChartQueries chart() {
	      return new ChartQueries();
	 }	
	 
	 @Bean
	 @ConfigurationProperties("dashboard.charts.details")
	 public DashBoardChartsDetailsQueries dashBoardChartDetails() {
	      return new DashBoardChartsDetailsQueries();
	 }	
	 
	 @Bean
	 @ConfigurationProperties("dashboard.charts")
	 public DashBoardChartsQueries dashBoardChart() {
	      return new DashBoardChartsQueries();
	 }
	 
	 @Bean
	 @ConfigurationProperties("favourite.order")
	 public FavouriteOrderQueries favouriteOrder() {
	      return new FavouriteOrderQueries();
	 }
	 
	 @Bean
	 @ConfigurationProperties("favourite.view")
	 public FavouriteViewQueries favouriteView() {
	      return new FavouriteViewQueries();
	 }
	 
	 @Bean
	 @ConfigurationProperties("user")
	 public UserTableQueries userTable() {
	      return new UserTableQueries();
	 }
	 
	
	

}
