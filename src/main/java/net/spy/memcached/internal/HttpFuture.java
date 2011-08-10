/**
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

import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.couch.HttpOperation;

/**
 * A future http response.
 */
public class HttpFuture<T> extends SpyObject implements Future<T> {
  protected final AtomicReference<T> objRef;
  protected final CountDownLatch latch;
  protected final long timeout;
  protected OperationStatus status;
  protected HttpOperation op;

  public HttpFuture(CountDownLatch latch, long timeout) {
    super();
    this.objRef = new AtomicReference<T>(null);
    this.latch = latch;
    this.timeout = timeout;
    this.status = null;
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
      status = new OperationStatus(false, "Timed out");
      throw new RuntimeException("Timed out waiting for operation", e);
    }
  }

  @Override
  public T get(long duration, TimeUnit units) throws InterruptedException,
      ExecutionException, TimeoutException {
    if (!latch.await(duration, units)) {
      if (op != null) {
        op.timeOut();
      }
      status = new OperationStatus(false, "Timed out");
      throw new TimeoutException("Timed out waiting for operation");
    }

    if (op != null && op.hasErrored()) {
      status = new OperationStatus(false, op.getException().getMessage());
      throw new ExecutionException(op.getException());
    }

    if (op.isCancelled()) {
      status = new OperationStatus(false, "Operation Cancelled");
      throw new ExecutionException(new RuntimeException("Cancelled"));
    }

    if (op != null && op.isTimedOut()) {
      status = new OperationStatus(false, "Timed out");
      throw new ExecutionException(new OperationTimeoutException(
          "Operation timed out."));
    }

    return objRef.get();
  }

  public OperationStatus getStatus() {
    if (status == null) {
      try {
        get();
      } catch (InterruptedException e) {
        status = new OperationStatus(false, "Interrupted");
        Thread.currentThread().isInterrupted();
      } catch (ExecutionException e) {
        getLogger().warn("Error getting status of operation", e);
      }
    }
    return status;
  }

  public void set(T oper, OperationStatus s) {
    objRef.set(oper);
    status = s;
  }

  @Override
  public boolean isDone() {
    assert op != null : "No operation";
    return latch.getCount() == 0 || op.isCancelled() || op.hasErrored();
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
