package com.zenfra.dataframe.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
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
        	/*Map<String, String> options = new HashMap<String, String>();
			options.put("url", oDbUrl);
			options.put("dbtable", "eoleosData");
			
			@SuppressWarnings("deprecation")
			Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			
			System.out.println("----------eoleosDataSet---------------------------");
			eoleosDataSet.show();*/
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
        Dataset<Row> eoleosDataSet = sparkSession.read().format("csv")
                     .option("header", "true").option("inferschema", false)
                     .load("E:\\opt\\eol.csv");
      
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
			
			System.out.println("----------eoleosDataSet hw---------------------------");
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
                    .load("/opt/ZENfra/AWS EC2 Pricing - US East Ohio.csv");
            awsPriceDataSet.createOrReplaceGlobalTempView("awsPricingDF");
        } catch (Exception e) {
            logger.error("Exception in getAzurePricing", e);
        }
    }

    public void getAzurePricing() {
        try {
            Dataset<Row> azurePriceDataSet = sparkSession.read().format("csv")
                    .option("header", "true").option("inferschema", false)
                    .load("/opt/ZENfra/Azure_Pricing_Data.csv");

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
                    .load("/opt/ZENfra/Google_Pricing_Data.csv");

            googlePriceDataSet.createOrReplaceGlobalTempView("googlePricingDF");
        } catch (Exception ex) {
            logger.error("Exception in generating dataframe for Google Pricing ", ex);
        }
        logger.info("Construct Google Pricing Dataframe Begins");
    }

    
  
    
    public void getAWSReport(String view) {
        try {
            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
                    " select report.log_date,report.`vCPU`, report.actual_os_type, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`," +
                    " report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`, report.`Number Of Processors` as `Number Of Processors`, report.`Logical Processor Count`, " +
                    " report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`," +
                    " report.`Number of Ports`, (report.`PricePerUnit` * 730) as `AWS On Demand Price`," +
                    " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='3yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 3 Year Price`, " +
                    " ((select min(a.PricePerUnit) from global_temp.awsPricingDF a where a.`Operating System` = report.`OperatingSystem` and a.PurchaseOption='No Upfront' and a.`Instance Type`=report.`AWS Instance Type` and a.LeaseContractLength='1yr' and cast(a.PricePerUnit as float) > 0) * 730) as `AWS 1 Year Price`, " +
                    " report.`AWS Instance Type`,report.`AWS Region`,report.`AWS Specs`," +
                    " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`PricePerUnit` as float) asc) as my_rank" +
                    " from (SELECT localDiscoveryDF.logDate,localDiscoveryDF.siteKey, localDiscoveryDF.actualOSType, localDiscoveryDF.serverName as `Server Name`, localDiscoveryDF.serverType as `Server Type`," +
                    " localDiscoveryDF.os as `OS Name`,localDiscoveryDF.osVersion as `OS Version`,localDiscoveryDF.serverModel as `Server Model`," +
                    " cast(localDiscoveryDF.processorCount as int) as `Logical Processor Count`,cast(localDiscoveryDF.processors as int) as `Number Of Processors`," +
                    " cast(localDiscoveryDF.memory as int) as `Memory`, round(localDiscoveryDF.totalSize,2) as `Total Size`, " +
                    " cast(localDiscoveryDF.cpuGHz as int) as `CPU GHz`, localDiscoveryDF.processorName as `Processor Name`, " +
                    " cast(localDiscoveryDF.numberOFCore as int) as `Number of Cores`," +
                    " localDiscoveryDF.dbService as `DB Service`, localDiscoveryDF.hbaSpeed as `HBA Speed`, cast(localDiscoveryDF.numberOfPorts as int) as `Number of Ports`," +
                    " awsPricing2.`Instance Type` as `AWS Instance Type`, awsPricing2.Location as `AWS Region`" +
                    " ,concat_ws(',', concat('Processor: ',awsPricing2.`Physical Processor`),concat('vCPU: ',awsPricing2.vCPU)" +
                    " ,concat('Clock Speed: ',awsPricing2.`Clock Speed`),concat('Processor Architecture: ',awsPricing2.`Processor Architecture`)" +
                    " ,concat('Memory: ',awsPricing2.Memory),concat('Storage: ',awsPricing2.Storage),concat('Network Performance: ',awsPricing2.`Network Performance`)) as `AWS Specs`" +
                    " , (case when localDiscoveryDF.processorCount is null  and localDiscoveryDF.processors is not null then " +
                    " cast(localDiscoveryDF.processors as int)  when localDiscoveryDF.processorCount is not null then " +
                    " localDiscoveryDF.processorCount else 0 end) as `vCPU`, (case when localDiscoveryDF.memory is null then 0 else cast(localDiscoveryDF.memory as int) end) as `MemorySize`, awsPricing2.PricePerUnit as `PricePerUnit`,awsPricing2.`Operating System` as `OperatingSystem`" +
                    " FROM global_temp."+view+" localDiscoveryDF" +
                    " join (Select localDiscoveryDF1.site_key siteKey,localDiscoveryDF1.serverName ServerName,max(localDiscoveryDF1.logDate) MaxLogDate " +
                    " from global_temp."+view+"  localDiscoveryDF1 group by localDiscoveryDF1.serverName,localDiscoveryDF1.siteKey) localDiscoveryTemp2 ON localDiscoveryDF.logDate = localDiscoveryTemp2.MaxLogDate and " +
                    " localDiscoveryTemp2.ServerName = localDiscoveryDF.ServerName and localDiscoveryDF.siteKey = localDiscoveryTemp2.siteKey" +
                    " left join (select `Operating System`,Memory,min(PricePerUnit) as pricePerUnit, vCPU,TermType from global_temp.awsPricingDF where `License Model`='No License required'" +
                    " and Location='US East (Ohio)' and Tenancy <> 'Host' and (`Product Family` = 'Compute Instance (bare metal)' or `Product Family` = 'Compute Instance') and cast(PricePerUnit as float) > 0 group by `Operating System`,Memory,vCPU,TermType) awsPricing on" +
                    " lcase(awsPricing.`Operating System`) = lcase((case when localDiscoveryDF.os like '%Red Hat%' then 'RHEL'" +
                    " when localDiscoveryDF.os like '%SUSE%' then 'SUSE' when localDiscoveryDF.os like '%Linux%' OR localDiscoveryDF.os like '%CentOS%' then 'Linux'" +
                    " when localDiscoveryDF.os like '%Windows%' then 'Windows' else localDiscoveryDF.serverType end)) and" +
                    " awsPricing.Memory >= (case when localDiscoveryDF.memory is null then 0 else cast(localDiscoveryDF.memory as int) end)" +
                    " and awsPricing.vCPU >= (case when localDiscoveryDF.processorCount is null  and localDiscoveryDF.processors is not null then " +
                    " cast(localDiscoveryDF.processors as int)  when localDiscoveryDF.processorCount is not null then " +
                    " localDiscoveryDF.processorCount else 0 end)" +
                    " left join global_temp.awsPricingDF awspricing2 on awspricing2.`Operating System` = awsPricing.`Operating System` and awsPricing2.PricePerUnit = awsPricing.pricePerUnit and awsPricing.Memory = " +
                    " awsPricing2.Memory and awsPricing.vCPU = awsPricing2.vCPU and awsPricing2.TermType='OnDemand' where cast(awsPricing2.PricePerUnit as float) > 0) report) reportData" +
                    " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
            dataCheck.createOrReplaceGlobalTempView("awsReport");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void getAzureReport(String view) {
        try {
            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
                    " select report.logDate,report.`vCPU`, report.siteKey, " +
                    " report.actualOSType, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`," +
                    " report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`," +
                    " report.`Number Of Processors` as `Number Of Processors`, report.`Logical Processor Count`, " +
                    " report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`," +
                    " report.`Number of Ports`,report.`Azure On Demand Price`,report.`Azure 3 Year Price`, report.`Azure 1 Year Price`, report.`Azure Instance Type`, report.`Azure Specs`, " +
                    " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`Azure On Demand Price` as float) asc) as my_rank" +
                    " from (SELECT localDiscoveryDF.logDate,localDiscoveryDF.siteKey, localDiscoveryDF.actualOSType, localDiscoveryDF.serverName as `Server Name`, localDiscoveryDF.serverType as `Server Type`," +
                    " localDiscoveryDF.os as `OS Name`,localDiscoveryDF.osVersion as `OS Version`,localDiscoveryDF.serverModel as `Server Model`," +
                    " localDiscoveryDF.processorCount as `Logical Processor Count`,localDiscoveryDF.processors as `Number Of Processors`," +
                    " cast(localDiscoveryDF.memory as int) as `Memory`, round(localDiscoveryDF.totalSize,2) as `Total Size`, " +
                    " cast(localDiscoveryDF.cpuGHz as int) as `CPU GHz`, localDiscoveryDF.processorName as `Processor Name`, " +
                    " cast(localDiscoveryDF.numberOFCore as int) as `Number of Cores`," +
                    " localDiscoveryDF.dbService as `DB Service`, localDiscoveryDF.hbaSpeed as `HBA Speed`, cast(localDiscoveryDF.numberOfPorts as int) as `Number of Ports`," +
                    " round(azurePricingDF.demandPrice,2) as `Azure On Demand Price`," +
                    " round(azurePricingDF.3YrPrice,2) as `Azure 3 Year Price`," +
                    " round(azurePricingDF.1YrPrice,2) as `Azure 1 Year Price`," +
                    " azurePricingDF.InstanceType as `Azure Instance Type`,azurePricingDF.`Azure Specs`," +
                    " (case when localDiscoveryDF.processorCount is null  and localDiscoveryDF.processors is not null then " +
                    " cast(localDiscoveryDF.processors as int)  when localDiscoveryDF.processorCount is not null then " +
                    " localDiscoveryDF.processorCount else 0 end) as `vCPU`" +
                    " FROM global_temp."+view+" localDiscoveryDF" +
                    " left join global_temp.azurePricingDF azurePricingDF on azurePricingDF.vCPUs >= (case when localDiscoveryDF.processorCount is null  and localDiscoveryDF.processors is not null then" +
                    " cast(localDiscoveryDF.processors as int)  when localDiscoveryDF.processorCount is not null then" +
                    " localDiscoveryDF.processorCount else 0 end)" +
                    " and azurePricingDF.Memory >= (case when localDiscoveryDF.memory is null then 0 else cast(localDiscoveryDF.memory as int) end) and" +
                    " lcase(azurePricingDF.OperatingSystem) = lcase((case when localDiscoveryDF.os like '%Red Hat%' then 'RHEL' " +
                    " when localDiscoveryDF.os like '%SUSE%' then 'SUSE' when localDiscoveryDF.os like '%Linux%' OR localDiscoveryDF.os like '%CentOS%' then 'Linux'" +
                    " when localDiscoveryDF.os like '%Windows%' then 'Windows' else localDiscoveryDF.serverType end))) report ) reportData " +
                    " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
            dataCheck.createOrReplaceGlobalTempView("azureReport");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void getGoogleReport(String view) {
        try {
            Dataset<Row> dataCheck = sparkSession.sql("select reportData.* from (" +
                    " select report.logDate,report.`vCPU`, report.siteKey, " +
                    " report.actualOSType, report.`Server Name`, report.`OS Name`,report.`OS Version`, report.`Server Type`, report.`Server Model`," +
                    " report.`Memory` as `Memory`, (case when report.`Total Size` is null then 0 else report.`Total Size` end) as `Total Size`," +
                    " report.`Number Of Processors` as `Number Of Processors`, report.`Logical Processor Count`, " +
                    " report.`CPU GHz`, report.`Processor Name`, report.`Number of Cores` as `Number of Cores`, report.`DB Service`, report.`HBA Speed`," +
                    " report.`Number of Ports`," +
                    " report.`Google Instance Type`,report.`Google On Demand Price`,report.`Google 1 Year Price`,report.`Google 3 Year Price`," +
                    " ROW_NUMBER() OVER (PARTITION BY report.`Server Name` ORDER BY cast(report.`Google On Demand Price` as float) asc) as my_rank" +
                    " from (SELECT localDiscoveryDF.logDate,localDiscoveryDF.siteKey, localDiscoveryDF.actualOSType, localDiscoveryDF.serverName as `Server Name`, localDiscoveryDF.serverType as `Server Type`," +
                    " localDiscoveryDF.os as `OS Name`,localDiscoveryDF.osVersion as `OS Version`,localDiscoveryDF.serverModel as `Server Model`," +
                    " cast(localDiscoveryDF.processorCount as int) as `Logical Processor Count`,cast(localDiscoveryDF.processors as int) as `Number Of Processors`," +
                    " cast(localDiscoveryDF.memory as int) as `Memory`, round(localDiscoveryDF.totalSize,2) as `Total Size`, " +
                    " cast(localDiscoveryDF.cpuGHz as int) as `CPU GHz`, localDiscoveryDF.processorName as `Processor Name`, " +
                    " cast(localDiscoveryDF.numberOFCore as int) as `Number of Cores`," +
                    " localDiscoveryDF.dbService as `DB Service`, localDiscoveryDF.hbaSpeed as `HBA Speed`, cast(localDiscoveryDF.numberOfPorts as int) as `Number of Ports`," +
                    " localDiscoveryDF.processorCount as `vCPU`," +
                    " googlePricing.InstanceType as `Google Instance Type`," +
                    " round(googlePricing.pricePerUnit*730 + " +
                    " ((case when localDiscoveryDF.totalSize is null then 0 else localDiscoveryDF.totalSize end) * 0.08) + " +
                    " (case when localDiscoveryDF.os like '%Windows%' then 67.16  when localDiscoveryDF.os like '%Red Hat%' then 43.8 else 0  end),2) as `Google On Demand Price`," +
                    " round(googlePricing.1YrPrice*730 + " +
                    " ((case when localDiscoveryDF.totalSize is null then 0 else localDiscoveryDF.totalSize end) * 0.05) + " +
                    " (case when localDiscoveryDF.os like '%Windows%' then 67.16  when localDiscoveryDF.os like '%Red Hat%' then 43.8 else 0  end),2) as `Google 1 Year Price`," +
                    " round(googlePricing.3YrPrice*730 + " +
                    " ((case when localDiscoveryDF.totalSize is null then 0 else localDiscoveryDF.totalSize end) * 0.04) + " +
                    " (case when localDiscoveryDF.os like '%Windows%' then 67.16  when localDiscoveryDF.os like '%Red Hat%' then 43.8 else 0  end),2) as `Google 3 Year Price`" +
                    " FROM global_temp."+view+" localDiscoveryDF " +
                    " join (Select localDiscoveryDF1.siteKey siteKey,localDiscoveryDF1.serverName ServerName,max(localDiscoveryDF1.logDate) MaxLogDate " +
                    " from global_temp."+view+" localDiscoveryDF1 group by localDiscoveryDF1.serverName,localDiscoveryDF1.siteKey) localDiscoveryTemp2 ON localDiscoveryDF.logDate = localDiscoveryTemp2.MaxLogDate and " +
                    " localDiscoveryTemp2.ServerName = localDiscoveryDF.ServerName and localDiscoveryDF.siteKey = localDiscoveryTemp2.siteKey" +
                    " left join (select cast(OnDemandPrice as float) as pricePerUnit,vCPUs,Memory,InstanceType,1YrPrice,3YrPrice from global_temp.googlePricingDF where " +
                    " Region='US East' order by cast(OnDemandPrice as float) asc) googlePricing on cast(googlePricing.VCPUs as float) >= " +
                    " (case when localDiscoveryDF.processorCount is null  and localDiscoveryDF.processors is not null then" +
                    " cast(localDiscoveryDF.processors as int)  when localDiscoveryDF.processorCount is not null then" +
                    " localDiscoveryDF.processorCount else 0 end) and " +
                    " cast(googlePricing.Memory as float) >= (case when localDiscoveryDF.memory is null then 0 else " +
                    " cast(localDiscoveryDF.memory as int) end)) report ) reportData " +
                    " where reportData.my_rank= 1 order by reportData.`Server Name` asc").toDF();
            dataCheck.createOrReplaceGlobalTempView("googleReport");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
 
}
