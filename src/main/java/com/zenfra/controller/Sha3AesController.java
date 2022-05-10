package com.zenfra.controller;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.zenfra.model.Response;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.TrippleDes;


@CrossOrigin("*")
@RestController
@RequestMapping("/rest")
public class Sha3AesController {
	
	@Autowired
	TrippleDes trippleDes;
	
	Cipher cipher; //provides functionality for encryption and decryption
	DBUtils dbUtils;
	
	  private static SecretKeySpec secretKeySpec; //Constructs a secret key from the given byte array.
	  private static byte[] key; //material of the secret key

	  public static void setKey(final String myKey) {
	    MessageDigest sha = null; //provides the functionality of a message digest algorithm, such as SHA-1 or SHA-256
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
	
	
	  public static String encrypt(final String strToEncrypt) {
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

		  public static String decrypt(final String strToDecrypt) {
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
		  
	@SuppressWarnings({ "unchecked", "static-access", "rawtypes" })
	@RequestMapping(value = "/password-migration-aes", method = RequestMethod.PUT)
	private Response passwordAes() throws Exception {
//		 final String secretKey = "3744696tfgvbzw35maq2";
	    Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {	
			String updateQuery = null;
			String selectQuery = "select user_id, existing_email, existing_password from user_temp";
			ResultSet rs = statement.executeQuery(selectQuery);
			while (rs.next()) {	
//				System.out.println("---------------------------------------------------------------------------------------------------------------DES PASSWORD"+rs.getString("password"));
				String emailExisting = rs.getString("existing_email");
//				String emailReal = trippleDes.decrypt(emailExisting);
//				String emailAesEncrypt = encrypt(emailReal);
////				System.out.println("--------Encrypted AES Email-----------------"+emailAesEncrypt);
//				String emailAesDecrypt = decrypt(emailAesEncrypt);
//				System.out.println("--------Decrypted  AES Email------------------"+emailAesDecrypt);
//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------				
				String passwordExisting = rs.getString("existing_password");
//				String passwordReal = trippleDes.decrypt(passwordExisting);
//				String passwordAesEncrypt = encrypt(passwordReal);
////				System.out.println("--------Encrypted AES Password-----------------"+passwordAesEncrypt);
//				String passwordAesDecrypt = decrypt(passwordAesEncrypt);
//				System.out.println("--------Decrypted AES Password------------------"+passwordAesDecrypt);
				
				try (Connection connection1 = DriverManager.getConnection(data.get("url"), data.get("userName"),
						data.get("password")); Statement statement1 = connection1.createStatement();) {
//					updateQuery = "update user_temp set email = '"+emailAesEncrypt+"',  password = '"+passwordAesEncrypt+"', aes_email = '"+emailAesEncrypt+"', aes_password = '"+passwordAesEncrypt+"' where user_id = '"+rs.getString("user_id")+"'";
					updateQuery = "update user_temp set email = '"+emailExisting+"',  password = '"+passwordExisting+"' where user_id = '"+rs.getString("user_id")+"'";
					statement1.executeUpdate(updateQuery);
					System.out.println("----------------------Update Query-------------------------"+updateQuery);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			response.setResponseCode(200);
			response.setResponseMsg("Success");
		} catch (Exception e) {
			e.printStackTrace();
			response.setResponseCode(500);
			response.setResponseMsg("failure");
		}
		return response;
	}	
}
