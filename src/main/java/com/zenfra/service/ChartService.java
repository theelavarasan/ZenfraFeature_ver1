package com.zenfra.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenfra.dao.ChartDAO;
import com.zenfra.dataframe.service.DashBoardService;
import com.zenfra.dataframe.service.DataframeService;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.model.DashboardChartDetails;
import com.zenfra.model.DashboardInputModel;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.ExceptionHandlerMail;

@Service
public class ChartService {

	@Autowired
	CommonFunctions functions;

	@Autowired
	ChartDAO chartDao;

	@Autowired
	DashBoardService dashBoardService;

	@Autowired
	DataframeService dataframeService;

	private JSONParser jsonParser = new JSONParser();

	public boolean saveChart(ChartModel_v2 chart) {
		boolean response = false;
		try {

			/*
			 * Map<String, Object> params = new HashMap<String, Object>();
			 * params.put(":chart_id", chart.getChartId());
			 * params.put(":chart_configuration",chart.getChartConfiguration());
			 * params.put(":is_dashboard", chart.isDashboard()); params.put(":site_key",
			 * chart.getSiteKey()); params.put(":report_name", chart.getReportName());
			 * params.put(":chart_name", chart.getChartName());
			 * params.put(":filter_property", chart.getFilterProperty());
			 * params.put(":chart_type", chart.getChartType()); params.put(":created_time",
			 * chart.getCreatedTime()); params.put(":update_time", chart.getUpdateTime());
			 * params.put(":is_active", chart.isActive()); params.put(":user_id",
			 * chart.getUserId());
			 * 
			 * responce=chartDao.SaveChart(params);
			 */
						
			if (chart.getChartId() == null || chart.getChartId().trim().isEmpty()) {
				chart.setCreatedTime(functions.getCurrentDateWithTime());
				chart.setChartId(functions.generateRandomId());
				chart.setUpdateTime(functions.getCurrentDateWithTime());
				chart.setIsActive(true);

				response = chartDao.saveEntityNew(chart);

			} else {
				chart.setIsActive(true);
				chart.setUpdateTime(functions.getCurrentDateWithTime());
				response = chartDao.updateEntityNew(chart);
			}
			
			
//			if (chart.getChartId() != null) {
//				response = chartDao.updateEntity(ChartModel_v2.class, chart);
//			} else {
//				response = chartDao.saveEntity(ChartModel_v2.class, chart);	
//			}

		}	catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return response;
	}

