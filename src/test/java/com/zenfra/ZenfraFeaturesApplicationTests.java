package com.zenfra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.orientechnologies.orient.jdbc.OrientJdbcConnection;

//@SpringBootTest
class ZenfraFeaturesApplicationTests {

	public static void main(String a[]) {
		/*SparkConf conf = new SparkConf();
		conf.setMaster("local");
		SparkSession spark = SparkSession.builder().appName("Orient_Spark_Session").config(conf).getOrCreate();
		Map<String, String> orientDBProps = new HashMap<>(); 
		orientDBProps.put("url","jdbc:orient:REMOTE:uatdb.zenfra.co/dellemcdb");
		orientDBProps.put("user", "root");
		orientDBProps.put("password", "27CH9610PUub25Y");
		orientDBProps.put("spark", "true");
		orientDBProps.put("dbtable", "eoleosData");
		Dataset<Row> tableDataset = spark.read().format("jdbc").options(orientDBProps).load();
		tableDataset.show(); */
		
		Properties info = new Properties();
        String user = "root";
        String password = "27CH9610PUub25Y";
        info.put("user", user);
        info.put("password", password);

        try
        {
        	System.out.println("--------connection--------" + info);
        	
              Class.forName("com.orientechnologies.orient.jdbc.OrientJdbcDriver");
              Connection connection = (OrientJdbcConnection) DriverManager.getConnection("jdbc:orient:remote:uatdb.zenfra.co/dellemcdb", info);
              System.out.println("--------connection--------" + connection);
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
}
