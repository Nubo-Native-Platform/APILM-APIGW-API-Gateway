package com.nubons.nnp.api.gw.util;

import java.util.Base64;

/**
 * 
 * @author Gourab Guha
 *
 */
public class Base64Util {
	
	private Base64Util() {}
	
	public static String decode(String encodedStr) {
		return new String(Base64.getDecoder().decode(encodedStr));
	}
	
	public static String decode(byte[] encodedArr) {
		return new String(Base64.getDecoder().decode(encodedArr));
	}

	public static String encode(String plainStr) {
		return new String(Base64.getEncoder().encode(plainStr.getBytes()));
	}
}
