package com.zenfra.dataframe.filter;

import java.util.List;

import org.json.simple.JSONObject;

public class TextColumnFilter extends ColumnFilter {
    private String value;
    
    private String type;
    
    private String filter;
  

    
    public TextColumnFilter(String value, String type, String filter) {
        this.value = value;//.replaceAll("\\s+", "_").toLowerCase();
        this.type = type;//.replaceAll("\\s+", "_").toLowerCase();
        this.filter = filter;//.replaceAll("\\s+", "_").toLowerCase();
    }

    public String getValue() {
        return value;
    } 
    
    public String getFilterType() {
        return filterType;
    }
	
	
	private JSONObject condition1;
	
	private JSONObject condition2;
	
	private String operator;

    public TextColumnFilter() {}

    public TextColumnFilter(JSONObject condition1, JSONObject condition2, String operator) {
        this.condition2 = condition2;
        this.condition1 = condition1;
        this.operator = operator;
    }

	public JSONObject getCondition1() {
		return condition1;
	}

	public void setCondition1(JSONObject condition1) {
		this.condition1 = condition1;
	}

	public JSONObject getCondition2() {
		return condition2;
	}

	public void setCondition2(JSONObject condition2) {
		this.condition2 = condition2;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

   
    
     public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
    public String toString() {
    	 String str = "";
    	 if(condition1 != null) {
    		 str =  "condition1" + " : " +condition1.toJSONString() + " : " + condition2.toJSONString() + " : " + operator;
    	 } else {
    		 str =  "defaultFilter" + " : " +getFilterType() + " : " + type + " : " + filter;
    	 }
    	 
      return str;
    } 
    
}
