package com.zenfra.controller;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

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
public class Sha3RsaController {

	@Autowired
	TrippleDes trippleDes;
	
	Cipher cipher; 
	DBUtils dbUtils;
	
	@SuppressWarnings({ "unchecked", "static-access", "rawtypes", "unused" })
	@RequestMapping(value = "/password-migration-rsa", method = RequestMethod.PUT)
	private Response passwordAes() throws Exception {
		 Signature sign = Signature.getInstance("SHA256withRSA");
	     KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
	     keyPairGen.initialize(2048);
	     KeyPair pair = keyPairGen.generateKeyPair();   
	     PublicKey publicKey = pair.getPublic();  
	     Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	     
	    Map<String, String> data = new HashMap();
		data = dbUtils.getPostgres();
		Response response = new Response();
		try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
				data.get("password")); Statement statement = connection.createStatement();) {	
			String updateQuery = null;
			String selectQuery = "select user_id, email, password from user_temp";
			ResultSet rs = statement.executeQuery(selectQuery);
			while (rs.next()) {	
//				System.out.println("---------------------------------------------------------------------------------------------------------------DES PASSWORD"+rs.getString("password"));
				
				String emailExisting = rs.getString("email");
				String emailReal = trippleDes.decrypt(emailExisting);
				
				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
				byte[] inputEmail = emailReal.getBytes();	
			    byte[] cipherTextEmail = cipher.doFinal();	
			    String encryptedRSAEmail =  new String(cipherTextEmail, "UTF8");
//			    System.out.println("Encrypted Data RSA: "+encryptedRSAEmail);
//			    System.out.println("Encrypted Data After toHex: "+ toHex(encryptedRSAEmail));
			    
			    cipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
			    byte[] decipheredTextEmail = cipher.doFinal(cipherTextEmail);
			    String decryptedRSAEmail =  new String(decipheredTextEmail);
			    System.out.println("Decypted Data: "+decryptedRSAEmail);
//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------				
				String passwordExisting = rs.getString("password");
				String passwordReal = trippleDes.decrypt(passwordExisting);
				
				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			    byte[] inputPassword = passwordReal.getBytes();	  
			    cipher.update(inputPassword);
			    byte[] cipherTextPassword = cipher.doFinal();	 
			    String encryptedRSAPassword =  new String(cipherTextPassword, "UTF8");
//			    System.out.println("Encrypted Data RSA: "+encryptedRSAPassword);
//			    System.out.println("Encrypted Data After toHex: "+ toHex(encryptedRSAPassword));
			    
			    cipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
			    byte[] decipheredTextPassword = cipher.doFinal(cipherTextPassword);
			    String decryptedRSAPassword =  new String(decipheredTextPassword);
			    System.out.println("Decypted Data: "+decryptedRSAPassword);
			    
			    try (Connection connection1 = DriverManager.getConnection(data.get("url"), data.get("userName"),
						data.get("password")); Statement statement1 = connection1.createStatement();) {
			    	updateQuery = "update user_temp set existing_email = '"+emailExisting+"',  existing_password = '"+passwordExisting+"', sha_email = '"+toHex(encryptedRSAEmail)+"', sha_password = '"+toHex(encryptedRSAPassword)+"' where user_id = '"+rs.getString("user_id")+"'";
					statement1.executeUpdate(updateQuery);;
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
	
	
	  public static String toHex(String arg) {
		    return String.format("%x", new BigInteger(1, arg.getBytes()));
		}
}
