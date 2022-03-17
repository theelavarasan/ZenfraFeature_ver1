package com.zenfra.utils;

import com.zenfra.model.ZKConstants;
import com.zenfra.model.ZKModel;

public class CommonUtils {
	
	public static String checkPortNumberForWildCardCertificate(String url) {
		try {
			
			if(ZKModel.getProperty(ZKConstants.WILD_CARD_CERTIFICATE).equalsIgnoreCase("true")) {
				url = url.replaceFirst(":\\d+", "");
            }			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return url;
	}

}

