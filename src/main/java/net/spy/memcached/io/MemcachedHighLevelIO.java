package net.spy.memcached.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.util.ArrayList;
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
import net.spy.memcached.CachedData;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.cas.CASResponse;
import net.spy.memcached.cas.CASValue;
import net.spy.memcached.nodes.MemcachedNode;
import net.spy.memcached.nodes.NodeLocator;
import net.spy.memcached.ops.BroadcastOpFactory;
import net.spy.memcached.ops.CASOperationStatus;
import net.spy.memcached.ops.CancelledOperationStatus;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationFactory;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.memcached.util.KeyUtil;

/**
 * High level IO operations.  This is the API upon which consumers are
 * implemented.
 */
public class MemcachedHighLevelIO extends SpyThread {

	/**
	 * Maximum supported key length.
	 */
	public static final int MAX_KEY_LENGTH = 250;

	private volatile boolean running=true;
	private volatile boolean shuttingDown=false;

    private final MemcachedLowLevelIO conn;

	final OperationFactory opFact;

	public MemcachedHighLevelIO(ConnectionFactory cf,
		List<InetSocketAddress> addrs) throws IOException {
		conn=cf.createConnection(addrs);
		assert conn != null : "Connection factory failed to make a connection";
		opFact=cf.getOperationFactory();

		setName("Memcached IO over " + conn);
		setDaemon(cf.isDaemon());
		start();
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
			}
		}
		getLogger().info("Shut down memcached client");
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#shutdown()
	 */
	public void shutdown() {
		shutdown(-1, TimeUnit.MILLISECONDS);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#shutdown(long, java.util.concurrent.TimeUnit)
	 */
	public synchronized boolean shutdown(long timeout, TimeUnit unit) {
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

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#waitForQueues(long, java.util.concurrent.TimeUnit)
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

    private void validateKey(byte[] keyBytes) {
		if(keyBytes.length > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key is too long (maxlen = "
					+ MAX_KEY_LENGTH + ")");
		}
		// Validate the key
		for(byte b : keyBytes) {
			if(b == ' ' || b == '\n' || b == '\r' || b == 0) {
				throw new IllegalArgumentException(
					"Key contains invalid characters:  ``"
						+ KeyUtil.getKeyString(keyBytes) + "''");
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
	public Operation addOp(final byte[] key, final Operation op) {
		validateKey(key);
		checkState();
		conn.addOperation(key, op);
		return op;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getAvailableServers()
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

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedClient#getUnavailableServers()
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

	public Future<Boolean> asyncStore(StoreType storeType, byte[] keyBytes,
			int exp, CachedData co, long timeout) {
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch,
				timeout);
		Operation op=opFact.store(storeType, keyBytes, co.getFlags(),
				exp, co.getData(), new OperationCallback() {
					public void receivedStatus(OperationStatus val) {
						rv.set(val.isSuccess());
					}
					public void complete() {
						latch.countDown();
					}});
		rv.setOperation(op);
		addOp(keyBytes, op);
		return rv;
	}

	public Future<Boolean> asyncCat(ConcatenationType catType, long cas,
			byte[] key, CachedData co, long operationTimeout) {
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch,
				operationTimeout);
		Operation op=opFact.cat(catType, cas, key,
				co.getData(), new OperationCallback() {
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

	public Future<CASResponse> asyncCAS(byte[] key, long casId, int exp,
			CachedData co, long timeout) {
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<CASResponse> rv=new OperationFuture<CASResponse>(
				latch, timeout);
		Operation op=opFact.cas(key, casId, co.getFlags(), exp,
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

	public <T> Future<T> asyncGet(final byte[] key,
			final Transcoder<T> tc, long operationTimeout) {
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
				assert k.equals(KeyUtil.getKeyString(key))
					: "Wrong key returned";
				val=tc.decode(new CachedData(flags, data));
			}
			public void complete() {
				latch.countDown();
			}});
		rv.setOperation(op);
		addOp(key, op);
		return rv;
	}

	public <T> Future<CASValue<T>> asyncGets(final byte[] key,
			final Transcoder<T> tc, long operationTimeout) {

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
				assert k.equals(KeyUtil.getKeyString(key))
					: "Wrong key returned";
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

	public Map<SocketAddress, String> getVersions(long timeout) {
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
			blatch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for versions", e);
		}
		return rv;
	}

	public Map<SocketAddress, Map<String, String>> getStats(final String arg,
			long timeout) {
		final Map<SocketAddress, Map<String, String>> rv
			=new HashMap<SocketAddress, Map<String, String>>();

		CountDownLatch blatch = broadcastOp(new BroadcastOpFactory(){
			public Operation newOp(final MemcachedNode n,
					final CountDownLatch latch) {
				final SocketAddress sa=n.getSocketAddress();
				rv.put(sa, new HashMap<String, String>());
				return opFact.stats(KeyUtil.getKeyBytes(arg),
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
			blatch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted waiting for stats", e);
		}
		return rv;

	}

	public long mutate(Mutator m, final byte[] key, int by, long def, int exp,
			long timeout) {
		final AtomicLong rv=new AtomicLong();
		final CountDownLatch latch=new CountDownLatch(1);
		addOp(key, opFact.mutate(m, key, by, def, exp,
				new OperationCallback() {
					public void receivedStatus(OperationStatus s) {
						rv.set(new Long(s.isSuccess()?s.getMessage():"-1"));
					}
					public void complete() {
						latch.countDown();
					}}));
		try {
			if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
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

	public Future<Boolean> delete(byte[] key, int when,
			long timeout) {
		final CountDownLatch latch=new CountDownLatch(1);
		final OperationFuture<Boolean> rv=new OperationFuture<Boolean>(latch,
			timeout);
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

	public Future<Boolean> flush(final int delay, long timeout) {
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
				timeout) {
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

	public <T> Future<Map<String, T>> asyncGetBulk(
		final Collection<byte[]> keys,
		final Transcoder<T> tc, long operationTimeout) {

		final Map<String, T> m=new ConcurrentHashMap<String, T>();
		// Break the gets down into groups by key
		final Map<MemcachedNode, Collection<byte[]>> chunks
			=new HashMap<MemcachedNode, Collection<byte[]>>();
		final NodeLocator locator=conn.getLocator();
		for(byte[] key : keys) {
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
			Collection<byte[]> ks=chunks.get(node);
			if(ks == null) {
				ks=new ArrayList<byte[]>();
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

		for(Map.Entry<MemcachedNode, Collection<byte[]>> me
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
			latch.await(globalOperationTimeout, TimeUnit.MILLISECONDS);
			assert isDone() : "Latch released, but operation wasn't done.";
			if(op != null && op.hasErrored()) {
				throw new ExecutionException(op.getException());
			}
			if(isCancelled()) {
				throw new ExecutionException(new RuntimeException("Cancelled"));
			}
			return objRef.get();
		}

		public T get(long duration, TimeUnit units)
			throws InterruptedException, TimeoutException {
			if(!latch.await(duration, units)) {
				throw new TimeoutException("Timed out waiting for operation");
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

	public NodeLocator getLocator() {
		return conn.getLocator().getReadonlyCopy();
	}

	public Future<Long> asyncMutate(Mutator m, byte[] key, int by,
			long def, int exp) {
        final CountDownLatch latch = new CountDownLatch(1);
        final OperationFuture<Long> rv = new OperationFuture<Long>(
                latch, Integer.MAX_VALUE);
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
}
