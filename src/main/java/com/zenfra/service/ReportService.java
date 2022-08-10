package com.zenfra.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.zenfra.dao.FavouriteDao_v2;
import com.zenfra.dao.ReportDao;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.model.ZenfraJSONObject;
import com.zenfra.utils.CommonUtils;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class ReportService {

	@Autowired
	private ReportDao reportDao;

	@Autowired
	DataframeService dataframeService;

	@Autowired
	ChartService chartService;
	
	@Autowired
	JdbcTemplate jdbc;

	private String commonPath;

	@PostConstruct
	public void init() {
		commonPath = ZKModel.getProperty(ZKConstants.DATAFRAME_PATH);
	}

	@Autowired
	private FavouriteDao_v2 favouriteDao_v2;

	public String getReportHeader(ServerSideGetRowsRequest request,String reportName, String deviceType, String reportBy, String siteKey,
			String reportList, String category, String actualDeviceType, String reportCategory, String analyticsType, String userId, boolean isHeader) {
		JSONArray result = new JSONArray();
		JSONObject groupResult = new JSONObject();
		if (reportName.equalsIgnoreCase("migrationautomation")) { // get headers from dataframe

			result = dataframeService.getReportHeaderForMigrationMethod(siteKey, deviceType);

		} else {
			
			if(reportBy.equalsIgnoreCase("Privileged Access")) {
				result = reportDao.getPrivillegeReportHeader(reportName, actualDeviceType, reportBy, siteKey, userId);
				groupResult = reportDao.getReportGroup(reportName, deviceType, reportBy, siteKey, userId);
			} else if((reportBy.equalsIgnoreCase("User") || reportBy.equalsIgnoreCase("Server") || reportBy.equalsIgnoreCase("Sudoers")) && reportName.equalsIgnoreCase("End-To-End-Basic")) {
				result = reportDao.getPrivillegeReportHeader(reportName, actualDeviceType, reportBy, siteKey, userId);
				groupResult = reportDao.getReportGroup(reportName, deviceType, reportBy, siteKey, userId);
			} else {
				result = reportDao.getReportHeader(reportName, deviceType, reportBy, siteKey, userId);
			}
		}
		
		System.out.println("!!!!! reportHeade: " + result.size());
		// for some reports we need to get column headers from report data
		if(result.isEmpty()) {
			result = dataframeService.getReportHeaderFromData(siteKey, category, reportList, deviceType, reportBy, analyticsType);
			if(result.isEmpty()) {
				dataframeService.getReportDataFromDF(request, isHeader);
				result = dataframeService.getReportHeaderFromData(siteKey, category, reportList, deviceType, reportBy, analyticsType);
			}
		}

		// String report_label = reportList + " " + deviceType + " by "+ reportBy;
		String report_label = getReportLabelName(category, reportList, deviceType, reportBy);
		String report_name = reportList + "_" + deviceType + "_by_" + reportBy;
		if (reportName.equalsIgnoreCase("optimization")) {
			report_label = "Cloud Cost Comparison Report";
			report_name = "optimization" + "_" + reportCategory + "_" + actualDeviceType + "_" + reportBy;
		}
		

		JSONObject resultObject = new JSONObject();
		resultObject.put("headerInfo", result);
		resultObject.put("columnGroupInfo", groupResult);
		if(category.equalsIgnoreCase("user") && reportList.equalsIgnoreCase("End-To-End-Basic") && reportBy.equalsIgnoreCase("user") && deviceType.equalsIgnoreCase("tanium")) {
			resultObject.put("report_label", "User Summary");
		} else if(category.equalsIgnoreCase("user") && reportList.equalsIgnoreCase("End-To-End-Basic") && reportBy.equalsIgnoreCase("server") && deviceType.equalsIgnoreCase("tanium")) {
			resultObject.put("report_label", "Server Summary");
		} else if(category.equalsIgnoreCase("user") && reportList.equalsIgnoreCase("End-To-End-Basic") && reportBy.equalsIgnoreCase("Sudoers") && deviceType.equalsIgnoreCase("tanium")) {
			resultObject.put("report_label", "Sudoers Summary");
		} else {
			resultObject.put("report_label", report_label);
		}
		resultObject.put("report_name", report_name);
		if(deviceType.equalsIgnoreCase("ibmsvc") || deviceType.equalsIgnoreCase("vmax") || (deviceType.equalsIgnoreCase("Tanium") && reportBy.equalsIgnoreCase("User"))) {
			resultObject.put("subLinkDetails", getDSRConfigData(request, deviceType));
		}
		

		JSONObject metrics = dataframeService.getUnitConvertDetails(reportName, deviceType);
		resultObject.put("unit_conv_details", metrics);

		return resultObject.toString();
	}

	

	private String getReportLabelName(String category, String reportList, String deviceType, String reportBy) {
		try {
			String label = "";
			if ((category.equalsIgnoreCase("Server") || category.equalsIgnoreCase("Project")
					|| category.equalsIgnoreCase("Third Party Data")) && reportList.equalsIgnoreCase("Local")) {
				label = "Server";
			}
			if (category.equalsIgnoreCase("Storage") && reportList.equalsIgnoreCase("Local")) {
				label = "Storage";
			}
			if (category.equalsIgnoreCase("Switch") && reportList.equalsIgnoreCase("Local")) {
				label = "Switch";
			}
			if ((category.equalsIgnoreCase("Server") || category.equalsIgnoreCase("Project")
					|| category.equalsIgnoreCase("Third Party Data"))
					&& reportList.equalsIgnoreCase("End-To-End-Basic")) {
				label = "Server - Switch - Storage Summary";
			}
			if (category.equalsIgnoreCase("Storage") && reportList.equalsIgnoreCase("End-To-End-Basic")) {
				label = "Server - Switch - Storage Summary";
			}
			if (category.equalsIgnoreCase("Switch") && reportList.equalsIgnoreCase("End-To-End-Basic")) {
				label = "Server - Switch - Storage Summary";
			}

			if ((category.equalsIgnoreCase("Server") || category.equalsIgnoreCase("Project")
					|| category.equalsIgnoreCase("Third Party Data"))
					&& reportList.equalsIgnoreCase("End-To-End-Detail")) {
				label = "Server - Switch - Storage Detailed";
			}
			if (category.equalsIgnoreCase("Storage") && reportList.equalsIgnoreCase("End-To-End-Detail")) {
				label = "Server - Switch - Storage Detailed";
			}
			if (category.equalsIgnoreCase("Switch") && reportList.equalsIgnoreCase("End-To-End-Detail")) {
				label = "Server - Switch - Storage Detailed";
			}
			String reportLabel = label + " " + deviceType + " by " + reportBy;
			return reportLabel;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return "";
	}

	public JSONArray getChartLayout(String userId, String siteKey, String reportName) {
		JSONArray jSONArray = reportDao.getChartLayout(userId, siteKey, reportName);
		return jSONArray;
	}

	public JSONObject getReportUserCustomData(String userId, String siteKey, String reportName) {
		// TODO Auto-generated method stub
		JSONObject reportDataObj = reportDao.getReportUserCustomData(userId, siteKey, reportName);
		JSONArray chartData = chartService.getMigarationReport(siteKey, userId, reportName);
		reportDataObj.put("chart", chartData);
		// JSONObject unitMetrics = dataframeService.getUnitConvertDetails(reportName,
		// "");
		// reportDataObj.put("unit_conv_details", unitMetrics);
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
            columnsNameArray.add("Host_Host Name");
            for (int a = 0; a < devicesArray.size(); a++) {
                columnsMap.put(devicesArray.get(a).toString().toLowerCase(), columnsNameArray);
            }

        } else if (reportName.trim().equalsIgnoreCase("project")) {
            JSONArray columnsNameArray = new JSONArray();
            columnsNameArray.add("Server Name");
            columnsNameArray.add("vCenter");
            columnsNameArray.add("VM");
            columnsNameArray.add("Host Name");
            columnsNameArray.add("Host_Host Name");
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

			// getHeader
			JSONParser jsonParser = new JSONParser();
			String reportName = request.getReportType();
			String deviceTypeHeder = "All";
			String reportBy = request.getReportType();
			JSONArray headers = reportDao.getReportHeader(reportName, deviceTypeHeder, reportBy, request.getSiteKey(), request.getUserId());

			List<String> columnHeaders = new ArrayList<>();
			if (headers != null && headers.size() > 0) {
				for (Object o : headers) {
					if (o instanceof JSONObject) {
						String col = (String) ((JSONObject) o).get("actualName");
						columnHeaders.add(col);
					}
				}
			}

			List<String> taskListServers = new ArrayList<>();
			if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
				List<Map<String, Object>> resultMap = favouriteDao_v2.getJsonarray(
						"select server_name from tasklist where project_id='" + request.getProjectId() + "'");
				if (resultMap != null && !resultMap.isEmpty()) {
					for (Map<String, Object> map : resultMap) {
						taskListServers.add((String) map.get("server_name"));
					}
				}
			}

			String deviceType = request.getDeviceType();
			String query = "select * from mview_aws_cost_report where site_key='" + request.getSiteKey()
					+ "' and lower(source_type) in ('windows', 'linux', 'vmware')";
			if (deviceType != null && !deviceType.equalsIgnoreCase("All")) {
				query = "select * from mview_aws_cost_report where site_key='" + request.getSiteKey()
						+ "' and lower(source_type)='" + deviceType.toLowerCase() + "'";
			}

			if (request.getProjectId() != null && !request.getProjectId().isEmpty() && !taskListServers.isEmpty()) {
				String serverNames = String.join(",",
						taskListServers.stream().map(name -> ("'" + name + "'")).collect(Collectors.toList()));
				query = "select * from mview_aws_cost_report where site_key='" + request.getSiteKey()
						+ "' and server_name in (" + serverNames + ")";
			}

			cloudCostData = favouriteDao_v2.getJsonarray(query);
			if (cloudCostData != null && !cloudCostData.isEmpty()) {
				for (Map<String, Object> map : cloudCostData) {

					JSONObject json = new JSONObject();

					Set<String> elementNamesFirstLevel = map.keySet();
					for (String elementName : elementNamesFirstLevel) {
						if (!elementName.equalsIgnoreCase("data_temp")) {
							if (map.get(elementName) instanceof String) {
								String value = (String) map.get(elementName);
								if (value == null || value.trim().isEmpty()) {
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
					object = jsonParser.parse(pgObject.toString());
					arrayObj = (JSONArray) object;

					map.remove("data_temp");
					for (int i = 0; i < arrayObj.size(); i++) {
						JSONObject data = (JSONObject) arrayObj.get(i);
						Set<String> elementNames = data.keySet();
						for (String elementName : elementNames) {

							if (columnHeaders.contains(elementName) && data.get(elementName) instanceof String) {
								String value = (String) data.get(elementName);
								if (value == null || value.trim().isEmpty()) {
									value = "N/A";
								}
								json.put(elementName, value);

							} else if (columnHeaders.contains(elementName)) {
								json.put(elementName, data.get(elementName));
							}
						}
					}

					Set<String> jsonKeySset = json.keySet();
					for (String key : columnHeaders) {
						if (!jsonKeySset.contains(key)) {
							json.put(key, "N/A");
						}
					}

					resultArray.add(json);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return resultArray;
	}

	private void refreshViews(String view) {
		try {
			Date date = new Date();
			reportDao.executeNativeQuery("REFRESH MATERIALIZED VIEW " + view + " WITH DATA");
			Date date2 = new Date();
			System.out.println("----------refresh time for view :: "+view  + " : " + (date2.getTime() - date.getTime()));
		} catch (Exception e) {
			e.printStackTrace();
			/*
			 * StringWriter errors = new StringWriter(); e.printStackTrace(new
			 * PrintWriter(errors)); String ex = errors.toString();
			 * ExceptionHandlerMail.errorTriggerMail(ex);
			 */
		}

	}

	public void refreshCloudCostViews() {
		refreshViews("mview_local_discovery_data");
		refreshViews("mview_localdisc_aws");
		refreshViews("mview_localdisc_azure");
		refreshViews("mview_localdisc_google");
		refreshViews("mview_localdisc_all_cloud");
		refreshViews("mview_ccr_data");

	}

	public JSONObject getReportUserCutomBySiteKey(String siteKey, String userId) {
		JSONObject result = new JSONObject();
		try {
			result = reportDao.getReportUserCustomDataBySiteKey(siteKey, userId);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return result;
	}

	public void runQuery() {
		
		try  
		{  
		File file=new File("C:\\Users\\Aravind\\Downloads\\ccr_insert_query.sql");    //creates a new file instance  
		FileReader fr=new FileReader(file);   //reads the file  
		BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream  
		StringBuffer sb=new StringBuffer();    //constructs a string buffer with no characters  
		String line;  
		int i = 0;
		while((line=br.readLine())!=null)  
		{  
		if(line.contains("cloud_cost_report_data")) {
			reportDao.executeNativeQuery(line);
			System.out.println(i + " <<--->> " + line);  
			
			i++;
		}
		}  
		fr.close();    //closes the stream and release the resources  
		System.out.println("Contents of File: ");  
		 
		}  
		catch(IOException e)  
		{  
		e.printStackTrace();  
		}  
		
		
		
	}
	
	public JSONObject getDSRLinks() {
		
		JSONObject resultObject = new JSONObject();
		JSONParser parser = new JSONParser();
		String query = "select key_value from zen_config where key_name = 'dsr_link'";
		try {
			List<Map<String,Object>> valueArray = jdbc.queryForList(query);
			for(Map<String, Object> list : valueArray) {
				resultObject = (JSONObject) parser.parse(list.get("key_value").toString());
			}	
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultObject;
	}
	
	private JSONObject getDSRConfigData(ServerSideGetRowsRequest request, String deviceType) {
		
		JSONObject resultObject = new JSONObject();
		
		try {
			String protocol = ZKModel.getProperty(ZKConstants.APP_SERVER_PROTOCOL);
	    	String appServerIp = ZKModel.getProperty(ZKConstants.APP_SERVER_IP);
	    	String port = ZKModel.getProperty(ZKConstants.APP_SERVER_PORT);
	        String uri = protocol + "://" + appServerIp + ":" + port + "/ZenfraV2/auth/dsr-config";
	        uri = uri+"?category="+request.getCategory()
	    	+"&reportList="+request.getReportList()
	    	+"&reportBy="+request.getReportBy()
	    	+"&reportName="+request.getReportType()
	    	+"&deviceType="+deviceType;
	        uri = CommonUtils.checkPortNumberForWildCardCertificate(uri);
	        RestTemplate restTemplate = new RestTemplate();
	        resultObject = restTemplate.getForObject(uri, JSONObject.class);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultObject;
		
	}

	 


}