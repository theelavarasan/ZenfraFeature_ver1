package com.zenfra.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.model.ZenfraJSONObject;
import com.zenfra.dao.FavouriteDao_v2;
import com.zenfra.dao.ReportDao;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.service.DataframeService;

@Component
public class ReportService {
	
	@Autowired
	private ReportDao reportDao;	

	 @Autowired
     DataframeService dataframeService;
	
	 @Autowired
	 ChartService chartService;
	 
	 @Value("${zenfra.path}")
	 private String commonPath;
	 
	 @Autowired
	 private FavouriteDao_v2 favouriteDao_v2;
	 
	public String getReportHeader(String reportName, String deviceType, String reportBy, String siteKey, String reportList, String category) {
		JSONArray result = new JSONArray();
		if(reportName.equalsIgnoreCase("migrationautomation")) { //get headers from dataframe
			 
			 result = dataframeService.getReportHeaderForMigrationMethod(siteKey, deviceType);
			 
		} else {
			result = reportDao.getReportHeader(reportName, deviceType, reportBy);
		}
		
				 	
			//String report_label = reportList + " " + deviceType + " by "+  reportBy;	
			String report_label = getReportLabelName(category, reportList, deviceType, reportBy);
	        String report_name = reportList + "_" + deviceType + "_by_"+  reportBy;	 
	        if(reportName.equalsIgnoreCase("optimization")) {
	        	report_label = "Cloud Cost Comparison Report";
	        	report_name =  "optimization"+ "_" + deviceType;
	        }
	        
	        JSONObject resultObject = new JSONObject();
	        resultObject.put("headerInfo", result);
	        resultObject.put("report_label", report_label);
	        resultObject.put("report_name", report_name);	        
		return resultObject.toString();
	}

	
	private String getReportLabelName(String category, String reportList, String deviceType, String reportBy) {
		try {
			String label = "";
	        if((category.equalsIgnoreCase("Server") || category.equalsIgnoreCase("Project") || category.equalsIgnoreCase("Third Party Data")) && reportList.equalsIgnoreCase("Local")) {
	            label = "Server";
	        }
	        if(category.equalsIgnoreCase("Storage") && reportList.equalsIgnoreCase("Local")) {
	            label = "Storage";
	        }
	        if(category.equalsIgnoreCase("Switch") && reportList.equalsIgnoreCase("Local")) {
	            label = "Switch";
	        }
	        if((category.equalsIgnoreCase("Server") || category.equalsIgnoreCase("Project") || category.equalsIgnoreCase("Third Party Data")) &&
	                reportList.equalsIgnoreCase("End-To-End-Basic")) {
	            label = "Server - Switch - Storage Summary";
	        }
	        if(category.equalsIgnoreCase("Storage") && reportList.equalsIgnoreCase("End-To-End-Basic")) {
	            label = "Server - Switch - Storage Summary";
	        }
	        if(category.equalsIgnoreCase("Switch") && reportList.equalsIgnoreCase("End-To-End-Basic")) {
	            label = "Server - Switch - Storage Summary";
	        }
	       
	        if((category.equalsIgnoreCase("Server") || category.equalsIgnoreCase("Project") || category.equalsIgnoreCase("Third Party Data")) &&
	                reportList.equalsIgnoreCase("End-To-End-Detail")) {
	            label = "Server - Switch - Storage Detailed";
	        }
	        if(category.equalsIgnoreCase("Storage") && reportList.equalsIgnoreCase("End-To-End-Detail")) {
	            label = "Server - Switch - Storage Detailed";
	        }
	        if(category.equalsIgnoreCase("Switch") && reportList.equalsIgnoreCase("End-To-End-Detail")) {
	            label = "Server - Switch - Storage Detailed";
	        }
	        String reportLabel = label + " " + deviceType + " by " + reportBy;
	        return reportLabel;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}


	public JSONArray getChartLayout(String userId, String siteKey, String reportName) {
		JSONArray jSONArray = reportDao.getChartLayout(userId, siteKey, reportName);		
		return jSONArray;
	}

	public JSONObject getReportUserCustomData(String userId, String siteKey, String reportName) {
		// TODO Auto-generated method stub
		JSONObject reportDataObj =  reportDao.getReportUserCustomData(userId, siteKey, reportName);
		JSONArray chartData = chartService.getMigarationReport(siteKey, userId, reportName);
		reportDataObj.put("chart", chartData);
		return reportDataObj;
	}
	

	@SuppressWarnings("unchecked")
    public JSONObject getSubReportList(String deviceType, String reportName) throws IOException, ParseException, org.json.simple.parser.ParseException {
        System.out.println("!!!!! deviceType: " + deviceType);
        
        JSONParser parser = new JSONParser();
       
        Map<String, JSONArray> columnsMap = new LinkedHashMap<String, JSONArray>();
        JSONObject result = new JSONObject();
        
        
        try {
        	String linkDevices = ZKModel.getProperty(ZKConstants.CRDevice);          
            JSONArray devicesArray = (JSONArray) parser.parse(linkDevices);
            //System.out.println("!!!!! devicesArray: " + devicesArray);
            if (reportName.trim().equalsIgnoreCase("discovery")) {
                String linkColumns = ZKModel.getProperty(ZKConstants.CRCOLUMNNAMES);               
              
                
                JSONArray columnsArray = (JSONArray) parser.parse(linkColumns);

                for (int a = 0; a < devicesArray.size(); a++) {
                    JSONArray columnsNameArray = new JSONArray();
                    for (int i = 0; i < columnsArray.size(); i++) {
                        JSONObject jsonObject = (JSONObject) columnsArray.get(i);
                        if (jsonObject.containsKey(devicesArray.get(a).toString().toLowerCase())) {
                            columnsNameArray = (JSONArray) parser.parse(jsonObject.get(devicesArray.get(a).toString().toLowerCase()).toString());
                            columnsMap.put(devicesArray.get(a).toString().toLowerCase(), columnsNameArray);
                        }
                    }
                }

            } else if (reportName.trim().equalsIgnoreCase("compatibility")) {
                JSONArray columnsNameArray = new JSONArray();
                columnsNameArray.add("Host Name");
                for (int a = 0; a < devicesArray.size(); a++) {
                    columnsMap.put(devicesArray.get(a).toString().toLowerCase(), columnsNameArray);
                }

            } else if (reportName.trim().equalsIgnoreCase("project")) {
                JSONArray columnsNameArray = new JSONArray();
                columnsNameArray.add("Server Name");
                columnsNameArray.add("vCenter");
                columnsNameArray.add("VM");
                columnsNameArray.add("Host Name");
                for (int a = 0; a < devicesArray.size(); a++) {
                    columnsMap.put(devicesArray.get(a).toString().toLowerCase(), columnsNameArray);
                }

            }
          
            
            //System.out.println("!!!!! columnsMap: " + columnsMap);
            if (!columnsMap.isEmpty()) {
                Map<String, Properties> propMap = new TreeMap<String, Properties>();
                if (deviceType.equalsIgnoreCase("all")) {
                    for (int i = 0; i < devicesArray.size(); i++) {
                        String path = "/opt/config/" + devicesArray.get(i).toString().toLowerCase().replace("-", "") + "ServerClickReport.properties";
                        System.out.println("!!!!! path: " + path);
                        InputStream inputFile = null;

                        try {
                            //ClassLoader classLoader = getClass().getClassLoader();
                            //URL resources = classLoader.getResource(path);
                            File file = new File(path);
                            if (file != null) {
                                //System.out.println("!!!!! resources.getFile(): " + resources.getFile());
                                inputFile = new FileInputStream(file);
                                Properties prop = new Properties();
                                prop.load(inputFile);
                                propMap.put(devicesArray.get(i).toString().toLowerCase().replace("-", ""), prop);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                       
                    }
                } else {
                    String path = "/opt/config/" + deviceType.toLowerCase().replace("-", "") + "ServerClickReport.properties";
                    System.out.println("!!!!! path: " + path);
                    InputStream inputFile = null;

                    try {
                        //ClassLoader classLoader = getClass().getClassLoader();
                        //URL resources = classLoader.getResource(path);
                        File file = new File(path);
                        if (file != null) {
                            //System.out.println("!!!!! resources.getFile(): " + resources.getFile());
                            inputFile = new FileInputStream(file);
                            Properties prop = new Properties();
                            prop.load(inputFile);
                            propMap.put(deviceType.toLowerCase().replace("-", ""), prop);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                //System.out.println("!!!!! propMap: " + propMap);
                List<String> propKeys = new ArrayList<String>(propMap.keySet());
                //System.out.println("!!!!! propKeys: " + propKeys);

                //JSONArray jsonArray = new JSONArray();
                ZenfraJSONObject resultObject = new ZenfraJSONObject();

                JSONArray postDataColumnArray = new JSONArray();
                List<String> columnsKey = new ArrayList<String>(columnsMap.keySet());
                //System.out.println("!!!!! columnsKey: " + columnsKey);
                //System.out.println("!!!!! columnsNameArray: " + columnsNameArray);

                for (int i = 0; i < columnsKey.size(); i++) {
                    JSONArray columnsNameArray = columnsMap.get(columnsKey.get(i));
                    JSONObject tabInfoObject = new JSONObject();
                    for (int j = 0; j < columnsNameArray.size(); j++) {
                        ZenfraJSONObject tabArrayObject = new ZenfraJSONObject();
                        for (int k = 0; k < propKeys.size(); k++) {
                            Properties prop = propMap.get(propKeys.get(k));
                            List<Object> tabKeys = new ArrayList<Object>(prop.keySet());
                            JSONArray tabInnerArray = new JSONArray();

                            for (int l = 0; l < tabKeys.size(); l++) {
                                ZenfraJSONObject tabValueObject = new ZenfraJSONObject();
                                String key = tabKeys.get(l).toString();
                                String keyId = tabKeys.get(l).toString();
                                //System.out.println("!!!!! key: " + key);
                                String value = "";
                                String keyName = "";
                                String keyLabel = "";
                                String keyView = "";
                                String keyOrdered = "";
                                if (key.contains("$")) {
                                    String[] keyArray = key.split("\\$");
                                    //System.out.println("!!!!! keyArray size: " + keyArray.length);
                                    //System.out.println("!!!!! keyArray[0]: " + keyArray[0]);
                                    //System.out.println("!!!!! keyArray[1]: " + keyArray[1]);
                                    //System.out.println("!!!!! keyArray[2]: " + keyArray[2]);
                                    value = keyArray[0];
                                    keyName = keyArray[0].replace("~", "");
                                    keyLabel = value.replace("~", " ");
                                    keyView = keyArray[1];
                                    keyOrdered = keyArray[2];
                                } else {
                                    keyName = key.replace("~", "");
                                    keyLabel = key.replace("~", " ");
                                    keyView = "H";
                                    keyOrdered = "0";
                                }

    							/*System.out.println("!!!!! keyId: " + keyId);
    							System.out.println("!!!!! keyName: " + keyName);
    							System.out.println("!!!!! keyLabel: " + keyLabel);
    							System.out.println("!!!!! keyView: " + keyView);*/
                                tabValueObject.put("value", keyId);
                                tabValueObject.put("name", keyName);
                                tabValueObject.put("label", keyLabel);
                                tabValueObject.put("view", keyView);
                                tabValueObject.put("ordered", keyOrdered);
                                tabInnerArray.add(tabValueObject);
                            }
                            if (!tabInnerArray.isEmpty()) {
                                tabArrayObject.put(propKeys.get(k), tabInnerArray);
                            }

                        }
                        if (!tabArrayObject.isEmpty()) {
                            tabInfoObject.put("tabInfo", tabArrayObject);
                            tabInfoObject.put("tabInfo", tabArrayObject);
                            if (reportName.equalsIgnoreCase("project")) {
                                JSONArray skipValueArray = new JSONArray();
                                skipValueArray.add("Not Discovered");
                                tabInfoObject.put("skipValues", skipValueArray);
                            } else {
                                tabInfoObject.put("skipValues", new JSONArray());
                            }
                            tabInfoObject.put("title", "Detailed Report for Server (" + columnsNameArray.get(j) + ")");
                            
                            if(!postDataColumnArray.contains(columnsNameArray.get(j))) {
    							if(deviceType.equalsIgnoreCase("vmware")) {
    								postDataColumnArray.add("VM");
    								postDataColumnArray.add("vCenter");
    							} else if(deviceType.equalsIgnoreCase("vmwarehost")) {
    								postDataColumnArray.add("Server Name");
    								postDataColumnArray.add("vCenter");
    							} else {
    								postDataColumnArray.add(columnsNameArray.get(j));
    							}
    							
    						}
                            
                            resultObject.put(columnsNameArray.get(j), tabInfoObject);
                            //resultObject.put("skipValues", new JSONArray());
                            result.put("subLinkColumns", resultObject);
                        }
                    }

                }               
                
                result.put("postDataColumns", postDataColumnArray);
                result.put("deviceType", deviceType.toLowerCase().trim().replace("-", ""));
                JSONArray refferedDeviceType = new JSONArray();
                if (reportName.equalsIgnoreCase("compatibility")) {
                    refferedDeviceType.add("OS Type");
                } else if (reportName.equalsIgnoreCase("project")) {
                    refferedDeviceType.add("Server Type");
                }
                result.put("deviceTypeRefColumn", refferedDeviceType);


            }
		} catch (Exception e) {
			e.printStackTrace();
		}


        

        //System.out.println("!!!!! result: " + result);
        return result;
    }


	public JSONArray getCloudCostData(ServerSideGetRowsRequest request) {
		List<Map<String, Object>> cloudCostData = new ArrayList<>();
		JSONArray resultArray = new JSONArray();
		try {
			
			//getHeader 
			JSONParser jsonParser = new JSONParser();
			String reportName = request.getReportType();
			String deviceTypeHeder = "All";
			String reportBy = request.getReportType();
			String siteKey = request.getSiteKey();
			String reportList = request.getReportList();
		  JSONArray headers = reportDao.getReportHeader(reportName, deviceTypeHeder, reportBy);
		
		   List<String> columnHeaders = new ArrayList<>();
		   if(headers != null && headers.size() > 0) {
			   for(Object o : headers){
				    if ( o instanceof JSONObject ) {
				    	String col = (String) ((JSONObject) o).get("actualName");
				    	columnHeaders.add(col);
				    }
				}
		   }
		   
			
			
		   String deviceType = request.getDeviceType();
			String query = "select * from mview_aws_cost_report where site_key='"+request.getSiteKey()+"' and lower(source_type) in ('windows', 'linux', 'vmware')";
			if(deviceType != null && !deviceType.equalsIgnoreCase("All")) {				
					query = "select * from mview_aws_cost_report where site_key='"+request.getSiteKey()+"' and lower(source_type)='"+deviceType.toLowerCase()+"'";
			}
			
			cloudCostData = favouriteDao_v2.getJsonarray(query) ;
			if(cloudCostData != null && !cloudCostData.isEmpty()) {
				for(Map<String, Object> map : cloudCostData) {
					
					JSONObject json = new JSONObject();
					
					 Set<String> elementNamesFirstLevel = map.keySet();	
					 for (String elementName : elementNamesFirstLevel) {	
						 if(!elementName.equalsIgnoreCase("data_temp")) {							
							 if(map.get(elementName) instanceof  String) {
								 String value = (String) map.get(elementName);
								 if(value == null || value.trim().isEmpty()) {
						    		  value = "N/A";
						    	  }
								 json.put(elementName, value);
							 } else {
								 json.put(elementName, map.get(elementName));
							 }
						 }
					 }
					 
					
					Object object = null;
					JSONArray arrayObj = null;					
					PGobject pgObject = (PGobject) map.get("data_temp");				
					object=jsonParser.parse(pgObject.toString());
					arrayObj=(JSONArray) object;
					
					map.remove("data_temp");
					for (int i = 0; i < arrayObj.size();  i++)  {
				      JSONObject data = (JSONObject) arrayObj.get(i);
				      Set<String> elementNames = data.keySet();				      
				      for (String elementName : elementNames) {	
				    	  
				    	  if(columnHeaders.contains(elementName) && data.get(elementName) instanceof  String) {
				    		  String value = (String) data.get(elementName);
					    	  if(value == null || value.trim().isEmpty()) {
					    		  value = "N/A";
					    	  }
					    	  json.put(elementName, value);
					    	  /*if(elementName.equalsIgnoreCase("actual_os_type")) {				    		
					    		  json.put("actual_os_type_data", value);
					    	  } else {				    		
					    		  json.put(elementName, value);
					    	  }*/
				    	  } else if(columnHeaders.contains(elementName)){
				    		  json.put(elementName, data.get(elementName));
				    		 /* if(elementName.equalsIgnoreCase("actual_os_type")) {				    		
					    		  json.put("actual_os_type_data", data.get(elementName));
					    	  } else {				    		
					    		  json.put(elementName, data.get(elementName));
					    	  } */
				    	  }
				      }
				    }
					
					Set<String> jsonKeySset =  json.keySet();
					for(String key : columnHeaders) {
					    if (!jsonKeySset.contains(key)) {					    	
					    	json.put(key, "N/A") ;
					    }
					}
					
					resultArray.add(json);
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return resultArray;
	}
	
	
	

}
