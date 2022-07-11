package com.zenfra.dataframe.response;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DataResult {
    private List<String> data;
    private long lastRow;
    private List<String> secondaryColumns;
    private long totalRecord;
    private List<String> countData;
   // private JSONObject unit_conv_details;
    


	public DataResult(List<String> data, long lastRow, List<String> secondaryColumns, long totalRecord, List<String> countData) {
        this.data = data;
        this.lastRow = lastRow;
        this.secondaryColumns = secondaryColumns;
        this.totalRecord = totalRecord; 
        this.countData = countData;
       // this.count = countData;
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

 

    public List<String> getCountData() {
		return countData;
	}

	public void setCountData(List<String> countData) {
		this.countData = countData;
	}
	

	   
}