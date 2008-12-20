// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.SpyObject;
import net.spy.memcached.cas.CASResponse;
import net.spy.memcached.cas.CASValue;
import net.spy.memcached.io.MemcachedHighLevelIO;
import net.spy.memcached.nodes.NodeLocator;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.memcached.util.KeyUtil;

/**
 * Client to a memcached server.
 *
 * <h2>Basic usage</h2>
 *
 * <pre>
 *	MemcachedClient c=new MemcachedClient(
 *		new InetSocketAddress("hostname", portNum));
 *
 *	// Store a value (async) for one hour
 *	c.set("someKey", 3600, someObject);
 *	// Retrieve a value.
 *	Object myObject=c.get("someKey");
 *	</pre>
 *
 *	<h2>Advanced Usage</h2>
 *
 *	<p>
 *	 MemcachedClient may be processing a great deal of asynchronous messages or
 *	 possibly dealing with an unreachable memcached, which may delay processing.
 *	 If a memcached is disabled, for example, MemcachedConnection will continue
 *	 to attempt to reconnect and replay pending operations until it comes back
 *	 up.  To prevent this from causing your application to hang, you can use
 *	 one of the asynchronous mechanisms to time out a request and cancel the
 *	 operation to the server.
 *	</p>
 *
 *	<pre>
 *	// Get a memcached client connected to several servers
 *	MemcachedClient c=new MemcachedClient(
 *		AddrUtil.getAddresses("server1:11211 server2:11211"));
 *
 *	// Try to get a value, for up to 5 seconds, and cancel if it doesn't return
 *	Object myObj=null;
 *	Future&lt;Object&gt; f=c.asyncGet("someKey");
 *	try {
 *		myObj=f.get(5, TimeUnit.SECONDS);
 *	} catch(TimeoutException e) {
 *		// Since we don't need this, go ahead and cancel the operation.  This
 *		// is not strictly necessary, but it'll save some work on the server.
 *		f.cancel();
 *		// Do other timeout related stuff
 *	}
 * </pre>
 */
