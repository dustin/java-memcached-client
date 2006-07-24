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
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.VersionOperation;

/**
 * Client to a memcached server.
 */
public class MemcachedClient extends SpyThread {

	private volatile boolean running=true;
	private MemcachedConnection conn=null;

	/**
	 * Get a memcache client operating on the specified memcached locations.
	 * 
	 * @param ia the memcached locations
	 * @throws IOException if connections cannot be established
	 */
	public MemcachedClient(InetSocketAddress... ia) throws IOException {
		super();
		conn=new MemcachedConnection(ia);
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
			int flags, int exp, byte[] value,
			StoreOperation.Callback callback) {
		conn.addOperation(getServerForKey(key),
				new StoreOperation(storeType, key, flags, exp, value,
						callback));
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
			int flags, int exp, byte[] value) {
		final SynchronizationObject<String> so=
			new SynchronizationObject<String>(null);
		storeAsync(storeType, key, flags, exp, value,
				new StoreOperation.Callback() {
					public void storeResult(String val) {
						so.set(val);
					}
			});
		waitForNotNull(so);
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
	public String add(String key, int flags, int exp, byte[] value) {
		return storeSync(StoreOperation.StoreType.add, key, flags, exp, value);
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
	public String set(String key, int flags, int exp, byte[] value) {
		return storeSync(StoreOperation.StoreType.set, key, flags, exp, value);
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
	public String replace(String key, int flags, int exp, byte[] value) {
		return storeSync(StoreOperation.StoreType.replace, key, flags, exp,
				value);
	}

	/**
	 * Perform an asynchronous get operation.
	 * 
	 * @param cb the callback for each key retrieval
	 * @param keys the keys to retrieve
	 */
	public void asyncGet(GetOperation.Callback cb, String... keys) {
		for(String key : keys) {
			conn.addOperation(getServerForKey(key), new GetOperation(key, cb));
		}
	}

	/**
	 * Get with a single key.
	 * 
	 * @param key the key to get
	 * @return the result from the cache (null if there is none)
	 */
	public byte[] get(final String key) {
		final byte[][] rvContainer=new byte[1][];
		final SynchronizationObject<Object> sync
			=new SynchronizationObject<Object>(null);
		asyncGet(new GetOperation.Callback() {
			public void getComplete() {
				sync.set(new Object());
			}
			public void gotData(String k, int flags, byte[] data) {
				assert k.equals(key) : "Incorrect key returned: " + k;
				rvContainer[0]=data;
			}}, key);
		waitForNotNull(sync);
		return rvContainer[0];
	}

	/**
	 * Get the values for multiple keys from the cache.
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 */
	public Map<String, byte[]> get(String... keys) {
		final Map<String, byte[]> rv=new ConcurrentHashMap<String, byte[]>();
		final AtomicInteger requests=new AtomicInteger();
		final SynchronizationObject<AtomicInteger> sync
			=new SynchronizationObject<AtomicInteger>(requests);
		GetOperation.Callback cb=new GetOperation.Callback() {
				public void getComplete() {
					requests.decrementAndGet();
					sync.set(requests);
				}
				public void gotData(String k, int flags, byte[] data) {
					rv.put(k ,data);
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
			conn.addOperation(me.getKey(), new GetOperation(me.getValue(), cb));
		}
		try {
			sync.waitUntilTrue(
					new SynchronizationObject.Predicate<AtomicInteger>() {
						public boolean evaluate(AtomicInteger val) {
							return val.get() == 0;
						}},
					Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for results", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting forever", e);
		}
		return rv;
	}

	/**
	 * Get the versions of all of the connected memcacheds.
	 */
	public Map<SocketAddress, String> getVersions() {
		final Map<SocketAddress, String>rv=
			new ConcurrentHashMap<SocketAddress, String>();

		Collection<SynchronizationObject<String>> syncs=
			new ArrayList<SynchronizationObject<String>>();
		for(int i=0; i<conn.getNumConnections(); i++) {
			final SocketAddress sa=conn.getAddressOf(i);
			final SynchronizationObject<String> sync=
				new SynchronizationObject<String>(null);
			syncs.add(sync);
			conn.addOperation(i, new VersionOperation(
					new VersionOperation.Callback() {
						public void versionResult(String s) {
							rv.put(sa, s);
							sync.set(s);
						}
					}));
		}
		for(SynchronizationObject<String> s : syncs) {
			waitForNotNull(s);
		}
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
			conn.addOperation(i, new StatsOperation(
					new StatsOperation.Callback() {
						public void gotStat(String name, String val) {
							rv.get(sa).put(name, val);
						}
						public void statsComplete() {
							todo.decrementAndGet();
							sync.set(todo);
						}}));
		}
		try {
			sync.waitUntilTrue(
					new SynchronizationObject.Predicate<AtomicInteger>() {
						public boolean evaluate(AtomicInteger c) {
							return c.intValue() == 0;
						}},
					Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for data", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting forever", e);
		}
		return rv;
	}

	private long mutate(MutatorOperation.Mutator m, String key, int by) {
		final SynchronizationObject<Long> sync=
			new SynchronizationObject<Long>(null);
		conn.addOperation(getServerForKey(key), new MutatorOperation(m, key, by,
				new MutatorOperation.Callback() {
					public void mutatorResult(Long val) {
						if(val == null) {
							val=new Long(-1);
						}
						sync.set(val);
					}}));
		waitForNotNull(sync);
		getLogger().info("Mutation returned %s", sync.get());
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
			String v=storeSync(StoreOperation.StoreType.add, key, 0, 0,
					String.valueOf(def).getBytes());
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
		conn.addOperation(getServerForKey(key), new DeleteOperation(key, when));
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
			conn.addOperation(i, new FlushOperation(delay));
		}
	}

	public void flush() {
		for(int i=0; i<conn.getNumConnections(); i++) {
			conn.addOperation(i, new FlushOperation());
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

	private void waitForNotNull(SynchronizationObject<?> sync) {
		try {
			sync.waitUntilNotNull(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for response.", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting forever", e);
		}
	}
}
