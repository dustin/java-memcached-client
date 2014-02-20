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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import net.spy.memcached.compat.SpyObject;

/**
 * The {@link AbstractListenableFuture} implements common functionality shared
 * by all futures that implement the {@link ListenableFuture}.
 *
 * It handles storage and notification of listeners and making sure this is
 * done in a thread-safe manner. The subclassing methods need to implement
 * the abstract methods, which in turn need to call both
 * {@link #addToListeners(GenericCompletionListener)} and
 * {@link #removeFromListeners(GenericCompletionListener)}. See the
 * {@link OperationFuture} as an example.
 */
public abstract class AbstractListenableFuture
  <T, L extends GenericCompletionListener>
  extends SpyObject
  implements ListenableFuture<T, L> {

  /**
   * The {@link ExecutorService} in which the notifications will be handled.
   */
  private final ExecutorService service;

  /**
   * Holds the list of listeners which will be notified upon completion.
   */
  private List<GenericCompletionListener<? extends Future<T>>> listeners;

  public AbstractListenableFuture(ExecutorService executor) {
    service = executor;
    listeners = new ArrayList<GenericCompletionListener<? extends Future<T>>>();
  }

  /**
   * Returns the current executor.
   *
   * @return the current executor service.
   */
  protected ExecutorService executor() {
    return service;
  }

  /**
   * Add the given listener to the total list of listeners to be notified.
   *
   * <p>If the future is already done, the listener will be notified
   * immediately.</p>
   *
   * @param listener the listener to add.
   * @return the current future to allow chaining.
   */
  protected Future<T> addToListeners(
    GenericCompletionListener<? extends Future<T>> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("The listener can't be null.");
    }

    if(isDone()) {
      notifyListener(executor(), this, listener);
      return this;
    }

    synchronized(this) {
      if (!isDone()) {
        listeners.add(listener);
        return this;
      }
    }

    notifyListener(executor(), this, listener);
    return this;
  }

  /**
   * Notify a specific listener of completion.
   *
   * @param executor the executor to use.
   * @param future the future to hand over.
   * @param listener the listener to notify.
   */
  protected void notifyListener(final ExecutorService executor,
    final Future<?> future, final GenericCompletionListener listener) {
    executor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          listener.onComplete(future);
        } catch(Throwable t) {
          getLogger().warn(
            "Exception thrown wile executing " + listener.getClass().getName()
            + ".operationComplete()", t);
        }
      }
    });
  }

  /**
   * Notify all registered listeners of future completion.
   */
  protected void notifyListeners() {
    notifyListeners(this);
  }

  /**
   * Notify all registered listeners with a special future on completion.
   *
   * This method can be used if a different future should be used for
   * notification than the current one (for example if an enclosing future
   * is used, but the enclosed future should be notified on).
   *
   * @param future the future to pass on to the listeners.
   */
  protected synchronized void notifyListeners(final Future<?> future) {
    for (GenericCompletionListener<? extends Future<? super T>> listener
      : listeners) {
      notifyListener(executor(), future, listener);
    }
  }

  /**
   * Remove a listener from the list of registered listeners.
   *
   * @param listener the listener to remove.
   * @return the current future to allow for chaining.
   */
  protected Future<T> removeFromListeners(
    GenericCompletionListener<? extends Future<T>> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("The listener can't be null.");
    }

    if(isDone()) {
      return this;
    }

    synchronized(this) {
      if (!isDone()) {
        listeners.remove(listener);
      }
    }

    return this;
  }
}
