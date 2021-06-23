package com.zenfra.dataframe.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zenfra.utils.DBUtils;



@Component
public class EolService {

	public static final Logger logger = LoggerFactory.getLogger(EolService.class);
	  
	 @Value("${zenfra.path}")
	 private String commonPath;
	
	@Autowired
	SparkSession sparkSession;
	
	 private String dbUrl = DBUtils.getPostgres().get("dbUrl");
	
	public int getEOLEOSData() {
       
        long count = 0;
        try {
        	Dataset<Row> eoleosDataSet =  sparkSession.sql("select * from global_temp.eolDataDF");	
        	count = eoleosDataSet.count();
		} catch (Exception e) {
        try {
        	Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", "eol_eos_software");
			
			@SuppressWarnings("deprecation")
			Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			      
            count = eoleosDataSet.count();
            if (count > 0) {
            	eoleosDataSet.createOrReplaceTempView("eolDataDFTmp");
            	eoleosDataSet = sparkSession.sql("select os_type, os_version, os_name, end_of_life_cycle, end_of_extended_support from eolDataDFTmp");
                eoleosDataSet.createOrReplaceGlobalTempView("eolDataDF");
                eoleosDataSet.cache();
               // eoleosDataSet.show();
            }
        } catch (Exception ex) {
            logger.error("Exception in generating dataframe for EOL/EOS OS data", ex);
        }
		}
        logger.info("Construct EOL/EOS Dataframe Ends");
        return Integer.parseInt(String.valueOf(count));
    }

    public int getEOLEOSHW() {
        logger.info("Construct EOL/EOS - HW Dataframe Begins");
        long count = 0;
       
        try {
        	Dataset<Row> eoleosDataSet =  sparkSession.sql("select * from global_temp.eolHWDataDF");	
        	count = eoleosDataSet.count();
		} catch (Exception e) {
			 try {  
		            Map<String, String> options = new HashMap<String, String>();
					options.put("url", dbUrl);
					options.put("dbtable", "eol_eos_hardware");
					
					@SuppressWarnings("deprecation")
					Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
					 count = eoleosDataSet.count();
					  if (count > 0) {
			            	eoleosDataSet.createOrReplaceTempView("eolHWDataDFTmp");
			            	eoleosDataSet = sparkSession.sql("select vendor, model, end_of_life_cycle, end_of_extended_support, source_link from eolHWDataDFTmp");
			                eoleosDataSet.createOrReplaceGlobalTempView("eolHWDataDF");
			                eoleosDataSet.cache();
			            }           
		        } catch (Exception ex) {
		            logger.error("Exception in generating dataframe for EOL/EOS HW data", ex);
		        }
		}
       
        logger.info("Construct EOL/EOS - HW Dataframe Ends");
        return Integer.parseInt(String.valueOf(count));
    }
    
    public void getAWSPricing() {
    	try {
        	sparkSession.sql("select * from global_temp.awsPricingDF");			
		} catch(Exception e) {
        try {
            Dataset<Row> awsPriceDataSet = sparkSession.read().format("csv")
                    .option("header", "true").option("inferschema", false)
                    .load(commonPath+"/Dataframe/data/AWS EC2 Pricing - US East Ohio.csv");
            awsPriceDataSet.createOrReplaceGlobalTempView("awsPricingDF");
            awsPriceDataSet.cache();
        } catch (Exception ex) {
            logger.error("Exception in getAzurePricing", ex);
        }
		}
    }

    public void getAzurePricing() {
    	try {
        	sparkSession.sql("select * from global_temp.azurePricingDF");			
		} catch(Exception e) {
			try {
            Dataset<Row> azurePriceDataSet = sparkSession.read().format("csv")
                    .option("header", "true").option("inferschema", false)
                    .load(commonPath+"/Dataframe/data/Azure_Pricing_Data.csv");

            azurePriceDataSet.createOrReplaceTempView("AzurePricing");
            azurePriceDataSet.printSchema();
            
            System.out.println("-----azurePriceDataSet- " + azurePriceDataSet.count());

            Dataset<Row> dataCheck = sparkSession.sql("Select concat_ws(',', concat('Operating System: ',az.OperatingSystem),concat('vCPU: ',az.VCPUs)" +
                    " ,concat('Memory: ',az.Memory)) as `Azure Specs`,az.InstanceType" +
                    ",az.OperatingSystem,az.VCPUs,az.Memory" +
                    ",(Min(az.PricePerHour)*730)+min(ifnull(az.SoftwareCost,0)) as demandPrice" +
                    ",(Min(b.PricePerHour)*730)+min(ifnull(b.SoftwareCost,0)) as 3YrPrice " +
                    ",(Min(c.PricePerHour)*730)+min(ifnull(c.SoftwareCost,0)) as 1YrPrice " +
                    "from AzurePricing az " +
                    "join AzurePricing b on b.InstanceType=az.InstanceType and b.Type='3yr' and b.OperatingSystem = az.OperatingSystem " +
                    "join AzurePricing c on c.InstanceType=az.InstanceType and c.Type='1yr' and c.OperatingSystem = az.OperatingSystem " +
                    "where az.Region = 'US East' and az.Type = 'OnDemand' and az.OperatingSystem is not null and az.InstanceType is not NULL " +
                    "group by az.OperatingSystem,az.InstanceType,az.VCPUs,az.Memory");
            dataCheck.createOrReplaceGlobalTempView("azurePricingDF"); 
            dataCheck.cache();
            System.out.println("-----azurePriceDataSet----------- " + dataCheck.count());
        } catch (Exception ex) {
        	ex.printStackTrace();
            logger.error("Exception in getAzurePricing:", ex);
        }
		}
    }
    
    public void getGooglePricing() {
        logger.info("Construct Google Pricing Dataframe Begins");
        
        try {
        	sparkSession.sql("select * from global_temp.googlePricingDF");			
		} catch(Exception e) {
			try {
		        logger.info("Construct Google Pricing Dataframe Ends");
            Dataset<Row> googlePriceDataSet = sparkSession.read().format("csv")
                    .option("header", "true").option("inferschema", false)
                    .load(commonPath+"/Dataframe/data/Google_Pricing_Data.csv");

            googlePriceDataSet.createOrReplaceGlobalTempView("googlePricingDF");
            googlePriceDataSet.cache(); 
           
        } catch (Exception ex) {
            logger.error("Exception in generating dataframe for Google Pricing ", ex);
        }
		}
        
        
    }
  
   
}
