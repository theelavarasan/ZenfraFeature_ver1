package com.zenfra;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.JsonArray;

//@SpringBootTest
class ZenfraFeaturesApplicationTests {

	public static void main(String a[]) throws SQLException, Exception {
		
		SparkSession sparkSession = SparkSession.builder().master("local").appName("simple").getOrCreate();
		 JavaSparkContext sc = new JavaSparkContext(sparkSession.sparkContext());
		/*Map<String, String> orientDBProps = new HashMap<String, String>(); 
		orientDBProps.put("url","jdbc:orient:remote:uatdb.zenfra.co/uatvpsdb");
		orientDBProps.put("user", "root");
		orientDBProps.put("password", "U5wrUbR4wO5A");
		//orientDBProps.put("spark", "true");
		orientDBProps.put("dbtable", "infinidat_Volumesbyarray");
		System.out.println("=============" + spark.logName());
		//Dataset<Row> tableDataset = spark.read().format("jdbc").options(orientDBProps).load();
		
		spark.sqlContext().load("jdbc", orientDBProps).registerTempTable("infinidat_Volumesbyarray");
		Dataset<Row> tableDataset = spark.sql("select ifnull(iboxarrayname,'') as `Array Name`, ifnull(iboxdevname,'') as `Device Name`, ifnull(iboxdevlunid,'') as `LUN ID`, ifnull(iboxdevsizegb,0) as `Size`, ifnull(iboxdevtype,'') as `Type`, ifnull(iboxdevserialnumber,'') as `Device Serial Number`, ifnull(iboxdevscsicanonicalname,'') as `Device SCSi Canonical Name`, ifnull(iboxdevclustered,'') as `Is Device Clustered`, ifnull(iboxdevcluster,'') as `Device Cluster`, ifnull(iboxdevmapped,'') as `Is Device Mapped`, ifnull(hostname,'') as `Host Name`, ifnull(hostwwn,'') as `Host WWN`, ifnull(usedbyvmfs,'') as `Usedby VMFS`, ifnull(usedbyvmrdm,'') as `Usedby VMRDM`, ifnull(iboxdevsource,'') as `Is Device Source`, ifnull(iboxdevtarget,'') as `Is Device Target` from ( select iboxarrayname, iboxdevname, iboxdevlunid, round(iboxdevsizegb,3) as iboxdevsizegb, iboxdevtype, iboxdevserialnumber, iboxdevscsicanonicalname, iboxdevclustered, iboxdevcluster, iboxdevmapped, hostname, usedbyvmfs, usedbyvmrdm, iboxdevesxhostpaths, iboxdevsource, iboxdevtarget, VolumesDevPersentedTo.wwn as hostwwn from ( select iboxarrayname, iboxdevname, iboxdevlunid, iboxdevsizegb, iboxdevtype, iboxdevserialnumber, iboxdevscsicanonicalname, iboxdevclustered, iboxdevcluster, iboxdevmapped, hostname, usedbyvmfs, usedbyvmrdm, iboxdevesxhostpaths, iboxdevsource, iboxdevtarget, 'infinidatVolumesbyarray_presentedto_VolumesDevPersentedTo' as VolumesDevPersentedTo from infinidat_Volumesbyarray where siteKey = 'ddccdf5f-674f-40e6-9d05-52ab36b10d0e' and iboxarrayname.append(logDate) in (select serverdate from (select iboxarrayname,iboxarrayname.append(MAXDate) as serverdate from ( select iboxarrayname,max(logDate) as MAXDate from infinidat_Volumesbyarray where siteKey ='ddccdf5f-674f-40e6-9d05-52ab36b10d0e' group by iboxarrayname) group by iboxarrayname) group by serverdate) group by iboxarrayname,iboxdevname,iboxdevserialnumber )group by iboxarrayname,iboxdevname,iboxdevserialnumber )group by iboxarrayname,iboxdevname,iboxdevserialnumber").toDF();
		System.out.println("--------Count----------- "+ tableDataset.count());*/
				
		/*Properties info = new Properties();
		info.put("user", "root");
		info.put("password", "U5wrUbR4wO5A");

		Connection conn = (OrientJdbcConnection) DriverManager.getConnection("jdbc:orient:remote:uatdb.zenfra.co/uatvpsdb", info);
		*/
		
		//System.out.println("-----------conn-----------" + conn.isClosed());
		
		
		 createDataframeForJsonData("", sparkSession);
		 getMigrationReport("", sparkSession);
	}
	
	
	public static void createDataframeForJsonData(String filePath, SparkSession sparkSession) {
		try {		
			filePath = "C:\\Senthil\\ddccdf5f-674f-40e6-9d05-52ab36b10d0e_discovery_Storage_3PAR_Host WWN.json";
			Dataset<Row> dataset = sparkSession.read().option("multiline", true).json(filePath); 	 
			File f = new File(filePath);
			String viewName = f.getName().replace(".json", "").replace("-", "").replace(" ", "");
			dataset.createOrReplaceGlobalTempView(viewName);
			System.out.println("-----------DF created------------");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static JSONObject getMigrationReport(String filePath, SparkSession sparkSession) throws IOException, ParseException {
		 JSONObject json = new JSONObject();
		 filePath = "C:\\Senthil\\ddccdf5f-674f-40e6-9d05-52ab36b10d0e_discovery_Storage_3PAR_Host WWN.json";
			
		 File f = new File(filePath);
		 String viewName = f.getName().replace(".json", "").replace("-", "").replace(" ", "");
		try {
			String datas =  sparkSession.sql("select * from global_temp."+viewName).toJSON().collectAsList().toString();
			 JSONParser parser = new JSONParser();
			 Object obj = parser.parse(datas);
			 JSONArray jsonArray = (JSONArray) obj;			
			 json = (JSONObject) jsonArray.get(0);
			
		} catch (Exception e) {							
				e.printStackTrace();					
			if(f.exists()) {								 
				createDataframeForJsonData(filePath, sparkSession);
			}
		}
		
		return json;
	}
	
}
