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
import java.util.Iterator;
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
import java.util.concurrent.atomic.AtomicReference;

import net.spy.SpyThread;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.protocol.ascii.DeleteOperationImpl;
import net.spy.memcached.protocol.ascii.FlushOperationImpl;
import net.spy.memcached.protocol.ascii.GetOperationImpl;
import net.spy.memcached.protocol.ascii.MutatorOperationImpl;
import net.spy.memcached.protocol.ascii.StatsOperationImpl;
import net.spy.memcached.protocol.ascii.StoreOperationImpl;
import net.spy.memcached.protocol.ascii.VersionOperationImpl;

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
 *  // Get a memcached client connected to several servers
 *  MemcachedClient c=new MemcachedClient(
 *      AddrUtil.getAddresses("server1:11211 server2:11211"));
 *
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

	/**
	 * (internal use) Add a raw operation to a numbered connection.
	 * This method is exposed for testing.
	 *
	 * @param which server number
	 * @param op the operation to perform
	 * @return the Operation
	 */
	Operation addOp(final String key, final Operation op) {
		if(shuttingDown) {
			throw new IllegalStateException("Shutting down");
		}
		if(key.length() > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ MAX_KEY_LENGTH + ")");
		}
		// Validate the key
		for(char c : key.toCharArray()) {
			if(Character.isWhitespace(c) || Character.isISOControl(c)) {
				throw new IllegalArgumentException(
					"Key contains invalid characters:  ``" + key + "''");
			}
		}
		assert isAlive() : "IO Thread is not running.";
		conn.addOperation(key, op);
		return op;
	}

	Operation addOp(final MemcachedNode node, final Operation op) {
		if(shuttingDown) {
			throw new IllegalStateException("Shutting down");
		}
		assert isAlive() : "IO Thread is not running.";
		conn.addOperation(node, op);
		return op;
	}


	CountDownLatch broadcastOp(final OperationFactory of) {
		return broadcastOp(of, true);
	}


	private CountDownLatch broadcastOp(OperationFactory of,
			boolean checkShuttingDown) {
		if(checkShuttingDown && shuttingDown) {
			throw new IllegalStateException("Shutting down");
		}
		return conn.broadcastOperation(of);
	}

	private Future<Boolean> asyncStore(StoreOperationImpl.StoreType storeType,
			String key, int exp, Object value) {
		CachedData co=transcoder.encode(value);
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch);
		Operation op=new StoreOperationImpl(storeType, key, co.getFlags(),
				exp, co.getData(), new OperationCallback() {
					public void receivedStatus(String val) {
						rv.set(val.equals("STORED"));
					}
					public void complete() {
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(key, op);
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

		Operation op=new GetOperationImpl(key,
				new GetOperationImpl.Callback() {
			private Object val=null;
			public void receivedStatus(String line) {
				rv.set(val);
			}
			public void gotData(String k, int flags, byte[] data) {
				assert key.equals(k) : "Wrong key returned";
				val=transcoder.decode(new CachedData(flags, data));
			}
			public void complete() {
				latch.countDown();
			}});
		rv.setOperation(op);
		addOp(key, op);
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
		final Map<MemcachedNode, Collection<String>> chunks
			=new HashMap<MemcachedNode, Collection<String>>();
		final NodeLocator locator=conn.getLocator();
		for(String key : keys) {
			final MemcachedNode primaryNode=locator.getPrimary(key);
			MemcachedNode node=null;
			if(primaryNode.isActive()) {
				node=primaryNode;
			} else {
				for(Iterator<MemcachedNode> i=locator.getSequence(key);
					node == null && i.hasNext();) {
					MemcachedNode n=i.next();
					if(n.isActive()) {
						node=n;
					}
				}
				if(node == null) {
					node=primaryNode;
				}
			}
			assert node != null : "Didn't find a node for " + key;
			Collection<String> ks=chunks.get(node);
			if(ks == null) {
				ks=new ArrayList<String>();
				chunks.put(node, ks);
			}
			ks.add(key);
		}
		final CountDownLatch latch=new CountDownLatch(chunks.size());
		final Collection<Operation> ops=new ArrayList<Operation>();

		GetOperationImpl.Callback cb=new GetOperationImpl.Callback() {
				@SuppressWarnings("synthetic-access")
				public void receivedStatus(String line) {
					if(!line.equals("END")) {
						getLogger().warn("Expected ``END'', was ``%s''", line);
					}
				}
				public void gotData(String k, int flags, byte[] data) {
					m.put(k, transcoder.decode(new CachedData(flags, data)));
				}
				public void complete() {
					latch.countDown();
				}
		};
		for(Map.Entry<MemcachedNode, Collection<String>> me
				: chunks.entrySet()) {
			ops.add(addOp(me.getKey(), new GetOperationImpl(me.getValue(), cb)));
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

		CountDownLatch blatch = broadcastOp(new OperationFactory(){
			public Operation newOp(final MemcachedNode n,
					final CountDownLatch latch) {
				final SocketAddress sa=n.getSocketAddress();
				return new VersionOperationImpl(
						new OperationCallback() {
							public void receivedStatus(String s) {
								rv.put(sa, s);
							}

							public void complete() {
								latch.countDown();
							}
						});
			}});
		try {
			blatch.await();
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

	private Map<SocketAddress, Map<String, String>> getStats(final String arg) {
		final Map<SocketAddress, Map<String, String>> rv
			=new HashMap<SocketAddress, Map<String, String>>();

		CountDownLatch blatch = broadcastOp(new OperationFactory(){
			public Operation newOp(final MemcachedNode n,
				final CountDownLatch latch) {
				final SocketAddress sa=n.getSocketAddress();
				rv.put(sa, new HashMap<String, String>());
				return new StatsOperationImpl(arg,
						new StatsOperationImpl.Callback() {
					public void gotStat(String name, String val) {
						rv.get(sa).put(name, val);
					}
					@SuppressWarnings("synthetic-access")
					public void receivedStatus(String line) {
						if(!line.equals("END")) {
							getLogger().warn("Expeted ``END'', was ``%s''",
								line);
						}
					}
					public void complete() {
						latch.countDown();
					}});
			}});
		try {
			blatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for stats", e);
		}
		return rv;
	}

	private long mutate(MutatorOperationImpl.Mutator m, String key, int by) {
		final AtomicLong rv=new AtomicLong();
		final CountDownLatch latch=new CountDownLatch(1);
		addOp(key, new MutatorOperationImpl(m, key, by,
				new OperationCallback() {
					public void receivedStatus(String val) {
						rv.set(new Long(val==null?"-1":val));
					}

					public void complete() {
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
		return mutate(MutatatorOperation.Mutator.incr, key, by);
	}

	/**
	 * Decrement the given key by the given value.
	 * 
	 * @param key the key
	 * @param by the value
	 * @return the new value (-1 if the key doesn't exist)
	 */
	public long decr(String key, int by) {
		return mutate(MutatatorOperation.Mutator.decr, key, by);
	}

	private long mutateWithDefault(MutatorOperationImpl.Mutator t, String key,
			int by, long def) {
		long rv=mutate(t, key, by);
		if(rv == -1) {
			Future<Boolean> f=asyncStore(StoreOperation.StoreType.add,
					key, 0,	String.valueOf(def));
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
		return mutateWithDefault(MutatatorOperation.Mutator.incr,
				key, by, def);
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
		return mutateWithDefault(MutatatorOperation.Mutator.decr, key, by, def);
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
		DeleteOperation op=new DeleteOperationImpl(key, when,
				new OperationCallback() {
					public void receivedStatus(String line) {
						rv.set(line.equals("DELETED"));
					}

					public void complete() {
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(key, op);
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
	public Future<Boolean> flush(final int delay) {
		final AtomicReference<Boolean> flushResult=
			new AtomicReference<Boolean>(null);
		CountDownLatch blatch = broadcastOp(new OperationFactory(){
			public Operation newOp(final MemcachedNode n,
					final CountDownLatch latch) {
				return new FlushOperationImpl(delay, new OperationCallback(){
					public void receivedStatus(String line) {
						flushResult.set(line.equals("OK"));
					}

					public void complete() {
						latch.countDown();
					}});
			}});
		return new OperationFuture<Boolean>(blatch, flushResult);
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
	 * Shut down this client gracefully.
	 */
	public boolean shutdown(long timeout, TimeUnit unit) {
		shuttingDown=true;
		String baseName=getName();
		setName(baseName + " - SHUTTING DOWN");
		boolean rv=false;
		try {
			// Conditionally wait
			if(timeout > 0) {
				setName(baseName + " - SHUTTING DOWN (waiting)");
				rv=waitForQueues(timeout, unit);
			}
		} finally {
			// But always begin the shutdown sequence
			try {
				setName(baseName + " - SHUTTING DOWN (telling client)");
				running=false;
				conn.shutdown();
				setName(baseName + " - SHUTTING DOWN (informed client)");
			} catch (IOException e) {
				getLogger().warn("exception while shutting down", e);
			}
		}
		return rv;
	}

	/**
	 * Wait for the queues to die down.
	 */
	public boolean waitForQueues(long timeout, TimeUnit unit) {
		CountDownLatch blatch = broadcastOp(new OperationFactory(){
			public Operation newOp(final MemcachedNode n,
					final CountDownLatch latch) {
				return new VersionOperationImpl(
						new OperationCallback() {
							// XXX:  Why do I count down the latch on two
							// conditions?
							public void receivedStatus(String s) {
								latch.countDown();
							}
							public void complete() {
								latch.countDown();
							}
						});
			}}, false);
		try {
			return blatch.await(timeout, unit);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for queues", e);
		}
	}

	static class BulkGetFuture implements Future<Map<String, Object>> {
		private final Map<String, Object> rvMap;
		private final Collection<Operation> ops;
		private final CountDownLatch latch;
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
				if(op.hasErrored()) {
					throw new ExecutionException(op.getException());
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

	static class OperationFuture<T> implements Future<T> {

		private final CountDownLatch latch;
		private final AtomicReference<T> objRef;
		private Operation op;

		public OperationFuture(CountDownLatch l) {
			this(l, new AtomicReference<T>(null));
		}

		public OperationFuture(CountDownLatch l, AtomicReference<T> oref) {
			super();
			latch=l;
			objRef=oref;
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
			if(op != null && op.hasErrored()) {
				throw new ExecutionException(op.getException());
			}
			return objRef.get();
		}

		public T get(long duration, TimeUnit units)
			throws InterruptedException, TimeoutException {
			latch.await(duration, units);
			return objRef.get();
		}

		void set(T o) {
			objRef.set(o);
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
