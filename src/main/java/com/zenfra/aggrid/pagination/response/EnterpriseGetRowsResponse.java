package com.zenfra.aggrid.pagination.response;

import java.util.List;
import java.util.Map;

public class EnterpriseGetRowsResponse {
    private List<Map<String, Object>> data;
    private int lastRow;
    private int totalCount;
    private List<String> secondaryColumnFields;

    public EnterpriseGetRowsResponse() { }

    public EnterpriseGetRowsResponse(List<Map<String, Object>> data, int lastRow, List<String> secondaryColumnFields, int totalCount) {
        this.data = data;
        this.lastRow = lastRow;
        this.secondaryColumnFields = secondaryColumnFields;
        this.totalCount = totalCount;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public int getLastRow() {
        return lastRow;
    }

    public void setLastRow(int lastRow) {
        this.lastRow = lastRow;
    }

    public List<String> getSecondaryColumnFields() {
        return secondaryColumnFields;
    }

    public void setSecondaryColumns(List<String> secondaryColumnFields) {
        this.secondaryColumnFields = secondaryColumnFields;
    }

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

}
