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
            
          /*  Dataset<Row> eoleosDataSet =  sparkSession.sqlContext().read()
            .format("org.apache.spark.orientdb.documents")
            .option("dburl", "uatdb.zenfra.co")
            .option("user", "root")
            .option("password", "TraBratRA2ozOJe")
            .option("class", "eoleosData")
            .option("query", "select ostype,osname,osversion,endoflifecycle,endofextendedsupport,sourceurl from eoleosData")
            .load();  
            */
            
        	////////////////////////////////////////////////////////
        	Map<String, String> options = new HashMap<String, String>();
			options.put("url", oDbUrl);
			options.put("dbtable", "eoleosData");
			
			@SuppressWarnings("deprecation")
			Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			
			System.out.println("----------eoleosDataSet---------------------------");
			eoleosDataSet.show();
            //////////////////////////////////////////////////////////////////
           /* Map<String, String> orientDBProps = new HashMap<>(); 
            orientDBProps.put("url","jdbc:orient:REMOTE:uatdb.zenfra.co/dellemcdb");
            orientDBProps.put("user", "root");
            orientDBProps.put("password", "TraBratRA2ozOJe");
            orientDBProps.put("spark", "true");
            orientDBProps.put("dbtable", "eoleosData");
            Dataset<Row> eoleosDataSet = sparkSession.sqlContext().read().format("jdbc").options(orientDBProps).load();
            eoleosDataSet.show(); 
*/
           // Dataset<Row> eoleosDataSet = queryToDataSetSparkSession(eolQuery, db, sparkSession);
        	/* Dataset<Row> eoleosDataSet = sparkSession.read().format("csv")
                     .option("header", "true").option("inferschema", false)
                     .load("E:\\opt\\eol.csv");
        	 */
            count = eoleosDataSet.count();
            if (count > 0) {
            	eoleosDataSet.createOrReplaceTempView("eolDataDFTmp");
            	eoleosDataSet = sparkSession.sql("select ostype, osversion, endoflifecycle, endofextendedsupport from eolDataDFTmp");
                eoleosDataSet.createOrReplaceGlobalTempView("eolDataDF");
               // eoleosDataSet.show();
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

            /*Dataset<Row> eoleosDataSet =  sparkSession.sqlContext().read()
                    .format("org.apache.spark.orientdb.documents")
                    .option("dburl", "uatdb.zenfra.co")
                    .option("user", "root")
                    .option("password", "27CH9610PUub25Y")
                    .option("class", "eoleosData")
                    .option("query", eolQuery)
                    .load();
            count = eoleosDataSet.count();
            */
            
            Map<String, String> options = new HashMap<String, String>();
			options.put("url", oDbUrl);
			options.put("dbtable", "eoleosDataHardWare");
			
			@SuppressWarnings("deprecation")
			Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			
			System.out.println("----------eoleosDataSet---------------------------");
			eoleosDataSet.show();        	
			  count = eoleosDataSet.count();
			  if (count > 0) {
	            	eoleosDataSet.createOrReplaceTempView("eolHWDataDFTmp");
	            	eoleosDataSet = sparkSession.sql("select vendor, model, endoflifecycle, endofextendedsupport, sourcelink from eolHWDataDFTmp");
	                eoleosDataSet.createOrReplaceGlobalTempView("eolHWDataDF");
	               // eoleosDataSet.show();
	            }           
        } catch (Exception ex) {
            logger.error("Exception in generating dataframe for EOL/EOS HW data", ex);
        }
        logger.info("Construct EOL/EOS - HW Dataframe Ends");
        return Integer.parseInt(String.valueOf(count));
    }
    
    public void getAWSPricing() {
        try {
            Dataset<Row> awsPriceDataSet = sparkSession.read().format("csv")
                    .option("header", "true").option("inferschema", false)
                    .load("/opt/ZENfra/Dataframe/data/AWS EC2 Pricing - US East Ohio.csv");
            awsPriceDataSet.createOrReplaceGlobalTempView("awsPricingDF");
        } catch (Exception e) {
            logger.error("Exception in getAzurePricing", e);
        }
    }

    public void getAzurePricing() {
        try {
            Dataset<Row> azurePriceDataSet = sparkSession.read().format("csv")
                    .option("header", "true").option("inferschema", false)
                    .load("/opt/ZENfra/Dataframe/data/Azure_Pricing_Data.csv");

            azurePriceDataSet.createOrReplaceTempView("azurePricing");

            Dataset<Row> dataCheck = sparkSession.sql("Select concat_ws(',', concat('Operating System: ',az.OperatingSystem),concat('vCPU: ',az.vCPUs)" +
                    " ,concat('Memory: ',az.Memory)) as `Azure Specs`,az.InstanceType" +
                    ",az.OperatingSystem,az.vCPUs,az.Memory" +
                    ",(Min(az.PricePerHour)*730)+min(ifnull(az.softwarecost,0)) as demandPrice" +
                    ",(Min(b.PricePerHour)*730)+min(ifnull(b.softwarecost,0)) as 3YrPrice " +
                    ",(Min(c.PricePerHour)*730)+min(ifnull(c.softwarecost,0)) as 1YrPrice " +
                    "from AzurePricing az " +
                    "join AzurePricing b on b.InstanceType=az.InstanceType and b.Type='3yr' and b.OperatingSystem = az.OperatingSystem " +
                    "join AzurePricing c on c.InstanceType=az.InstanceType and c.Type='1yr' and c.OperatingSystem = az.OperatingSystem " +
                    "where az.Region = 'US East' and az.Type = 'OnDemand' and az.OperatingSystem is not null and az.InstanceType is not NULL " +
                    "group by az.OperatingSystem,az.InstanceType,az.vCPUs,az.Memory");
            dataCheck.createOrReplaceGlobalTempView("azurePricingDF");
        } catch (Exception e) {
            logger.error("Exception in getAzurePricing:", e);
        }
    }
    
    public void getGooglePricing() {
        logger.info("Construct Google Pricing Dataframe Begins");
        try {
            Dataset<Row> googlePriceDataSet = sparkSession.read().format("csv")
                    .option("header", "true").option("inferschema", false)
                    .load("/opt/ZENfra/Dataframe/data/Google_Pricing_Data.csv");

            googlePriceDataSet.createOrReplaceGlobalTempView("googlePricingDF");
        } catch (Exception ex) {
            logger.error("Exception in generating dataframe for Google Pricing ", ex);
        }
        logger.info("Construct Google Pricing Dataframe Begins");
    }


 
}
