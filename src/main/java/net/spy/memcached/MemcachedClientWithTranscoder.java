package net.spy.memcached;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import net.spy.memcached.transcoders.Transcoder;

/**
 * Extended MemcachedClient interface tat allows for a transcoder to specified
 * per request.
 */
public interface MemcachedClientWithTranscoder extends MemcachedClient {

	/**
	 * Set the default transcoder for managing the cache representations
	 * of objects going in and out of the cache.
	 */
	void setTranscoder(Transcoder<Object> tc);

	/**
	 * Get the default transcoder that's in use.
	 */
	Transcoder<Object> getTranscoder();

	/**
	 * Append to an existing value in the cache.
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be appended
	 * @param val the value to append
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future indicating success
	 */
	<T> Future<Boolean> append(long cas, String key, T val, Transcoder<T> tc);

	/**
	 * Prepend to an existing value in the cache.
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be prepended
	 * @param val the value to append
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future indicating success
	 */
	<T> Future<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc);

	/**
	 * Asynchronous CAS operation.
	 *
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param value the new value
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future that will indicate the status of the CAS
	 */
	<T> Future<CASResponse> asyncCAS(String key, long casId, T value,
			Transcoder<T> tc);

	/**
	 * Perform a synchronous CAS operation.
	 *
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param value the new value
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a CASResponse
	 * @throws OperationTimeoutException if global operation timeout is exceeded
	 */
	<T> CASResponse cas(String key, long casId, T value, Transcoder<T> tc)
			throws OperationTimeoutException;

	/**
	 * Add an object to the cache iff it does not exist already.
	 *
	 * <p>
	 * The <code>exp</code> value is passed along to memcached exactly as
	 * given, and will be processed per the memcached protocol specification:
	 * </p>
	 *
	 * <blockquote>
	 * <p>
	 * The actual value sent may either be
	 * Unix time (number of seconds since January 1, 1970, as a 32-bit
	 * value), or a number of seconds starting from current time. In the
	 * latter case, this number of seconds may not exceed 60*60*24*30 (number
	 * of seconds in 30 days); if the number sent by a client is larger than
	 * that, the server will consider it to be real Unix time value rather
	 * than an offset from current time.
	 * </p>
	 * </blockquote>
	 *
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 */
	<T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc);

	/**
	 * Set an object in the cache regardless of any existing value.
	 *
	 * <p>
	 * The <code>exp</code> value is passed along to memcached exactly as
	 * given, and will be processed per the memcached protocol specification:
	 * </p>
	 *
	 * <blockquote>
	 * <p>
	 * The actual value sent may either be
	 * Unix time (number of seconds since January 1, 1970, as a 32-bit
	 * value), or a number of seconds starting from current time. In the
	 * latter case, this number of seconds may not exceed 60*60*24*30 (number
	 * of seconds in 30 days); if the number sent by a client is larger than
	 * that, the server will consider it to be real Unix time value rather
	 * than an offset from current time.
	 * </p>
	 * </blockquote>
	 *
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 */
	<T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc);

	/**
	 * Replace an object with the given value iff there is already a value
	 * for the given key.
	 *
	 * <p>
	 * The <code>exp</code> value is passed along to memcached exactly as
	 * given, and will be processed per the memcached protocol specification:
	 * </p>
	 *
	 * <blockquote>
	 * <p>
	 * The actual value sent may either be
	 * Unix time (number of seconds since January 1, 1970, as a 32-bit
	 * value), or a number of seconds starting from current time. In the
	 * latter case, this number of seconds may not exceed 60*60*24*30 (number
	 * of seconds in 30 days); if the number sent by a client is larger than
	 * that, the server will consider it to be real Unix time value rather
	 * than an offset from current time.
	 * </p>
	 * </blockquote>
	 *
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 */
	<T> Future<Boolean> replace(String key, int exp, T o, Transcoder<T> tc);

	/**
	 * Get the given key asynchronously.
	 *
	 * @param key the key to fetch
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a future that will hold the return value of the fetch
	 */
	<T> Future<T> asyncGet(final String key, final Transcoder<T> tc);

	/**
	 * Gets (with CAS support) the given key asynchronously.
	 *
	 * @param key the key to fetch
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a future that will hold the return value of the fetch
	 */
	<T> Future<CASValue<T>> asyncGets(final String key, final Transcoder<T> tc);

	/**
	 * Gets (with CAS support) with a single key.
	 *
	 * @param key the key to get
	 * @param tc the transcoder to serialize and unserialize value
	 * @return the result from the cache and CAS id (null if there is none)
	 * @throws OperationTimeoutException if global operation timeout is exceeded
	 */
	<T> CASValue<T> gets(String key, Transcoder<T> tc)
			throws OperationTimeoutException;

	/**
	 * Get with a single key.
	 *
	 * @param key the key to get
	 * @param tc the transcoder to serialize and unserialize value
	 * @return the result from the cache (null if there is none)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	<T> T get(String key, Transcoder<T> tc) throws OperationTimeoutException;

	/**
	 * Asynchronously get a bunch of objects from the cache.
	 *
	 * @param keys the keys to request
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a Future result of that fetch
	 */
	<T> Future<Map<String, T>> asyncGetBulk(Collection<String> keys,
			final Transcoder<T> tc);

	/**
	 * Varargs wrapper for asynchronous bulk gets.
	 *
	 * @param tc the transcoder to serialize and unserialize value
	 * @param keys one more more keys to get
	 * @return the future values of those keys
	 */
	<T> Future<Map<String, T>> asyncGetBulk(Transcoder<T> tc, String... keys);

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param keys the keys
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	<T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc)
			throws OperationTimeoutException;

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param tc the transcoder to serialize and unserialize value
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	<T> Map<String, T> getBulk(Transcoder<T> tc, String... keys)
			throws OperationTimeoutException;

}