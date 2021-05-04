package com.zenfra.dataframe.filter;

import org.json.simple.JSONObject;

public class NumberColumnFilter extends ColumnFilter {
    private String type;
    private Integer filter;
    private Integer filterTo;
   

    public NumberColumnFilter(String type, Integer filter, Integer filterTo) {
        this.type = type;
        this.filter = filter;
        this.filterTo = filterTo;
    }

   

    public String getType() {
        return type;
    }

    public Integer getFilter() {
        return filter;
    }

    public Integer getFilterTo() {
        return filterTo;
    } 
	
	
   private JSONObject condition1;
	
	private JSONObject condition2;
	
	private String operator;

    public NumberColumnFilter() {}

    public NumberColumnFilter(JSONObject condition1, JSONObject condition2, String operator) {
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
	
	 public String getFilterType() {
	        return filterType;
	    }

	 @Override
	    public String toString() {
		 String str = "";
		 if(condition1 != null) {
    		 str =   "condition1" + " : " + condition1.toJSONString() + " : " + condition2.toJSONString() + " : " + operator;
    	 }else {
    		 str =  "defaultFilter" + ":"+ getFilterType();
    	 }
    	 
      return str;
	    } 
	    
} 
