package net.spy.memcached;

/**
 * A Map interface to memcached.
 *
 * <p>
 *   Do note that nothing that iterates over the map will work (such is
 *   memcached).  All iteration mechanisms will return empty iterators and
 *   such.
 * </p>
 */
public class CacheMap extends BaseCacheMap<Object> {

	/**
	 * Construct a CacheMap over the given MemcachedClient.
	 *
	 * @param c the client
	 * @param expiration the expiration to set for keys written to the cache
	 * @param prefix a prefix used to make keys in this map unique
	 */
	public CacheMap(MemcachedClientIF c, int expiration, String prefix) {
		super(c, expiration, prefix, c.getTranscoder());
	}

	/**
	 * Construct a CacheMap over the given MemcachedClient with no expiration.
	 *
	 * <p>
	 *  Keys written into this Map will only expire when the LRU pushes them
	 *  out.
	 * </p>
	 *
	 * @param c the client
	 * @param prefix a prefix used to make keys in this map unique
	 */
	public CacheMap(MemcachedClientIF c, String prefix) {
		this(c, 0, prefix);
	}

}
