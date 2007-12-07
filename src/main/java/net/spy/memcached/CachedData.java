// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached;

import java.util.Arrays;

/**
 * Cached data with its attributes.
 */
public final class CachedData {

	private final int flags;
	private final byte[] data;

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