	public ChartModel_v2 getChartByChartId(String chartId) {
		ChartModel_v2 chart = new ChartModel_v2();
		try {
//			System.out.println(chartDao.findEntityById(ChartModel_v2.class, chartId));
//			chart = (ChartModel_v2) chartDao.findEntityById(ChartModel_v2.class, chartId);
			
			chart = chartDao.getChartNew(chartId);
			
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return chart;
	}

	public boolean deleteChartByObject(ChartModel_v2 chart) {
		boolean response = false;
		try {

			response = chartDao.updateEntity(ChartModel_v2.class, chart);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return response;
	}

	public List<ChartModel_v2> getChartByUserId(String userId) {

		List<ChartModel_v2> object = new ArrayList<ChartModel_v2>();
		try {

			object = (List<ChartModel_v2>) (Object) chartDao.getChartByUserId(userId);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return object;
	}

	public JSONArray getMigarationReport(String siteKey, String userId, String reportName) {

		JSONArray output = new JSONArray();
		List<Map<String, Object>> object = new ArrayList<Map<String, Object>>();
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			object = chartDao.getMigarationReport(siteKey, userId, reportName);
			for (Map<String, Object> s : object) {

				output.add(functions.convertGetMigarationReport(s));
			}

		} catch (Exception e) {
			e.printStackTrace();
			try {
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String ex = errors.toString();
				ExceptionHandlerMail.errorTriggerMail(ex);
			} catch (Exception e2) {
				// TODO: handle exception
			}
			

		}
		return output;
	}

	public Boolean eveitEntity(ChartModel_v2 chart) {
		try {

			chartDao.eveitEntity(chart);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);

		}
		return true;
	}

	public List<Object> getChartByCategoryId(String categoryId) {
		try {

			return chartDao.getChartByCategoryId(categoryId);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
			return null;
		}
	}

	public void getChartDatas(String siteKey, String sourceType) {
		try {
			List<Map<String, Object>> chatDatas = chartDao.getChartsBySiteKeyAndLogType(siteKey, sourceType);

			List<String> numbericalHeader = dataframeService.getReportNumericalHeaders("Discovery", sourceType,
					"Discovery", siteKey);

			// System.out.println("---chatDatas---------- >> " + chatDatas);

			// Set<String> chartParameters = new HashSet<String>();
			Map<String, Set<String>> chartInputs = new HashMap<String, Set<String>>();
			Map<String, JSONObject> chartObjects = new HashMap<String, JSONObject>();
			if (chatDatas != null && !chatDatas.isEmpty()) {
				for (Map<String, Object> chart : chatDatas) {

					JSONObject chartDetails = (JSONObject) jsonParser
							.parse(chart.get("chart_configuration").toString());
					JSONObject filterDetails = (JSONObject) jsonParser.parse(chart.get("filter_property").toString());

					if (!filterDetails.isEmpty()) {
						String componentType = (String) filterDetails.get("category");

						// System.out.println("--componentType---------- >> " + componentType);
						Set<String> chartParameters = new HashSet<>();
						if (!chartDetails.isEmpty()) {
							JSONArray xAxis = (JSONArray) chartDetails.get("xaxis");
							JSONArray yAxis = (JSONArray) chartDetails.get("yaxis");

							for (int i = 0; i < xAxis.size(); i++) {
								JSONObject xAxisData = (JSONObject) xAxis.get(i);
								chartParameters.add((String) xAxisData.get("label"));
							}

							for (int i = 0; i < yAxis.size(); i++) {
								JSONObject yAxisData = (JSONObject) yAxis.get(i);
								chartParameters.add((String) yAxisData.get("label"));
							}
						}

						if (componentType != null) {
							if (chartInputs.containsKey(componentType)) {
								Set<String> values = chartInputs.get(componentType);
								values.addAll(chartParameters);
								chartInputs.put(componentType, values);
							} else {
								Set<String> values = new HashSet<>();
								values.addAll(chartParameters);
								chartInputs.put(componentType, values);
							}
						}
						chartDetails.put("chartType", chart.get("chart_type"));
						chartDetails.put("siteKey", chart.get("site_key"));
						chartDetails.put("chartName", chart.get("chart_name"));
						chartObjects.put((String) chart.get("chart_id"), chartDetails);
					}

				}
			}
			// System.out.println("------------- >> " + chartInputs);

			if (!chartObjects.isEmpty()) {
				for (Map.Entry<String, Set<String>> chartInput : chartInputs.entrySet()) {
					String componentType = chartInput.getKey();
					System.out.println("--componentType--- : " + componentType);

					if (componentType.equalsIgnoreCase("Server")) { // local discovery table
						Set<String> chartParams = chartInputs.get("Server"); // .values().stream().findFirst().get();
						String query = formAggrigationQuery(siteKey, componentType, sourceType, chartParams);
						Map<String, Object> serverChartAggValues = chartDao.getServerDiscoveryChartAggValues(query);

						// System.out.println("------------- >> " + serverChartAggValues);

						for (Map.Entry<String, JSONObject> entry : chartObjects.entrySet()) {
							try {
								String chartId = entry.getKey();
								JSONObject chartConf = entry.getValue();
								String chartType = (String) chartConf.get("chartType");
								String siteKeyRef = (String) chartConf.get("siteKey");
								DashboardInputModel dashboardInputModel = new DashboardInputModel();
								dashboardInputModel.setSiteKey(siteKeyRef);
								dashboardInputModel.setChartId(chartId);

								DashboardChartDetails dashboardChartDetailsObj = dashBoardService
										.getDashboardChartDetailsBySiteKey(siteKeyRef, chartId);

								JSONObject dashboardChartDetails = dashBoardService
										.getDashboardChartDetails(dashboardInputModel);

								System.out.println(
										"-------------dashboardChartDetails-----------" + dashboardChartDetails);

								if (chartType.equalsIgnoreCase("pie")) {
									JSONArray columnArray = null;
									try {
										columnArray = (JSONArray) jsonParser.parse((String) chartConf.get("column"));
									} catch (Exception e) {
										columnArray = (JSONArray) chartConf.get("column");
									}

									JSONArray traceArr = new JSONArray();
									for (int i = 0; i < columnArray.size(); i++) {
										JSONObject columnObj = (JSONObject) columnArray.get(i);
										String fieldName = (String) columnObj.get("field");
										fieldName = fieldName.replaceAll("\\s", "_");
										String aggData = (String) serverChartAggValues.get(fieldName.trim());

										JSONArray aggDataArray = new JSONArray();
										if (aggData != null) {
											String[] strary = aggData.split(",");
											for (int j = 0; j < strary.length; j++) {
												aggDataArray.add(strary[j].trim());
											}
										}

										JSONObject traceObj = new JSONObject();
										if (numbericalHeader.contains((String) columnObj.get("field"))) {
											List<Float> arr = new ArrayList<>();
											for (int k = 0; k < aggDataArray.size(); k++) {
												arr.add(Float.parseFloat((String) aggDataArray.get(k)));
											}
											traceObj.put("values", arr);
										} else {
											traceObj.put("values", aggDataArray);
										}
										if (numbericalHeader.contains((String) columnObj.get("field"))) {
											List<Float> arr = new ArrayList<>();
											for (int k = 0; k < aggDataArray.size(); k++) {
												arr.add(Float.parseFloat((String) aggDataArray.get(k)));
											}
											traceObj.put("labels", arr);
										} else {
											traceObj.put("labels", aggDataArray);
										}
										if (numbericalHeader.contains((String) columnObj.get("field"))) {
											List<Float> arr = new ArrayList<>();
											for (int k = 0; k < aggDataArray.size(); k++) {
												arr.add(Float.parseFloat((String) aggDataArray.get(k)));
											}
											traceObj.put("text", arr);
										} else {
											traceObj.put("text", aggDataArray);
										}

										traceObj.put("type", chartType);
										traceObj.put("name", "");
										traceObj.put("textinfo", "value");
										traceObj.put("hoverinfo", "label+value");
										traceObj.put("legendinfo", "label+value");
										traceObj.put("textposition", "inside");
										JSONObject domainObj = new JSONObject();
										domainObj.put("column", 0);
										traceObj.put("domain", domainObj);
										traceArr.add(traceObj);

									}
									dashboardChartDetails.put("traces", traceArr);
									dashboardChartDetailsObj.setChartDetails(dashboardChartDetails.toJSONString());

								} else if (chartType.equalsIgnoreCase("bar") || chartType.equalsIgnoreCase("line")) {
									JSONArray xaxisArray = (JSONArray) chartConf.get("xaxis");
									JSONArray yaxisArray = (JSONArray) chartConf.get("yaxis");
									List<String> xaxisParams = new ArrayList<String>();
									List<String> yaxisParams = new ArrayList<String>();
									for (int i = 0; i < xaxisArray.size(); i++) {
										JSONObject xObj = (JSONObject) xaxisArray.get(i);
										xaxisParams.add((String) xObj.get("field"));
									}

									for (int i = 0; i < yaxisArray.size(); i++) {
										JSONObject yObj = (JSONObject) yaxisArray.get(i);
										yaxisParams.add((String) yObj.get("field"));
									}
									JSONArray traceArr = new JSONArray();
									for (int k = 0; k < yaxisParams.size(); k++) {
										JSONArray aggDataXaixsArray = new JSONArray();
										JSONArray aggDataYaixsArray = new JSONArray();
										JSONObject traceObj = new JSONObject();

										if (k < xaxisParams.size()) {
											String aggDataX = (String) serverChartAggValues
													.get(xaxisParams.get(k).replaceAll("\\s", "_"));

											if (aggDataX != null) {
												String[] straryXaixs = aggDataX.split(",");
												for (int j = 0; j < straryXaixs.length; j++) {
													aggDataXaixsArray.add(straryXaixs[j].trim());
												}
											}

											if (numbericalHeader.contains((String) xaxisParams.get(k))) {
												// traceObj.put("x", aggDataXaixsArray.toJSONString().replaceAll("\"",
												// "").replaceAll("\"\\[", "\\[").replaceAll("\\]\"", "\\]"));

												List<Float> arr = new ArrayList<>();
												for (int l = 0; l < aggDataXaixsArray.size(); l++) {
													arr.add(Float.parseFloat((String) aggDataXaixsArray.get(l)));
												}
												traceObj.put("x", arr);
											} else {
												traceObj.put("x", aggDataXaixsArray);
											}

										}
										String aggDataY = (String) serverChartAggValues
												.get(yaxisParams.get(k).replaceAll("\\s", "_"));

										if (aggDataY != null) {
											String[] straryYAxis = aggDataY.split(",");
											for (int j = 0; j < straryYAxis.length; j++) {
												aggDataYaixsArray.add(straryYAxis[j].trim());
											}

											if (numbericalHeader.contains((String) yaxisParams.get(k))) {
												List<Float> arr = new ArrayList<>();
												for (int l = 0; l < aggDataYaixsArray.size(); l++) {
													arr.add(Float.parseFloat((String) aggDataYaixsArray.get(l)));
												}

												traceObj.put("y", arr);
											} else {
												traceObj.put("y", aggDataYaixsArray);
											}
										}

										if (chartType.equalsIgnoreCase("line")) {
											traceObj.put("type", "scatter");
										} else {
											traceObj.put("type", chartType);
										}
										traceObj.put("name", yaxisParams.get(k));
										traceObj.put("width", "auto");
										traceObj.put("autosize", true);
										if (aggDataYaixsArray.size() >= 1) {
											traceObj.put("marker", new JSONObject());
										}
										traceArr.add(traceObj);

									}
									dashboardChartDetails.put("traces", traceArr);
									dashboardChartDetailsObj.setChartDetails(dashboardChartDetails.toJSONString());

								} else if (chartType.equalsIgnoreCase("table")) {
									JSONArray columnArray = null;
									try {
										columnArray = (JSONArray) jsonParser
												.parse((String) chartConf.get("tableColumns"));
									} catch (Exception e) {
										columnArray = (JSONArray) chartConf.get("tableColumns");
									}

									JSONArray traceValueArr = new JSONArray();
									for (int i = 0; i < columnArray.size(); i++) {
										JSONObject columnObj = (JSONObject) columnArray.get(i);
										String fieldName = (String) columnObj.get("field");
										fieldName = fieldName.replaceAll("\\s", "_");
										String aggData = (String) serverChartAggValues.get(fieldName.trim());
										JSONArray aggDataArray = new JSONArray();
										if (aggData != null) {
											String[] strary = aggData.split(",");
											for (int j = 0; j < strary.length; j++) {
												aggDataArray.add(strary[j].trim());
											}
										}

										if (numbericalHeader.contains((String) columnObj.get("field"))) {
											List<Float> arr = new ArrayList<>();
											for (int l = 0; l < aggDataArray.size(); l++) {
												arr.add(Float.parseFloat((String) aggDataArray.get(l)));
											}
											traceValueArr.add(arr);
										} else {
											traceValueArr.add(aggDataArray);
										}
									}

									List<LinkedHashMap> traceJson = (List<LinkedHashMap>) dashboardChartDetails
											.get("traces");
									LinkedHashMap cells = new LinkedHashMap();
									for (LinkedHashMap json : traceJson) {
										if (json.containsKey("cells")) {
											cells = (LinkedHashMap) json.get("cells");
											cells.put("values", traceValueArr);
										}
									}

									dashboardChartDetails.put("traces", traceJson);
									dashboardChartDetailsObj.setChartDetails(dashboardChartDetails.toJSONString());
								}

								chartDao.updateEntity(DashboardChartDetails.class, dashboardChartDetailsObj);
							} catch (Exception e) {
								e.printStackTrace();
								StringWriter errors = new StringWriter();
								e.printStackTrace(new PrintWriter(errors));
								String ex = errors.toString();
								ExceptionHandlerMail.errorTriggerMail(ex);
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

	}

	public boolean isNumeric(String s) {
		return s != null && s.matches("[-+]?\\d*\\.?\\d+");
	}

	private String formAggrigationQuery(String siteKey, String componentType, String sourceType,
			Set<String> chartParams) {
		String query = "";
		if (componentType.equalsIgnoreCase("Server")) {
			String selectQuery = String
					.join(",",
							chartParams
									.stream().map(param -> ("string_agg(a.\"" + param.replaceAll("\\s+", "_")
											+ "\", ',') as \"" + param.replaceAll("\\s+", "_") + "\""))
									.collect(Collectors.toList()));

			String whereQuery = String
					.join(",",
							chartParams
									.stream().map(param -> ("json_array_elements(data_temp::json) ->> '" + param
											+ "' as \"" + param.replaceAll("\\s+", "_") + "\""))
									.collect(Collectors.toList()));

			/// query = "select a.source_id, "+selectQuery+" from (select source_id,
			/// "+whereQuery+" from local_discovery where site_key='"+siteKey+"' and
			/// lower(source_type)='"+sourceType.toLowerCase()+"') a group by a.source_id";
			// query = "select a.source_id, "+selectQuery+" from (select source_id,
			/// "+whereQuery+" from local_discovery where site_key='"+siteKey+"' and
			/// lower(source_type)='"+sourceType.toLowerCase()+"') a group by a.source_id";
			query = "select " + selectQuery + " from (select source_id,  " + whereQuery
					+ " from local_discovery	where site_key='" + siteKey + "' and lower(source_type)='"
					+ sourceType.toLowerCase() + "') a";

			System.out.println("----------Query----------" + query);
		}
		return query;
	}

	public boolean deleteChartById(String chartId) {
		// TODO Auto-generated method stub
		return chartDao.deleteChartById(chartId);
	}

	/*
	 * public void refreshChart(String chartId, String siteKey) { try { String
	 * chartConfigQuery =
	 * "select from chartConfig where isActive = true and chartId = '" + chartId +
	 * "'"; JSONArray chartConfigArray =
	 * QueryExecutor.orientDBQueryExecution(chartConfigQuery); OrientGraphNoTx graph
	 * = utilities.getGraphApi(); if (!chartConfigArray.isEmpty()) {
	 * ZenfraJSONObject chartObj = (ZenfraJSONObject) chartConfigArray.get(0);
	 * String reportName = chartObj.get("reportName").toString().toLowerCase();
	 * String[] parsedReportName = reportName.split("_"); String siteKeyQuery =
	 * "select from Site where isActive=true"; JSONArray siteArr =
	 * QueryExecutor.orientDBQueryExecution(siteKeyQuery); for (int count = 0; count
	 * < siteArr.size(); count++) { ZenfraJSONObject siteObj = (ZenfraJSONObject)
	 * siteArr.get(count); if
	 * (!siteObj.get("siteKey").toString().equalsIgnoreCase(siteKey)) {
	 * processChartData(siteObj.get("siteKey").toString(), parsedReportName[1],
	 * redisUtil, graph, chartObj); } } } } catch (Exception e) {
	 * 
	 * }
	 * 
	 * }
	 * 
	 * private void processChartData(String siteKey, String deviceType, RedisUtil
	 * redisUtil, OrientGraphNoTx graph, ZenfraJSONObject chartObj) {
	 * 
	 * try { if () { //local discovery chart data list String chartId =
	 * chartObj.get("chartId").toString(); String chartType =
	 * chartObj.get("chartType").toString(); String chartSite =
	 * chartObj.get("siteKey").toString();
	 * 
	 * HashMap<?, ?> chartObject = (HashMap) chartObj.get("chartConfiguration");
	 * JSONObject chartConfig = getChartConfig(chartObject);
	 * 
	 * String chartConfigQuery =
	 * "select from dashBoardChartDetails where isActive = true and chartId = '" +
	 * chartId + "' and siteKey='" + chartSite + "'"; JSONArray chartConfigArr =
	 * QueryExecutor.orientDBQueryExecution(chartConfigQuery); if
	 * (!chartConfigArr.isEmpty()) { JSONObject chartDetails = new JSONObject();
	 * 
	 * ZenfraJSONObject chartConfigObj = (ZenfraJSONObject) chartConfigArr.get(0);
	 * JSONArray chartDet = new JSONArray();
	 * chartDet.add(chartConfigObj.get("chartDetails")); chartDetails =
	 * getChartDetails(chartDet); if (!chartDetails.isEmpty()) { if
	 * (chartType.equalsIgnoreCase("bar") || chartType.equalsIgnoreCase("line")) {
	 * List<String> yaxisLst = (List<String>) chartConfig.get("yaxis");
	 * List<Map<String, Object>> chartDataLst =
	 * generateBarAndLineChart(chartConfig.get("xaxis").toString(), yaxisLst,
	 * siteKey, deviceType); JSONArray traceArr = new JSONArray(); for (int k = 0; k
	 * < chartDataLst.size(); k++) { Map<String, Object> chartData = (Map<String,
	 * Object>) chartDataLst.get(k); if (!chartData.isEmpty()) { JSONObject traceObj
	 * = new JSONObject(); traceObj.put("x", chartData.get("x")); traceObj.put("y",
	 * chartData.get("y")); if (chartType.equalsIgnoreCase("line")) {
	 * traceObj.put("type", "scatter"); } else { traceObj.put("type", chartType); }
	 * traceObj.put("name", chartData.get("name").toString()); traceObj.put("width",
	 * "auto"); traceObj.put("autosize", true); if (yaxisLst.size() >= 1) {
	 * traceObj.put("marker", new JSONObject()); } traceArr.add(traceObj); } }
	 * chartDetails.replace("traces", chartDetails.get("traces"), traceArr); } else
	 * { Map<String, Object> chartData =
	 * generatePieChart(chartConfig.get("column").toString(), siteKey, deviceType);
	 * if (!chartData.isEmpty()) { JSONArray traceArr = new JSONArray(); JSONObject
	 * traceObj = new JSONObject(); traceObj.put("values", chartData.get("values"));
	 * traceObj.put("labels", chartData.get("labels")); traceObj.put("type",
	 * chartType); traceObj.put("name", ""); traceObj.put("textinfo", "value");
	 * traceObj.put("text", chartData.get("labels")); traceObj.put("hoverinfo",
	 * "label+value"); traceObj.put("legendinfo", "label+value");
	 * traceObj.put("textposition", "inside"); JSONObject domainObj = new
	 * JSONObject(); domainObj.put("column", 0); traceObj.put("domain", domainObj);
	 * traceArr.add(traceObj);
	 * 
	 * chartDetails.replace("traces", chartDetails.get("traces"), traceArr); } }
	 * 
	 * JSONObject jsonObject = new JSONObject(); jsonObject.put("chartId", chartId);
	 * jsonObject.put("siteKey", siteKey); jsonObject.put("chartDetails",
	 * chartDetails); jsonObject.put("favouriteId", ""); jsonObject.put("isActive",
	 * true);
	 * 
	 * if (chartSite.equalsIgnoreCase(siteKey)) { Iterable<Vertex> vertexList =
	 * graph.command(new OCommandSQL(chartConfigQuery)).execute(); for (Vertex
	 * currentVertex : vertexList) { currentVertex.setProperty("chartDetails",
	 * chartDetails); } } else { OrientVertex obj =
	 * graph.addVertex("class:dashBoardChartDetails", jsonObject); } graph.commit();
	 * } } } } catch (Exception e) {
	 * logger.error("Exception in Processing Chart Data ", e); e.printStackTrace();
	 * StringWriter errors = new StringWriter(); e.printStackTrace(new
	 * PrintWriter(errors)); String ex = errors.toString();
	 * ExceptionHandlerMail.errorTriggerMail(ex); }
	 * logger.info("Processing Chart Data Ends"); }
	 */

}
