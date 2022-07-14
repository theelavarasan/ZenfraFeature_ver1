package com.zenfra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataType;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import scala.collection.JavaConverters;

 

public class VMaxDiskSAN {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SparkSession sparkSession = SparkSession.builder().master("local").appName("simple").getOrCreate();
		JavaSparkContext sc = new JavaSparkContext(sparkSession.sparkContext());
		sc.setLogLevel("ERROR");
		sparkSession.sparkContext().setLogLevel("ERROR");
		try {
			String filePath = "C:\\Senthil\\Form16\\Vmax\\1bf6bb39-04b0-4394-8f90-ea87bae51a51_discovery_Storage_VMAX_Local_Disk-SAN.json";
			String opPath = "C:\\Senthil\\Form16\\Vmax\\1bf6bb39-04b0-4394-8f90-ea87bae51a51_discovery_Storage_VMAX_Local_Disk-SAN_new.json";
			
			JSONParser parser = new JSONParser();
			  ObjectMapper mapper = new ObjectMapper();			 
			  JSONObject jsonObject = mapper.readValue(new File(filePath), JSONObject.class);
			  List<Map<String, Object>> dataArray =  (List<Map<String, Object>>) jsonObject.get("data");
			  mapper.writeValue(new File(opPath), dataArray);
			  
			  
			  //System.out.println(dataArray);
			  
			    File f = new File(opPath);
				 Dataset<Row> datasetA = sparkSession.read().option("nullValue", "").json(f.getAbsolutePath()); 
				 String viewName = f.getName().split("_")[0].replaceAll("-", "")+"vmax_disk_san";
				 
				 System.out.println("------datasetA-------------- " + datasetA.count() + " : "  + viewName);
				// datasetA = datasetA.withColumn("Local FA Port", datasetA.col("Local FA Port").cast("String"));
				 datasetA.createOrReplaceGlobalTempView("vmax_disk_san_local");
				 
				/* Dataset<Row> datasetB = datasetA;
				 for (String column : datasetB.columns()) {
					 if(column.contains("Remote")) {
						 datasetB = datasetB.withColumnRenamed(column, column.replace("Remote", "RemoteB"));
					 } else {
						 datasetB = datasetB.withColumnRenamed(column, column.replace("Local", "Remote"));
					 }
						
					
					
				    }
				 
				 datasetB = datasetB.withColumn("Remote FA Port", datasetB.col("Remote FA Port").cast("String"));
				 
				 datasetB.createOrReplaceGlobalTempView("vmax_disk_san_remote");
				 System.out.println("-----******************************************************");
				 datasetA.printSchema();
				 datasetB.printSchema();
			 */
						/*
						 * Dataset<Row> result =
						 * datasetA.join(datasetB,datasetA.col("Local Device ID").equalTo(datasetB.
						 * col("Remote Device Name")).and(datasetA.col("Local Serial Number").equalTo(
						 * datasetB.col("Remote Target ID"))));
						 */
					  
					
					/*  Dataset<Row> result = sparkSession.sqlContext().
					  sql("select a.`Local Device ID` as `Local Device ID`, a.`Local Serial Number` as `Local Serial Number`, b.`Local Serial Number` as `Remote Serial Number`  from global_temp."+viewName +" a left join global_temp."+viewName+" b on a.`Local Device ID` = b.`Remote Device Name` and a.`Local Serial Number` = b.`Remote Target ID`").toDF();
					  
					  System.out.println("------result-------------- " + result.count());
					  
					  result.show();*/
					 
					  
					 
					 
				 
			  Dataset<Row> result = datasetA.sqlContext().sql("select " + 
				 		"a.`Local Device ID`, " + 
				 		"a.`Local Serial Number`, " + 
				 		"a.`Local Device Configuration`, " + 
				 		"a.`Local Device Capacity`, " + 
				 		"a.`Local Device WWN`, " + 
				 		"a.`Local Device Status`, " + 
				 		"a.`Local Host Access Mode`, " + 
				 		"a.`Local Clone Source Device (SRC)`, " + 
				 		"a.`Local Clone Target Device (TGT)`, " + 
				 		"a.`Local BCV Device Name`, " + 
				 		"a.`Local BCV Device Status`, " + 
				 		"a.`Local BCV State of Pair`, " + 
				 		"a.`Local Storage Group`, " + 
				 		"a.`Local Masking View`, " + 
				 		"a.`Local Initiator Group`, " + 
				 		"a.`Local Initiator Name`, " + 
				 		"a.`Local Initiator WWN`, " + 
				 		"a.`Local Possible Server Name`, " + 
				 		"a.`Local FA Port`," + 
				 		"a.`Local FA Port WWN`,  " + 
				 		"b.`Local Device ID` as `Remote Device ID`," + 
				 		"b.`Local Serial Number` as `Remote Serial Number`," + 
				 		"b.`Local Device Configuration` as `Remote Device Configuration`," + 
				 		"b.`Local Device Capacity` as `Remote Device Capacity`," + 
				 		"b.`Local Device WWN` as `Remote Device WWN`," + 
				 		"b.`Local Device Status` as `Remote Device Status`," + 
				 		"b.`Local Host Access Mode` as `Remote Host Access Mode`," + 
				 		"b.`Local Clone Source Device (SRC)` as `Remote Clone Source Device (SRC)`," + 
				 		"b.`Local Clone Target Device (TGT)` as `Remote Clone Target Device (TGT)`," + 
				 		"b.`Local BCV Device Name` as `Remote BCV Device Name`," + 
				 		"b.`Local BCV Device Status` as `Remote BCV Device Status`," + 
				 		"b.`Local BCV State of Pair` as `Remote BCV State of Pair`," + 
				 		"b.`Local Storage Group` as `Remote Storage Group`," + 
				 		"b.`Local Masking View` as `Remote Masking View`," + 
				 		"b.`Local Initiator Group` as `Remote Initiator Group`," + 
				 		"b.`Local Initiator Name` as `Remote Initiator Name`," + 
				 		"b.`Local Initiator WWN` as `Remote Initiator WWN`," + 
				 		"b.`Local Possible Server Name` as `Remote Possible Server Name`," + 
				 		"b.`Local FA Port` as `Remote FA Port`," + 
				 		"b.`Local FA Port WWN` as `Remote FA Port WWN` " + 
				 	     "from global_temp.vmax_disk_san_local a  " + 
				 		"left join global_temp.vmax_disk_san_local b on a.`Remote Device Name` = b.`Local Device ID` and a.`Remote Target ID` = b.`Local Serial Number`");
				 
				 
				 
				 
			/*  File file = new File(filePath);
				String dataWritePath = file.getParentFile().getAbsoluteFile()+File.separator+"Disk_san"+File.separator;
				result.coalesce(1).write().option("ignoreNullFields", false)
				.format("org.apache.spark.sql.json")				
				.mode(SaveMode.Overwrite).save(dataWritePath);
				
				//find json file from folder
				String resultDataFilePath = findResultDataPath(dataWritePath);
				System.out.println("------resultDataFilePath---------- " + resultDataFilePath);
				if(resultDataFilePath != null) {
					JSONArray vmaxDiskSanObj = new JSONArray();
					 BufferedReader br = null;
				        JSONParser parser = new JSONParser();
				        try {
				        	   String sCurrentLine;

				               br = new BufferedReader(new FileReader(resultDataFilePath));

				               while ((sCurrentLine = br.readLine()) != null) {
				            	   try {
				            		JSONObject row =    (JSONObject) parser.parse(sCurrentLine);
				            		vmaxDiskSanObj.put(row);
								} catch (Exception e) {
									e.printStackTrace();
								}
				               }
				               

						} catch (Exception e) {
							// TODO: handle exception
						}
				    	System.out.println("------vmaxDiskSanObj---------- " );
					jsonObject.put("data", vmaxDiskSanObj);	
				}
					
			 */
				
			  
			  jsonObject.put("data", parser.parse(result.toJSON().collectAsList().toString()));	
				System.out.println("------vmaxDiskSanObj---------- " );
		        try (JsonGenerator jGenerator =
		                     mapper.getFactory().createGenerator(
		                             new File(filePath.replaceAll(".json", "result_nnn.json"))
		                             , JsonEncoding.UTF8)) {
		            
		        	jGenerator.writeObject(jsonObject);
		                                       // }

		        } catch (Exception e) {
		            e.printStackTrace();
		        } 
				
				
				
				 /* FileWriter fw = new FileWriter(filePath);
				  BufferedWriter bw = new BufferedWriter(fw);
				  bw.write(jsonObject.toString()); bw.close();
				  bw.close();
				  */
				  
				  
				  
				
				 
				 
				
				 
				 
				/*
				 * result.coalesce(1).write().mode("overwrite").json(opPath.replaceAll(".json",
				 * "result_.json")); result.coalesce(1).write().option("nullValue",
				 * "").mode(SaveMode.Overwrite) .json(opPath.replaceAll(".json",
				 * "result_.json"));
				 */
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
			
			

	}
	
	private static String findResultDataPath(String dataWritePath) {
		File dir = new File(dataWritePath);
		File filesList[] = dir.listFiles();
		for(File file : filesList) {
	       if(file.getAbsolutePath().endsWith(".json")) {
	    	   return file.getAbsolutePath();
	       }
	      }
		return null;
	}

}
