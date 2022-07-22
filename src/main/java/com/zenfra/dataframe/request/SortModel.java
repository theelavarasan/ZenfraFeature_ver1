package com.zenfra.dataframe.request;

import java.io.Serializable;
import java.util.Objects;

public class SortModel implements Serializable {

    private String colId;
    private String sort;
    private String actualColId;


    public SortModel() {}

    public SortModel(String colId, String sort) {
    	this.actualColId = colId;
        this.colId = colId.replace("\\s+", "_").toLowerCase();
        this.sort = sort;
    }

    public String getColId() {
        return colId.replace("\\s+", "_").toLowerCase();
    }

    public void setColId(String colId) {
    	this.actualColId = colId;
        this.colId = colId.replace("\\s+", "_").toLowerCase();
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
    
    

    public String getActualColId() {
		return actualColId;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortModel sortModel = (SortModel) o;
        return Objects.equals(colId, sortModel.colId) &&
                Objects.equals(sort, sortModel.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(colId, sort);
    }
}