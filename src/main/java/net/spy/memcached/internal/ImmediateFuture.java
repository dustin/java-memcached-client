/**
 *
 */
package net.spy.memcached.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future that fires immediately.
 */
public class ImmediateFuture implements Future<Boolean> {
	private final Boolean value;
	private final ExecutionException exception;

	public ImmediateFuture(Boolean returnValue) {
		value = returnValue;
		exception = null;
	}

	public ImmediateFuture(Exception e) {
		value = null;
		exception = new ExecutionException(e);
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	public Boolean get() throws InterruptedException, ExecutionException {
		if(exception != null) {
			throw exception;
		}
		return value;
	}

	public Boolean get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException,
			TimeoutException {
		if(exception != null) {
			throw exception;
		}
		return value;
	}

	public boolean isCancelled() {
		return false;
	}

	public boolean isDone() {
		return true;
	}

}