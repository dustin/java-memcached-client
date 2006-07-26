// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 8F357832-263E-473E-BB88-3FFBC813180F

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import net.spy.SpyThread;
import net.spy.concurrent.SynchronizationObject;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.MutatorOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.VersionOperation;

/**
 * Client to a memcached server.
 */
public class MemcachedClient extends SpyThread {

	private volatile boolean running=true;
	private MemcachedConnection conn=null;
	private Transcoder transcoder=null;

	/**
	 * Get a memcache client operating on the specified memcached locations.
	 * 
	 * @param ia the memcached locations
	 * @throws IOException if connections cannot be established
	 */
	public MemcachedClient(InetSocketAddress... ia) throws IOException {
		super();
		transcoder=new SerializingTranscoder();
		conn=new MemcachedConnection(ia);
		setName("Memcached IO over " + conn);
		start();
	}

	/**
	 * Set the transcoder for managing the cache representations of objects
	 * going in and out of the cache.
	 */
	public void setTranscoder(Transcoder to) {
		if(to == null) {
			throw new NullPointerException("Can't use a null transcoder");
		}
		transcoder=to;
	}

	/**
	 * Get the current transcoder that's in use.
	 */
	public Transcoder getTranscoder() {
		return transcoder;
	}

	private void addOp(int which, Operation op) {
		assert isAlive() : "IO Thread is not running.";
		conn.addOperation(which, op);
	}

	/**
	 * Submit an asynchronous store to the current connection.
	 * 
	 * @param storeType the storage type
	 * @param key the key under which to store this value
	 * @param flags the storage flags
	 * @param exp the expiration value for this store
	 * @param value the value to store
	 * @param callback the callback to send the result to
	 */
	public void storeAsync(StoreOperation.StoreType storeType, String key,
			int exp, Object value, StoreOperation.Callback callback) {
		CachedData co=transcoder.encode(value);
		addOp(getServerForKey(key),
				new StoreOperation(storeType, key, co.getFlags(), exp,
						co.getData(), callback));
	}

	private SynchronizationObject<String> setupStoreSync(
			StoreOperation.StoreType storeType, String key,
			int exp, Object o) {
		final SynchronizationObject<String> so=
			new SynchronizationObject<String>(null);
		storeAsync(storeType, key, exp, o,
				new StoreOperation.Callback() {
					public void storeResult(String val) {
						so.set(val);
					}
			});
		return so;
	}

	/**
	 * Synchronous store to the cache.
	 * 
	 * @param storeType the type of store
	 * @param key the key under which to store
	 * @param flags the storage flags
	 * @param exp the expiration date or duration
	 * @param value the value to store
	 * @return the result of the store
	 */
	public String storeSync(StoreOperation.StoreType storeType, String key,
			int exp, Object o) {
		SynchronizationObject<String> so=setupStoreSync(
				storeType, key, exp, o);
		waitForNotNull(so);
		return so.get();
	}

	/**
	 * Synchronous store to the cache.
	 * 
	 * @param timeout how long to wait for the results of this store.
	 * @param storeType the type of store
	 * @param key the key under which to store
	 * @param flags the storage flags
	 * @param exp the expiration date or duration
	 * @param value the value to store
	 * @return the result of the store
	 * @throws TimeoutException if the store status isn't known before the
	 *         request timeout
	 */
	public String storeSync(long timeout, StoreOperation.StoreType storeType,
			String key, int exp, Object o) throws TimeoutException {
		SynchronizationObject<String> so=setupStoreSync(
				storeType, key, exp, o);
		waitForNotNull(timeout, so);
		return so.get();
	}

	/**
	 * Convenience method to add a value to the memcached.
	 * 
	 * @param key the storage key
	 * @param flags the flags
	 * @param exp the expiration
	 * @param value the value to store
	 * @return the storage return
	 */
	public String add(String key, int exp, Object o) {
		return storeSync(StoreOperation.StoreType.add, key, exp, o);
	}

	/**
	 * Convenience method to set a value in the memcached.
	 * 
	 * @param key the storage key
	 * @param flags the flags
	 * @param exp the expiration
	 * @param value the value to store
	 * @return the storage return
	 */
	public String set(String key, int exp, Object o) {
		return storeSync(StoreOperation.StoreType.set, key, exp, o);
	}

	/**
	 * Convenience method to replace a value in the memcached.
	 * 
	 * @param key the storage key
	 * @param flags the flags
	 * @param exp the expiration
	 * @param value the value to store
	 * @return the storage return
	 */
	public String replace(String key, int exp, Object o) {
		return storeSync(StoreOperation.StoreType.replace, key, exp, o);
	}

	/**
	 * Perform an asynchronous get operation.
	 * 
	 * @param cb the callback for each key retrieval
	 * @param keys the keys to retrieve
	 */
	public void asyncGet(GetOperation.Callback cb, String... keys) {
		for(String key : keys) {
			addOp(getServerForKey(key), new GetOperation(key, cb));
		}
	}

