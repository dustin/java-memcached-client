package net.spy.memcached.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.protocol.couchdb.HttpOperation;

public class HttpFuture<T> implements Future<T>{
	private final AtomicReference<T> objRef;
	private final CountDownLatch latch;
    private final long timeout;
    private HttpOperation op;

    private volatile boolean completed;

    public HttpFuture(CountDownLatch latch, long timeout) {
        super();
        this.objRef = new AtomicReference<T>(null);
        this.latch = latch;
        this.timeout = timeout;
    }

    public boolean isCompleted() {
        return this.completed;
    }

	public boolean cancel(boolean c) {
		op.cancel();
		return true;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		try {
			return get(timeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new RuntimeException(
				"Timed out waiting for operation", e);
		}
	}

	@Override
	public T get(long duration, TimeUnit units) throws InterruptedException,
			ExecutionException, TimeoutException {
		if(!latch.await(duration, units)) {
			if (op != null) {
				op.timeOut();
			}
			throw new TimeoutException(
					"Timed out waiting for operation");
		}

		if(op != null && op.hasErrored()) {
			throw new ExecutionException(op.getException());
		}

		if(op.isCancelled()) {
			throw new ExecutionException(new RuntimeException("Cancelled"));
		}

		/*if(op != null && op.isTimedOut()) {
            throw new ExecutionException(new CheckedOperationTimeoutException("Operation timed out.", op));
		}*/

		return objRef.get();
	}

	public void set(T op) {
		objRef.set(op);
	}

	@Override
	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setOperation(HttpOperation to) {
		this.op = to;
	}

	@Override
	public boolean isCancelled() {
		assert op != null : "No operation";
		return op.isCancelled();
	}
}
