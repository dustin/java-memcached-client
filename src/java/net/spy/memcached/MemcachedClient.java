// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import net.spy.SpyThread;
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

	private static final int MAX_KEY_LENGTH = 250;

	private volatile boolean running=true;
	private volatile boolean shuttingDown=false;
	private MemcachedConnection conn=null;
	private HashAlgorithm hashAlg=HashAlgorithm.NATIVE_HASH;
	Transcoder transcoder=null;

	/** 
	 * Allow mockery.
	 */
	protected MemcachedClient() {
		super();
	}

	/**
	 * Get a memcache client operating on the specified memcached locations.
	 * 
	 * @param ia the memcached locations
	 * @throws IOException if connections cannot be established
	 */
	public MemcachedClient(InetSocketAddress... ia) throws IOException {
		this(new DefaultConnectionFactory(), Arrays.asList(ia));
	}


	/**
	 * Get a memcache client over the specified memcached locations.
	 *
	 * @param addrs the socket addrs
	 * @throws IOException if connections cannot be established
	 */
	public MemcachedClient(List<InetSocketAddress> addrs)
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
	public MemcachedClient(ConnectionFactory cf, List<InetSocketAddress> addrs)
		throws IOException {
		transcoder=new SerializingTranscoder();
		conn=cf.createConnection(addrs);
		setName("Memcached IO over " + conn);
		start();
	}

	/**
	 * Set the hash algorithm.
	 */
	public HashAlgorithm getHashAlgorithm() {
		return hashAlg;
	}

	/**
	 * Set the hash algorithm for computing which server should receive
	 * requests for a given key.
	 */
	public void setHashAlgorithm(HashAlgorithm to) {
		if(to == null) {
			throw new NullPointerException("Null hash algorithm not allowed");
		}
		hashAlg=to;
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
		if(shuttingDown) {
			throw new IllegalStateException("Shutting down");
		}
		assert isAlive() : "IO Thread is not running.";
		conn.addOperation(which, op);
		return op;
	}

	private Future<Boolean> asyncStore(StoreOperation.StoreType storeType,
			String key, int exp, Object value) {
		CachedData co=transcoder.encode(value);
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch);
		Operation op=new StoreOperation(storeType, key, co.getFlags(), exp,
				co.getData(), new OperationCallback() {
					public void receivedStatus(String val) {
						rv.set(val.equals("STORED"));
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(getServerForKey(key), op);
		return rv;
	}

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
	 * @return a future representing the processing of this operation
	 */
	public Future<Boolean> add(String key, int exp, Object o) {
		return asyncStore(StoreOperation.StoreType.add, key, exp, o);
	}

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
	 * @return a future representing the processing of this operation
	 */
	public Future<Boolean> set(String key, int exp, Object o) {
		return asyncStore(StoreOperation.StoreType.set, key, exp, o);
	}

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
	 * @return a future representing the processing of this operation
	 */
	public Future<Boolean> replace(String key, int exp, Object o) {
		return asyncStore(StoreOperation.StoreType.replace, key, exp, o);
	}

	/**
	 * Get the given key asynchronously.
	 * 
	 * @param key the key to fetch
	 * @return a future that will hold the return value of the fetch
	 */
	public Future<Object> asyncGet(final String key) {

		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Object> rv=new OperationFuture<Object>(latch);

		Operation op=new GetOperation(key, new GetOperation.Callback() {
			private Object val=null;
			public void receivedStatus(String line) {
				rv.set(val);
				latch.countDown();
			}
			public void gotData(String k, int flags, byte[] data) {
				assert key.equals(k) : "Wrong key returned";
				val=transcoder.decode(new CachedData(flags, data));
			}});
		rv.setOperation(op);
		addOp(getServerForKey(key), op);
		return rv;
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
		final Map<String, Object> m=new ConcurrentHashMap<String, Object>();
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
		final CountDownLatch latch=new CountDownLatch(chunks.size());
		final Collection<Operation> ops=new ArrayList<Operation>();

		GetOperation.Callback cb=new GetOperation.Callback() {
				public void receivedStatus(String line) {
					latch.countDown();
				}
				public void gotData(String k, int flags, byte[] data) {
					m.put(k, transcoder.decode(new CachedData(flags, data)));
				}
		};
		for(Map.Entry<Integer, Collection<String>> me : chunks.entrySet()) {
			ops.add(addOp(me.getKey(), new GetOperation(me.getValue(), cb)));
		}
		return new BulkGetFuture(m, ops, latch);
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
		final CountDownLatch latch=new CountDownLatch(conn.getNumConnections());

		for(int i=0; i<conn.getNumConnections(); i++) {
			final SocketAddress sa=conn.getAddressOf(i);
			addOp(i, new VersionOperation(
					new OperationCallback() {
						public void receivedStatus(String s) {
							rv.put(sa, s);
							latch.countDown();
						}
					}));
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for versions", e);
		}
		return rv;
	}

	/**
	 * Get all of the stats from all of the connections.
	 */
	public Map<SocketAddress, Map<String, String>> getStats() {
		return getStats(null);
	}

	private Map<SocketAddress, Map<String, String>> getStats(String arg) {
		final Map<SocketAddress, Map<String, String>> rv
			=new HashMap<SocketAddress, Map<String, String>>();
		final CountDownLatch latch=new CountDownLatch(conn.getNumConnections());
		for(int i=0; i<conn.getNumConnections(); i++) {
			final SocketAddress sa=conn.getAddressOf(i);
			rv.put(sa, new HashMap<String, String>());
			addOp(i, new StatsOperation(arg,
					new StatsOperation.Callback() {
						public void gotStat(String name, String val) {
							rv.get(sa).put(name, val);
						}
						public void receivedStatus(String line) {
							latch.countDown();
						}}));
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for stats", e);
		}
		return rv;
	}

	private long mutate(MutatorOperation.Mutator m, String key, int by) {
		final AtomicLong rv=new AtomicLong();
		final CountDownLatch latch=new CountDownLatch(1);
		addOp(getServerForKey(key), new MutatorOperation(m, key, by,
				new OperationCallback() {
					public void receivedStatus(String val) {
						rv.set(new Long(val==null?"-1":val));
						latch.countDown();
					}}));
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted", e);
		}
		getLogger().debug("Mutation returned %s", rv);
		return rv.get();
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
			Future<Boolean> f=asyncStore(StoreOperation.StoreType.add, key, 0,
					String.valueOf(def));
			try {
				if(f.get()) {
					rv=def;
				} else {
					rv=mutate(t, key, by);
					assert rv != -1 : "Failed to mutate or init value";
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
	public Future<Boolean> delete(String key, int when) {
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch);
		DeleteOperation op=new DeleteOperation(key, when,
				new OperationCallback() {
					public void receivedStatus(String line) {
						rv.set(line.equals("DELETED"));
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(getServerForKey(key), op);
		return rv;
	}

	/**
	 * Shortcut to delete that will immediately delete the item from the cache.
	 */
	public Future<Boolean> delete(String key) {
		return delete(key, 0);
	}

	/**
	 * Flush all caches from all servers with a delay of application.
	 */
	public Future<Boolean> flush(int delay) {
		final CountDownLatch latch=new CountDownLatch(conn.getNumConnections());
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch);
		// XXX:  We will not be able to cancel this operation
		for(int i=0; i<conn.getNumConnections(); i++) {
			addOp(i, new FlushOperation(delay, new OperationCallback(){
				public void receivedStatus(String line) {
					rv.set(line.equals("OK"));
					latch.countDown();
				}}));
		}
		return rv;
	}

	/**
	 * Flush all caches from all servers immediately.
	 */
	public Future<Boolean> flush() {
		return flush(-1);
	}

	/**
	 * Find keys matching a given prefix.
	 * This method uses undocumented mechanisms in the memcached wire protocol,
	 * so it may just altogether stop working at some point.
	 */
	public Collection<String> findKeys(String prefix) {
		Map<SocketAddress, Map<String, String>> stats = getStats("items");
		Collection<String> chunks=new HashSet<String>();
		for(Map<String, String> m : stats.values()) {
			for(String stuff : m.keySet()) {
				String parts[]=stuff.split(":");
				assert parts.length == 3
					: "Incorrect number of parts in " + stuff;
				chunks.add(parts[1]);
			}
		}

		// Now that we know what chunks we need, ask for the keys
		Collection<String> rv=new TreeSet<String>();
		for(String chunk : chunks) {
			for(Map<String, String> m
					: getStats("cachedump " + chunk + " 10000000").values()) {
				rv.addAll(m.keySet());
			}
		}
		
		return rv;
	}

	/**
	 * Infinitely loop processing IO.
	 */
	@Override
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
	 * Shut down immediately.
	 */
	public void shutdown() {
		shutdown(-1, TimeUnit.MILLISECONDS);
	}

	/**
	 * Shut down this client graceful.
	 */
	public boolean shutdown(long timeout, TimeUnit unit) {
		shuttingDown=true;
		String baseName=getName();
		setName(baseName + " - SHUTTING DOWN");
		boolean rv=false;
		if(timeout > 0) {
			setName(baseName + " - SHUTTING DOWN (waiting)");
			rv=waitForQueues(timeout, unit);
		}
		try {
			setName(baseName + " - SHUTTING DOWN (telling client)");
			running=false;
			conn.shutdown();
			setName(baseName + " - SHUTTING DOWN (informed client)");
		} catch (IOException e) {
			getLogger().warn("exception while shutting down", e);
		}
		return rv;
	}

	/**
	 * Wait for the queues to die down.
	 */
	public boolean waitForQueues(long timeout, TimeUnit unit) {
		boolean rv=false;
		final CountDownLatch latch=new CountDownLatch(conn.getNumConnections());

		// This is implemented by directly adding VersionOperations to all the
		// existing queues and then waiting for them to be processed.  We're
		// directly adding to the queue instead of using addOp because we want
		// to be able to do this during a shutdown after disallowing any
		// additional operations.
		for(int i=0; i<conn.getNumConnections(); i++) {
			assert isAlive() : "IO Thread is not running.";
			conn.addOperation(i, new VersionOperation(
					new OperationCallback() {
						public void receivedStatus(String s) {
							latch.countDown();
						}
					}));
		}
		try {
			rv=latch.await(timeout, unit);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for versions", e);
		}
		return rv;
	}

	private int getServerForKey(String key) {
		if(key.length() > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ MAX_KEY_LENGTH + ")");
		}
		if(key.indexOf(' ') >= 0) {
			throw new IllegalArgumentException(
					"Key contains invalid characters: " + key);
		}
		int rv=hashAlg.hash(key) % conn.getNumConnections();
		assert rv >= 0 : "Returned negative key for key " + key;
		assert rv < conn.getNumConnections()
			: "Invalid server number " + rv + " for key " + key;
		getLogger().debug("Returning server #%d for %s(%s) (%d total conns)",
				rv, hashAlg, key, conn.getNumConnections());
		return rv;
	}

	static class BulkGetFuture implements Future<Map<String, Object>> {
		private Map<String, Object> rvMap;
		private Collection<Operation> ops;
		private CountDownLatch latch=null;
		private boolean cancelled=false;

		public BulkGetFuture(Map<String, Object> m,
				Collection<Operation> getOps, CountDownLatch l) {
			super();
			rvMap = m;
			ops = getOps;
			latch=l;
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
			latch.await(timeout, unit);
			for(Operation op : ops) {
				if(op.isCancelled()) {
					throw new ExecutionException(
							new RuntimeException("Cancelled"));
				}
			}
			return rvMap;
		}

		public boolean isCancelled() {
			return cancelled;
		}

		public boolean isDone() {
			return latch.getCount() == 0;
		}
	}

	private static class OperationFuture<T> implements Future<T> {

		private T obj=null;
		private CountDownLatch latch=null;
		private Operation op=null;

		public OperationFuture(CountDownLatch l) {
			super();
			latch=l;
		}

		public boolean cancel(boolean ign) {
			assert op != null : "No operation";
			op.cancel();
			// This isn't exactly correct, but it's close enough.  If we're in
			// a writing state, we *probably* haven't started.
			return op.getState() == Operation.State.WRITING;
		}

		public T get() throws InterruptedException, ExecutionException {
			latch.await();
			if(op != null && op.isCancelled()) {
				throw new ExecutionException(new RuntimeException("Cancelled"));
			}
			return obj;
		}

		public T get(long duration, TimeUnit units)
			throws InterruptedException, TimeoutException {
			latch.await(duration, units);
			return obj;
		}

		void set(T o) {
			obj=o;
		}

		void setOperation(Operation to) {
			op=to;
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
