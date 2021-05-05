package com.zenfra.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class NullAwareBeanUtilsBean{

	public static String[] getNullPropertyNames (Object target) {
		try {

		    final BeanWrapper src = new BeanWrapperImpl(target);
		    java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

		    Set<String> emptyNames = new HashSet<String>();
		    for(java.beans.PropertyDescriptor pd : pds) {
		        Object srcValue = src.getPropertyValue(pd.getName());
		        if (srcValue == null) {
		        	emptyNames.add(pd.getName());
		        }
		    }
		    String[] result = new String[emptyNames.size()];
		    return emptyNames.toArray(result);
		
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
