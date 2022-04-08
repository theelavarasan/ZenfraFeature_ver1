package com.zenfra;

import java.io.PrintWriter;
import java.io.StringWriter;

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
import org.springframework.web.client.RestTemplate;

import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.dataframe.service.EolService;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.ExceptionHandlerMail;
import com.zenfra.utils.ZookeeperConnection;

@SpringBootApplication
public class ZenfraFeaturesApplication extends SpringBootServletInitializer {

	@Autowired
	DataframeService dataframeService;

	@Autowired
	EolService eolService;

	public static void main(String[] args) {
		SpringApplication.run(ZenfraFeaturesApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(ZenfraFeaturesApplication.class);
	}

	@Bean
	public SparkConf sparkConf() {
		SparkConf sparkConf = new SparkConf().setAppName("ZenfraV2Df").setMaster("local[*]")
				.set("spark.sql.crossJoin.enabled", "true").set("spark.sql.shuffle.partitions", "1")
				.set("spark.sql.caseSensitive", "true").set("spark.driver.memory", "571859200")
				.set("spark.testing.memory", "2147480000");

		return sparkConf;
	}

	@Bean
	public JavaSparkContext javaSparkContextNew() {
		return new JavaSparkContext(sparkConf());
	}

	@Bean
	public SparkSession sparkSession() {
		return SparkSession.builder().sparkContext(javaSparkContextNew().sc()).appName("ZenfraV2Df")
				.config("spark.driver.memory", "571859200").config("spark.testing.memory", "2147480000")
				.config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
				.config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
				.config("spark.network.timeout", "3200000ms")
				.config("spark.storage.blockManagerSlaveTimeoutMs", "3100000ms")
				.config("spark.executor.heartbeatInterval", "1000s")
				.config("spark.sql.sources.partitionOverwriteMode", "dynamic").getOrCreate();
	}

	@PostConstruct
	public void createDataframeView() {
		ZookeeperConnection zkConnection = new ZookeeperConnection();
		try {
			ZKModel.zkData = zkConnection.getZKData();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	
		eolService.getEOLEOSHW();
		eolService.getEOLEOSData();
		dataframeService.createDataframeForLocalDiscovery("local_discovery");

		//eolService.getGooglePricing();
		//eolService.getAzurePricing();
		//eolService.getAWSPricing();

	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
