// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 6BAE472D-7E89-40F3-879D-6674EE2DB78B

package net.spy.memcached;

import java.util.Arrays;

/**
 * Cached data with its attributes.
 */
public class CachedData {

	private int flags=0;
	private byte[] data=null;

	/**
	 * Get a CachedData instance for the given flags and byte array.
	 * 
	 * @param f the flags
	 * @param d the data
	 */
	public CachedData(int f, byte[] d) {
		super();
		flags=f;
		data=d;
	}

	/**
	 * Get the stored data.
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Get the flags stored along with this value.
	 */
	public int getFlags() {
		return flags;
	}

	@Override
	public String toString() {
		return "{CachedData flags=" + flags + " data="
			+ Arrays.toString(data) + "}";
	}
}
