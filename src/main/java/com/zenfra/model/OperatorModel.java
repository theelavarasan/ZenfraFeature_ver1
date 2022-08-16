package com.zenfra.model;

import java.util.HashMap;
import java.util.Map;

public class OperatorModel {
	
	static Map<String, String> operators = new HashMap<String, String>();
	
	public OperatorModel() {
    	operators.put("equals", "=");
    	operators.put("contains", "ilike");
    	operators.put("notContains", "not ilike");
    	operators.put("startsWith", "ilike");
    	operators.put("endsWith", "ilike");
    	operators.put("blank", "=");
    	operators.put("notBlank", "<>");
    	operators.put("Blanks", "=");
    	operators.put("Not Blanks", "<>");
    	operators.put("notEqual", "<>");
    	operators.put("lessThan", "<");
    	operators.put("lessThanOrEqual", "<=");
    	operators.put("greaterThan", ">");
    	operators.put("greaterThanOrEqual", ">=");
    	operators.put("inRange", "between");
    } 
	
	public static String getOperator(String key) {
		return operators.get(key);
	}

}
