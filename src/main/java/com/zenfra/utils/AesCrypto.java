package com.zenfra.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
//import java.util.HashMap;
//import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AesCrypto {

	Cipher cipher; 
	static DBUtils dbUtils;
	
	private static SecretKeySpec secretKeySpec;
	private static byte[] key; 
//	private static String secretKey; 
	 public static void setKey(final String myKey) {
		    MessageDigest sha = null; 
		    try {
		      key = myKey.getBytes("UTF-8");
		      sha = MessageDigest.getInstance("SHA-256");
		      key = sha.digest(key);
		      key = Arrays.copyOf(key, 16);
		      secretKeySpec = new SecretKeySpec(key, "AES");
		    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
		      e.printStackTrace();
		    }
		  }
	
	 public String encrypt(final String strToEncrypt) {
	    	String secretKey = "c+al@gbp+*f)%=95opjcr=&d)zi%#1_llx%q1&8vc!o6xq!_";
		    try {
		      setKey(secretKey);
		      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
		      return Base64.getEncoder()
		        .encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		    } catch (Exception e) {
		      System.out.println("Error while encrypting: " + e.toString());
		    }
			return null;
		  
		  }
	 
	 public String decrypt(final String strToDecrypt) {
	    	String secretKey = "c+al@gbp+*f)%=95opjcr=&d)zi%#1_llx%q1&8vc!o6xq!_";
	    try {
	      setKey(secretKey);
	      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
	      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
	      return new String(cipher.doFinal(Base64.getDecoder()
	        .decode(strToDecrypt)));
	    } catch (Exception e) {
	      System.out.println("Error while decrypting: " + e.toString());
	    }
		return null;
	
	  }
	

	
//	public static String secretKey() {  
//		Map<String, String> data = new HashMap();
//		data = dbUtils.getPostgres();
//		try  (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
//				data.get("password")); Statement statement = connection.createStatement();) {
//			String query = "select * from zen_config where key_name = 'secretKey'";
//			ResultSet rs = statement.executeQuery(query);
//			while (rs.next()) {
//				secretKey = rs.getString("key_value");
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		System.out.println("-------------------------------SecretKey--------------------------"+secretKey);
//		return secretKey;		
//	}
	
}

