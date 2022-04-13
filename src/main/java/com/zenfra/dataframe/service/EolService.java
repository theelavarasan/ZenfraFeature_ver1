package com.zenfra.dataframe.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class EolService {

	public static final Logger logger = LoggerFactory.getLogger(EolService.class);

	private String commonPath;

	@PostConstruct
	public void init() {
		commonPath = ZKModel.getProperty(ZKConstants.DATAFRAME_PATH);
	}

	@Autowired
	SparkSession sparkSession;

	private String dbUrl = DBUtils.getPostgres().get("dbUrl");

	public int getEOLEOSData() {

		long count = 0;
		try {
			Dataset<Row> eoleosDataSet = sparkSession.sql("select * from global_temp.eolDataDF");
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
					eoleosDataSet = sparkSession.sql(
							"select os_type, os_version, os_name, end_of_life_cycle, end_of_extended_support from eolDataDFTmp");
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
			Dataset<Row> eoleosDataSet = sparkSession.sql("select * from global_temp.eolHWDataDF");
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
					eoleosDataSet = sparkSession.sql(
							"select vendor, model, end_of_life_cycle, end_of_extended_support, source_link from eolHWDataDFTmp");
					eoleosDataSet.createOrReplaceGlobalTempView("eolHWDataDF");
					eoleosDataSet.cache();
				}
			} catch (Exception ex) {
				e.printStackTrace();
				/*StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex1 = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex1);*/
				logger.error("Exception in generating dataframe for EOL/EOS HW data", ex);
			}
		}

		logger.info("Construct EOL/EOS - HW Dataframe Ends");
		return Integer.parseInt(String.valueOf(count));
	}

	public void getAWSPricing() {
		try {
			sparkSession.sql("select * from global_temp.awsPricingDF");
		} catch (Exception e) {
			try {
				Dataset<Row> awsPriceDataSet = sparkSession.read().format("csv").option("header", "true")
						.option("inferschema", false)
						.load(commonPath + "/Dataframe/data/AWS EC2 Pricing - US East Ohio.csv");
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
		} catch (Exception e) {
			try {
				Dataset<Row> azurePriceDataSet = sparkSession.read().format("csv").option("header", "true")
						.option("inferschema", false).load(commonPath + "/Dataframe/data/Azure_Pricing_Data.csv");

				azurePriceDataSet.createOrReplaceTempView("AzurePricing");
				azurePriceDataSet.printSchema();

				System.out.println("-----azurePriceDataSet- " + azurePriceDataSet.count());

				Dataset<Row> dataCheck = sparkSession.sql(
						"Select concat_ws(',', concat('Operating System: ',az.OperatingSystem),concat('vCPU: ',az.VCPUs)"
								+ " ,concat('Memory: ',az.Memory)) as `Azure Specs`,az.InstanceType"
								+ ",az.OperatingSystem,cast(az.VCPUs as int), cast(az.Memory as int)"
								+ ",cast((Min(az.PricePerHour)*730)+min(ifnull(az.SoftwareCost,0)) as float) as demandPrice"
								+ ",cast((Min(b.PricePerHour)*730)+min(ifnull(b.SoftwareCost,0)) as float) as 3YrPrice "
								+ ",cast((Min(c.PricePerHour)*730)+min(ifnull(c.SoftwareCost,0)) as float) as 1YrPrice "
								+ "from AzurePricing az "
								+ "left join AzurePricing b on b.InstanceType=az.InstanceType and b.Type='3yr' and b.OperatingSystem = az.OperatingSystem "
								+ "left join AzurePricing c on c.InstanceType=az.InstanceType and c.Type='1yr' and c.OperatingSystem = az.OperatingSystem "
								+ "where az.Region = 'US East' and az.Type = 'OnDemand' and az.OperatingSystem is not null and az.InstanceType is not NULL "
								+ "group by az.VCPUs, az.Memory, az.InstanceType, az.OperatingSystem");
				dataCheck.createOrReplaceGlobalTempView("azurePricingDF");
				System.out.println("-----azurePriceDataSet----------- " + dataCheck.count());
				
				Dataset<Row> dataCheck12 = sparkSession.sql("select `Azure Specs`, InstanceType, demandPrice, 3YrPrice,1YrPrice from global_temp.azurePricingDF where  OperatingSystem='Windows'");
				
						dataCheck12.show(50000, false);
				
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
		} catch (Exception e) {
			try {
				logger.info("Construct Google Pricing Dataframe Ends");
				Dataset<Row> googlePriceDataSet = sparkSession.read().format("csv").option("header", "true")
						.option("inferschema", false).load(commonPath + "/Dataframe/data/Google_Pricing_Data.csv");

				googlePriceDataSet.createOrReplaceGlobalTempView("googlePricingDF");
				googlePriceDataSet.cache();

			} catch (Exception ex) {
				logger.error("Exception in generating dataframe for Google Pricing ", ex);
			}
		}

	}

	public void recreateEolEosDataframe() {
		System.out.println("-------------recreate EOL EOS dataframe---------");
		try {
			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", "eol_eos_hardware");

			@SuppressWarnings("deprecation")
			Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));
			long count = eoleosDataSet.count();
			if (count > 0) {
				eoleosDataSet.createOrReplaceTempView("eolHWDataDFTmp");
				eoleosDataSet = sparkSession.sql(
						"select vendor, model, end_of_life_cycle, end_of_extended_support, source_link from eolHWDataDFTmp");
				eoleosDataSet.createOrReplaceGlobalTempView("eolHWDataDF");
				eoleosDataSet.cache();
			}
		} catch (Exception ex) {
			logger.error("Exception in generating dataframe for EOL/EOS HW data", ex);
		}

		try {
			Map<String, String> options = new HashMap<String, String>();
			options.put("url", dbUrl);
			options.put("dbtable", "eol_eos_software");

			@SuppressWarnings("deprecation")
			Dataset<Row> eoleosDataSet = sparkSession.sqlContext().jdbc(options.get("url"), options.get("dbtable"));

			long count = eoleosDataSet.count();
			if (count > 0) {
				eoleosDataSet.createOrReplaceTempView("eolDataDFTmp");
				eoleosDataSet = sparkSession.sql(
						"select os_type, os_version, os_name, end_of_life_cycle, end_of_extended_support from eolDataDFTmp");
				eoleosDataSet.createOrReplaceGlobalTempView("eolDataDF");
				eoleosDataSet.cache();
			}
		} catch (Exception ex) {
			logger.error("Exception in generating dataframe for EOL/EOS OS data", ex);
		}

	}

}
