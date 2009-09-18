package net.spy.memcached.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.ops.Operation;

/**
 * Future returned for GET operations.
 *
 * Not intended for general use.
 *
 * @param <T> Type of object returned from the get
 */
public class GetFuture<T> implements Future<T> {

	private final OperationFuture<Future<T>> rv;

	public GetFuture(CountDownLatch l, long opTimeout) {
		this.rv = new OperationFuture<Future<T>>(l, opTimeout);
	}

	public boolean cancel(boolean ign) {
		return rv.cancel(ign);
	}

	public T get() throws InterruptedException, ExecutionException {
		Future<T> v = rv.get();
		return v == null ? null : v.get();
	}

	public T get(long duration, TimeUnit units)
		throws InterruptedException, TimeoutException, ExecutionException {
		Future<T> v = rv.get(duration, units);
		return v == null ? null : v.get();
	}

	public void set(Future<T> d) {
		rv.set(d);
	}

	public void setOperation(Operation to) {
		rv.setOperation(to);
	}

	public boolean isCancelled() {
		return rv.isCancelled();
	}

	public boolean isDone() {
		return rv.isDone();
	}
}