package com.zenfra;

import javax.annotation.PostConstruct;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.dataframe.service.EolService;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class ZenfraFeaturesApplication extends SpringBootServletInitializer{

	
	@Autowired
	DataframeService dataframeService;
	
	@Autowired
	EolService eolService;
	
	public static void main(String[] args) {
		SpringApplication.run(ZenfraFeaturesApplication.class, args);
	}

	 @Bean
	   public Docket productApi() {
	      return new Docket(DocumentationType.SWAGGER_2).select()
	         .apis(RequestHandlerSelectors.basePackage("com.zenfra")).build();
	   }
	 

	   @Override
	   protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	      return application.sources(ZenfraFeaturesApplication.class);
	   }
	   
	   @Bean
	    public SparkConf sparkConf() {
	    	 SparkConf sparkConf = new SparkConf()
	                 .setAppName("ZenfraV2Df")
	                 .setMaster("local[*]")
	                 .set("spark.sql.crossJoin.enabled", "true")
	                 .set("spark.sql.shuffle.partitions", "1")
	                 .set("spark.sql.caseSensitive", "true")
			         .set("spark.driver.memory", "571859200")
	    	          .set("spark.testing.memory", "2147480000");
	    	          

	        return sparkConf;
	    }

	    @Bean
	    public JavaSparkContext javaSparkContextNew() {
	        return new JavaSparkContext(sparkConf());
	    }

	    @Bean
	    public SparkSession sparkSession() {
	        return SparkSession
	                .builder()
	                .sparkContext(javaSparkContextNew().sc())
	                .appName("ZenfraV2Df")
	                .config("spark.driver.memory", "491859200")
	                .config("spark.testing.memory", "2147480000")
	                .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
	                .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
	                .config("spark.network.timeout", "3200000ms")
	                .config("spark.storage.blockManagerSlaveTimeoutMs", "3100000ms")
	                .config("spark.executor.heartbeatInterval", "1000s")
	                .config("spark.sql.sources.partitionOverwriteMode", "dynamic")
	                .getOrCreate();
	    }
	    
	   
	    
	  
	    
	   @PostConstruct
	    public void createDataframeView() {		
	    	dataframeService.createDataframeGlobalView();
	        
	    	//eolService.getEOLEOSData();
	    	//eolService.getEOLEOSHW();
	    	//eolService.getGooglePricing();
	    	//eolService.getAzurePricing();
	    	//eolService.getAWSPricing();
	    	
	    }
	   
	   
}