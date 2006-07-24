// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 29FED6F7-422A-4E06-BBF4-D074CAA22661

package net.spy.memcached;

/**
 * Transcoder is an interface for classes that convert between byte arrays and
 * objects for storage in the cache.
 */
public interface Transcoder {

	/**
	 * Encode the given object for storage.
	 * 
	 * @param o the object
	 * @return the CachedData representing what should be sent
	 */
	CachedData encode(Object o);

	/**
	 * Decode the cached object into the object it represents.
	 * 
	 * @param d the data
	 * @return the return value
	 */
	Object decode(CachedData d);
}
