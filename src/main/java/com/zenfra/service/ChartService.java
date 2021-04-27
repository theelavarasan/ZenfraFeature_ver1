package com.zenfra.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zenfra.dao.ChartDAO;
import com.zenfra.model.ChartModel_v2;
import com.zenfra.utils.CommonFunctions;

@Service
public class ChartService {

	@Autowired
	CommonFunctions functions;
	
	
	@Autowired
	ChartDAO chartDao;
	
	public boolean saveChart(ChartModel_v2 chart) {
		boolean response=false;
		try {
			
			/*Map<String, Object> params = new HashMap<String, Object>();
				params.put(":chart_id", chart.getChartId());
				params.put(":chart_configuration",chart.getChartConfiguration());
				params.put(":is_dashboard", chart.isDashboard());
				params.put(":site_key", chart.getSiteKey());
				params.put(":report_name", chart.getReportName());
				params.put(":chart_name", chart.getChartName());
				params.put(":filter_property", chart.getFilterProperty());			
				params.put(":chart_type", chart.getChartType());
				params.put(":created_time", chart.getCreatedTime());
				params.put(":update_time", chart.getUpdateTime());	
				params.put(":is_active", chart.isActive());	
				params.put(":user_id", chart.getUserId());	
			
				responce=chartDao.SaveChart(params);	*/
			
			response=chartDao.saveEntity(ChartModel_v2.class, chart);
		} catch (Exception e) {
			e.printStackTrace();			
		}
		return response;
	}
	
	
	public ChartModel_v2 getChartByChartId(String chartId) {
		ChartModel_v2 chart=new ChartModel_v2();
		try {
			
			System.out.println(chartDao.findEntityById(ChartModel_v2.class, chartId));
			chart=(ChartModel_v2) chartDao.findEntityById(ChartModel_v2.class, chartId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chart;
	}


	public boolean deleteChartByObject(ChartModel_v2 chart) {
		boolean response=false;
		try {
			
			response=chartDao.deleteByEntity(chart);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return response;
	}


	public List<ChartModel_v2> getChartByUserId(String userId) {
		
		List<ChartModel_v2> object=new ArrayList<ChartModel_v2>();
		try {
			
			object=(List<ChartModel_v2>) (Object) chartDao.getChartByUserId(userId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return object;
	}
}
