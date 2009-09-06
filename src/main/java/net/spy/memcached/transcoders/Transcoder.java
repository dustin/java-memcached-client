// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.transcoders;

import net.spy.memcached.CachedData;

/**
 * Transcoder is an interface for classes that convert between byte arrays and
 * objects for storage in the cache.
 */
public interface Transcoder<T> {

	/**
	 * Should the transcoder be run asyncronously.
     * @return True if the CachedData should be decoded Asyncronously
	 */
	boolean asyncDecode(CachedData d);

	/**
	 * Encode the given object for storage.
	 *
	 * @param o the object
	 * @return the CachedData representing what should be sent
	 */
	CachedData encode(T o);

	/**
	 * Decode the cached object into the object it represents.
	 *
	 * @param d the data
	 * @return the return value
	 */
	T decode(CachedData d);

	/**
	 * Get the maximum size of objects handled by this transcoder.
	 */
	int getMaxSize();
}
