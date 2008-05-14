// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.SpyThread;
import net.spy.memcached.ops.CASOperationStatus;
import net.spy.memcached.ops.CancelledOperationStatus;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

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
public final class MemcachedClient extends SpyThread {

	/**
	 * Maximum supported key length.
	 */
	public static final int MAX_KEY_LENGTH = 250;

	private volatile boolean running=true;
	private volatile boolean shuttingDown=false;

    private final long operationTimeout;

    private final MemcachedConnection conn;
	final OperationFactory opFact;

	Transcoder<Object> transcoder=null;

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
		opFact=cf.getOperationFactory();
		assert opFact != null : "Connection factory failed to make op factory";
		conn=cf.createConnection(addrs);
		assert conn != null : "Connection factory failed to make a connection";
		operationTimeout = cf.getOperationTimeout();
		setName("Memcached IO over " + conn);
		setDaemon(cf.isDaemon());
		start();
	}

	/**
	 * Get the addresses of available servers.
	 *
	 * <p>
	 * This is based on a snapshot in time so shouldn't be considered
	 * completely accurate, but is a useful for getting a feel for what's
	 * working and what's not working.
	 * </p>
	 */
	public Collection<SocketAddress> getAvailableServers() {
		Collection<SocketAddress> rv=new ArrayList<SocketAddress>();
		for(MemcachedNode node : conn.getLocator().getAll()) {
			if(node.isActive()) {
				rv.add(node.getSocketAddress());
			}
		}
		return rv;
	}

	/**
	 * Get the addresses of unavailable servers.
	 *
	 * <p>
	 * This is based on a snapshot in time so shouldn't be considered
	 * completely accurate, but is a useful for getting a feel for what's
	 * working and what's not working.
	 * </p>
	 */
	public Collection<SocketAddress> getUnavailableServers() {
		Collection<SocketAddress> rv=new ArrayList<SocketAddress>();
		for(MemcachedNode node : conn.getLocator().getAll()) {
			if(!node.isActive()) {
				rv.add(node.getSocketAddress());
			}
		}
		return rv;
	}

	/**
	 * Set the default transcoder for managing the cache representations
	 * of objects going in and out of the cache.
	 */
	public void setTranscoder(Transcoder<Object> tc) {
		if(tc == null) {
			throw new NullPointerException("Can't use a null transcoder");
		}
		transcoder=tc;
	}

	/**
	 * Get the default transcoder that's in use.
	 */
	public Transcoder<Object> getTranscoder() {
		return transcoder;
	}

    private void validateKey(String key) {
		byte[] keyBytes=KeyUtil.getKeyBytes(key);
		if(keyBytes.length > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ MAX_KEY_LENGTH + ")");
		}
		// Validate the key
		for(byte b : keyBytes) {
			if(b == ' ' || b == '\n' || b == '\r' || b == 0) {
				throw new IllegalArgumentException(
					"Key contains invalid characters:  ``" + key + "''");
			}
		}
	}

	private void checkState() {
		if(shuttingDown) {
			throw new IllegalStateException("Shutting down");
		}
		assert isAlive() : "IO Thread is not running.";
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
		validateKey(key);
		checkState();
		conn.addOperation(key, op);
		return op;
	}

	CountDownLatch broadcastOp(final BroadcastOpFactory of) {
		return broadcastOp(of, true);
	}

	private CountDownLatch broadcastOp(BroadcastOpFactory of,
			boolean checkShuttingDown) {
		if(checkShuttingDown && shuttingDown) {
			throw new IllegalStateException("Shutting down");
		}
		return conn.broadcastOperation(of);
	}

	private <T> Future<Boolean> asyncStore(StoreType storeType, String key,
					       int exp, T value, Transcoder<T> tc) {
		CachedData co=tc.encode(value);
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch,
				operationTimeout);
		Operation op=opFact.store(storeType, key, co.getFlags(),
				exp, co.getData(), new OperationCallback() {
					public void receivedStatus(OperationStatus val) {
						rv.set(val.isSuccess());
					}
					public void complete() {
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(key, op);
		return rv;
	}

	private Future<Boolean> asyncStore(StoreType storeType,
			String key, int exp, Object value) {
		return asyncStore(storeType, key, exp, value, transcoder);
	}

	private <T> Future<Boolean> asyncCat(
			ConcatenationType catType, long cas, String key,
			T value, Transcoder<T> tc) {
		CachedData co=tc.encode(value);
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch,
				operationTimeout);
		Operation op=opFact.cat(catType, cas, key, co.getData(),
				new OperationCallback() {
			public void receivedStatus(OperationStatus val) {
				rv.set(val.isSuccess());
			}
			public void complete() {
				latch.countDown();
			}});
		rv.setOperation(op);
		addOp(key, op);
		return rv;
	}

	/**
	 * Append to an existing value in the cache.
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be appended
	 * @param val the value to append
	 * @return a future indicating success
	 */
	public Future<Boolean> append(long cas, String key, Object val) {
		return append(cas, key, val, transcoder);
	}

	/**
	 * Append to an existing value in the cache.
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be appended
	 * @param val the value to append
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future indicating success
	 */
	public <T> Future<Boolean> append(long cas, String key, T val,
			Transcoder<T> tc) {
		return asyncCat(ConcatenationType.append, cas, key, val, tc);
	}

	/**
	 * Prepend to an existing value in the cache.
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be prepended
	 * @param val the value to append
	 * @return a future indicating success
	 */
	public Future<Boolean> prepend(long cas, String key, Object val) {
		return prepend(cas, key, val, transcoder);
	}

	/**
	 * Prepend to an existing value in the cache.
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be prepended
	 * @param val the value to append
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future indicating success
	 */
	public <T> Future<Boolean> prepend(long cas, String key, T val,
			Transcoder<T> tc) {
		return asyncCat(ConcatenationType.prepend, cas, key, val, tc);
	}

	/**
	 * Asynchronous CAS operation.
	 *
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param value the new value
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future that will indicate the status of the CAS
	 */
	public <T> Future<CASResponse> asyncCAS(String key, long casId, T value,
			Transcoder<T> tc) {
		CachedData co=tc.encode(value);
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<CASResponse> rv=new OperationFuture<CASResponse>(
				latch, operationTimeout);
		Operation op=opFact.cas(key, casId, co.getFlags(),
				co.getData(), new OperationCallback() {
					public void receivedStatus(OperationStatus val) {
						if(val instanceof CASOperationStatus) {
							rv.set(((CASOperationStatus)val).getCASResponse());
						} else if(val instanceof CancelledOperationStatus) {
							// Cancelled, ignore and let it float up
						} else {
							throw new RuntimeException(
								"Unhandled state: " + val);
						}
					}
					public void complete() {
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(key, op);
		return rv;
	}

	/**
	 * Asynchronous CAS operation using the default transcoder.
	 *
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param value the new value
	 * @return a future that will indicate the status of the CAS
	 */
	public Future<CASResponse> asyncCAS(String key, long casId, Object value) {
		return asyncCAS(key, casId, value, transcoder);
	}

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
	public <T> CASResponse cas(String key, long casId, T value,
			Transcoder<T> tc) throws OperationTimeoutException {
		try {
			return asyncCAS(key, casId, value, tc).get(operationTimeout,
					TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for value", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Exception waiting for value", e);
		} catch (TimeoutException e) {
			throw new OperationTimeoutException("Timeout waiting for value", e);
		}
	}

	/**
	 * Perform a synchronous CAS operation with the default transcoder.
	 *
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param value the new value
	 * @return a CASResponse
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public CASResponse cas(String key, long casId, Object value)
		throws OperationTimeoutException {
		return cas(key, casId, value, transcoder);
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
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 */
	public <T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc) {
		return asyncStore(StoreType.add, key, exp, o, tc);
	}

	/**
	 * Add an object to the cache (using the default transcoder)
	 * iff it does not exist already.
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
		return asyncStore(StoreType.add, key, exp, o, transcoder);
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
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 */
	public <T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc) {
		return asyncStore(StoreType.set, key, exp, o, tc);
	}

	/**
	 * Set an object in the cache (using the default transcoder)
	 * regardless of any existing value.
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
		return asyncStore(StoreType.set, key, exp, o, transcoder);
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
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 */
	public <T> Future<Boolean> replace(String key, int exp, T o,
		Transcoder<T> tc) {
		return asyncStore(StoreType.replace, key, exp, o, tc);
	}

	/**
	 * Replace an object with the given value (transcoded with the default
	 * transcoder) iff there is already a value for the given key.
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
		return asyncStore(StoreType.replace, key, exp, o, transcoder);
	}

	/**
	 * Get the given key asynchronously.
	 *
	 * @param key the key to fetch
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a future that will hold the return value of the fetch
	 */
	public <T> Future<T> asyncGet(final String key, final Transcoder<T> tc) {

		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<T> rv=new OperationFuture<T>(latch,
			operationTimeout);

		Operation op=opFact.get(key,
				new GetOperation.Callback() {
			private T val=null;
			public void receivedStatus(OperationStatus status) {
				rv.set(val);
			}
			public void gotData(String k, int flags, byte[] data) {
				assert key.equals(k) : "Wrong key returned";
				val=tc.decode(new CachedData(flags, data));
			}
			public void complete() {
				latch.countDown();
			}});
		rv.setOperation(op);
		addOp(key, op);
		return rv;
	}

	/**
	 * Get the given key asynchronously and decode with the default
	 * transcoder.
	 *
	 * @param key the key to fetch
	 * @return a future that will hold the return value of the fetch
	 */
	public Future<Object> asyncGet(final String key) {
		return asyncGet(key, transcoder);
	}

	/**
	 * Gets (with CAS support) the given key asynchronously.
	 *
	 * @param key the key to fetch
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a future that will hold the return value of the fetch
	 */
	public <T> Future<CASValue<T>> asyncGets(final String key,
			final Transcoder<T> tc) {

		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<CASValue<T>> rv=
			new OperationFuture<CASValue<T>>(latch, operationTimeout);

		Operation op=opFact.gets(key,
				new GetsOperation.Callback() {
			private CASValue<T> val=null;
			public void receivedStatus(OperationStatus status) {
				rv.set(val);
			}
			public void gotData(String k, int flags, long cas, byte[] data) {
				assert key.equals(k) : "Wrong key returned";
				assert cas > 0 : "CAS was less than zero:  " + cas;
				val=new CASValue<T>(cas,
						tc.decode(new CachedData(flags, data)));
			}
			public void complete() {
				latch.countDown();
			}});
		rv.setOperation(op);
		addOp(key, op);
		return rv;
	}

	/**
	 * Gets (with CAS support) the given key asynchronously and decode using
	 * the default transcoder.
	 *
	 * @param key the key to fetch
	 * @return a future that will hold the return value of the fetch
	 */
	public Future<CASValue<Object>> asyncGets(final String key) {
		return asyncGets(key, transcoder);
	}

	/**
	 * Gets (with CAS support) with a single key.
	 *
	 * @param key the key to get
	 * @param tc the transcoder to serialize and unserialize value
	 * @return the result from the cache and CAS id (null if there is none)
     * @throws OperationTimeoutException if global operation timeout is exceeded
	 */
	public <T> CASValue<T> gets(String key, Transcoder<T> tc)
		throws OperationTimeoutException {
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

	/**
	 * Gets (with CAS support) with a single key using the default transcoder.
	 *
	 * @param key the key to get
	 * @return the result from the cache and CAS id (null if there is none)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public CASValue<Object> gets(String key) throws OperationTimeoutException {
		return gets(key, transcoder);
	}

	/**
	 * Get with a single key.
	 *
	 * @param key the key to get
	 * @param tc the transcoder to serialize and unserialize value
	 * @return the result from the cache (null if there is none)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public <T> T get(String key, Transcoder<T> tc)
		throws OperationTimeoutException {
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

	/**
	 * Get with a single key and decode using the default transcoder.
	 *
	 * @param key the key to get
	 * @return the result from the cache (null if there is none)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public Object get(String key) throws OperationTimeoutException {
		return get(key, transcoder);
	}

	/**
	 * Asynchronously get a bunch of objects from the cache.
	 *
	 * @param keys the keys to request
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a Future result of that fetch
	 */
	public <T> Future<Map<String, T>> asyncGetBulk(Collection<String> keys,
		final Transcoder<T> tc) {
		final Map<String, T> m=new ConcurrentHashMap<String, T>();
		// Break the gets down into groups by key
		final Map<MemcachedNode, Collection<String>> chunks
			=new HashMap<MemcachedNode, Collection<String>>();
		final NodeLocator locator=conn.getLocator();
		for(String key : keys) {
			validateKey(key);
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

		GetOperation.Callback cb=new GetOperation.Callback() {
				@SuppressWarnings("synthetic-access")
				public void receivedStatus(OperationStatus status) {
					if(!status.isSuccess()) {
						getLogger().warn("Unsuccessful get:  %s", status);
					}
				}
				public void gotData(String k, int flags, byte[] data) {
					T val = tc.decode(new CachedData(flags, data));
					// val may be null if the transcoder did not understand
					// the value.
					if(val != null) {
						m.put(k, val);
					}
				}
				public void complete() {
					latch.countDown();
				}
		};

		// Now that we know how many servers it breaks down into, and the latch
		// is all set up, convert all of these strings collections to operations
		final Map<MemcachedNode, Operation> mops=
			new HashMap<MemcachedNode, Operation>();

		for(Map.Entry<MemcachedNode, Collection<String>> me
				: chunks.entrySet()) {
			Operation op=opFact.get(me.getValue(), cb);
			mops.put(me.getKey(), op);
			ops.add(op);
		}
		assert mops.size() == chunks.size();
		checkState();
		conn.addOperations(mops);
		return new BulkGetFuture<T>(m, ops, latch);
	}

	/**
	 * Asynchronously get a bunch of objects from the cache and decode them
	 * with the given transcoder.
	 *
	 * @param keys the keys to request
	 * @return a Future result of that fetch
	 */
	public Future<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
		return asyncGetBulk(keys, transcoder);
	}

	/**
	 * Varargs wrapper for asynchronous bulk gets.
	 *
	 * @param tc the transcoder to serialize and unserialize value
	 * @param keys one more more keys to get
	 * @return the future values of those keys
	 */
	public <T> Future<Map<String, T>> asyncGetBulk(Transcoder<T> tc,
		String... keys) {
		return asyncGetBulk(Arrays.asList(keys), tc);
	}

	/**
	 * Varargs wrapper for asynchronous bulk gets with the default transcoder.
	 *
	 * @param keys one more more keys to get
	 * @return the future values of those keys
	 */
	public Future<Map<String, Object>> asyncGetBulk(String... keys) {
		return asyncGetBulk(Arrays.asList(keys), transcoder);
	}

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param keys the keys
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public <T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc)
		throws OperationTimeoutException {
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

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public Map<String, Object> getBulk(Collection<String> keys)
		throws OperationTimeoutException {
		return getBulk(keys, transcoder);
	}

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param tc the transcoder to serialize and unserialize value
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys)
		throws OperationTimeoutException {
		return getBulk(Arrays.asList(keys), tc);
	}

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public Map<String, Object> getBulk(String... keys)
		throws OperationTimeoutException {
		return getBulk(Arrays.asList(keys), transcoder);
	}

	/**
	 * Get the versions of all of the connected memcacheds.
	 */
	public Map<SocketAddress, String> getVersions() {
		final Map<SocketAddress, String>rv=
			new ConcurrentHashMap<SocketAddress, String>();

		CountDownLatch blatch = broadcastOp(new BroadcastOpFactory(){
			public Operation newOp(final MemcachedNode n,
					final CountDownLatch latch) {
				final SocketAddress sa=n.getSocketAddress();
				return opFact.version(
						new OperationCallback() {
							public void receivedStatus(OperationStatus s) {
								rv.put(sa, s.getMessage());
							}
							public void complete() {
								latch.countDown();
							}
						});
			}});
		try {
			blatch.await(operationTimeout, TimeUnit.MILLISECONDS);
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

		CountDownLatch blatch = broadcastOp(new BroadcastOpFactory(){
			public Operation newOp(final MemcachedNode n,
				final CountDownLatch latch) {
				final SocketAddress sa=n.getSocketAddress();
				rv.put(sa, new HashMap<String, String>());
				return opFact.stats(arg,
						new StatsOperation.Callback() {
					public void gotStat(String name, String val) {
						rv.get(sa).put(name, val);
					}
					@SuppressWarnings("synthetic-access") // getLogger()
					public void receivedStatus(OperationStatus status) {
						if(!status.isSuccess()) {
							getLogger().warn("Unsuccessful stat fetch:  %s",
									status);
						}
					}
					public void complete() {
						latch.countDown();
					}});
			}});
		try {
			blatch.await(operationTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for stats", e);
		}
		return rv;
	}

	private long mutate(Mutator m, String key, int by, long def, int exp)
		throws OperationTimeoutException {
		final AtomicLong rv=new AtomicLong();
		final CountDownLatch latch=new CountDownLatch(1);
		addOp(key, opFact.mutate(m, key, by, def, exp, new OperationCallback() {
					public void receivedStatus(OperationStatus s) {
						// XXX:  Potential abstraction leak.
						// The handling of incr/decr in the binary protocol
						// Allows us to avoid string processing.
						rv.set(new Long(s.isSuccess()?s.getMessage():"-1"));
					}
					public void complete() {
						latch.countDown();
					}}));
		try {
			if (!latch.await(operationTimeout, TimeUnit.MILLISECONDS)) {
				throw new OperationTimeoutException(
					"Mutate operation timed out, unable to modify counter ["
						+ key + "]");
			}
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
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public long incr(String key, int by) throws OperationTimeoutException {
		return mutate(Mutator.incr, key, by, 0, -1);
	}

	/**
	 * Decrement the given key by the given value.
	 *
	 * @param key the key
	 * @param by the value
	 * @return the new value (-1 if the key doesn't exist)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public long decr(String key, int by) throws OperationTimeoutException {
		return mutate(Mutator.decr, key, by, 0, -1);
	}

	private long mutateWithDefault(Mutator t, String key,
			int by, long def, int exp) throws OperationTimeoutException {
		long rv=mutate(t, key, by, def, exp);
		// The ascii protocol doesn't support defaults, so I added them
		// manually here.
		if(rv == -1) {
			Future<Boolean> f=asyncStore(StoreType.add,
					key, 0,	String.valueOf(def));
			try {
				if(f.get(operationTimeout, TimeUnit.MILLISECONDS)) {
					rv=def;
				} else {
					rv=mutate(t, key, by, 0, 0);
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

	/**
	 * Increment the given counter, returning the new value.
	 *
	 * @param key the key
	 * @param by the amount to increment
	 * @param def the default value (if the counter does not exist)
	 * @return the new value, or -1 if we were unable to increment or add
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public long incr(String key, int by, int def)
		throws OperationTimeoutException {
		return mutateWithDefault(Mutator.incr, key, by, def, 0);
	}

	/**
	 * Decrement the given counter, returning the new value.
	 *
	 * @param key the key
	 * @param by the amount to decrement
	 * @param def the default value (if the counter does not exist)
	 * @return the new value, or -1 if we were unable to decrement or add
	 * @throws OperationTimeoutException if the global operation timeout is
	 *         exceeded
	 */
	public long decr(String key, int by, long def)
		throws OperationTimeoutException {
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
	 */
	public Future<Boolean> delete(String key, int hold) {
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch,
			operationTimeout);
		DeleteOperation op=opFact.delete(key, hold,
				new OperationCallback() {
					public void receivedStatus(OperationStatus s) {
						rv.set(s.isSuccess());
					}
					public void complete() {
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(key, op);
		return rv;
	}

	/**
	 * Shortcut to delete that doesn't put a hold on the key.
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
		final ConcurrentLinkedQueue<Operation> ops=
			new ConcurrentLinkedQueue<Operation>();
		CountDownLatch blatch = broadcastOp(new BroadcastOpFactory(){
			public Operation newOp(final MemcachedNode n,
					final CountDownLatch latch) {
				Operation op=opFact.flush(delay, new OperationCallback(){
					public void receivedStatus(OperationStatus s) {
						flushResult.set(s.isSuccess());
					}
					public void complete() {
						latch.countDown();
					}});
				ops.add(op);
				return op;
			}});
		return new OperationFuture<Boolean>(blatch, flushResult,
				operationTimeout) {
			@Override
			public boolean cancel(boolean ign) {
				boolean rv=false;
				for(Operation op : ops) {
					op.cancel();
					rv |= op.getState() == OperationState.WRITING;
				}
				return rv;
			}
			@Override
			public boolean isCancelled() {
				boolean rv=false;
				for(Operation op : ops) {
					rv |= op.isCancelled();
				}
				return rv;
			}
			@Override
			public boolean isDone() {
				boolean rv=true;
				for(Operation op : ops) {
					rv &= op.getState() == OperationState.COMPLETE;
				}
				return rv || isCancelled();
			}
		};
	}

	/**
	 * Flush all caches from all servers immediately.
	 */
	public Future<Boolean> flush() {
		return flush(-1);
	}

	private void logRunException(Exception e) {
		if(shuttingDown) {
			// There are a couple types of errors that occur during the
			// shutdown sequence that are considered OK.  Log at debug.
			getLogger().debug("Exception occurred during shutdown", e);
		} else {
			getLogger().warn("Problem handling memcached IO", e);
		}
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
				logRunException(e);
			} catch(CancelledKeyException e) {
				logRunException(e);
			} catch(ClosedSelectorException e) {
				logRunException(e);
			} catch(IllegalStateException e) {
				logRunException(e);
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
		// Guard against double shutdowns (bug 8).
		if(shuttingDown) {
			getLogger().info("Suppressing duplicate attempt to shut down");
			return false;
		}
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
		CountDownLatch blatch = broadcastOp(new BroadcastOpFactory(){
			public Operation newOp(final MemcachedNode n,
					final CountDownLatch latch) {
				return opFact.noop(
						new OperationCallback() {
							public void complete() {
								latch.countDown();
							}
							public void receivedStatus(OperationStatus s) {
								// Nothing special when receiving status, only
								// necessary to complete the interface
							}
						});
			}}, false);
		try {
			return blatch.await(timeout, unit);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for queues", e);
		}
	}

	static class BulkGetFuture<T> implements Future<Map<String, T>> {
		private final Map<String, T> rvMap;
		private final Collection<Operation> ops;
		private final CountDownLatch latch;
		private boolean cancelled=false;

		public BulkGetFuture(Map<String, T> m,
				Collection<Operation> getOps, CountDownLatch l) {
			super();
			rvMap = m;
			ops = getOps;
			latch=l;
		}

		public boolean cancel(boolean ign) {
			boolean rv=false;
			for(Operation op : ops) {
				rv |= op.getState() == OperationState.WRITING;
				op.cancel();
			}
			cancelled=true;
			return rv;
		}

		public Map<String, T> get()
			throws InterruptedException, ExecutionException {
			try {
				return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("Timed out waiting forever", e);
			}
		}

		public Map<String, T> get(long timeout, TimeUnit unit)
			throws InterruptedException,
			ExecutionException, TimeoutException {
			if(!latch.await(timeout, unit)) {
				throw new TimeoutException("Operation timed out.");
			}
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
		private final long globalOperationTimeout;

		public OperationFuture(CountDownLatch l, long globalOperationTimeout) {
			this(l, new AtomicReference<T>(null), globalOperationTimeout);
		}

		public OperationFuture(CountDownLatch l, AtomicReference<T> oref,
			long timeout) {
			super();
			latch=l;
			objRef=oref;
			globalOperationTimeout = timeout;
		}

		public boolean cancel(boolean ign) {
			assert op != null : "No operation";
			op.cancel();
			// This isn't exactly correct, but it's close enough.  If we're in
			// a writing state, we *probably* haven't started.
			return op.getState() == OperationState.WRITING;
		}

		public T get() throws InterruptedException, ExecutionException {
			try {
				return get(globalOperationTimeout, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException(
					"Timed out waiting for operation", e);
			}
		}

		public T get(long duration, TimeUnit units)
			throws InterruptedException, TimeoutException, ExecutionException {
			if(!latch.await(duration, units)) {
				throw new TimeoutException("Timed out waiting for operation");
			}
			if(op != null && op.hasErrored()) {
				throw new ExecutionException(op.getException());
			}
			if(isCancelled()) {
				throw new ExecutionException(new RuntimeException("Cancelled"));
			}

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
			return latch.getCount() == 0 ||
				op.isCancelled() || op.getState() == OperationState.COMPLETE;
		}

	}
}
