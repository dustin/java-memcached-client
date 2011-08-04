/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.compat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * Thread that invokes a callable multiple times concurrently.
 */
public class SyncThread<T> extends SpyThread {

  private final Callable<T> callable;
  private final CyclicBarrier barrier;
  private final CountDownLatch latch;
  private Throwable throwable = null;
  private T rv = null;

  /**
   * Get a SyncThread that will call the given callable when the given barrier
   * allows it past.
   *
   * @param b the barrier
   * @param c the callable
   */
  public SyncThread(CyclicBarrier b, Callable<T> c) {
    super("SyncThread");
    setDaemon(true);
    callable = c;
    barrier = b;
    latch = new CountDownLatch(1);
    start();
  }

  /**
   * Wait for the barrier, invoke the callable and capture the result or an
   * exception.
   */
  @Override
  public void run() {
    try {
      barrier.await();
      rv = callable.call();
    } catch (Throwable t) {
      throwable = t;
    }
    latch.countDown();
  }

  /**
   * Get the result from the invocation.
   *
   * @return the result
   * @throws Throwable if an error occurred when evaluating the callable
   */
  public T getResult() throws Throwable {
    latch.await();
    if (throwable != null) {
      throw throwable;
    }
    return rv;
  }

  /**
   * Get a collection of SyncThreads that all began as close to the same time as
   * possible and have all completed.
   *
   * @param <T> the result type of the SyncThread
   * @param num the number of concurrent threads to execute
   * @param callable the thing to call
   * @return the completed SyncThreads
   * @throws InterruptedException if we're interrupted during join
   */
  public static <T> Collection<SyncThread<T>> getCompletedThreads(int num,
      Callable<T> callable) throws InterruptedException {
    Collection<SyncThread<T>> rv = new ArrayList<SyncThread<T>>(num);

    CyclicBarrier barrier = new CyclicBarrier(num);
    for (int i = 0; i < num; i++) {
      rv.add(new SyncThread<T>(barrier, callable));
    }

    for (SyncThread<T> t : rv) {
      t.join();
    }

    return rv;
  }

  /**
   * Get the distinct result count for the given callable at the given
   * concurrency.
   *
   * @param <T> the type of the callable
   * @param num the concurrency
   * @param callable the callable to invoke
   * @return the number of distinct (by identity) results found
   * @throws Throwable if an exception occurred in one of the invocations
   */
  public static <T> int getDistinctResultCount(int num, Callable<T> callable)
    throws Throwable {
    IdentityHashMap<T, Object> found = new IdentityHashMap<T, Object>();
    Collection<SyncThread<T>> threads = getCompletedThreads(num, callable);
    for (SyncThread<T> s : threads) {
      found.put(s.getResult(), new Object());
    }
    return found.size();
  }
}
