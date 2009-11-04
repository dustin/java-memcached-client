package net.spy.memcached;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.transcoders.Transcoder;

/**
 * This interface is provided as a helper for testing clients of the MemcachedClient.
 */
public interface MemcachedClientIF {
	/**
	 * Maximum supported key length.
	 */
	int MAX_KEY_LENGTH = 250;

	Collection<SocketAddress> getAvailableServers();

	Collection<SocketAddress> getUnavailableServers();

	Transcoder<Object> getTranscoder();

	NodeLocator getNodeLocator();

	Future<Boolean> append(long cas, String key, Object val);

	<T> Future<Boolean> append(long cas, String key, T val,
			Transcoder<T> tc);

	Future<Boolean> prepend(long cas, String key, Object val);

	<T> Future<Boolean> prepend(long cas, String key, T val,
			Transcoder<T> tc);

	<T> Future<CASResponse> asyncCAS(String key, long casId, T value,
					Transcoder<T> tc);

	Future<CASResponse> asyncCAS(String key, long casId, Object value);

	<T> CASResponse cas(String key, long casId, T value,
			Transcoder<T> tc) throws OperationTimeoutException;

	CASResponse cas(String key, long casId, Object value)
				throws OperationTimeoutException;

	<T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc);

	Future<Boolean> add(String key, int exp, Object o);

	<T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc);

	Future<Boolean> set(String key, int exp, Object o);

	<T> Future<Boolean> replace(String key, int exp, T o,
		Transcoder<T> tc);

	Future<Boolean> replace(String key, int exp, Object o);

	<T> Future<T> asyncGet(String key, Transcoder<T> tc);

	Future<Object> asyncGet(String key);

	<T> Future<CASValue<T>> asyncGets(String key,
			Transcoder<T> tc);

	Future<CASValue<Object>> asyncGets(String key);

	<T> CASValue<T> gets(String key, Transcoder<T> tc)
		throws OperationTimeoutException;

	CASValue<Object> gets(String key) throws OperationTimeoutException;

	<T> T get(String key, Transcoder<T> tc)
		throws OperationTimeoutException;

	Object get(String key) throws OperationTimeoutException;

	<T> Future<Map<String, T>> asyncGetBulk(Collection<String> keys,
		Transcoder<T> tc);

	Future<Map<String, Object>> asyncGetBulk(Collection<String> keys);

	<T> Future<Map<String, T>> asyncGetBulk(Transcoder<T> tc,
		String... keys);

	Future<Map<String, Object>> asyncGetBulk(String... keys);

	<T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc)
		throws OperationTimeoutException;

	Map<String, Object> getBulk(Collection<String> keys)
			throws OperationTimeoutException;

	<T> Map<String, T> getBulk(Transcoder<T> tc, String... keys)
				throws OperationTimeoutException;

	Map<String, Object> getBulk(String... keys)
					throws OperationTimeoutException;

	Map<SocketAddress, String> getVersions();

	Map<SocketAddress, Map<String, String>> getStats();

	Map<SocketAddress, Map<String, String>> getStats(String prefix);

	long incr(String key, int by) throws OperationTimeoutException;

	long decr(String key, int by) throws OperationTimeoutException;

	long incr(String key, int by, long def, int exp)
		throws OperationTimeoutException;

	long decr(String key, int by, long def, int exp)
		throws OperationTimeoutException;

	Future<Long> asyncIncr(String key, int by);

	Future<Long> asyncDecr(String key, int by);

	long incr(String key, int by, long def)
		throws OperationTimeoutException;

	long decr(String key, int by, long def)
			throws OperationTimeoutException;

	Future<Boolean> delete(String key);

	Future<Boolean> flush(int delay);

	Future<Boolean> flush();

	void shutdown();

	boolean shutdown(long timeout, TimeUnit unit);

	boolean waitForQueues(long timeout, TimeUnit unit);

	boolean addObserver(ConnectionObserver obs);

	boolean removeObserver(ConnectionObserver obs);

	/**
	 * Get the set of SASL mechanisms supported by the servers.
	 *
	 * @return the union of all SASL mechanisms supported by the servers.
	 */
	Set<String> listSaslMechanisms();
}
