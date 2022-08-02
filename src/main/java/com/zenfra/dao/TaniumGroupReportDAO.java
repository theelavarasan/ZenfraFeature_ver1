package com.zenfra.dao;

import static com.zenfra.aggrid.pagination.builder.EnterpriseResponseBuilder.createResponseServerSide;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zenfra.aggrid.pagination.builder.TaniumGroupReportQueryBuilder;
import com.zenfra.aggrid.pagination.response.EnterpriseGetRowsResponse;
import com.zenfra.dataframe.request.ColumnVO;
import com.zenfra.dataframe.request.ServerSideGetRowsRequest;
import com.zenfra.utils.CommonFunctions;

@Repository("taniumGroupReportDAO")
public class TaniumGroupReportDAO {
	
	@Autowired
	CommonFunctions utilities;

    private JdbcTemplate template;
    private TaniumGroupReportQueryBuilder queryBuilder;
    
    

    @Autowired
    public TaniumGroupReportDAO(JdbcTemplate template) {
        this.template = template;
        queryBuilder = new TaniumGroupReportQueryBuilder();
    }

    public EnterpriseGetRowsResponse getData(ServerSideGetRowsRequest request) {
        String tableName = "privillege_data"; // could be supplied in request as a lookup key?
        
        // first obtain the pivot values from the DB for the requested pivot columns
        //Map<String, List<String>> pivotValues = getPivotValues(request.getPivotCols());
        Map<String, List<String>> pivotValues = new HashMap<String, List<String>>();

        // generate sql
        String sql = queryBuilder.createSql(request, tableName, pivotValues);
        
        List<Map<String, Object>> rows = utilities.getDBDatafromJdbcTemplate(sql); //template.queryForList(sql);
        JSONArray resultArray = dataNormalize(rows);
        //System.out.println("!!!!! pagination data: " + rows);
        // create response with our results
        int rowCount = rows.isEmpty() ? 0 : getRowCount(rows.get(0));
        return createResponseServerSide(request, resultArray, pivotValues, rowCount);
    }

    private Map<String, List<String>> getPivotValues(List<ColumnVO> pivotCols) {
        return pivotCols.stream()
                .map(ColumnVO::getField)
                .collect(toMap(pivotCol -> pivotCol, this::getPivotValues, (a, b) -> a, LinkedHashMap::new));
    }

    private List<String> getPivotValues(String pivotColumn) {
        String sql = "SELECT DISTINCT %s FROM trade";
        return template.queryForList(format(sql, pivotColumn), String.class);
    }
    
    @SuppressWarnings("unchecked")
	private JSONArray dataNormalize(List<Map<String, Object>> rows) {
    	
    	JSONArray resultArray = new JSONArray();
    	JSONParser parser = new JSONParser();
    	try {
    		for(Map<String, Object> row : rows) {
    			JSONObject resultObject = new JSONObject();
    			List<String> keys = new ArrayList<>(row.keySet());
    			for(int i = 0; i < keys.size(); i++) {
    				if(keys.get(i).equalsIgnoreCase("data")) {
    					JSONArray dataArray = (JSONArray) parser.parse(row.get(keys.get(i)) == null ? "[]" : row.get(keys.get(i)).toString());
    					if(!dataArray.isEmpty()) {
    						JSONObject dataObject = (JSONObject) dataArray.get(0);
    						List<String> dataKeys = new ArrayList<>(dataObject.keySet());
        					for(int j = 0; j < dataObject.size(); j++) {
        						
        						resultObject.put(dataKeys.get(j), dataObject.get(dataKeys.get(j)));
        					}
    					}
    					
    				} /*else {
    					if(!keys.get(i).equalsIgnoreCase("row_count")) {
    						if(keys.get(i).equalsIgnoreCase("server_name")) {
    							resultObject.put("Server Data~Server Name", row.get(keys.get(i)));
    						} else if(keys.get(i).equalsIgnoreCase("source_id")) {
    							resultObject.put("Server Data~User Name", row.get(keys.get(i)));
    						} else {
    							resultObject.put("Server Data~" + keys.get(i), row.get(keys.get(i)));
    						}
    						
    					} 
    				}*/
    			}
    			if(!resultObject.isEmpty()) {
    				resultArray.add(resultObject);
    			}
    			
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return resultArray;
    }
    
    private int getRowCount(Map<String, Object> row) {
    	
    	int rowCount = 0;
    	try {
    		rowCount = Integer.parseInt(row.get("row_count").toString());
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return rowCount;
    	
    }
    
    private String formatDateStringToUtc(String value) {
		try {
			value = value.replaceAll("UTC", "").replaceAll("utc", "").trim();
			//System.out.println("!!!!! value: " + value);
			value = utilities.convertToUtc(value);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return value;
	}

}
