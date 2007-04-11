package net.spy.memcached;

import java.util.zip.CRC32;

/**
 * Known hashing algorithms for locating a server for a key.
 */
public enum HashAlgorithm {

	/**
	 * Native hash (String.hashCode()).
	 */
	NATIVE_HASH,
	/**
	 * CRC32_HASH as used by the perl API. This will be more consistent both
	 * across multiple API users as well as java versions, but is mostly likely
	 * significantly slower.
	 */
	CRC32_HASH,
	/**
	 * FNV hashes are designed to be fast while maintaining a low collision
	 * rate. The FNV speed allows one to quickly hash lots of data while
	 * maintaining a reasonable collision rate.
	 * 
	 * @see http://www.isthe.com/chongo/tech/comp/fnv/
	 * @see http://en.wikipedia.org/wiki/Fowler_Noll_Vo_hash
	 */
	FNV_HASH;

	private static final long FNV1_64_INIT = 0xcbf29ce484222325L;
	private static final long FNV_64_PRIME = 0x100000001b3L;

	/**
	 * Compute the hash for the given key.
	 * 
	 * @return a positive integer hash
	 */
	public long hash(String k) {
		long rv = 0;
		switch (this) {
			case NATIVE_HASH:
				rv = k.hashCode();
				break;
			case CRC32_HASH:
				// return (crc32(shift) >> 16) & 0x7fff;
				CRC32 crc32 = new CRC32();
				crc32.update(k.getBytes());
				rv = (crc32.getValue() & 0xffffffff);
				rv = (rv >> 16) & 0x7fff;
				break;
			case FNV_HASH:
				// Thanks to pierre@demartines.com for the pointer
				rv = FNV1_64_INIT;
				int len = k.length();
				for (int i = 0; i < len; i++) {
					rv *= FNV_64_PRIME;
					rv ^= k.charAt(i);
				}
				break;
			default:
				assert false;
		}
		return Math.abs(rv);
	}
}
