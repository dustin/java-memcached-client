package net.spy.memcached.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;

/**
 * Managed future for operations.
 *
 * Not intended for general use.
 *
 * @param <T> Type of object returned from this future.
 */
public class OperationFuture<T> implements Future<T> {

	private final CountDownLatch latch;
	private final AtomicReference<T> objRef;
	private final long timeout;
	private Operation op;

	// add continuous timeout close conntion

	public OperationFuture(CountDownLatch l, long opTimeout) {
		this(l, new AtomicReference<T>(null), opTimeout);
	}

	public OperationFuture(CountDownLatch l, AtomicReference<T> oref,
		long opTimeout) {
		super();
		latch=l;
		objRef=oref;
		timeout = opTimeout;
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
			return get(timeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new RuntimeException(
				"Timed out waiting for operation", e);
		}
	}

	public T get(long duration, TimeUnit units)
		throws InterruptedException, TimeoutException, ExecutionException {
		if(!latch.await(duration, units)) {
			// whenever timeout occurs, continuous timeout counter will increase by 1.
			MemcachedConnection.setContinuousTimeout(true);
			throw new CheckedOperationTimeoutException(
					"Timed out waiting for operation", op);
		} else {
			// continuous timeout counter will be reset
			MemcachedConnection.setContinuousTimeout(false);
		}
		if(op != null && op.hasErrored()) {
			throw new ExecutionException(op.getException());
		}
		if(isCancelled()) {
			throw new ExecutionException(new RuntimeException("Cancelled"));
		}

		return objRef.get();
	}

	public void set(T o) {
		objRef.set(o);
	}

	public void setOperation(Operation to) {
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
