package com.att.ebig.sso;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.ebig.jpa.EbigUser;
import com.att.ebig.jpa.Leaf;
import com.att.ebig.jpa.ManagedApplication;
import com.att.ebig.sso.ctiv.IvSyncBase;
import com.att.ebig.ui.CookieValueMapper;
import com.att.ebig.ui.CurrentUser;

public class SecurityCookieMapper implements CookieValueMapper
{
    private static Logger logger = LogManager.getLogger(SecurityCookieMapper.class);

	public String getValue(CurrentUser usr, Leaf l)
	{
		EbigUser eu = usr.getUser();
		ManagedApplication domain = l.getSystem();
		String username = IvSyncBase.makeIVConnectString(eu, domain);
		Calendar now = new GregorianCalendar();
		Date dnow = now.getTime();
		updateCalToTimeout(now);
		Date expires = now.getTime();
		
		String hash = makeHash(username, dnow, expires);
		return username+"|"+dnow.getTime()+"|"+expires.getTime()+"|"+hash;
	}

	void updateCalToTimeout(Calendar now)
	{
		now.add(Calendar.MINUTE, Integer.getInteger("ebig.timeout.minutes", 30));
	}

	private final static char[] hexArray = "0123456789abcdef".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	String makeHash(String username, Date dnow, Date expires)
	{
		String joke = java.lang.System.getProperty("ebig.joke.of.day",
				"In Xanadu did Kubla Khan\n A stately pleasure-dome decree:");
		String yourString = joke+":"+username+":"+dnow.getTime()+":"+expires.getTime();
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			
			byte[] bytesOfMessage = yourString.getBytes("UTF-8");
			byte[] thedigest = md.digest(bytesOfMessage);
			
			String hashtext = toHexString(thedigest);
			
			//hashtext is hex of digested bytes of "yourString"
			
			md.reset();

			//Big integer conversion to hex can cause dropped data by removing leading zeros.
			BigInteger bigInt = new BigInteger(1, joke.getBytes("UTF-8"));			
			bytesOfMessage = (bigInt.toString(16)+hashtext).getBytes("UTF-8");
			thedigest = md.digest(bytesOfMessage);
			
			String ret = toHexString(thedigest);
			
			return ret ; 
			//return  is hex of digested bytes of joke + hashtext

		}
		catch (UnsupportedEncodingException e)
		{
			logger.error("failed to make security cookie", e);
		}
		catch (NoSuchAlgorithmException e)
		{
			logger.error("failed to make security cookie", e);
		}
		return null;
	}

	private String toHexString(byte[] thedigest)
	{
		BigInteger bigInt = new BigInteger(1,thedigest);
		//bring it out in hex. with dropped leading zeros
		return bigInt.toString(16);
		// without drop
		//return bytesToHex(thedigest);
	}

}
