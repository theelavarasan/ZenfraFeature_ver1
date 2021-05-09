package com.zenfra.dataframe.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.utils.DBUtils;



@Component
public class EolService {

	public static final Logger logger = LoggerFactory.getLogger(EolService.class);
	  
	//@Autowired
	//ODatabaseSession db;
	
	@Autowired
	SparkSession sparkSession;
	
	 private String oDbUrl = DBUtils.getOrientDb().get("dbUrl");
	
	public int getEOLEOSData() {
       
        long count = 0;
        try {
          
           // String eolQuery = "select ostype,osname,osversion,endoflifecycle,endofextendedsupport,sourceurl from eoleosData";
            
           /* Dataset<Row> eoleosDataSet =  sparkSession.sqlContext().read()
            .format("org.apache.spark.orientdb.documents")
            .option("dburl", "uatdb.zenfra.co")
            .option("user", "root")
            .option("password", "TraBratRA2ozOJe")
            .option("class", "eoleosData")
            .option("query", "select ostype,osname,osversion,endoflifecycle,endofextendedsupport,sourceurl from eoleosData")
            .load();  */
            
        	Map<String, String> options = new HashMap<String, String>();
			options.put("url", oDbUrl);
			options.put("dbtable", "eoleosData");
			
			@SuppressWarnings("deprecation")
			Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			eoleosDataSet.show();
            
           /* Map<String, String> orientDBProps = new HashMap<>(); 
            orientDBProps.put("url","jdbc:orient:REMOTE:uatdb.zenfra.co/dellemcdb");
            orientDBProps.put("user", "root");
            orientDBProps.put("password", "TraBratRA2ozOJe");
            orientDBProps.put("spark", "true");
            orientDBProps.put("dbtable", "eoleosData");
            Dataset<Row> eoleosDataSet = sparkSession.sqlContext().read().format("jdbc").options(orientDBProps).load();
            eoleosDataSet.show(); */

           // Dataset<Row> eoleosDataSet = queryToDataSetSparkSession(eolQuery, db, sparkSession);
            count = eoleosDataSet.count();
            if (count > 0) {
                eoleosDataSet.createOrReplaceGlobalTempView("eolDataDF");
            }
        } catch (Exception ex) {
            logger.error("Exception in generating dataframe for EOL/EOS OS data", ex);
        }
        logger.info("Construct EOL/EOS Dataframe Ends");
        return Integer.parseInt(String.valueOf(count));
    }

    public int getEOLEOSHW() {
        logger.info("Construct EOL/EOS - HW Dataframe Begins");
        long count = 0;
        try {           
            String eolQuery = "select vendor,model,endoflifecycle,endofextendedsupport,sourcelink from eoleosDataHardWare";

            Dataset<Row> eoleosDataSet =  sparkSession.sqlContext().read()
                    .format("org.apache.spark.orientdb.documents")
                    .option("dburl", "uatdb.zenfra.co")
                    .option("user", "root")
                    .option("password", "27CH9610PUub25Y")
                    .option("class", "eoleosData")
                    .option("query", eolQuery)
                    .load();
            count = eoleosDataSet.count();
            if (count > 0) {
                eoleosDataSet.createOrReplaceGlobalTempView("eolHWDataDF");
            }
        } catch (Exception ex) {
            logger.error("Exception in generating dataframe for EOL/EOS HW data", ex);
        }
        logger.info("Construct EOL/EOS - HW Dataframe Ends");
        return Integer.parseInt(String.valueOf(count));
    }

 
}