public class MemcachedClientImpl extends SpyObject
	implements MemcachedClientWithTranscoder {

	private final MemcachedHighLevelIO conn;
    private final long operationTimeout;

	final Transcoder<Object> transcoder;

	/**
	 * Get a memcache client operating on the specified memcached locations.
	 *
	 * @param ia the memcached locations
	 * @throws IOException if connections cannot be established
	 */
	public MemcachedClientImpl(InetSocketAddress... ia) throws IOException {
		this(new DefaultConnectionFactory(), Arrays.asList(ia));
	}

	/**
	 * Get a memcache client over the specified memcached locations.
	 *
	 * @param addrs the socket addrs
	 * @throws IOException if connections cannot be established
	 */
	public MemcachedClientImpl(List<InetSocketAddress> addrs)
		throws IOException {
		this(new DefaultConnectionFactory(), addrs);
	}

	/**
	 * Get a memcache client over the specified memcached locations.
	 *
	 * @param bufSize read buffer size per connection (in bytes)
	 * @param addrs the socket addresses
	 * @throws IOException if connections cannot be established
	 */
	public MemcachedClientImpl(ConnectionFactory cf, List<InetSocketAddress> addrs)
		throws IOException {
		if(cf == null) {
			throw new NullPointerException("Connection factory required");
		}
		if(addrs == null) {
			throw new NullPointerException("Server list required");
		}
		if(addrs.isEmpty()) {
			throw new IllegalArgumentException(
				"You must have at least one server to connect to");
		}
		if(cf.getOperationTimeout() <= 0) {
			throw new IllegalArgumentException(
				"Operation timeout must be positive.");
		}
		transcoder=new SerializingTranscoder();
		conn=new MemcachedHighLevelIO(cf, addrs);
		operationTimeout = cf.getOperationTimeout();
	}

	/**
	 * Get a read-only wrapper around the node locator wrapping this instance.
	 */
	public NodeLocator getNodeLocator() {
		return conn.getLocator();
	}

	/**
	 * (internal use) Add a raw operation to a numbered connection.
	 * This method is exposed for testing.
	 *
	 * @param which server number
	 * @param op the operation to perform
	 * @return the Operation
	 */
	Operation addOp(final String key, final Operation op) {
		return conn.addOp(KeyUtil.getKeyBytes(key), op);
	}

	private <T> Future<Boolean> asyncStore(StoreType storeType, String key,
						   int exp, T value, Transcoder<T> tc) {
		CachedData co=tc.encode(value);
		return conn.asyncStore(storeType, KeyUtil.getKeyBytes(key), exp, co,
			operationTimeout);
	}

	private Future<Boolean> asyncStore(StoreType storeType,
			String key, int exp, Object value) {
		return asyncStore(storeType, key, exp, value, transcoder);
	}

	private <T> Future<Boolean> asyncCat(
			ConcatenationType catType, long cas, String key,
			T value, Transcoder<T> tc) {
		CachedData co=tc.encode(value);
		return conn.asyncCat(catType, cas,
			KeyUtil.getKeyBytes(key), co, operationTimeout);
	}


	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#append(long, java.lang.String, java.lang.Object)
	 */
	public Future<Boolean> append(long cas, String key, Object val) {
		return append(cas, key, val, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#append(long, java.lang.String, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<Boolean> append(long cas, String key, T val,
			Transcoder<T> tc) {
		return asyncCat(ConcatenationType.append, cas, key, val, tc);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#prepend(long, java.lang.String, java.lang.Object)
	 */
	public Future<Boolean> prepend(long cas, String key, Object val) {
		return prepend(cas, key, val, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#prepend(long, java.lang.String, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<Boolean> prepend(long cas, String key, T val,
			Transcoder<T> tc) {
		return asyncCat(ConcatenationType.prepend, cas, key, val, tc);
	}

    /* (non-Javadoc)
     * @see net.spy.memcached.MemcachedClientWithTranscoder#asyncCAS(java.lang.String, long, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
     */
    public <T> Future<CASResponse> asyncCAS(String key, long casId, T value,
            Transcoder<T> tc) {
        return asyncCAS(key, casId, 0, value, tc);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncCAS(java.lang.String, long, int, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<CASResponse> asyncCAS(String key, long casId, int exp, T value,
			Transcoder<T> tc) {
		return conn.asyncCAS(KeyUtil.getKeyBytes(key), casId, exp,
				tc.encode(value), operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncCAS(java.lang.String, long, java.lang.Object)
	 */
	public Future<CASResponse> asyncCAS(String key, long casId, Object value) {
		return asyncCAS(key, casId, value, transcoder);
	}

    /* (non-Javadoc)
     * @see net.spy.memcached.MemcachedClientWithTranscoder#cas(java.lang.String, long, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
     */
    public <T> CASResponse cas(String key, long casId, T value,
            Transcoder<T> tc) {
        return cas(key, casId, 0, value, tc);
    }

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#cas(java.lang.String, long, int, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> CASResponse cas(String key, long casId, int exp, T value,
			Transcoder<T> tc) {
		try {
			return asyncCAS(key, casId, exp, value, tc).get(operationTimeout,
					TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for value", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Exception waiting for value", e);
		} catch (TimeoutException e) {
			throw new OperationTimeoutException("Timeout waiting for value", e);
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#cas(java.lang.String, long, java.lang.Object)
	 */
	public CASResponse cas(String key, long casId, Object value) {
		return cas(key, casId, value, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#add(java.lang.String, int, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc) {
		return asyncStore(StoreType.add, key, exp, o, tc);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#add(java.lang.String, int, java.lang.Object)
	 */
	public Future<Boolean> add(String key, int exp, Object o) {
		return asyncStore(StoreType.add, key, exp, o, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#set(java.lang.String, int, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc) {
		return asyncStore(StoreType.set, key, exp, o, tc);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#set(java.lang.String, int, java.lang.Object)
	 */
	public Future<Boolean> set(String key, int exp, Object o) {
		return asyncStore(StoreType.set, key, exp, o, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#replace(java.lang.String, int, java.lang.Object, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<Boolean> replace(String key, int exp, T o,
		Transcoder<T> tc) {
		return asyncStore(StoreType.replace, key, exp, o, tc);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#replace(java.lang.String, int, java.lang.Object)
	 */
	public Future<Boolean> replace(String key, int exp, Object o) {
		return asyncStore(StoreType.replace, key, exp, o, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#asyncGet(java.lang.String, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<T> asyncGet(final String key, final Transcoder<T> tc) {
		return conn.asyncGet(KeyUtil.getKeyBytes(key), tc, operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncGet(java.lang.String)
	 */
	public Future<Object> asyncGet(final String key) {
		return asyncGet(key, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#asyncGets(java.lang.String, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<CASValue<T>> asyncGets(final String key,
			final Transcoder<T> tc) {
		return conn.asyncGets(KeyUtil.getKeyBytes(key), tc, operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncGets(java.lang.String)
	 */
	public Future<CASValue<Object>> asyncGets(final String key) {
		return asyncGets(key, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#gets(java.lang.String, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> CASValue<T> gets(String key, Transcoder<T> tc) {
		try {
			return asyncGets(key, tc).get(
				operationTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for value", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Exception waiting for value", e);
		} catch (TimeoutException e) {
			throw new OperationTimeoutException("Timeout waiting for value", e);
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#gets(java.lang.String)
	 */
	public CASValue<Object> gets(String key) {
		return gets(key, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#get(java.lang.String, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> T get(String key, Transcoder<T> tc) {
		try {
			return asyncGet(key, tc).get(
				operationTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for value", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Exception waiting for value", e);
		} catch (TimeoutException e) {
			throw new OperationTimeoutException("Timeout waiting for value", e);
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#get(java.lang.String)
	 */
	public Object get(String key) {
		return get(key, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#asyncGetBulk(java.util.Collection, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Future<Map<String, T>> asyncGetBulk(Collection<String> keys,
		final Transcoder<T> tc) {
		return conn.asyncGetBulk(KeyUtil.getKeyBytes(keys), tc,
			operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncGetBulk(java.util.Collection)
	 */
	public Future<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
		return asyncGetBulk(keys, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#asyncGetBulk(net.spy.memcached.transcoders.Transcoder, java.lang.String[])
	 */
	public <T> Future<Map<String, T>> asyncGetBulk(Transcoder<T> tc,
		String... keys) {
		return asyncGetBulk(Arrays.asList(keys), tc);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncGetBulk(java.lang.String[])
	 */
	public Future<Map<String, Object>> asyncGetBulk(String... keys) {
		return asyncGetBulk(Arrays.asList(keys), transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#getBulk(java.util.Collection, net.spy.memcached.transcoders.Transcoder)
	 */
	public <T> Map<String, T> getBulk(Collection<String> keys,
			Transcoder<T> tc) {
		try {
			return asyncGetBulk(keys, tc).get(
				operationTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted getting bulk values", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Failed getting bulk values", e);
		} catch (TimeoutException e) {
			throw new OperationTimeoutException(
				"Timeout waiting for bulkvalues", e);
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getBulk(java.util.Collection)
	 */
	public Map<String, Object> getBulk(Collection<String> keys) {
		return getBulk(keys, transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClientWithTranscoder#getBulk(net.spy.memcached.transcoders.Transcoder, java.lang.String[])
	 */
	public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) {
		return getBulk(Arrays.asList(keys), tc);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getBulk(java.lang.String[])
	 */
	public Map<String, Object> getBulk(String... keys) {
		return getBulk(Arrays.asList(keys), transcoder);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getVersions()
	 */
	public Map<SocketAddress, String> getVersions() {
		return conn.getVersions(operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getStats()
	 */
	public Map<SocketAddress, Map<String, String>> getStats() {
		return getStats(null);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getStats(java.lang.String)
	 */
	public Map<SocketAddress, Map<String, String>> getStats(final String arg) {
		return conn.getStats(arg, operationTimeout);
	}

	private long mutate(Mutator m, String key, int by, long def, int exp)
		throws OperationTimeoutException {
		return conn.mutate(m, KeyUtil.getKeyBytes(key), by, def, exp,
				operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#incr(java.lang.String, int)
	 */
	public long incr(String key, int by) {
		return mutate(Mutator.incr, key, by, 0, -1);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#decr(java.lang.String, int)
	 */
	public long decr(String key, int by) {
		return mutate(Mutator.decr, key, by, 0, -1);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#incr(java.lang.String, int, long, int)
	 */
	public long incr(String key, int by, long def, int exp) {
		return mutateWithDefault(Mutator.incr, key, by, def, exp);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#decr(java.lang.String, int, long, int)
	 */
	public long decr(String key, int by, long def, int exp) {
		return mutateWithDefault(Mutator.decr, key, by, def, exp);
	}

	private long mutateWithDefault(Mutator t, String key,
			int by, long def, int exp) {
		long rv=mutate(t, key, by, def, exp);
		// The ascii protocol doesn't support defaults, so I added them
		// manually here.
		if(rv == -1) {
			Future<Boolean> f=asyncStore(StoreType.add,
					key, exp, String.valueOf(def));
			try {
				if(f.get(operationTimeout, TimeUnit.MILLISECONDS)) {
					rv=def;
				} else {
					rv=mutate(t, key, by, 0, exp);
					assert rv != -1 : "Failed to mutate or init value";
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted waiting for store", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Failed waiting for store", e);
			} catch (TimeoutException e) {
				throw new OperationTimeoutException(
					"Timeout waiting to mutate or init value", e);
			}
		}
		return rv;
	}

	private Future<Long> asyncMutate(Mutator m, String key, int by, long def,
			int exp) {
		return conn.asyncMutate(m, KeyUtil.getKeyBytes(key), by, def, exp);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncIncr(java.lang.String, int)
	 */
	public Future<Long> asyncIncr(String key, int by) {
		return asyncMutate(Mutator.incr, key, by, 0, -1);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#asyncDecr(java.lang.String, int)
	 */
	public Future<Long> asyncDecr(String key, int by) {
		return asyncMutate(Mutator.decr, key, by, 0, -1);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#incr(java.lang.String, int, long)
	 */
	public long incr(String key, int by, long def) {
		return mutateWithDefault(Mutator.incr, key, by, def, 0);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#decr(java.lang.String, int, long)
	 */
	public long decr(String key, int by, long def) {
		return mutateWithDefault(Mutator.decr, key, by, def, 0);
	}

	/**
	 * Delete the given key from the cache.
	 *
	 * <p>
	 * The hold argument specifies the amount of time in seconds (or Unix time
	 * until which) the client wishes the server to refuse "add" and "replace"
	 * commands with this key. For this amount of item, the item is put into a
	 * delete queue, which means that it won't possible to retrieve it by the
	 * "get" command, but "add" and "replace" command with this key will also
	 * fail (the "set" command will succeed, however). After the time passes,
	 * the item is finally deleted from server memory.
	 * </p>
	 *
	 * @param key the key to delete
	 * @param hold how long the key should be unavailable to add commands
	 *
	 * @deprecated Hold values are no longer honored.
	 */
	@Deprecated
	public Future<Boolean> delete(String key, int hold) {
		return conn.delete(KeyUtil.getKeyBytes(key), hold, operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#delete(java.lang.String)
	 */
	public Future<Boolean> delete(String key) {
		return delete(key, 0);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#flush(int)
	 */
	public Future<Boolean> flush(final int delay) {
		return conn.flush(delay, operationTimeout);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#flush()
	 */
	public Future<Boolean> flush() {
		return flush(-1);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getAvailableServers()
	 */
	public Collection<SocketAddress> getAvailableServers() {
		return conn.getAvailableServers();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getUnavailableServers()
	 */
	public Collection<SocketAddress> getUnavailableServers() {
		return conn.getUnavailableServers();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#shutdown(long, java.util.concurrent.TimeUnit)
	 */
	public boolean shutdown(long timeout, TimeUnit unit) {
		return conn.shutdown(timeout, unit);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#shutdown()
	 */
	public void shutdown() {
		conn.shutdown();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#waitForQueues(long, java.util.concurrent.TimeUnit)
	 */
	public boolean waitForQueues(long timeout, TimeUnit unit) {
		return conn.waitForQueues(timeout, unit);
	}

}
