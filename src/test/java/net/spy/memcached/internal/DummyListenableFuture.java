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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A very basic {@link ListenableFuture} to verify and test basic
 * add, remove and notification behavior.
 */
public class DummyListenableFuture<T>
  extends AbstractListenableFuture<T, GenericCompletionListener> {

  private boolean done;
  private boolean cancelled = false;

  private T content = null;

  public DummyListenableFuture(boolean alreadyDone, ExecutorService service) {
    super(service);
    this.done = alreadyDone;
  }

  @Override
  public boolean cancel(boolean bln) {
    cancelled = true;
    notifyListeners();
    return true;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public boolean isDone() {
    return done;
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    try {
      return get(1, TimeUnit.SECONDS);
    } catch (TimeoutException ex) {
      return null;
    }
  }

  @Override
  public T get(long l, TimeUnit tu)
    throws InterruptedException, ExecutionException, TimeoutException {
    return content;
  }

  public void set(T c) {
    notifyListeners();
    content = c;
  }

  @Override
  public DummyListenableFuture<T> addListener(
    GenericCompletionListener listener) {
    super.addToListeners(listener);
    return this;
  }

  @Override
  public DummyListenableFuture<T> removeListener(
    GenericCompletionListener listener) {
    super.removeFromListeners(listener);
    return this;
  }

}
