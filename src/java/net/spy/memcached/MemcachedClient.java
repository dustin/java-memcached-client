// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 8F357832-263E-473E-BB88-3FFBC813180F

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.VersionOperation;

/**
 * Client to a memcached server.
 * 
 * <h2>Basic usage</h2>
 * 
 * <pre>
 *  MemcachedClient c=new MemcachedClient(
 *      new InetSocketAddress("hostname", portNum));
 *  
 *  // Store a value (async) for one hour
 *  c.set("someKey", 3600, someObject);
 *  // Retrieve a value.
 *  Object myObject=c.get("someKey");
 *  </pre>
 *  
 *  <h2>Advanced Usage</h2>
 *  
 *  <p>
 *   MemcachedClient may be processing a great deal of asynchronous messages or
 *   possibly dealing with an unreachable memcached, which may delay processing.
 *   If a memcached is disabled, for example, MemcachedConnection will continue
 *   to attempt to reconnect and replay pending operations until it comes back
 *   up.  To prevent this from causing your application to hang, you can use
 *   one of the asynchronous mechanisms to time out a request and cancel the
 *   operation to the server.
 *  </p>
 *  
 *  <pre>
 *  // Try to get a value, for up to 5 seconds, and cancel if it doesn't return
 *  Object myObj=null;
 *  Future&lt;Object&gt; f=c.asyncGet("someKey");
 *  try {
 *      myObj=f.get(5, TimeUnit.SECONDS);
 *  } catch(TimeoutException e) {
 *      // Since we don't need this, go ahead and cancel the operation.  This
 *      // is not strictly necessary, but it'll save some work on the server.
 *      f.cancel();
 *      // Do other timeout related stuff
 *  }
 * </pre>
 */
public class MemcachedClient extends SpyThread {

	private volatile boolean running=true;
	private MemcachedConnection conn=null;
	private Transcoder transcoder=null;

	/** 
	 * Allow mockery.
	 */
	protected MemcachedClient() {
	}

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

	private Operation addOp(int which, Operation op) {
		assert isAlive() : "IO Thread is not running.";
		conn.addOperation(which, op);
		return op;
	}

	private Future<String> asyncStore(StoreOperation.StoreType storeType,
			String key, int exp, Object value) {
		CachedData co=transcoder.encode(value);
		final SynchronizationObject<String> sync
			=new SynchronizationObject<String>(null);
		Operation op=new StoreOperation(storeType, key, co.getFlags(), exp,
				co.getData(), new OperationCallback() {
					public void receivedStatus(String val) {
						sync.set(val);
					}});
		OperationFuture<String> rv=new OperationFuture<String>(sync, op);
		addOp(getServerForKey(key), op);
		return rv;
	}

	/**
	 * Add an object to the cache iff it does not exist already.
	 * 
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @return a future representing the processing of this operation
	 */
	public Future<String> add(String key, int exp, Object o) {
		return asyncStore(StoreOperation.StoreType.add, key, exp, o);
	}

	/**
	 * Set an object in the cache regardless of any existing value.
	 * 
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @return a future representing the processing of this operation
	 */
	public Future<String> set(String key, int exp, Object o) {
		return asyncStore(StoreOperation.StoreType.set, key, exp, o);
	}

	/**
	 * Replace an object with the given value iff there is already a value
	 * for the given key.
	 * 
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @return a future representing the processing of this operation
	 */
	public Future<String> replace(String key, int exp, Object o) {
		return asyncStore(StoreOperation.StoreType.replace, key, exp, o);
	}

	/**
	 * Get the given key asynchronously.
	 * 
	 * @param key the key to fetch
	 * @return a future that will hold the return value of the fetch
	 */
	public Future<Object> asyncGet(final String key) {
		// This marker object will be the default value of synchronization.
		// When the synchronization object no longer contains this value, the
		// get is complete.
		final Object marker=new Object();

		final SynchronizationObject<Object> sync
			=new SynchronizationObject<Object>(marker);

		Operation op=new GetOperation(key, new GetOperation.Callback() {
			private Object val=null;
			public void receivedStatus(String line) {
				sync.set(val);
			}
			public void gotData(String k, int flags, byte[] data) {
				assert key.equals(k) : "Wrong key returned";
				val=transcoder.decode(new CachedData(flags, data));
			}});
		addOp(getServerForKey(key), op);

		// Return an operation future that waits for completion
		return new OperationFuture<Object>(sync, op) {
			@Override
			protected void waitForIt(long duration, TimeUnit units)
				throws InterruptedException, TimeoutException {
				sync.waitUntilTrue(
						new SynchronizationObject.Predicate<Object>() {
							public boolean evaluate(Object o) {
								return o != marker;
							}
						}, duration, units);
			}
		};
	}