	private SynchronizationObject<CachedData[]> setupGet(final String key) {
		final CachedData[] rvContainer=new CachedData[1];
		rvContainer[0]=null;
		final SynchronizationObject<CachedData[]> rv
			=new SynchronizationObject<CachedData[]>(null);
			asyncGet(new GetOperation.Callback() {
				public void getComplete() {
					rv.set(rvContainer);
				}
				public void gotData(String k, int flags, byte[] data) {
					assert k.equals(key) : "Incorrect key returned: " + k;
					rvContainer[0]=new CachedData(flags, data);
				}}, key);
			return rv;
	}

	/**
	 * Get with a single key.
	 * 
	 * @param key the key to get
	 * @return the result from the cache (null if there is none)
	 */
	public Object get(String key) {
		SynchronizationObject<CachedData[]> sync=setupGet(key);
		waitForNotNull(sync);
		CachedData[] rvContainer=sync.get();
		assert rvContainer.length == 1;
		Object rv=null;
		if(rvContainer[0] != null) {
			rv=transcoder.decode(rvContainer[0]);
		}
		return rv;
	}

	/**
	 * Get with a single key.
	 * 
	 * @param timeout how long to wait for the result
	 * @param key the key to get
	 * @return the result from the cache (null if there is none)
	 * @throws TimeoutException if a timeout occurs before results are complete
	 */
	public Object get(long timeout, String key) throws TimeoutException {
		SynchronizationObject<CachedData[]> sync=setupGet(key);
		waitForNotNull(timeout, sync);
		CachedData[] rvContainer=sync.get();
		assert rvContainer.length == 1;
		Object rv=null;
		if(rvContainer[0] != null) {
			rv=transcoder.decode(rvContainer[0]);
		}
		return rv;
	}

	private SynchronizationObject<AtomicInteger> setupBulkGet(
			final Map<String, Object> m, String[] keys) {
		final AtomicInteger requests=new AtomicInteger();
		final SynchronizationObject<AtomicInteger> sync
			=new SynchronizationObject<AtomicInteger>(requests);
		GetOperation.Callback cb=new GetOperation.Callback() {
				public void getComplete() {
					requests.decrementAndGet();
					sync.set(requests);
				}
				public void gotData(String k, int flags, byte[] data) {
					m.put(k, transcoder.decode(new CachedData(flags, data)));
				}
		};
		// Break the gets down into groups by key
		Map<Integer, Collection<String>> chunks
			=new HashMap<Integer, Collection<String>>();
		for(String key : keys) {
			int which=getServerForKey(key);
			Collection<String> ks=chunks.get(which);
			if(ks == null) {
				ks=new ArrayList<String>();
				chunks.put(which, ks);
			}
			ks.add(key);
		}
		for(Map.Entry<Integer, Collection<String>> me : chunks.entrySet()) {
			requests.incrementAndGet();
			addOp(me.getKey(), new GetOperation(me.getValue(), cb));
		}
		return sync;
	}

	/**
	 * Get the values for multiple keys from the cache.
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 */
	public Map<String, Object> get(String... keys) {
		Map<String, Object> rv=new ConcurrentHashMap<String, Object>(); 
		SynchronizationObject<AtomicInteger> sync=setupBulkGet(rv, keys);
		waitForOperations(sync);
		return rv;
	}

	/**
	 * Get the values for multiple keys from the cache.
	 * @param timeout how long to wait for the results
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws TimeoutException if we don't get the results before the timeout
	 */
	public Map<String, Object> get(long timeout, String... keys)
		throws TimeoutException {

		Map<String, Object> rv=new ConcurrentHashMap<String, Object>(); 
		SynchronizationObject<AtomicInteger> sync=setupBulkGet(rv, keys);
		waitForOperations(timeout, sync);
		return rv;
	}

	/**
	 * Get the versions of all of the connected memcacheds.
	 */
	public Map<SocketAddress, String> getVersions() {
		final Map<SocketAddress, String>rv=
			new ConcurrentHashMap<SocketAddress, String>();
		final AtomicInteger ai=new AtomicInteger(0);
		final SynchronizationObject<AtomicInteger> sync=
			new SynchronizationObject<AtomicInteger>(ai);

		for(int i=0; i<conn.getNumConnections(); i++) {
			final SocketAddress sa=conn.getAddressOf(i);
			ai.incrementAndGet();
			addOp(i, new VersionOperation(
					new VersionOperation.Callback() {
						public void versionResult(String s) {
							rv.put(sa, s);
							ai.decrementAndGet();
							sync.set(ai);
						}
					}));
		}
		waitForOperations(sync);
		return rv;
	}

