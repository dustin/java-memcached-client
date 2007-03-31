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
	 * CRC32_HASH as used by the perl API.  This will be more consistent
	 * both across multiple API users as well as java versions.
	 */
	CRC32_HASH;

	/**
	 * Compute the hash for the given key.
	 * @return a positive integer hash
	 */
	public int hash(String k) {
		int rv=0;
		switch(this) {
			case NATIVE_HASH:
				rv=k.hashCode();
				break;
			case CRC32_HASH:
				// return (crc32(shift) >> 16) & 0x7fff;
				CRC32 crc32=new CRC32();
				crc32.update(k.getBytes());
				rv=(int)(crc32.getValue() & 0xffffffff);
				rv=(rv >> 16) & 0x7fff;
				break;
			default: assert false;
		}
		return Math.abs(rv);
	}
}