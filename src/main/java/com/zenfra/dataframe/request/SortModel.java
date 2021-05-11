package com.zenfra.dataframe.request;

import java.io.Serializable;
import java.util.Objects;

public class SortModel implements Serializable {

    private String colId;
    private String sort;

    public SortModel() {}

    public SortModel(String colId, String sort) {
        this.colId = colId.replace("\\s+", "_").toLowerCase();;
        this.sort = sort;
    }

    public String getColId() {
        return colId.replace("\\s+", "_").toLowerCase();
    }

    public void setColId(String colId) {
        this.colId = colId.replace("\\s+", "_").toLowerCase();
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
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