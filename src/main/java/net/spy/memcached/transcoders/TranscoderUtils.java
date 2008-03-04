// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.transcoders;

import net.spy.memcached.CachedData;

/**
 * Utility class for transcoding Java types.
 */
public final class TranscoderUtils {
	/**
	 * Hash any integer (positive or negative) into a value safe for
	 * use as a Memcached flags value, an unsigned 32-bit value.
	 *
	 * @param i any integer
	 * @return a non-negative integer
	 */
	public static int hashForFlags(int i) {
		// Because Math.abs(java.lang.Integer.MIN_VALUE) is equal to
		// java.lang.Integer.MIN_VALUE and MAX_VALUE < -MIN_VALUE,
		// this will always return a non-negative number.
		return Math.abs(i % java.lang.Integer.MAX_VALUE);
	}

	/**
	 * Hash any long (positive or negative) into a value safe for
	 * use as a Memcached flags value, an unsigned 32-bit value.
	 *
	 * @param l any long
	 * @return a non-negative integer
	 */
	public static int hashForFlags(long l) {
		// Because Math.abs(java.lang.Long.MIN_VALUE) is equal to
		// java.lang.Long.MIN_VALUE and MAX_VALUE < -MIN_VALUE,
		// this will always return a non-negative number.
		return Math.abs((int)(l % java.lang.Integer.MAX_VALUE));
	}

	public static byte[] encodeNum(long l, int maxBytes) {
		byte[] rv=new byte[maxBytes];
		for(int i=0; i<rv.length; i++) {
			int pos=rv.length-i-1;
			rv[pos]=(byte) ((l >> (8 * i)) & 0xff);
		}
		int firstNonZero=0;
		for(;firstNonZero<rv.length && rv[firstNonZero]==0; firstNonZero++) {
			// Just looking for what we can reduce
		}
		if(firstNonZero > 0) {
			byte[] tmp=new byte[rv.length - firstNonZero];
			System.arraycopy(rv, firstNonZero, tmp, 0, rv.length-firstNonZero);
			rv=tmp;
		}
		return rv;
	}

	public static byte[] encodeLong(long l) {
		return encodeNum(l, 8);
	}

	public static long decodeLong(byte[] b) {
		long rv=0;
		for(byte i : b) {
			rv = (rv << 8) | (i<0?256+i:i);
		}
		return rv;
	}

	public static byte[] encodeInt(int in) {
		return encodeNum(in, 4);
	}

	public static int decodeInt(byte[] in) {
		assert in.length <= 4
			: "Too long to be an int (" + in.length + ") bytes";
		return (int)decodeLong(in);
	}

	public static byte[] encodeByte(byte in) {
		return new byte[]{in};
	}

	public static byte decodeByte(byte[] in) {
		assert in.length <= 1 : "Too long for a byte";
		byte rv=0;
		if(in.length == 1) {
			rv=in[0];
		}
		return rv;
	}

	public static byte[] encodeBoolean(boolean b) {
		byte[] rv=new byte[1];
		rv[0]=(byte)(b?'1':'0');
		return rv;
	}

	public static boolean decodeBoolean(byte[] in) {
		assert in.length == 1 : "Wrong length for a boolean";
		return in[0] == '1';
	}

}