	/**
	 * Get with a single key.
	 * 
	 * @param key the key to get
	 * @return the result from the cache (null if there is none)
	 */
	public Object get(String key) {
		try {
			return asyncGet(key).get();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for value", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Exception waiting for value", e);
		}
	}

	/**
	 * Asynchronously get a bunch of objects from the cache.
	 * 
	 * @param keys the keys to request
	 * @return a Future result of that fetch
	 */
	public Future<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
		final AtomicInteger requests=new AtomicInteger();
		final Map<String, Object> m=new ConcurrentHashMap<String, Object>();
		final SynchronizationObject<AtomicInteger> sync
			=new SynchronizationObject<AtomicInteger>(requests);
		GetOperation.Callback cb=new GetOperation.Callback() {
				public void receivedStatus(String line) {
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
		final Collection<Operation> ops=new ArrayList<Operation>();
		for(Map.Entry<Integer, Collection<String>> me : chunks.entrySet()) {
			requests.incrementAndGet();
			ops.add(addOp(me.getKey(), new GetOperation(me.getValue(), cb)));
		}
		return new BulkGetFuture(requests, m, ops, sync);
	}

	/**
	 * Varargs wrapper for asynchronous bulk gets.
	 * 
	 * @param keys one more more keys to get
	 * @return the future values of those keys
	 */
	public Future<Map<String, Object>> asyncGetBulk(String... keys) {
		return asyncGetBulk(Arrays.asList(keys));
	}
	/**
	 * Get the values for multiple keys from the cache.
	 * 
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 */
	public Map<String, Object> getBulk(Collection<String> keys) {
		try {
			return asyncGetBulk(keys).get();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted getting bulk values", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Failed getting bulk values", e);
		}
	}

	/**
	 * Get the values for multiple keys from the cache.
	 * 
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 */
	public Map<String, Object> getBulk(String... keys) {
		return getBulk(Arrays.asList(keys));
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
					new OperationCallback() {
						public void receivedStatus(String s) {
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
						public void receivedStatus(String line) {
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
				new OperationCallback() {
					public void receivedStatus(String val) {
						sync.set(new Long(val==null?"-1":val));
					}}));
		try {
			sync.waitUntilNotNull(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for mutation", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting forever.");
		}
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
			Future<String> f=asyncStore(StoreOperation.StoreType.add, key, 0,
					String.valueOf(def));
			try {
				if(f.get().equals("STORED")) {
					rv=def;
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted waiting for store", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Failed waiting for store", e);
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
	 * Flush all caches from all servers with a delay of application.
	 */
	public void flush(int delay) {
		for(int i=0; i<conn.getNumConnections(); i++) {
			addOp(i, new FlushOperation(delay));
		}
	}

	/**
	 * Flush all caches from all servers immediately.
	 */
	public void flush() {
		for(int i=0; i<conn.getNumConnections(); i++) {
			addOp(i, new FlushOperation());
		}
	}

	/**
	 * Infinitely loop processing IO.
	 */
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

	/**
	 * Shut down this client.
	 */
	public void shutdown() {
		running=false;
		try {
			conn.shutdown();
		} catch (IOException e) {
			getLogger().warn("exception while shutting down", e);
		}
	}

	private int getServerForKey(String key) {
		if(key.matches(".*\\s.*")) {
			throw new IllegalArgumentException(
					"Key contains invalid characters: " + key);
		}
		return key.hashCode() % conn.getNumConnections();
	}

	private void waitForOperations(
			final SynchronizationObject<AtomicInteger> sync) {
		try {
			sync.waitUntilTrue(
					new SynchronizationObject.Predicate<AtomicInteger>() {
						public boolean evaluate(AtomicInteger val) {
							return val.get() == 0;
						}},
					Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for results", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting forever", e);
		}
	}

	private static class BulkGetFuture implements Future<Map<String, Object>> {
		private AtomicInteger requests;
		private Map<String, Object> m;
		private Collection<Operation> ops;
		private SynchronizationObject<AtomicInteger> sync;
		private boolean cancelled=false;

		private BulkGetFuture(AtomicInteger requests, Map<String, Object> m,
				Collection<Operation> ops,
				SynchronizationObject<AtomicInteger> sync) {
			super();
			this.requests = requests;
			this.m = m;
			this.ops = ops;
			this.sync = sync;
		}

		public boolean cancel(boolean ign) {
			boolean rv=false;
			for(Operation op : ops) {
				rv |= op.getState() == Operation.State.WRITING;
				op.cancel();
			}
			cancelled=true;
			return rv;
		}

		public Map<String, Object> get()
			throws InterruptedException, ExecutionException {
			try {
				return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("Timed out waiting forever", e);
			}
		}

		public Map<String, Object> get(long timeout, TimeUnit unit)
			throws InterruptedException,
			ExecutionException, TimeoutException {
			sync.waitUntilTrue(
					new SynchronizationObject.Predicate<AtomicInteger>() {
						public boolean evaluate(AtomicInteger i) {
							return i.get() == 0;
						}},
					timeout, unit);
			for(Operation op : ops) {
				if(op.isCancelled()) {
					throw new ExecutionException(
							new RuntimeException("Cancelled"));
				}
			}
			return m;
		}

		public boolean isCancelled() {
			return cancelled;
		}

		public boolean isDone() {
			return requests.get() == 0;
		}
	}

	private static class OperationFuture<T> implements Future<T> {

		private Operation op=null;
		private SynchronizationObject<T> sync=null;

		public OperationFuture(SynchronizationObject<T> so, Operation o) {
			super();
			sync=so;
			op=o;
		}

		public boolean cancel(boolean ign) {
			assert op != null : "No operation";
			op.cancel();
			// This isn't exactly correct, but it's close enough.  If we're in
			// a writing state, we *probably* haven't started.
			return op.getState() == Operation.State.WRITING;
		}

		public T get() throws InterruptedException, ExecutionException {
			try {
				waitForIt(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				assert false : "Timed out waiting forever.";
			}
			if(op.isCancelled()) {
				throw new ExecutionException(new RuntimeException("Cancelled"));
			}
			return sync.get();
		}

		public T get(long duration, TimeUnit units)
			throws InterruptedException, TimeoutException {
			waitForIt(duration, units);
			return sync.get();
		}

		protected void waitForIt(long duration, TimeUnit units)
			throws InterruptedException, TimeoutException {
			sync.waitUntilNotNull(duration, units);
		}

		public boolean isCancelled() {
			assert op != null : "No operation";
			return op.isCancelled();
		}

		public boolean isDone() {
			assert op != null : "No operation";
			return op.getState() == Operation.State.COMPLETE;
		}
		
	}
}
