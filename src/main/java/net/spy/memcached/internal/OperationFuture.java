/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.ErrorCode;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;

/**
 * Managed future for operations.
 *
 * Not intended for general use.
 *
 * @param <T> Type of object returned from this future.
 */
public class OperationFuture<T> extends SpyObject implements Future<T> {

  private final CountDownLatch latch;
  private final AtomicReference<T> objRef;
  protected OperationStatus status;
  private final long timeout;
  private Operation op;
  private final String key;

  public OperationFuture(String k, CountDownLatch l, long opTimeout) {
    this(k, l, new AtomicReference<T>(null), opTimeout);
  }

  public OperationFuture(String k, CountDownLatch l, AtomicReference<T> oref,
      long opTimeout) {
    super();
    latch = l;
    objRef = oref;
    status = null;
    timeout = opTimeout;
    key = k;
  }

  public boolean cancel(boolean ign) {
    assert op != null : "No operation";
    op.cancel();
    // This isn't exactly correct, but it's close enough. If we're in
    // a writing state, we *probably* haven't started.
    return op.getState() == OperationState.WRITE_QUEUED;
  }

  public T get() throws InterruptedException, ExecutionException {
    try {
      return get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new RuntimeException("Timed out waiting for operation", e);
    }
  }

  public T get(long duration, TimeUnit units) throws InterruptedException,
      TimeoutException, ExecutionException {
    if (!latch.await(duration, units)) {
      // whenever timeout occurs, continuous timeout counter will increase by 1.
      MemcachedConnection.opTimedOut(op);
      if (op != null) { // op can be null on a flush
        op.timeOut();
      }
      throw new CheckedOperationTimeoutException(
          "Timed out waiting for operation", op);
    } else {
      // continuous timeout counter will be reset
      MemcachedConnection.opSucceeded(op);
    }
    if (op != null && op.hasErrored()) {
      throw new ExecutionException(op.getException());
    }
    if (isCancelled()) {
      throw new ExecutionException(new RuntimeException("Cancelled"));
    }
    if (op != null && op.isTimedOut()) {
      throw new ExecutionException(new CheckedOperationTimeoutException(
          "Operation timed out.", op));
    }

    return objRef.get();
  }

  public String getKey() {
    return key;
  }

  public OperationStatus getStatus() {
    if (status == null) {
      try {
        get();
      } catch (InterruptedException e) {
        status = new OperationStatus(false, "Interrupted",
            ErrorCode.EXCEPTION);
        Thread.currentThread().isInterrupted();
      } catch (ExecutionException e) {
        getLogger().warn("Error getting status of operation", e);
      }
    }
    return status;
  }

  public void set(T o, OperationStatus s) {
    objRef.set(o);
    status = s;
  }

  public void setOperation(Operation to) {
    op = to;
  }

  public boolean isCancelled() {
    assert op != null : "No operation";
    return op.isCancelled();
  }

  public boolean isDone() {
    assert op != null : "No operation";
    return latch.getCount() == 0 || op.isCancelled()
        || op.getState() == OperationState.COMPLETE;
  }
}
