/**
 * Copied from http://code.google.com/p/jaolt/source/browse/trunk/src/de/shandschuh/jaolt/tools/BASE64.java?r=1146
 * jaolt - Java Auction Organisation, Listing Tool
 * Copyright (C) 2010 Stefan Handschuh
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version. 
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 */

// original: package de.shandschuh.jaolt.tools;
package net.zaczek.PPodCast.util;


public class Base64 {
	private static char[] TOCHAR = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
									'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
									'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};

	private static byte[] TOBYTE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
									62, // '+'
									0, 0, 0, 
									63, // '/'
									52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 0, 0, 0, 0, 0, 0, 0, // 0 .. 9
									0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // A .. Z
									0, 0, 0, 0, 0, 0, 
									26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51}; // a.. z
									
	public static String encode(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		
		int i = bytes.length;
		
		int k = i/3;

		for (int n = 0; n < k; n++) {
			if (n > 0 && n % 19 == 0) {
				builder.append('\n');
			}
			builder.append(convertToString(bytes[3*n], bytes[3*n+1], bytes[3*n+2]));
		}
		
		k = i % 3;
		if (k == 2) {
			char[] chars = convertToString(bytes[i-2], bytes[i-1], 0);
			
			chars[3] = '=';
			builder.append(chars);
		} else if (k == 1) {
			char[] chars = convertToString(bytes[i-1], 0, 0);
			
			chars[2] = '=';
			chars[3] = '=';
			builder.append(chars);
		}
		return builder.toString();
	}
	
	private static char[] convertToString(int b, int c, int d) {
		char[] result = new char[4];
		if (b < 0) {
			b += 256;
		}
		if (c < 0) {
			c += 256;
		}
		if (d < 0) {
			d += 256;
		}

		int f = d+(c+b*256)*256;

		result[3] = TOCHAR[f % 64];
		f /= 64;
		result[2] = TOCHAR[f % 64];
		f /= 64;
		result[1] = TOCHAR[f % 64];
		f /= 64;
		result[0] = TOCHAR[f % 64];
		
		return result;
	}

	public static byte[] decode(String base64) {
		return decode(base64.replace("\n", "").toCharArray());
	}
	
	private static byte[] decode(char[] base64) {
		int length = base64.length;
		
		if (length % 4 > 0) {
			throw new IllegalArgumentException();
		}
		
		int dummyCount = base64[length-2] == '=' ? 2 : (base64[length-1] == '=' ? 1 : 0);
		
		byte[] result = new byte[(length / 4)*3-dummyCount];
		
		int count = (length / 4) - 1;

		for (int n = 0; n < count; n++) {
			byte[] buffer = convertToByte(base64[4*n], base64[4*n+1], base64[4*n+2], base64[4*n+3]);
			
			result[3*n] = buffer[0];
			result[3*n+1] = buffer[1];
			result[3*n+2] = buffer[2];
		}
		if (dummyCount == 0) {
			byte[] buffer = convertToByte(base64[length-4], base64[length-3], base64[length-2], base64[length-1]);
			
			result[3*count] = buffer[0];
			result[3*count+1] = buffer[1];
			result[3*count+2] = buffer[2];
		} else if (dummyCount == 1) {
			byte[] buffer = convertToByte(base64[length-4], base64[length-3], base64[length-2], 'A');
			
			result[3*count] = buffer[0];
			result[3*count+1] = buffer[1];
		} else {
			byte[] buffer = convertToByte(base64[length-4], base64[length-3], 'A', 'A');
			
			result[3*count] = buffer[0];
		}
		return result;
	}
	
	private static byte[] convertToByte(char c, char d, char e, char f) {
		byte[] result = new byte[3];
		
		int g = TOBYTE[f] + (TOBYTE[e] + (TOBYTE[d] + TOBYTE[c]*64)*64)*64;

		result[2] = (byte) (g % 256);
		g /= 256;
		result[1] = (byte) (g % 256);
		g /= 256;
		result[0] = (byte) (g % 256);

		return result;
	}

}