	/**
	 * Get all of the stats from all of the connections.
	 */
	public Map<SocketAddress, Map<String, String>> getStats() {
		final Map<SocketAddress, Map<String, String>> rv
			=new HashMap<SocketAddress, Map<String, String>>();
		final AtomicInteger todo=new AtomicInteger(conn.getNumConnections());
		final SynchronizationObject<AtomicInteger> sync
			=new SynchronizationObject<AtomicInteger>(todo);
		for(int i=0; i<conn.getNumConnections(); i++) {
			final SocketAddress sa=conn.getAddressOf(i);
			rv.put(sa, new HashMap<String, String>());
			addOp(i, new StatsOperation(
					new StatsOperation.Callback() {
						public void gotStat(String name, String val) {
							rv.get(sa).put(name, val);
						}
						public void statsComplete() {
							todo.decrementAndGet();
							sync.set(todo);
						}}));
		}
		waitForOperations(sync);
		return rv;
	}

	private long mutate(MutatorOperation.Mutator m, String key, int by) {
		final SynchronizationObject<Long> sync=
			new SynchronizationObject<Long>(null);
		addOp(getServerForKey(key), new MutatorOperation(m, key, by,
				new MutatorOperation.Callback() {
					public void mutatorResult(Long val) {
						if(val == null) {
							val=new Long(-1);
						}
						sync.set(val);
					}}));
		waitForNotNull(sync);
		getLogger().debug("Mutation returned %s", sync.get());
		return sync.get().longValue();
	}

	/**
	 * Increment the given key by the given amount.
	 * 
	 * @param key the key
	 * @param by the amount to increment
	 * @return the new value (-1 if the key doesn't exist)
	 */
	public long incr(String key, int by) {
		return mutate(MutatorOperation.Mutator.incr, key, by);
	}

	/**
	 * Decrement the given key by the given value.
	 * 
	 * @param key the key
	 * @param by the value
	 * @return the new value (-1 if the key doesn't exist)
	 */
	public long decr(String key, int by) {
		return mutate(MutatorOperation.Mutator.decr, key, by);
	}

	private long mutateWithDefault(MutatorOperation.Mutator t, String key,
			int by, long def) {
		long rv=mutate(t, key, by);
		if(rv == -1) {
			String v=storeSync(StoreOperation.StoreType.add, key, 0,
					String.valueOf(def));
			if(v.equals("STORED")) {
				rv=def;
			}
		}
		return rv;
	}

	/**
	 * Increment the given counter, returning the new value.
	 * 
	 * @param key the key
	 * @param by the amount to increment
	 * @param def the default value (if the counter does not exist)
	 * @return the new value, or -1 if we were unable to increment or add
	 */
	public long incr(String key, int by, long def) {
		return mutateWithDefault(MutatorOperation.Mutator.incr, key, by, def);
	}

	/**
	 * Decrement the given counter, returning the new value.
	 * 
	 * @param key the key
	 * @param by the amount to decrement
	 * @param def the default value (if the counter does not exist)
	 * @return the new value, or -1 if we were unable to decrement or add
	 */
	public long decr(String key, int by, long def) {
		return mutateWithDefault(MutatorOperation.Mutator.decr, key, by, def);
	}

	/**
	 * Delete the given key from the cache.
	 * 
	 * @param key the key to delete
	 * @param when when the deletion should take effect
	 */
	public void delete(String key, int when) {
		addOp(getServerForKey(key), new DeleteOperation(key, when));
	}

	/**
	 * Shortcut to delete that will immediately delete the item from the cache.
	 */
	public void delete(String key) {
		delete(key, 0);
	}

	/**
	 * Flush all caches from all servers.
	 */
	public void flush(int delay) {
		for(int i=0; i<conn.getNumConnections(); i++) {
			addOp(i, new FlushOperation(delay));
		}
	}

	public void flush() {
		for(int i=0; i<conn.getNumConnections(); i++) {
			addOp(i, new FlushOperation());
		}
	}

	public void run() {
		while(running) {
			try {
				conn.handleIO();
			} catch(IOException e) {
				getLogger().warn("Problem handling memcached IO", e);
			}
		}
		getLogger().info("Shut down memcached client");
	}

	public void shutdown() {
		running=false;
		try {
			conn.shutdown();
		} catch (IOException e) {
			getLogger().warn("exception while shutting down", e);
		}
	}

	private int getServerForKey(String key) {
		return key.hashCode() % conn.getNumConnections();
	}

	private void waitForNotNull(long timeout, SynchronizationObject<?> sync)
		throws TimeoutException {
		try {
			sync.waitUntilNotNull(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for response.", e);
		}
	}

	private void waitForNotNull(SynchronizationObject<?> sync) {
		try {
			waitForNotNull(Long.MAX_VALUE, sync);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting forever", e);
		}
	}

	private void waitForOperations(long timeout,
			final SynchronizationObject<AtomicInteger> sync)
		throws TimeoutException {
		try {
			sync.waitUntilTrue(
					new SynchronizationObject.Predicate<AtomicInteger>() {
						public boolean evaluate(AtomicInteger val) {
							return val.get() == 0;
						}},
					Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for results", e);
		}
	}

	private void waitForOperations(
			final SynchronizationObject<AtomicInteger> sync) {
		try {
			waitForOperations(Long.MAX_VALUE, sync);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting forever", e);
		}
	}
}
