package com.zenfra.dataframe.response;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DataResult {
    private List<String> data;
    private long lastRow;
    private List<String> secondaryColumns;
    private long totalRecord;
   // private JSONObject unit_conv_details;
    

    public DataResult(List<String> data, long lastRow, List<String> secondaryColumns, long totalRecord) {
        this.data = data;
        this.lastRow = lastRow;
        this.secondaryColumns = secondaryColumns;
        this.totalRecord = totalRecord; 
       // this.unit_conv_details = unit_conv_details;
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    public long getLastRow() {
        return lastRow;
    }

    public void setLastRow(long lastRow) {
        this.lastRow = lastRow;
    }

    public List<String> getSecondaryColumns() {
        return secondaryColumns;
    }

    public void setSecondaryColumns(List<String> secondaryColumns) {
        this.secondaryColumns = secondaryColumns;
    }

	public long getTotalRecord() {
		return totalRecord;
	}

	public void setTotalRecord(long totalRecord) {
		this.totalRecord = totalRecord;
	}

	   
}