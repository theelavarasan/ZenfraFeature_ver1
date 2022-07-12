package com.zenfra.service;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@Component
public class ValidationRuleService {

	@Autowired
	SparkSession sparkSession;

	private String commonPath;

	@PostConstruct
	public void init() {
		commonPath = ZKModel.getProperty(ZKConstants.DATAFRAME_PATH);
	}

	@Autowired
	DataframeService dataframeService;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	CommonFunctions commonFunctions;

	private ObjectMapper mapper = new ObjectMapper();
	private JSONParser parser = new JSONParser();

	public Map<String, List<Object>> getDiscoveryReportValues(String siteKey, String reportBy, String columnName,
			String category, String deviceType, String reportList, String analyticsType) {

		Dataset<Row> dataset = sparkSession.emptyDataFrame();
		Map<String, List<Object>> resutData = new HashMap<>();

		List<String> serverList = commonFunctions.convertToArrayList(ZKModel.getProperty(ZKConstants.SERVER_LIST), ",");
		List<String> storageList = commonFunctions.convertToArrayList(ZKModel.getProperty(ZKConstants.STORAGE_LIST),
				",");
		List<String> switchList = commonFunctions.convertToArrayList(ZKModel.getProperty(ZKConstants.SWITCH_LIST), ",");

		if (serverList.contains(deviceType.toLowerCase())) {
			category = "Server";
		} else if (storageList.contains(deviceType.toLowerCase())) {
			category = "Storage";
		} else if (switchList.contains(deviceType.toLowerCase())) {
			category = "Switch";
		}

		System.out.println("------category---------" + category);

		/*if (reportBy != null && ((reportBy.trim().equalsIgnoreCase("Server") && category.equalsIgnoreCase("Server"))
				|| ((reportBy.trim().equalsIgnoreCase("VM") || reportBy.trim().equalsIgnoreCase("Host"))
						&& deviceType.equalsIgnoreCase("Nutanix"))
				|| ((reportBy.trim().equalsIgnoreCase("VM") || reportBy.trim().equalsIgnoreCase("Host"))
						&& deviceType.equalsIgnoreCase("Hyper-V"))
				|| ((reportBy.trim().equalsIgnoreCase("VM") || reportBy.trim().equalsIgnoreCase("Host"))
						&& deviceType.equalsIgnoreCase("vmware")))) {*/
			try {
				deviceType = deviceType.toLowerCase();
				if (deviceType != null && !deviceType.trim().isEmpty() && deviceType.contains("hyper")) {
					deviceType = deviceType + "-" + reportBy.toLowerCase();
				} else if (deviceType != null && !deviceType.trim().isEmpty()
						&& (deviceType.contains("vmware") && reportBy.toLowerCase().contains("host"))) {
					deviceType = deviceType + "-" + reportBy.toLowerCase();
				} else if (deviceType != null && !deviceType.trim().isEmpty()
						&& (deviceType.contains("nutanix") && reportBy.toLowerCase().contains("host"))) {
					deviceType = deviceType + "-" + reportBy.toLowerCase();
				} else if (deviceType != null && !deviceType.trim().isEmpty()
						&& (deviceType.contains("nutanix") && reportBy.toLowerCase().equalsIgnoreCase("vm"))) {
					deviceType = deviceType + "-" + "guest";
				}

				/*String viewName = siteKey + "_" + deviceType;
				viewName = viewName.replaceAll("-", "").replaceAll("\\s+", "");*/
				
				String viewNameWithHypen = siteKey + "_" + analyticsType.toLowerCase() + "_"
						+ category + "_" + deviceType + "_" + reportList + "_" + reportBy;
				String viewName = viewNameWithHypen.replaceAll("-", "").replaceAll("\\s+", "");
				dataset = sparkSession.sql("select * from global_temp." + viewName);

				String dataArray = dataset.toJSON().collectAsList().toString();

				try {
					JSONArray dataObj = (JSONArray) parser.parse(dataArray);

					for (int i = 0; i < dataObj.size(); i++) {
						JSONObject data = (JSONObject) dataObj.get(i);
						Set<String> keys = data.keySet();
						for (String key : keys) {
							if (resutData.containsKey(key.trim())) {
								List<Object> values = resutData.get(key.trim());
								if (!values.contains(data.get(key))) {
									values.add(data.get(key));
									values.removeAll(Arrays.asList("", null));
									resutData.put(key, values);
								}
							} else {

								List<Object> values = new ArrayList<>();
								values.add(data.get(key));
								values.removeAll(Arrays.asList("", null));
								resutData.put(key, values);
							}
						}

					}
				} catch (Exception e) {
					e.printStackTrace();
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					String ex = errors.toString();
					ExceptionHandlerMail.errorTriggerMail(ex);
				}

			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}
		/*} else {
			String actualDfFolderPath = null;
			String actualDfFilePath = null;
			String dataframePath = commonPath + "Dataframe" + File.separator + siteKey + File.separator;

			File dir = new File(dataframePath);

			for (File file : dir.listFiles()) {

				if (file.isDirectory() && file.getName().equalsIgnoreCase(deviceType)) {
					actualDfFolderPath = file.getAbsolutePath();
					break;
				}
			}

			if (actualDfFolderPath != null) {
				File d = new File(actualDfFolderPath);
				for (File file : d.listFiles()) {
					if (file.isFile() && file.getName().toLowerCase().contains(category.toLowerCase())
							&& (file.getName().toLowerCase().contains(reportBy.toLowerCase() + ".json") || file.getName().toLowerCase().contains(reportBy.toLowerCase().replaceAll("\\s+", "") + ".json"))
							&& (file.getName().toLowerCase().contains(reportList.toLowerCase()) || file.getName().toLowerCase().contains(reportList.toLowerCase().replaceAll("\\s+", "")))) { // &&
																									// file.getName().toLowerCase().contains(category.toLowerCase())
						actualDfFilePath = file.getAbsolutePath();
						break;
					}
				}
				System.out.println("-------actualDfFilePath------------ " + actualDfFilePath);
				if (actualDfFilePath != null) {
					File f = new File(actualDfFilePath);
					String viewName = f.getName().replace(".json", "").replaceAll("-", "").replaceAll("\\s+", "");
					try {
						dataset = sparkSession.sql("select * from global_temp." + viewName);
					} catch (Exception e) {
						dataframeService.createDataframeForJsonData(f.getAbsolutePath());
					}

					dataset = sparkSession.sql("select data from global_temp." + viewName);

				}
			}

			
			String dataArray = dataset.toJSON().collectAsList().toString();

			try {
				if(dataArray != null && !dataArray.isEmpty()) {
					JSONArray dataObj = mapper.readValue(dataArray, JSONArray.class);

					for (int i = 0; i < dataObj.size(); i++) {
						LinkedHashMap<String, Object> jsonObject = (LinkedHashMap) dataObj.get(i);
						List<Object> dataAry = (List<Object>) jsonObject.get("data");

						for (int j = 0; j < dataAry.size(); j++) {
							LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) dataAry.get(j);
							Set<String> keys = data.keySet();
							for (String key : keys) {

								if (resutData.containsKey(key.trim())) {

									List<Object> values = resutData.get(key.trim());
									if (!values.contains(data.get(key))) {
										values.add(data.get(key));
										values.removeAll(Arrays.asList("", null));
										resutData.put(key, values);
									}
								} else {

									List<Object> values = new ArrayList<>();
									values.add(data.get(key));
									values.removeAll(Arrays.asList("", null));
									resutData.put(key, values);
								}
							}

						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			}

		}*/

		// dirPath+siteKey+"_"+reportType+"_"+category+"_"+providers+"_"+reportList+"_"+reportBy+".json";

		return resutData;
	}

