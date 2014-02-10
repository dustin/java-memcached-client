/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2013 Couchbase, Inc.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.compat.log.LoggerFactory;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;

/**
 * Future for handling results from bulk gets.
 *
 * Not intended for general use.
 *
 * @param <T> types of objects returned from the GET
 */
public class BulkGetFuture<T>
  extends AbstractListenableFuture<Map<String, T>, BulkGetCompletionListener>
  implements BulkFuture<Map<String, T>> {

  private final Map<String, Future<T>> rvMap;
  private final Collection<Operation> ops;
  private final CountDownLatch latch;
  private OperationStatus status;
  private boolean cancelled = false;
  private boolean timeout = false;

  public BulkGetFuture(Map<String, Future<T>> m, Collection<Operation> getOps,
      CountDownLatch l, ExecutorService service) {
    super(service);
    rvMap = m;
    ops = getOps;
    latch = l;
    status = null;
  }

  public boolean cancel(boolean ign) {
    boolean rv = false;
    for (Operation op : ops) {
      rv |= op.getState() == OperationState.WRITE_QUEUED;
      op.cancel();
    }
    for (Future<T> v : rvMap.values()) {
      v.cancel(ign);
    }
    cancelled = true;
    status = new OperationStatus(false, "Cancelled", StatusCode.CANCELLED);
    notifyListeners();
    return rv;
  }

  public Map<String, T> get() throws InterruptedException, ExecutionException {
    try {
      return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new RuntimeException("Timed out waiting forever", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.internal.BulkFuture#getSome(long,
   * java.util.concurrent.TimeUnit)
   */
  public Map<String, T> getSome(long to, TimeUnit unit)
    throws InterruptedException, ExecutionException {
    Collection<Operation> timedoutOps = new HashSet<Operation>();
    Map<String, T> ret = internalGet(to, unit, timedoutOps);
    if (timedoutOps.size() > 0) {
      timeout = true;
      LoggerFactory.getLogger(getClass()).warn(
          new CheckedOperationTimeoutException("Operation timed out: ",
              timedoutOps).getMessage());
    }
    return ret;

  }

  /*
   * get all or nothing: timeout exception is thrown if all the data could not
   * be retrieved
   *
   * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
   */
  public Map<String, T> get(long to, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
    Collection<Operation> timedoutOps = new HashSet<Operation>();
    Map<String, T> ret = internalGet(to, unit, timedoutOps);
    if (timedoutOps.size() > 0) {
      this.timeout = true;
      throw new CheckedOperationTimeoutException("Operation timed out.",
          timedoutOps);
    }
    return ret;
  }

  /**
   * refactored code common to both get(long, TimeUnit) and getSome(long,
   * TimeUnit).
   *
   * @param to
   * @param unit
   * @param timedoutOps
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private Map<String, T> internalGet(long to, TimeUnit unit,
      Collection<Operation> timedoutOps) throws InterruptedException,
      ExecutionException {
    if (!latch.await(to, unit)) {
      for (Operation op : ops) {
        if (op.getState() != OperationState.COMPLETE) {
          MemcachedConnection.opTimedOut(op);
          timedoutOps.add(op);
        } else {
          MemcachedConnection.opSucceeded(op);
        }
      }
    }
    for (Operation op : ops) {
      if (op.isCancelled()) {
        throw new ExecutionException(new CancellationException("Cancelled"));
      }
      if (op.hasErrored()) {
        throw new ExecutionException(op.getException());
      }
    }
    Map<String, T> m = new HashMap<String, T>();
    for (Map.Entry<String, Future<T>> me : rvMap.entrySet()) {
      m.put(me.getKey(), me.getValue().get());
    }
    return m;
  }

  public OperationStatus getStatus() {
    if (status == null) {
      try {
        get();
      } catch (InterruptedException e) {
        status = new OperationStatus(false, "Interrupted", StatusCode.INTERRUPTED);
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        return status;
      }
    }
    return status;
  }

  public void setStatus(OperationStatus s) {
    status = s;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public boolean isDone() {
    return latch.getCount() == 0;
  }

  /*
   * set to true if timeout was reached.
   *
   * @see net.spy.memcached.internal.BulkFuture#isTimeout()
   */
  public boolean isTimeout() {
    return timeout;
  }

  @Override
  public Future<Map<String, T>> addListener(
    BulkGetCompletionListener listener) {
    super.addToListeners((GenericCompletionListener) listener);
    return this;
  }

  @Override
  public Future<Map<String, T>> removeListener(
    BulkGetCompletionListener listener) {
    super.removeFromListeners((GenericCompletionListener) listener);
    return this;
  }

  /**
   * Signals that this future is complete.
   */
  public void signalComplete() {
    notifyListeners();
  }

}
