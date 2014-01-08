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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationStatus;

/**
 * Future returned for GET operations.
 *
 * Not intended for general use.
 *
 * @param <T> Type of object returned from the get
 */
public class GetFuture<T>
  extends AbstractListenableFuture<T, GetCompletionListener>
  implements Future<T> {

  private final OperationFuture<Future<T>> rv;

  public GetFuture(CountDownLatch l, long opTimeout, String key,
    ExecutorService service) {
    super(service);
    this.rv = new OperationFuture<Future<T>>(key, l, opTimeout, service);
  }

  public boolean cancel(boolean ign) {
    boolean result = rv.cancel(ign);
    notifyListeners();
    return result;
  }

  public T get() throws InterruptedException, ExecutionException {
    Future<T> v = rv.get();
    return v == null ? null : v.get();
  }

  public T get(long duration, TimeUnit units) throws InterruptedException,
      TimeoutException, ExecutionException {
    Future<T> v = rv.get(duration, units);
    return v == null ? null : v.get();
  }

  public OperationStatus getStatus() {
    return rv.getStatus();
  }

  public void set(Future<T> d, OperationStatus s) {
    rv.set(d, s);
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

  @Override
  public GetFuture<T> addListener(GetCompletionListener listener) {
    super.addToListeners((GenericCompletionListener) listener);
    return this;
  }

  @Override
  public GetFuture<T> removeListener(GetCompletionListener listener) {
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
