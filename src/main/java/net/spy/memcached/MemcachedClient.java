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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.AuthThreadMonitor;
import net.spy.memcached.compat.SpyThread;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.BulkGetFuture;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.internal.SingleElementInfiniteIterator;
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
import net.spy.memcached.transcoders.TranscodeService;
import net.spy.memcached.transcoders.Transcoder;

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
 *      // Get a memcached client connected to several servers
 *      // over the binary protocol
 *      MemcachedClient c = new MemcachedClient(new BinaryConnectionFactory(),
 *              AddrUtil.getAddresses("server1:11211 server2:11211"));
 *
 *      // Try to get a value, for up to 5 seconds, and cancel if it
 *      // doesn't return
 *      Object myObj = null;
 *      Future&lt;Object&gt; f = c.asyncGet("someKey");
 *      try {
 *          myObj = f.get(5, TimeUnit.SECONDS);
 *      // throws expecting InterruptedException, ExecutionException
 *      // or TimeoutException
 *      } catch (Exception e) {  /*  /
 *          // Since we don't need this, go ahead and cancel the operation.
 *          // This is not strictly necessary, but it'll save some work on
 *          // the server.  It is okay to cancel it if running.
 *          f.cancel(true);
 *          // Do other timeout related stuff
 *      }
 * </pre>
 */
public class MemcachedClient extends SpyThread
	implements MemcachedClientIF, ConnectionObserver {

	private volatile boolean running=true;
	private volatile boolean shuttingDown=false;
	public boolean isFrontCache = false;

	private final long operationTimeout;

	private final MemcachedConnection conn;
	final OperationFactory opFact;

	final Transcoder<Object> transcoder;

	final TranscodeService tcService;

	final AuthDescriptor authDescriptor;

	private final AuthThreadMonitor authMonitor = new AuthThreadMonitor();

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
	 * @param cf the connection factory to configure connections for this client
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
		tcService = new TranscodeService(cf.isDaemon());
		transcoder=cf.getDefaultTranscoder();
		opFact=cf.getOperationFactory();
		assert opFact != null : "Connection factory failed to make op factory";
		conn=cf.createConnection(addrs);
		assert conn != null : "Connection factory failed to make a connection";
		operationTimeout = cf.getOperationTimeout();
		authDescriptor = cf.getAuthDescriptor();
		if(authDescriptor != null) {
			addObserver(this);
		}
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
	 *
	 * @return point-in-time view of currently available servers
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
	 *
	 * @return point-in-time view of currently available servers
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
	 * Get a read-only wrapper around the node locator wrapping this instance.
	 *
	 * @return this instance's NodeLocator
	 */
	public NodeLocator getNodeLocator() {
		return conn.getLocator().getReadonlyCopy();
	}

	/**
	 * Get the default transcoder that's in use.
	 *
	 * @return this instance's Transcoder
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
		if(keyBytes.length == 0) {
			throw new IllegalArgumentException(
				"Key must contain at least one character.");
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
		return broadcastOp(of, conn.getLocator().getAll(), true);
	}

	CountDownLatch broadcastOp(final BroadcastOpFactory of,
			Collection<MemcachedNode> nodes) {
		return broadcastOp(of, nodes, true);
	}

	private CountDownLatch broadcastOp(BroadcastOpFactory of,
			Collection<MemcachedNode> nodes,
			boolean checkShuttingDown) {
		if(checkShuttingDown && shuttingDown) {
			throw new IllegalStateException("Shutting down");
		}
		return conn.broadcastOperation(of, nodes);
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
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be appended
	 * @param val the value to append
	 * @return a future indicating success, false if there was no change
	 *         to the value
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Boolean> append(long cas, String key, Object val) {
		return append(cas, key, val, transcoder);
	}

	/**
	 * Append to an existing value in the cache.
	 *
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
	 *
	 * @param <T>
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be appended
	 * @param val the value to append
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future indicating success
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> Future<Boolean> append(long cas, String key, T val,
			Transcoder<T> tc) {
		return asyncCat(ConcatenationType.append, cas, key, val, tc);
	}

	/**
	 * Prepend to an existing value in the cache.
	 *
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
	 *
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be prepended
	 * @param val the value to append
	 * @return a future indicating success
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Boolean> prepend(long cas, String key, Object val) {
		return prepend(cas, key, val, transcoder);
	}

	/**
	 * Prepend to an existing value in the cache.
	 *
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
	 *
	 * @param <T>
	 * @param cas cas identifier (ignored in the ascii protocol)
	 * @param key the key to whose value will be prepended
	 * @param val the value to append
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future indicating success
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> Future<Boolean> prepend(long cas, String key, T val,
			Transcoder<T> tc) {
		return asyncCat(ConcatenationType.prepend, cas, key, val, tc);
	}

	/**
     * Asynchronous CAS operation.
     *
     * @param <T>
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param value the new value
     * @param tc the transcoder to serialize and unserialize the value
     * @return a future that will indicate the status of the CAS
     * @throws IllegalStateException in the rare circumstance where queue
     *         is too full to accept any more requests
     */
    public <T> Future<CASResponse> asyncCAS(String key, long casId, T value,
            Transcoder<T> tc) {
        return asyncCAS(key, casId, 0, value, tc);
	}

	/**
	 * Asynchronous CAS operation.
	 *
	 * @param <T>
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param exp the expiration of this object
	 * @param value the new value
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future that will indicate the status of the CAS
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> Future<CASResponse> asyncCAS(String key, long casId, int exp, T value,
			Transcoder<T> tc) {
		CachedData co=tc.encode(value);
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<CASResponse> rv=new OperationFuture<CASResponse>(
				latch, operationTimeout);
		Operation op=opFact.cas(StoreType.set, key, casId, co.getFlags(), exp,
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
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<CASResponse> asyncCAS(String key, long casId, Object value) {
		return asyncCAS(key, casId, value, transcoder);
	}

	/**
     * Perform a synchronous CAS operation.
     *
     * @param <T>
     * @param key the key
     * @param casId the CAS identifier (from a gets operation)
     * @param value the new value
     * @param tc the transcoder to serialize and unserialize the value
     * @return a CASResponse
     * @throws OperationTimeoutException if global operation timeout is
     *         exceeded
     * @throws IllegalStateException in the rare circumstance where queue
     *         is too full to accept any more requests
     */
    public <T> CASResponse cas(String key, long casId, T value,
            Transcoder<T> tc) {
        return cas(key, casId, 0, value, tc);
    }

	/**
	 * Perform a synchronous CAS operation.
	 *
	 * @param <T>
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param exp the expiration of this object
	 * @param value the new value
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a CASResponse
	 * @throws OperationTimeoutException if global operation timeout is
	 *         exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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

	/**
	 * Perform a synchronous CAS operation with the default transcoder.
	 *
	 * @param key the key
	 * @param casId the CAS identifier (from a gets operation)
	 * @param value the new value
	 * @return a CASResponse
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public CASResponse cas(String key, long casId, Object value) {
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
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
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
	 * @param <T>
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
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
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
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
	 * @param <T>
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
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
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
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
	 * @param <T>
	 * @param key the key under which this object should be added.
	 * @param exp the expiration of this object
	 * @param o the object to store
	 * @param tc the transcoder to serialize and unserialize the value
	 * @return a future representing the processing of this operation
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 * <p>Note that the return will be false any time a mutation has not
	 * occurred.</p>
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
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Boolean> replace(String key, int exp, Object o) {
		return asyncStore(StoreType.replace, key, exp, o, transcoder);
	}

	/**
	 * Get the given key asynchronously.
	 *
	 * @param <T>
	 * @param key the key to fetch
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a future that will hold the return value of the fetch
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> Future<T> asyncGet(final String key, final Transcoder<T> tc) {

		final CountDownLatch latch=new CountDownLatch(1);
		final GetFuture<T> rv=new GetFuture<T>(latch, operationTimeout);

		Operation op=opFact.get(key,
				new GetOperation.Callback() {
			private Future<T> val=null;
			public void receivedStatus(OperationStatus status) {
				rv.set(val);
			}
			public void gotData(String k, int flags, byte[] data) {
				assert key.equals(k) : "Wrong key returned";
				val=tcService.decode(tc,
					new CachedData(flags, data, tc.getMaxSize()));
			}
			public void complete() {
				if (isFrontCache) {
					putFrontCache(key, val, operationTimeout);
				}
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
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Object> asyncGet(final String key) {
		return asyncGet(key, transcoder);
	}

	/**
	 * Gets (with CAS support) the given key asynchronously.
	 *
	 * @param <T>
	 * @param key the key to fetch
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a future that will hold the return value of the fetch
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
				val=new CASValue<T>(cas, tc.decode(
					new CachedData(flags, data, tc.getMaxSize())));
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
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<CASValue<Object>> asyncGets(final String key) {
		return asyncGets(key, transcoder);
	}

	/**
	 * Gets (with CAS support) with a single key.
	 *
	 * @param <T>
	 * @param key the key to get
	 * @param tc the transcoder to serialize and unserialize value
	 * @return the result from the cache and CAS id (null if there is none)
	 * @throws OperationTimeoutException if global operation timeout is
	 * 		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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

	/**
	 * Gets (with CAS support) with a single key using the default transcoder.
	 *
	 * @param key the key to get
	 * @return the result from the cache and CAS id (null if there is none)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public CASValue<Object> gets(String key) {
		return gets(key, transcoder);
	}

	/**
	 * Get with a single key.
	 *
	 * @param <T>
	 * @param key the key to get
	 * @param tc the transcoder to serialize and unserialize value
	 * @return the result from the cache (null if there is none)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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

	/**
	 * Get with a single key and decode using the default transcoder.
	 *
	 * @param key the key to get
	 * @return the result from the cache (null if there is none)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Object get(String key) {
		return get(key, transcoder);
	}

	/**
	 * Asynchronously get a bunch of objects from the cache.
	 *
	 * @param <T>
	 * @param keys the keys to request
	 * @param tc_iter an iterator of transcoders to serialize and
	 *        unserialize values; the transcoders are matched with
	 *        the keys in the same order.  The minimum of the key
	 *        collection length and number of transcoders is used
	 *        and no exception is thrown if they do not match
	 * @return a Future result of that fetch
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
		Iterator<Transcoder<T>> tc_iter) {
		final Map<String, Future<T>> m=new ConcurrentHashMap<String, Future<T>>();

		// This map does not need to be a ConcurrentHashMap
		// because it is fully populated when it is used and
		// used only to read the transcoder for a key.
		final Map<String, Transcoder<T>> tc_map = new HashMap<String, Transcoder<T>>();

		// Break the gets down into groups by key
		final Map<MemcachedNode, Collection<String>> chunks
			=new HashMap<MemcachedNode, Collection<String>>();
		final NodeLocator locator=conn.getLocator();
		Iterator<String> key_iter=keys.iterator();
		while (key_iter.hasNext() && tc_iter.hasNext()) {
			String key=key_iter.next();
			tc_map.put(key, tc_iter.next());
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
					Transcoder<T> tc = tc_map.get(k);
					m.put(k, tcService.decode(tc,
							new CachedData(flags, data, tc.getMaxSize())));
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
	 * Asynchronously get a bunch of objects from the cache.
	 *
	 * @param <T>
	 * @param keys the keys to request
	 * @param tc the transcoder to serialize and unserialize values
	 * @return a Future result of that fetch
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
		Transcoder<T> tc) {
		return asyncGetBulk(keys, new SingleElementInfiniteIterator(tc));
	}

	/**
	 * Asynchronously get a bunch of objects from the cache and decode them
	 * with the given transcoder.
	 *
	 * @param keys the keys to request
	 * @return a Future result of that fetch
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
		return asyncGetBulk(keys, transcoder);
	}

	/**
	 * Varargs wrapper for asynchronous bulk gets.
	 *
	 * @param <T>
	 * @param tc the transcoder to serialize and unserialize value
	 * @param keys one more more keys to get
	 * @return the future values of those keys
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc,
		String... keys) {
		return asyncGetBulk(Arrays.asList(keys), tc);
	}

	/**
	 * Varargs wrapper for asynchronous bulk gets with the default transcoder.
	 *
	 * @param keys one more more keys to get
	 * @return the future values of those keys
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys) {
		return asyncGetBulk(Arrays.asList(keys), transcoder);
	}

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param <T>
	 * @param keys the keys
	 * @param tc the transcoder to serialize and unserialize value
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Map<String, Object> getBulk(Collection<String> keys) {
		return getBulk(keys, transcoder);
	}

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param <T>
	 * @param tc the transcoder to serialize and unserialize value
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) {
		return getBulk(Arrays.asList(keys), tc);
	}

	/**
	 * Get the values for multiple keys from the cache.
	 *
	 * @param keys the keys
	 * @return a map of the values (for each value that exists)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Map<String, Object> getBulk(String... keys) {
		return getBulk(Arrays.asList(keys), transcoder);
	}

	/**
	 * Get the versions of all of the connected memcacheds.
	 *
	 * @return a Map of SocketAddress to String for connected servers
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 *
	 * @return a Map of a Map of stats replies by SocketAddress
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Map<SocketAddress, Map<String, String>> getStats() {
		return getStats(null);
	}

	/**
	 * Get a set of stats from all connections.
	 *
	 * @param arg which stats to get
	 * @return a Map of the server SocketAddress to a map of String stat
	 *		   keys to String stat values.
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Map<SocketAddress, Map<String, String>> getStats(final String arg) {
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
							getLogger().warn("Unsuccessful stat fetch:	%s",
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

	private long mutate(Mutator m, String key, int by, long def, int exp) {
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
	 * Due to the way the memcached server operates on items, incremented
	 * and decremented items will be returned as Strings with any
	 * operations that return a value.
	 *
	 * @param key the key
	 * @param by the amount to increment
	 * @return the new value (-1 if the key doesn't exist)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public long incr(String key, int by) {
		return mutate(Mutator.incr, key, by, 0, -1);
	}

	/**
	 * Decrement the given key by the given value.
	 *
	 * Due to the way the memcached server operates on items, incremented
	 * and decremented items will be returned as Strings with any
	 * operations that return a value.
	 *
	 * @param key the key
	 * @param by the value
	 * @return the new value (-1 if the key doesn't exist)
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public long decr(String key, int by) {
		return mutate(Mutator.decr, key, by, 0, -1);
	}

	/**
	 * Increment the given counter, returning the new value.
	 *
	 * Due to the way the memcached server operates on items, incremented
	 * and decremented items will be returned as Strings with any
	 * operations that return a value.
	 *
	 * @param key the key
	 * @param by the amount to increment
	 * @param def the default value (if the counter does not exist)
	 * @param exp the expiration of this object
	 * @return the new value, or -1 if we were unable to increment or add
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public long incr(String key, int by, long def, int exp) {
		return mutateWithDefault(Mutator.incr, key, by, def, exp);
	}

	/**
	 * Decrement the given counter, returning the new value.
	 *
	 * Due to the way the memcached server operates on items, incremented
	 * and decremented items will be returned as Strings with any
	 * operations that return a value.
	 *
	 * @param key the key
	 * @param by the amount to decrement
	 * @param def the default value (if the counter does not exist)
	 * @param exp the expiration of this object
	 * @return the new value, or -1 if we were unable to decrement or add
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
		final CountDownLatch latch = new CountDownLatch(1);
		final OperationFuture<Long> rv = new OperationFuture<Long>(
				latch, operationTimeout);
		Operation op = addOp(key, opFact.mutate(m, key, by, def, exp,
				new OperationCallback() {
			public void receivedStatus(OperationStatus s) {
				rv.set(new Long(s.isSuccess() ? s.getMessage() : "-1"));
			}
			public void complete() {
				latch.countDown();
			}
		}));
		rv.setOperation(op);
		return rv;
	}

	/**
	 * Asychronous increment.
	 *
	 * @param key key to increment
	 * @param by the amount to increment the value by
	 * @return a future with the incremented value, or -1 if the
	 *		   increment failed.
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Long> asyncIncr(String key, int by) {
		return asyncMutate(Mutator.incr, key, by, 0, -1);
	}

	/**
	 * Asynchronous decrement.
	 *
	 * @param key key to increment
	 * @param by the amount to increment the value by
	 * @return a future with the decremented value, or -1 if the
	 *		   increment failed.
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Long> asyncDecr(String key, int by) {
		return asyncMutate(Mutator.decr, key, by, 0, -1);
	}

	/**
	 * Increment the given counter, returning the new value.
	 *
	 * @param key the key
	 * @param by the amount to increment
	 * @param def the default value (if the counter does not exist)
	 * @return the new value, or -1 if we were unable to increment or add
	 * @throws OperationTimeoutException if the global operation timeout is
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public long incr(String key, int by, long def) {
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
	 *		   exceeded
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 * @return whether or not the operation was performed
	 * @deprecated Hold values are no longer honored.
	 */
	@Deprecated
	public Future<Boolean> delete(String key, int hold) {
		return delete(key);
	}

	/**
	 * Delete the given key from the cache.
	 *
	 * @param key the key to delete
	 * @return whether or not the operation was performed
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Boolean> delete(String key) {
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch,
			operationTimeout);
		DeleteOperation op=opFact.delete(key,
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
	 * Flush all caches from all servers with a delay of application.
	 * @param delay the period of time to delay, in seconds
	 * @return whether or not the operation was accepted
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
	 * @return whether or not the operation was performed
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
	 */
	public Future<Boolean> flush() {
		return flush(-1);
	}

	public Set<String> listSaslMechanisms() {
		final ConcurrentMap<String, String> rv
			= new ConcurrentHashMap<String, String>();

		CountDownLatch blatch = broadcastOp(new BroadcastOpFactory() {
			public Operation newOp(MemcachedNode n,
					final CountDownLatch latch) {
				return opFact.saslMechs(new OperationCallback() {
					public void receivedStatus(OperationStatus status) {
						for(String s : status.getMessage().split(" ")) {
							rv.put(s, s);
						}
					}
					public void complete() {
						latch.countDown();
					}
				});
			}
		});

		try {
			blatch.await();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return rv.keySet();
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
	 *
	 * @param timeout the amount of time time for shutdown
	 * @param unit the TimeUnit for the timeout
	 * @return result of the shutdown request
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
				tcService.shutdown();
			} catch (IOException e) {
				getLogger().warn("exception while shutting down", e);
			}
		}
		return rv;
	}

	/**
	 * Wait for the queues to die down.
	 *
	 * @param timeout the amount of time time for shutdown
	 * @param unit the TimeUnit for the timeout
	 * @return result of the request for the wait
	 * @throws IllegalStateException in the rare circumstance where queue
	 *         is too full to accept any more requests
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
			}}, conn.getLocator().getAll(), false);
		try {
			// XXX:  Perhaps IllegalStateException should be caught here
			// and the check retried.
			return blatch.await(timeout, unit);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for queues", e);
		}
	}

	/**
	 * Add a connection observer.
	 *
	 * If connections are already established, your observer will be called
	 * with the address and -1.
	 *
	 * @param obs the ConnectionObserver you wish to add
	 * @return true if the observer was added.
	 */
	public boolean addObserver(ConnectionObserver obs) {
		boolean rv = conn.addObserver(obs);
		if(rv) {
			for(MemcachedNode node : conn.getLocator().getAll()) {
				if(node.isActive()) {
					obs.connectionEstablished(node.getSocketAddress(), -1);
				}
			}
		}
		return rv;
	}

	/**
	 * Remove a connection observer.
	 *
	 * @param obs the ConnectionObserver you wish to add
	 * @return true if the observer existed, but no longer does
	 */
	public boolean removeObserver(ConnectionObserver obs) {
		return conn.removeObserver(obs);
	}

	public void connectionEstablished(SocketAddress sa, int reconnectCount) {
		if(authDescriptor != null) {
                    if (authDescriptor.authThresholdReached()) {
                        this.shutdown();
                    }
			authMonitor.authConnection(conn, opFact, authDescriptor, findNode(sa));
		}
	}

	private MemcachedNode findNode(SocketAddress sa) {
		MemcachedNode node = null;
		for(MemcachedNode n : conn.getLocator().getAll()) {
			if(n.getSocketAddress().equals(sa)) {
				node = n;
			}
		}
		assert node != null : "Couldn't find node connected to " + sa;
		return node;
	}

	public void connectionLost(SocketAddress sa) {
		// Don't care.
	}
	
	/**
	 * After the fetch from remote cache server, 
	 * if the fetch is not null, it will stored to front cache server
	 * 
	 * @param <T> 
	 * @param key the key to store
	 * @param val a future that will hold the return value of the fetch
	 * @param operationTimeout operation timeout
	 * @return
	 */
	public <T> boolean putFrontCache(String key, Future<T> val, long operationTimeout) {
		return false;
	}

}