	public JSONArray getVR_Compatibility(String siteKey, String columnName, String category, String deviceType,
			String model) throws ParseException {

		System.out.println("!!!!! siteKey: " + siteKey);
		System.out.println("!!!!! columnName: " + columnName);
		System.out.println("!!!!! category: " + category);
		System.out.println("!!!!! deviceType: " + deviceType);
		System.out.println("!!!!! model: " + model);
		boolean isServer = false;
		boolean isStorage = false;
		boolean isSwitch = false;

		JSONArray resultArray = new JSONArray();

		JSONArray serverArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SERVER_LIST));
		JSONArray storageArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.STORAGE_LIST));
		JSONArray switchArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SWITCH_LIST));

		if (serverArray.contains(deviceType.toLowerCase())) {
			isServer = true;
		}
		if (storageArray.contains(deviceType.toLowerCase())) {
			isStorage = true;
		}
		if (switchArray.contains(deviceType.toLowerCase())) {
			isSwitch = true;
		}

		if (deviceType != null && !deviceType.isEmpty() && deviceType.equalsIgnoreCase("vmware")) {
			deviceType = "vmware-host";
		}

		if (deviceType != null && !deviceType.isEmpty() && deviceType.equalsIgnoreCase("7-mode")) {
			deviceType = "7-mode server";
		}
		if (deviceType != null && !deviceType.isEmpty() && deviceType.equalsIgnoreCase("c-mode")) {
			deviceType = "c-mode server";
		}
		try {
			JSONArray defaultArray = getDefaultPIData(deviceType, model);
			String query = "select keys, json_agg(column_values) as column_values from ( \r\n"
					+ "select distinct keys, column_values from ( \r\n"
					+ "select keys, data ->> keys as column_values from (\r\n"
					+ "select data, json_object_keys(data) as keys from (\r\n"
					+ "select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( select site_key, source_type, source_id, source_id as server_name, coalesce(metricsdate, 'Data Not Available') as metrics_date, \r\n"
					+ "coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(pidata, '" + defaultArray
					+ "') as pidata, coalesce(vm_name, '') as vm_name, \r\n" + "coalesce(vcenter, '') as vcenter,  \r\n"
					+ "coalesce(os_version, 'Data Not Available') as os_version, row_number() over(partition by source_id) as row_num  from (  \r\n"
					+ "select site_key, source_type, coalesce(server_name, source_id) as source_id, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter, \r\n"
					+ "(case when lower(source_type) = 'vmware' then esx_version else os_version end) as os_version from (  \r\n"
					+ "select site_key, source_type, source_id, json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,  \r\n"
					+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name, \r\n"
					+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter, \r\n"
					+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version, \r\n"
					+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version, \r\n"
					+ "row_number() over(partition by site_key, server_name order by log_date desc)  as row_num  \r\n"
					+ "from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('"
					+ deviceType + "')  \r\n" + ") a where row_num = 1  \r\n" + ") ld  \r\n"
					+ "LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data  \r\n"
					+ "where sitekey = '" + siteKey + "' and lower(sourcetype) = lower('" + deviceType
					+ "') and lower(destinationtype) = lower('" + model + "') \r\n"
					+ "and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('"
					+ model + "') and lower(device) = lower('" + deviceType + "'))) cd on  \r\n"
					+ "cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id) order by source_id ) b where row_num = 1 \r\n"
					+ ") e ) f where keys = '" + columnName + "' order by keys ) g ) h group by keys";

			if (isServer) {
				// JSONArray defaultArray = getDefaultPIData(deviceType, model);
				query = "select keys, json_agg(column_values) as column_values from ( \r\n"
						+ "select distinct keys, column_values from ( \r\n"
						+ "select keys, data ->> keys as column_values from (\r\n"
						+ "select data, json_object_keys(data) as keys from (\r\n"
						+ "select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( select site_key, source_type, source_id, source_id as server_name, coalesce(metricsdate, 'Data Not Available') as metrics_date, \r\n"
						+ "coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(pidata, '"
						+ defaultArray + "') as pidata, coalesce(vm_name, '') as vm_name, \r\n"
						+ "coalesce(vcenter, '') as vcenter,  \r\n"
						+ "coalesce(os_version, 'Data Not Available') as os_version, row_number() over(partition by source_id) as row_num  from (  \r\n"
						+ "select site_key, source_type, coalesce(server_name, source_id) as source_id, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter, \r\n"
						+ "(case when lower(source_type) = 'vmware' then esx_version else os_version end) as os_version from (  \r\n"
						+ "select site_key, source_type, source_id, json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,  \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name, \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter, \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version, \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version, \r\n"
						+ "row_number() over(partition by site_key, server_name order by log_date desc)  as row_num  \r\n"
						+ "from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('"
						+ deviceType + "')  \r\n" + ") a where row_num = 1  \r\n" + ") ld  \r\n"
						+ "LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data  \r\n"
						+ "where sitekey = '" + siteKey + "' and lower(sourcetype) = lower('" + deviceType
						+ "') and lower(destinationtype) = lower('" + model + "') \r\n"
						+ "and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('"
						+ model + "') and lower(device) = lower('" + deviceType + "'))) cd on  \r\n"
						+ "cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id) order by source_id ) b where row_num = 1 \r\n"
						+ ") e ) f where keys = '" + columnName + "' order by keys ) g ) h group by keys";
			}

			if (isStorage) {

				defaultArray = getDefaultPIData("project", model);
				query = "select keys, json_agg(column_values) as column_values from ( \r\n"
						+ "select distinct keys, column_values from ( \r\n"
						+ "select keys, data ->> keys as column_values from ( \r\n"
						+ "select data, json_object_keys(data) as keys from ( \r\n"
						+ "select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( \r\n"
						+ "select site_key, coalesce(source_type, 'Not Discovered') as source_type, source_id, coalesce(metricsdate, 'Data Not Available') as metricsdate,\r\n"
						+ "coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter,\r\n"
						+ "coalesce(pidata, '" + defaultArray
						+ "') as pidata, coalesce(os_version, 'Data Not Available') as os_version  from (\r\n"
						+ "select site_key, source_type, source_id, vm_name, vcenter, os_version from (\r\n"
						+ "select site_key, ld.source_type, sd.source_id, vm_name, vcenter, (case when lower(ld.source_type) = 'vmware' then esx_version else os_version end) as os_version, \r\n"
						+ "row_number() over(partition by site_key, sd.source_id order by log_date desc)  as row_num\r\n"
						+ "from storage_discovery sd \r\n"
						+ "LEFT JOIN (select source_id, source_type, coalesce(server_name, source_id) as server_name, coalesce(vm_name, '') as vm_name,\r\n"
						+ "coalesce(vcenter, '') as vcenter, os_version, esx_version from (select source_id, lower(source_type) as source_type, \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,\r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name, \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter, \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version, \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version, \r\n"
						+ "row_number() over(partition by source_id order by log_date desc) as row_num from local_discovery \r\n"
						+ "where site_key = '" + siteKey + "') a ) ld on lower(ld.source_id) = lower(sd.source_id)\r\n"
						+ "where site_key = '" + siteKey + "' \r\n" + "and lower(sd.source_type) = lower('" + deviceType
						+ "')\r\n" + ") a where row_num = 1\r\n" + ") ld\r\n"
						+ "LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data\r\n"
						+ "where sitekey = '" + siteKey + "' and lower(destinationtype) = lower('" + model + "') \r\n"
						+ "and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('"
						+ model + "'))) cd on\r\n"
						+ "cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id)\r\n"
						+ "order by source_id ) e ) f ) g where keys = '" + columnName
						+ "' order by keys) h ) k group by keys";
			}

			if (isSwitch) {

				defaultArray = getDefaultPIData("project", model);
				query = "select keys, json_agg(column_values) as column_values from ( \r\n"
						+ "select distinct keys, column_values from ( \r\n"
						+ "select keys, data ->> keys as column_values from (\r\n"
						+ "select data, json_object_keys(data) as keys from (\r\n"
						+ "select json_array_elements(replace(replace(pidata, '\"Host_Host Name\":\"Data Not Available\"', concat('\"Host_Host Name\":\"', source_id,'\"')), '\"Host_OS Type\":\"Data Not Available\"',concat('\"Host_OS Type\":\"', source_type,'\"'))::json) as data from ( \r\n"
						+ "select site_key, coalesce(source_type, 'Not Discovered') as source_type, source_id, coalesce(metricsdate, 'Data Not Available') as metricsdate,  \r\n"
						+ "coalesce(destinationtype, 'Data Not Available') as array_type, coalesce(vm_name, '') as vm_name, coalesce(vcenter, '') as vcenter,  \r\n"
						+ "coalesce(pidata, '" + defaultArray
						+ "') as pidata, coalesce(os_version, 'Data Not Available') as os_version  from (  \r\n"
						+ "select site_key, source_type, source_id, vm_name, vcenter, os_version from (  \r\n"
						+ "select site_key, ld.source_type, sd.source_id, vm_name, vcenter, (case when (ld.source_type) = 'vmware' then esx_version else os_version end) as os_version, \r\n"
						+ "row_number() over(partition by site_key, sd.source_id order by log_date desc)  as row_num  \r\n"
						+ "from switch_discovery sd  \r\n"
						+ "LEFT JOIN (select source_id, source_type, coalesce(server_name, source_id) as server_name, coalesce(vm_name, '') as vm_name,  \r\n"
						+ "coalesce(vcenter, '') as vcenter, os_version, esx_version from (select source_id, lower(source_type) as source_type,  \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'Server Name' as server_name,  \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'VM' as vm_name,  \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'vCenter' as vcenter,  \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'OS Version' as os_version,  \r\n"
						+ "json_array_elements(coalesce(data_temp, data_new::json)) ->> 'ESX Version' as esx_version,  \r\n"
						+ "row_number() over(partition by source_id order by log_date desc) as row_num from local_discovery  \r\n"
						+ "where site_key = '" + siteKey
						+ "') a ) ld on lower(ld.source_id) = lower(sd.source_id)  \r\n" + "where site_key = '"
						+ siteKey + "'  \r\n" + "and lower(sd.source_type) = lower('" + deviceType + "')  \r\n"
						+ ") a where row_num = 1  \r\n" + ") ld  \r\n"
						+ "LEFT JOIN(select sitekey, sourceid, sourcetype, metricsdate, destinationtype, pidata from comp_data  \r\n"
						+ "where sitekey = '" + siteKey + "' and  \r\n" + " lower(destinationtype) = lower('" + model
						+ "') \r\n"
						+ "and to_date(metricsdate,'MM-dd-yyyy')::text in (select max(to_date(metrics_date,'MM-dd-yyyy'))::text from comp_destination where lower(model) = lower('"
						+ model + "'))) cd on  \r\n"
						+ "cd.sitekey = ld.site_key and lower(cd.sourceid) = lower(ld.source_id)  \r\n"
						+ "where source_id != ''  \r\n" + "order by source_id\r\n" + ") e ) f ) g where keys = '"
						+ columnName + "' order by keys) h ) k group by keys";
			}

			System.out.println("!!!! compatibility validation query: " + query);
			List<Map<String, Object>> valueArray = getObjectFromQuery(query);
			JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for (Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
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

	public JSONArray getVR_MigrationMethod(String siteKey, String columnName, String category, String deviceType)
			throws ParseException {

		System.out.println("!!!!! siteKey: " + siteKey);
		System.out.println("!!!!! columnName: " + columnName);
		System.out.println("!!!!! category: " + category);
		System.out.println("!!!!! deviceType: " + deviceType);
		JSONParser parser = new JSONParser();

		boolean isServer = false;
		boolean isStorage = false;
		boolean isSwitch = false;

		JSONArray resultArray = new JSONArray();

		JSONArray serverArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SERVER_LIST));
		JSONArray storageArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.STORAGE_LIST));
		JSONArray switchArray = (JSONArray) parser.parse(ZKModel.getProperty(ZKConstants.SWITCH_LIST));

		if (serverArray.contains(deviceType.toLowerCase())) {
			isServer = true;
		}
		if (storageArray.contains(deviceType.toLowerCase())) {
			isStorage = true;
		}
		if (switchArray.contains(deviceType.toLowerCase())) {
			isSwitch = true;
		}

		try {

			String vmware_os_column = "OS Version";
			if (deviceType.equalsIgnoreCase("vmware")) {
				vmware_os_column = "ESX Version";
			} else {
				vmware_os_column = "OS Version";
			}

			String query = "select keys, json_agg(column_values) as column_values from ( \r\n"
					+ "select distinct keys, column_values from (  \r\n"
					+ "select keys, data::json ->> keys as column_values from ( \r\n"
					+ "select data, json_object_keys(data::json) as keys from ( \r\n"
					+ "select replace(data, '}{', ',') as data from (\r\n"
					+ "select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from ( \r\n"
					+ "select source_id, source_type, os_version, json_array_elements(data::json) as data from ( \r\n"
					+ "select source_id, source_type, os_version, replace(replace(data, '}, {', ','), '},{', ',') as data from ( \r\n"
					+ "select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n"
					+ "coalesce(source, 'Data Not Available') as source, json_agg(data)::text as data, vcenter from (  \r\n"
					+ "select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data, vcenter from (  \r\n"
					+ "select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values, vcenter from (  \r\n"
					+ "select device, methods, source_id, site_key, os_version, source_type, source,  \r\n"
					+ "json_array_elements(data::json) ->> methods as column_values, vcenter from (  \r\n"
					+ "select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, \r\n"
					+ "coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data, vcenter  from migration_method mm  \r\n"
					+ "LEFT JOIN (select mld.source_id, mld.source, mld.source as source_type, mld.os_version, mld.site_key, mld.vcenter, md.data from (  \r\n"
					+ "select site_key, source_id, source, os_version, coalesce(vcenter, '') as vcenter from ( \r\n"
					+ "select site_key, source_id, lower(source_type) as source, json_array_elements(data_temp::json) ->> '"
					+ vmware_os_column + "' as os_version,  \r\n"
					+ "json_array_elements(data_temp::json) ->> 'vCenter' as vcenter, row_number() over(partition by source_id order by log_date desc) as row_num \r\n"
					+ "from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('"
					+ deviceType + "') \r\n" + ") a where row_num = 1 \r\n" + ") mld \r\n"
					+ "LEFT JOIN (select source_id, site_key, os_version, source_type, source, data from migration_data \r\n"
					+ "where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType
					+ "')) md on md.source_id = mld.source_id)  \r\n"
					+ "dt on lower(dt.source_type) = lower(mm.device)  \r\n" + "where lower(mm.device) = lower('"
					+ deviceType
					+ "') and lower(method_name) in (select lower(method_name) from migration_method where lower(device) = lower('"
					+ deviceType + "')) and \r\n"
					+ "lower(mode_name) in (select lower(mode_name) from migration_method where lower(device) = lower('"
					+ deviceType + "'))  \r\n" + ") a  \r\n" + ") b  \r\n" + ") c  \r\n"
					+ ") d where source_id != '' group by source_id, site_key, os_version, source_type, source, vcenter  \r\n"
					+ "order by source_id \r\n" + ") e ) f ) d ) g ) h ) i where keys = '" + columnName
					+ "' ) j order by keys ) k group by keys";

			if (isServer) {

				query = "select keys, json_agg(column_values) as column_values from ( \r\n"
						+ "select distinct keys, column_values from (  \r\n"
						+ "select keys, data::json ->> keys as column_values from ( \r\n"
						+ "select data, json_object_keys(data::json) as keys from ( \r\n"
						+ "select replace(data, '}{', ',') as data from (\r\n"
						+ "select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from ( \r\n"
						+ "select source_id, source_type, os_version, json_array_elements(data::json) as data from ( \r\n"
						+ "select source_id, source_type, os_version, replace(replace(data, '}, {', ','), '},{', ',') as data from ( \r\n"
						+ "select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n"
						+ "coalesce(source, 'Data Not Available') as source, json_agg(data)::text as data, vcenter from (  \r\n"
						+ "select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data, vcenter from (  \r\n"
						+ "select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values, vcenter from (  \r\n"
						+ "select device, methods, source_id, site_key, os_version, source_type, source,  \r\n"
						+ "json_array_elements(data::json) ->> methods as column_values, vcenter from (  \r\n"
						+ "select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, \r\n"
						+ "coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data, vcenter  from migration_method mm  \r\n"
						+ "LEFT JOIN (select mld.source_id, mld.source, mld.source as source_type, mld.os_version, mld.site_key, mld.vcenter, md.data from (  \r\n"
						+ "select site_key, source_id, source, os_version, coalesce(vcenter, '') as vcenter from ( \r\n"
						+ "select site_key, source_id, lower(source_type) as source, json_array_elements(data_temp::json) ->> '"
						+ vmware_os_column + "' as os_version,  \r\n"
						+ "json_array_elements(data_temp::json) ->> 'vCenter' as vcenter, row_number() over(partition by source_id order by log_date desc) as row_num \r\n"
						+ "from local_discovery where site_key = '" + siteKey + "' and lower(source_type) = lower('"
						+ deviceType + "') \r\n" + ") a where row_num = 1 \r\n" + ") mld \r\n"
						+ "LEFT JOIN (select source_id, site_key, os_version, source_type, source, data from migration_data \r\n"
						+ "where site_key = '" + siteKey + "' and lower(source_type) = lower('" + deviceType
						+ "')) md on md.source_id = mld.source_id)  \r\n"
						+ "dt on lower(dt.source_type) = lower(mm.device)  \r\n" + "where lower(mm.device) = lower('"
						+ deviceType
						+ "') and lower(method_name) in (select lower(method_name) from migration_method where lower(device) = lower('"
						+ deviceType + "')) and \r\n"
						+ "lower(mode_name) in (select lower(mode_name) from migration_method where lower(device) = lower('"
						+ deviceType + "'))  \r\n" + ") a  \r\n" + ") b  \r\n" + ") c  \r\n"
						+ ") d where source_id != '' group by source_id, site_key, os_version, source_type, source, vcenter  \r\n"
						+ "order by source_id \r\n" + ") e ) f ) d ) g ) h ) i where keys = '" + columnName
						+ "') j order by keys ) k group by keys";
			}

			if (isStorage) {

				query = "select keys, json_agg(column_values) as column_values from ( \r\n"
						+ "select distinct keys, column_values from (  \r\n"
						+ "select keys, data::json ->> keys as column_values from ( \r\n"
						+ "select data, json_object_keys(data::json) as keys from ( \r\n"
						+ "select replace(data, '}{', ',') as data from (\r\n"
						+ "select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from ( \r\n"
						+ "select source_id, source_type, os_version, json_array_elements(data::json) as data from ( \r\n"
						+ "select source_id, source_type, os_version, replace(replace(data::text, '}, {', ','), '},{', ',') as data from (\r\n"
						+ "select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n"
						+ "coalesce(source, 'Data Not Available') as source, json_agg(data) as data from ( \r\n"
						+ "select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data from ( \r\n"
						+ "select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values from ( \r\n"
						+ "select device, methods, source_id, site_key, os_version, source_type, source, \r\n"
						+ "json_array_elements(data::json) ->> methods as column_values from ( \r\n"
						+ "select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data  from migration_method mm \r\n"
						+ "LEFT JOIN (select b.site_key, b.source_id, b.os_version, b.source_type, md.source, md.data from ( \r\n"
						+ "select site_key, source_id, os_version, source_type, vcenter from (select sd.source_id, sd.site_key, data, ld.os_version, ld.source_type, ld.vcenter, \r\n"
						+ "row_number() over(partition by sd.source_id order by log_date desc) as row_num \r\n"
						+ "from storage_discovery sd \r\n"
						+ "LEFT JOIN (select site_key, source_id, source_type, coalesce(os_version, 'Data Not Available') as os_version,  \r\n"
						+ "coalesce(vcenter, 'Data Not Available') as vcenter from ( \r\n"
						+ "select site_key,source_id, lower(source_type) as source_type, json_array_elements(data_temp::json) ->> 'OS Version' as os_version, \r\n"
						+ "json_array_elements(data_temp::json) ->> 'vCenter' as vcenter,  \r\n"
						+ "row_number() over(partition by source_id order by log_date desc) as row_num \r\n"
						+ "from local_discovery where site_key = '" + siteKey + "' \r\n"
						+ ") a1 where row_num = 1) ld on lower(ld.source_id) = lower(sd.source_id) and ld.site_key = sd.site_key \r\n"
						+ "where sd.site_key = '" + siteKey + "' and \r\n" + "lower(sd.source_type) = lower('"
						+ deviceType + "')) a  where row_num = 1 \r\n" + ") b \r\n"
						+ "LEFT JOIN(select source_id, site_key, os_version, source_type, source, data from migration_data \r\n"
						+ "where site_key = '" + siteKey + "') md on lower(md.source_id) = lower(b.source_id)) \r\n"
						+ "dt on 1 = 1 \r\n"
						+ " and lower(method_name) in (select lower(method_name) from migration_method) and mode_name in (select lower(mode_name) from migration_method)) a \r\n"
						+ ") b \r\n" + ") c \r\n"
						+ ") d where source_id != '' group by source_id, site_key, os_version, source_type, source \r\n"
						+ "order by source_id \r\n" + ") e ) f ) d ) g ) h ) i where keys = '" + columnName
						+ "' ) j order by keys ) k group by keys ";
			}

			if (isSwitch) {
				query = "select keys, json_agg(column_values) as column_values from (  \r\n"
						+ "select distinct keys, column_values from (  \r\n"
						+ "select keys, data::json ->> keys as column_values from (  \r\n"
						+ "select data, json_object_keys(data::json) as keys from ( \r\n"
						+ "select replace(data, '}{', ',') as data from (  \r\n"
						+ "select data::jsonb || concat('{\"Server Name\":\"', source_id, '\",\"OS Version\":\"', os_version, '\",\"Source\":\"', source_type, '\"}') as data from (  \r\n"
						+ "select source_id, source_type, os_version, json_array_elements(data::json) as data from (  \r\n"
						+ "select source_id, source_type, os_version, replace(replace(data::text, '}, {', ','), '},{', ',') as data from (\r\n"
						+ "select source_id, site_key, coalesce(os_version, 'Data Not Available') as os_version, coalesce(source_type, 'Data Not Available') as source_type, \r\n"
						+ "coalesce(source, 'Data Not Available') as source, json_agg(data) as data from ( \r\n"
						+ "select source_id, site_key, os_version, source_type, source, json_build_object(methods, column_values) as data from ( \r\n"
						+ "select device, methods, source_id, site_key, os_version, source_type, source, coalesce(column_values, 'Not Applicable') as column_values from ( \r\n"
						+ "select device, methods, source_id, site_key, os_version, source_type, source, \r\n"
						+ "json_array_elements(data::json) ->> methods as column_values from ( \r\n"
						+ "select mm.device, mm.methods, dt.source_id, dt.site_key, dt.os_version, dt.source_type, dt.source, coalesce(dt.data, concat('[{\"', mm.methods, '\":\"', 'Not Applicable' , '\"}]')) as data  from migration_method mm \r\n"
						+ "LEFT JOIN (select b.site_key, b.source_id, b.os_version, b.source_type, md.source, md.data from ( \r\n"
						+ "select site_key, source_id, os_version, source_type, vcenter from (select sd.source_id, sd.site_key, data, ld.os_version, ld.source_type, ld.vcenter, \r\n"
						+ "row_number() over(partition by sd.source_id order by log_date desc) as row_num \r\n"
						+ "from switch_discovery sd \r\n"
						+ "LEFT JOIN (select site_key, source_id, source_type, coalesce(os_version, 'Data Not Available') as os_version,  \r\n"
						+ "coalesce(vcenter, 'Data Not Available') as vcenter from ( \r\n"
						+ "select site_key,source_id, lower(source_type) as source_type, json_array_elements(data_temp::json) ->> 'OS Version' as os_version, \r\n"
						+ "json_array_elements(data_temp::json) ->> 'vCenter' as vcenter,  \r\n"
						+ "row_number() over(partition by source_id order by log_date desc) as row_num \r\n"
						+ "from local_discovery where site_key = '" + siteKey + "' \r\n"
						+ ") a1 where row_num = 1) ld on lower(ld.source_id) = lower(sd.source_id) and ld.site_key = sd.site_key \r\n"
						+ "where sd.site_key = '" + siteKey + "' and \r\n" + "lower(sd.source_type) = '"
						+ deviceType.toLowerCase() + "') a  where row_num = 1 \r\n" + ") b \r\n"
						+ "LEFT JOIN(select source_id, site_key, os_version, source_type, source, data from migration_data \r\n"
						+ "where site_key = '" + siteKey + "') md on lower(md.source_id) = lower(b.source_id)) \r\n"
						+ "dt on 1 = 1 \r\n"
						+ " and lower(method_name) in (select lower(method_name) from migration_method) and lower(mode_name) in (select lower(mode_name) from migration_method)) a \r\n"
						+ ") b \r\n" + ") c \r\n"
						+ ") d where source_id != '' group by source_id, site_key, os_version, source_type, source \r\n"
						+ "order by source_id \r\n" + ") e ) f ) d ) g ) h ) i where keys = '" + columnName
						+ "') j order by keys ) k group by keys";
			}

			System.out.println("!!!!! query: " + query);
			List<Map<String, Object>> valueArray = getObjectFromQuery(query);
			// JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for (Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
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

	public List<Map<String, Object>> getObjectFromQuery(String query) {
		List<Map<String, Object>> obj = new ArrayList<>();
		try {

			obj = jdbc.queryForList(query);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	private JSONArray getDefaultPIData(String device, String destinationType) {

		JSONParser parser = new JSONParser();
		JSONArray resultArray = new JSONArray();

		try {
			String query = "select json_agg(column_name) as column_name from ( \r\n"
					+ "select json_object_agg(column_name,'Data Not Available') as column_name from (\r\n"
					+ "select distinct(column_name), seq::integer from report_columns where report_name = 'Compatibility' and lower(device_type) = '"
					+ device.toLowerCase() + "' \r\n" + "order by seq::integer\r\n" + ") a ) b";

			System.out.println("!!!!! query: " + query);
			List<Map<String, Object>> valueArray = getObjectFromQuery(query);
			// JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for (Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_name").toString());
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

	/*
	 * public JSONArray getVR_PrivilledgeData(String siteKey, String columnName) {
	 * 
	 * JSONArray resultArray = new JSONArray();
	 * 
	 * try {
	 * 
	 * String query =
	 * "select keys, json_agg(column_values) as column_values from (\r\n" +
	 * "select distinct keys, column_values from (\r\n" +
	 * "select concat('Server Data~', keys) as keys, json_array_elements(data::json) ->> keys as column_values from ( \r\n"
	 * + "select data, json_object_keys(data_object) as keys from (\r\n" +
	 * "select data, json_array_elements(data::json) as data_object from privillege_data  \r\n"
	 * + "where site_key = '" + siteKey + "'  \r\n" + ") a \r\n" + ") b\r\n" +
	 * "union all \r\n" +
	 * "select concat(source_name, '~', keys) as keys, data::json ->> keys as column_values from ( \r\n"
	 * +
	 * "select source_name, data, json_object_keys(data::json) as keys from (  \r\n"
	 * +
	 * "select sd.source_id, s.source_name, primary_key_value, replace(data, '.0\",', '\",') as data, row_number() over(partition by sd.source_id, primary_key_value order by update_time desc) as row_num \r\n"
	 * + "from source_data sd  \r\n" +
	 * "LEFT JOIN source s on s.source_id = sd.source_id and s.site_key = '" +
	 * siteKey + "'  \r\n" + "where sd.site_key = '" + siteKey +
	 * "' and sd.source_id in ( \r\n" + "select source_id from (  \r\n" +
	 * "select source_id, json_array_elements(fields::json) ->> 'primary' as is_primary,  \r\n"
	 * +
	 * "json_array_elements(fields::json) ->> 'primaryKey' as primary_key from source  \r\n"
	 * + "where site_key = '" + siteKey + "'  \r\n" +
	 * ") a where is_primary::boolean = true and primary_key = 'userName' ) \r\n" +
	 * ") b  \r\n" + "where row_num = 1  \r\n" + ") c \r\n" +
	 * ") d where keys <> 'sourceId' and keys <> 'siteKey'\r\n" +
	 * ") e where keys = '" + columnName + "' \r\n" + "group by keys ";
	 * 
	 * System.out.println("!!!!! query: " + query); List<Map<String,Object>>
	 * valueArray = getObjectFromQuery(query); //JSONParser parser = new
	 * JSONParser(); System.out.println("!!!!! valueArray: " + valueArray);
	 * for(Map<String, Object> list : valueArray) { resultArray = (JSONArray)
	 * parser.parse(list.get("column_values").toString()); }
	 * 
	 * } catch(Exception e) { e.printStackTrace(); StringWriter errors = new
	 * StringWriter(); e.printStackTrace(new PrintWriter(errors)); String ex =
	 * errors.toString(); ExceptionHandlerMail.errorTriggerMail(ex); }
	 * 
	 * return resultArray;
	 * 
	 * }
	 */
	public JSONArray getVR_PrivilledgeData(String siteKey, String columnName) {

		JSONArray resultArray = new JSONArray();

		try {

			String query = "select keys, json_agg(column_values) as column_values from (\r\n"
					+ "select distinct keys, column_values from (\r\n"
					+ "select concat('Server Data~', keys) as keys, json_array_elements(data::json) ->> keys as column_values from ( \r\n"
					+ "select data, json_object_keys(data_object) as keys from (\r\n"
					+ "select data, json_array_elements(data::json) as data_object from privillege_data  \r\n"
					+ "where site_key = '" + siteKey + "'  \r\n" + ") a \r\n" + ") b\r\n" + "union all \r\n"
					+ "select concat(source_name, '~', keys) as keys, data::json ->> keys as column_values from ( \r\n"
					+ "select source_name, data, json_object_keys(data::json) as keys from (  \r\n"
					+ "select sd.source_id, s.source_name, primary_key_value, replace(data, '.0\"', '\"') as data, row_number() over(partition by sd.source_id, primary_key_value order by update_time desc) as row_num \r\n"
					+ "from source_data sd  \r\n"
					+ "LEFT JOIN source s on s.source_id = sd.source_id and s.site_key = '" + siteKey + "'  \r\n"
					+ "where sd.site_key = '" + siteKey + "' and sd.source_id in ( \r\n"
					+ "select source_id from (  \r\n"
					+ "select source_id, json_array_elements(fields::json) ->> 'primary' as is_primary,  \r\n"
					+ "json_array_elements(fields::json) ->> 'primaryKey' as primary_key from source  \r\n"
					+ "where site_key = '" + siteKey + "'  \r\n"
					+ ") a where is_primary::boolean = true and primary_key = 'userName' ) \r\n" + ") b  \r\n"
					+ "where row_num = 1  \r\n" + ") c \r\n" + ") d where keys <> 'sourceId' and keys <> 'siteKey'\r\n"
					+ ") e where keys = '" + columnName + "' \r\n" + "group by keys ";

			System.out.println("!!!!! query: " + query);
			List<Map<String, Object>> valueArray = getObjectFromQuery(query);
			// JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for (Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
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
	
	
	public JSONArray getCloudCostReportValuesPostgres(String siteKey, String columnName, String category, String deviceType,
			String report_by) {
		JSONArray resultData = new JSONArray();
		columnName = columnName.replaceAll("\\s+", "_").toLowerCase();
		try {
			String inputDeviceType = deviceType;	
			if (deviceType.equalsIgnoreCase("All")) {
				deviceType = " lower(source_type) in ('windows','linux', 'vmware', 'ec2')";
			} else {
				deviceType = " lower(source_type)='" + deviceType.toLowerCase() + "'";
			}

			if (category.toLowerCase().equalsIgnoreCase("AWS Instances")) {
				if (inputDeviceType.equalsIgnoreCase("All")) {
					deviceType = " lower(server_type)='ec2'  and lower(source_type) in ('windows','linux', 'vmware')";
				} else {
					inputDeviceType = " lower(server_type)='ec2'  and lower(source_type) = '" + inputDeviceType.toLowerCase()
							+ "'";
				}

			}

			if (report_by.equalsIgnoreCase("All")) {
				report_by = "report_by in ('Physical Servers','AWS Instances','Custom Excel Data')";
			} else {
				report_by = "report_by='" + report_by + "'";
			}

			List<String> data = new ArrayList<>();
			try {
				String query = "select distinct("+columnName+") from cloud_cost_report_data where "+columnName+" is not null and "+columnName+" !='' and site_key='"+siteKey+"' and "+ deviceType + " and " + report_by + " order by "+columnName+"";
				System.out.println("--query--------- " + query);
				
				data = jdbc.queryForList(query, String.class);
			} catch (Exception e) {
				e.printStackTrace();
			}		 

			boolean isPriceColumn = false;
			if (columnName.toLowerCase().contains("price")) {
				isPriceColumn = true;
			}
			if (data != null && !data.isEmpty()) {
				for (String str : data) {
					if (str != null && !str.isEmpty()) {
						if (!isPriceColumn) {
							resultData.add(str);
						} else {
							resultData.add("$" + str);
						}
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return resultData;
	}
	
	public JSONArray getUniqueValues(String siteKey,String reportBy,String columnName) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONArray resultArray = new JSONArray();
		
		
		try {
			String uniqueFilterQuery = "select keys, json_agg(data) as data from (\r\n"
					+ "select keys, json_build_object('id', coalesce(option_id,''), 'value', coalesce(option_value,'')) as data from (\r\n"
					+ "select project_id, report_type_column_id as keys, report_type_column_value,option_id, option_value,\r\n"
					+ "concat(report_type_column_value, '_', option_id) as label from (\r\n"
					+ "select project_id, report_type_column_id, report_type_column_value, option_id, option_value,\r\n"
					+ "row_number() over(partition by report_type_column_value,option_id, option_value) as row_num\r\n"
					+ "from (\r\n"
					+ "select project_id,\r\n"
					+ "report_type_column_id,\r\n"
					+ "report_type_column_value, option_id, option_value from (\r\n"
					+ "select pr.project_id, rt.column_id as report_type_column_id, rt.column_label as report_type_column_value,\r\n"
					+ "pcf.field_id as default_field_id, rt.option_id, rt.option_value\r\n"
					+ "from project pr\r\n"
					+ "LEFT JOIN (\r\n"
					+ "select project_id, migration_category, column_id, column_label, column_type, coalesce(option_id, other_option_id) as option_id,\r\n"
					+ "coalesce(option_value, other_option_value) as option_value from (\r\n"
					+ "select b.project_id, migration_category, column_id, column_label, column_type, \r\n"
					+ "json_array_elements(options::json) ->> 'id' as option_id,\r\n"
					+ "json_array_elements(options::json) ->> 'label' as option_value, ptl.option_id as other_option_id, ptl.option_value as other_option_value from (\r\n"
					+ "select project_id, migration_category, column_id, column_label, column_type,\r\n"
					+ "coalesce((case when options = '[]' then '[{}]' else options end), '[{}]') as options from (\r\n"
					+ "select project_id, migration_category, json_array_elements(system_fields::json) ->> 'fieldId' as column_id,\r\n"
					+ "json_array_elements(system_fields::json) ->> 'label' as column_label,\r\n"
					+ "json_array_elements(system_fields::json) ->> 'type' as column_type,\r\n"
					+ "json_array_elements(system_fields::json) ->> 'reportConfig' as report_config,\r\n"
					+ "json_array_elements(system_fields::json) ->> 'options' as options from project\r\n"
					+ "where 1 = 1 and site_key = '" + siteKey + "' and project_id = '" + reportBy + "'\r\n"
					+ ") a\r\n"
					+ ") b\r\n"
					+ "LEFT JOIN (select project_id, field_id, field_value as option_id, field_value as option_value from project_tasklist\r\n"
					+ "where project_id = '" + reportBy + "' and (field_value != 'null' and trim(field_value) != '') and field_label not ilike '%_largeDataArr'\r\n"
					+ "order by project_id, field_id, field_value) ptl on ptl.project_id = b.project_id and b.column_id = ptl.field_id\r\n"
					+ " ) c\r\n"
					+ ") rt on rt.project_id = pr.project_id\r\n"
					+ "LEFT JOIN project_custom_field pcf on pcf.field_id = rt.column_id\r\n"
					+ "where rt.column_id is not null and pr.site_key = '" + siteKey + "' and pr.project_id = '" + reportBy + "'\r\n"
					+ "order by rt.column_label\r\n"
					+ ") b\r\n"
					+ ") c\r\n"
					+ ") d where row_num = 1\r\n"
					+ "union all\r\n"
					+ "select project_id, report_type_column_id, report_type_column_value, option_id, option_value, concat(report_type_column_value, '_', option_id) as label from (\r\n"
					+ "select project_id, report_type_column_id, report_type_column_value, column_type, option_id, option_value,\r\n"
					+ "row_number() over(partition by report_type_column_value,option_id, option_value) as row_num from (\r\n"
					+ "select project_id, id as report_type_column_id, group_name as report_type_column_value,\r\n"
					+ "'select' as column_type, option_id, option_value from (\r\n"
					+ "select pr.project_id, id, group_name, profile_id as option_id, profile_name as option_value from (\r\n"
					+ "select tenant_group_fields_id as id, group_name from tenant_group_fields where\r\n"
					+ "tenant_group_fields_id not in (select tenant_group_fields_id from site_group_fields where site_key = '" + siteKey + "' and\r\n"
					+ "tenant_group_fields_id is not null) and\r\n"
					+ "tenant_group_fields_id not in (select ref_id from project_group_fields where ref_id is not null and site_key = '" + siteKey + "' and project_id = '" + reportBy + "' )\r\n"
					+ "union all\r\n"
					+ "select site_group_fields_id as id, group_name from site_group_fields where\r\n"
					+ "tenant_group_fields_id not in (select ref_id from project_group_fields where ref_id is not null and site_key = '" + siteKey + "' and project_id = '" + reportBy + "' )\r\n"
					+ "union all\r\n"
					+ "select ref_id as id, group_name from project_group_fields where site_key = '" + siteKey + "' and project_id = '" + reportBy + "'\r\n"
					+ ") a\r\n"
					+ "JOIN destination_profile pr on pr.migration_group_id = a.id and pr.project_id = '" + reportBy + "' and pr.is_active::boolean = true \r\n"
					+ ") b\r\n"
					+ ") c\r\n"
					+ ") d where row_num = 1\r\n"
					+ "order by option_value \r\n"
					+ ") e where option_id is not null or option_id <> '' \r\n"
					+ ") f where keys = '" + columnName + "' group by keys \r\n" +
					"union all \r\n" +
					"select keys, data from ( \r\n" + 
					"select keys, json_agg(data) as data from (  \r\n" + 
					"select keys, data from ( \r\n" +
					"select distinct keys, json_array_elements(LocalDiscoveryData) ->> keys as data from ( \r\n" + 
					"select LocalDiscoveryData, json_object_keys(data) as keys from ( \r\n" + 
					"select LocalDiscoveryData, json_array_elements(LocalDiscoveryData) as data from (\r\n" + 
					"Select TL.site_key,TL.task_data_id,lower(TL.server_name) as server_name,\r\n" + 
					"(case when TL.is_manual = true then TL.server_type else LD.source_type end) as server_type,TL.project_id,TL.task_id,TL.default_custom_fields_new,\r\n" + 
					"TL.updated_time,LD.source_category,LD.wwn,\r\n" + 
					"LD.actual_os_type,LD.data_temp LocalDiscoveryData from Tasklist TL\r\n" + 
					"left join (select *,Row_number() over(partition by server_name order by log_date desc) as Row_Num from local_discovery\r\n" + 
					"where site_key = '" + siteKey + "') LD on LD.site_key = TL.site_key and lower(LD.source_id) = lower(TL.task_id) or lower(LD.server_name) = lower(TL.server_name)\r\n" + 
					"and LD.Row_Num = 1\r\n" + 
					"where TL.is_active=true and TL.project_id= '" + reportBy + "' \r\n" + 
					") a \r\n" + 
					") b \r\n" + 
					") c order by keys \r\n" + 
					") c1 order by data \r\n" +
					") d where data is not null and trim(data) <> '' group by keys \r\n" + 
					") e where keys = substring('" + columnName + "', position('_' in '" + columnName + "') + 1, length('" + columnName + "')) \r\n" +
					"union all \r\n" + 
					"select keys, data from ( \r\n" + 
					"select keys, json_agg(data) as data from ( \r\n" + 
					"select keys, data from ( \r\n" +
					"select distinct keys, json_array_elements(data) ->> keys as data from ( \r\n" + 
					"select data, json_object_keys(data1) as keys from ( \r\n" + 
					"select ServerDiscoveryData as data, json_array_elements(ServerDiscoveryData) as data1 from ( \r\n" + 
					"Select TL.site_key,TL.task_data_id,TL.server_type,lower(TL.server_name) as server_name,\r\n" + 
					"TL.project_id,TL.task_id,TL.default_custom_fields_new,\r\n" + 
					"TL.updated_time,SD.source_category,SD.wwn,coalesce(SD.source_type, server_type) as server_type,\r\n" + 
					"coalesce(SD.data_temp, concat('[{\"Server Type\":\"',coalesce(SD.source_type, server_type), '\",\"Server Name\":\"',server_name, '\"}]')::json) ServerDiscoveryData\r\n" + 
					"from Tasklist TL\r\n" + 
					"left join (select site_key, source_id, source_category, wwn, source_type, data_temp, Row_number() over(partition by source_id order by log_date desc) as Row_Num\r\n" + 
					"from server_discovery\r\n" + 
					"where site_key = '" + siteKey + "') SD on SD.site_key = TL.site_key and SD.source_id = TL.task_id and\r\n" + 
					"SD.Row_Num = 1\r\n" + 
					"where TL.is_active=true and TL.project_id='" + reportBy + "' \r\n" + 
					") a \r\n" + 
					") b \r\n" + 
					") c where keys <> 'OS Version' order by keys \r\n" + 
					") c1 order by data \r\n" +
					") d where data is not null and trim(data) <> '' group by keys \r\n" + 
					") e where keys = substring('" + columnName + "', position('_' in '" + columnName + "') + 1, length('" + columnName + "')) \r\n " +
					"union all \r\n " +
					" select keys, json_agg(data) as data from (\r\n" + 
					"select concat(source_name,'_',keys) as keys, data from (\r\n" + 
					"select distinct source_name, keys, data::json ->> keys as data from (\r\n" + 
					"select source_name, data, keys from (\r\n" + 
					"select source_name, primary_key, data, json_object_keys(data::json) as keys from (\r\n" + 
					"select source_name, primary_key, data from source_data sd \r\n" + 
					"LEFT JOIN source sc on sc.source_id = sd.source_id\r\n" + 
					"where sd.source_id in (select json_array_elements_text((input_source::jsonb || third_party_list::jsonb)::json) from project where project_id = '" + reportBy + "') \r\n" + 
					"and lower(primary_key_value) in (select lower(server_name) from tasklist where is_active = true and project_id = '" + reportBy + "') \r\n" +
					") a ) b where keys not in (primary_key, 'siteKey', 'sourceId')\r\n" + 
					") c\r\n" + 
					") d order by data\r\n" + 
					") e where keys = '" + columnName + "' group by keys \r\n" +
					"union all \r\n" + 
					"select keys, json_agg(data) as data from ( \r\n" + 
					"select distinct keys, data from (\r\n" + 
					"select concat('server~',keys) as keys, json_array_elements(data::json) ->> keys as data from (\r\n" + 
					"select data, json_object_keys(json_array_elements(data::json)) as keys from privillege_data where site_key = '" + siteKey + "' \r\n" + 
					"and lower(server_name) in (select server_name from tasklist where project_id = '" + reportBy + "') and \r\n" + 
					"lower(source_id) in (select task_id from tasklist where project_id = '" + reportBy + "') \r\n" + 
					") a \r\n" + 
					") b where data <> '' order by data\r\n" + 
					") c where keys = '" + columnName + "' group by keys";
					
			
			System.out.println("!!!!! uniqueFilterQuery: " + uniqueFilterQuery);
			List<Map<String,Object>> valueArray = getObjectFromQuery(uniqueFilterQuery); 
			//JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("data").toString());
			}
			System.out.println("------------ resultArray : " + resultArray);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return resultArray;
	}

public JSONArray getOnpremisesCostFieldType(String siteKey, String columnName, String osType) {
		
		JSONArray resultArray = new JSONArray();
		
		String columnName1 = columnName;
		
		if(columnName.equalsIgnoreCase("OS")) {
			columnName1 = "Server Type";
		}
		
		try {
			
			String query = "select json_agg(values) as column_values from (\r\n "
					+ "select distinct coalesce(values, values1) as values from (\r\n "
					+ "select data_temp, json_array_elements(data_temp) ->> '" + columnName + "' as values, \r\n"
					+ "json_array_elements(data_temp) ->> '" + columnName1 + "' as values1 from local_discovery \r\n"
					+ "where site_key = '" + siteKey + "' \r\n"
					+ ") a where values is not null and trim(values) <> '' \r\n"
					+ "order by values \r\n"
					+ ") b";
			
			System.out.println("!!!!! query: " + query);
			List<Map<String,Object>> valueArray= getObjectFromQuery(query); 
			System.out.println("!!!!! valueArray: " + valueArray);
			for(Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
		}	
			
			
			if(osType != null && !osType.isEmpty()) {
				 query = "select json_agg(values) as column_values from (\r\n"
						+ "select distinct values from (\r\n"
						+ "select data_temp, json_array_elements(data_temp) ->> 'OS' as os1,\r\n"
						+ "json_array_elements(data_temp) ->> 'Server Type' as os2,\r\n"
						+ "json_array_elements(data_temp) ->> '"+columnName+"' as values from local_discovery\r\n"
						+ "where site_key = '"+siteKey+"'\r\n"
						+ ") a where coalesce(os1, os2) = '"+osType+"' and values is not null and trim(values) <> ''\r\n"
						+ "order by values\r\n"
						+ ") b";
				
				
				System.out.println("!!!!! query: " + query);
				List<Map<String,Object>> valueArray1 = getObjectFromQuery(query); 
				System.out.println("!!!!! valueArray: " + valueArray1);
				for(Map<String, Object> list : valueArray1) {
					resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
		        }
						
			}	
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return resultArray;
		
	}

	public JSONArray getVR_VanguardGroupInfo(String siteKey, String columnName) {

	JSONArray resultArray = new JSONArray();

	try {

		String query = "select keys, column_values from (\r\n" + 
				"select keys, json_agg(column_values) as column_values from (\r\n" + 
				"select distinct keys, data ->> keys as column_values from (\r\n" + 
				"select json_build_object('Server Name', server_name, 'Group Name', \"group\", 'Owner', group_owner, 'Superior Group', superior_group, 'Creation Date', creation_date,\r\n" + 
				"'TERMUACC', termuacc, 'Model Name', model_name, 'Users Connected to the Group', user_name, 'User Id', user_id) as data, keys from ( \r\n" + 
				"select vsi.server_name, vgi.group, vgi.group_owner, vgi.superior_group, vgi.creation_date,\r\n" + 
				"vgi.termuacc, vgi.model_name, vui.user_name, vui.user_id\r\n" + 
				"from vanguard_server_info vsi\r\n" + 
				"LEFT JOIN vanguard_group_info vgi on vgi.server_name = vsi.server_name\r\n" + 
				"LEFT JOIN vanguard_user_info vui on vui.server_name = vsi.server_name and vgi.group = vui.group\r\n" + 
				"where vsi.site_key = '" + siteKey + "' order by vsi.server_name \r\n" + 
				") a \r\n" + 
				"LEFT JOIN (\r\n" + 
				"select column_name as keys from report_columns where device_type = 'Vanguard' and report_by = 'Group Info' \r\n" + 
				") cl on 1 = 1 \r\n" + 
				") b \r\n" + 
				") c group by keys \r\n" + 
				") d where keys = '" + columnName + "'";
				

		System.out.println("!!!!! query: " + query);
		List<Map<String, Object>> valueArray = getObjectFromQuery(query);
		// JSONParser parser = new JSONParser();
		System.out.println("!!!!! valueArray: " + valueArray);
		for (Map<String, Object> list : valueArray) {
			resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
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
	
	
	public JSONArray getVR_TaniumGroup(String siteKey, String columnName) {

		JSONArray resultArray = new JSONArray();

		try {

			String query = "select keys, json_agg(column_values) as column_values from ( \r\n" + 
					"select distinct keys, coalesce(column_values, '') as column_values from (\r\n" + 
					"select keys, data ->> keys as column_values from ( \r\n" + 
					"select data, json_object_keys(data) as keys from (\r\n" + 
					"select serverName, groupName,json_build_object('Server Name', serverName, 'Group Name', groupName, 'Member Of Group', memberOfGroup,\r\n" + 
					"'Sudoers Access', sudoPrivileges, 'Is Sudoers', isSudoers, 'Group Id', groupId, 'Os Version', osVersion) as data from\r\n" + 
					"(select\r\n" + 
					"ugi.server_name serverName\r\n" + 
					",ugi.group_name groupName\r\n" + 
					",ugi.gid groupId\r\n" + 
					",ugi.member_of_group memberOfGroup\r\n" + 
					",case when usi.user_name is null then 'No' else 'Yes' end as isSudoers\r\n" + 
					",usi.sudo_privileges as sudoPrivileges\r\n" + 
					",hd.operating_system as osVersion\r\n" + 
					"from linux_users_group_info ugi\r\n" + 
					"left join linux_user_sudo_info usi on ugi.server_name = usi.server_name and ugi.site_key = usi.site_key\r\n" + 
					"and ugi.group_name = usi.user_name and usi.is_group_user = 'true'\r\n" + 
					"join linux_host_details hd on hd.server_name = ugi.server_name and ugi.site_key = hd.site_key\r\n" + 
					"where ugi.site_key = '" + siteKey + "') as d \r\n" + 
					") a \r\n" + 
					") b \r\n" + 
					") c\r\n" + 
					") d where keys = '" + columnName + "' group by keys";
					

			System.out.println("!!!!! query: " + query);
			List<Map<String, Object>> valueArray = getObjectFromQuery(query);
			// JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for (Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
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
	
	public JSONArray getVR_ZoomUsers(String siteKey, String columnName) {

		JSONArray resultArray = new JSONArray();

		try {

			String query = "select keys, column_values from (\r\n"
					+ "select keys, json_agg(column_values) as column_values from (\r\n"
					+ "select distinct keys, data ->> keys as column_values from (\r\n"
					+ "select json_build_object('userId',user_id,'firstName', first_name,'lastName', last_name, \r\n"
					+ "'department', department,'email', email, 'role', role, 'status',status,'licenseType', license_type, \r\n"
					+ "'lastLoginTime', last_login_time, 'lastClientVersion', last_client_version,\r\n"
					+ "'timeZone',timezone, 'isVerified', is_verified, 'groupNames', group_names, \r\n"
					+ "'isGroupAdmin',is_group_admin) as data, keys from (\r\n"
					+ "	\r\n"
					+ "select zrm.identity as user_id,zrm.first_name,zrm.last_name,zrm.department,zrm.email,zr.name as Role\r\n"
					+ ",case when zu.status is not null then zu.status else 'Inactive' end as status\r\n"
					+ ",case when zrm.type = '1' then 'Basic'\r\n"
					+ "when zrm.type = '2' then 'Licensed'\r\n"
					+ "when zrm.type = '3' then 'On-prem'\r\n"
					+ "when zrm.type = '99' then 'None'\r\n"
					+ "when zrm.type is null then 'None'\r\n"
					+ "end as License_type\r\n"
					+ ",zu.last_login_time,zu.last_client_version,zu.timezone\r\n"
					+ ",case when zu.verified = '1' then 'Yes' else 'No' end as Is_Verified\r\n"
					+ ",zgm.group_names as group_names\r\n"
					+ ",case when zgm.is_group_admin is null then 'No' else 'Yes' end as is_group_admin\r\n"
					+ "from zoom_roles_members zrm\r\n"
					+ "left join zoom_users zu on zrm.identity = zu.identity and zrm.site_key = zu.site_key\r\n"
					+ "left join zoom_roles zr on zr.identity = zrm.role_id and zrm.site_key = zr.site_key\r\n"
					+ "left join (select zgm.site_key,zgm.identity,string_agg(zg.name, ',') as group_names\r\n"
					+ ",zga.identity as is_group_admin\r\n"
					+ "from zoom_group_members zgm\r\n"
					+ "join zoom_groups zg on zg.identity = zgm.group_id and zg.site_key = zgm.site_key\r\n"
					+ "left join zoom_group_admins zga on zga.group_id = zg.identity and zga.identity = zgm.identity\r\n"
					+ "group by zgm.site_key,zgm.identity,zga.identity\r\n"
					+ ") zgm on zgm.identity = zrm.identity and zgm.site_key = zrm.site_key) \r\n"
					+ "	\r\n"
					+ "a	\r\n"
					+ "LEFT JOIN (\r\n"
					+ "select column_name as keys from report_columns where device_type = 'Zoom' and report_by = 'User'\r\n"
					+ ") cl on 1 = 1\r\n"
					+ ") b\r\n"
					+ ") c group by keys\r\n"
					+ ") d where keys = '"+columnName+"'\r\n";
					
			System.out.println("!!!!! query: " + query);
			List<Map<String, Object>> valueArray = getObjectFromQuery(query);
			// JSONParser parser = new JSONParser();
			System.out.println("!!!!! valueArray: " + valueArray);
			for (Map<String, Object> list : valueArray) {
				resultArray = (JSONArray) parser.parse(list.get("column_values").toString());
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
}
