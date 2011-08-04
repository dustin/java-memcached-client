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

package net.spy.memcached.transcoders;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.spy.memcached.CachedData;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.internal.BasicThreadFactory;

/**
 * Asynchronous transcoder.
 */
public class TranscodeService extends SpyObject {

  private final ThreadPoolExecutor pool;

  public TranscodeService(boolean daemon) {
    pool = new ThreadPoolExecutor(1, 10, 60L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(100), new BasicThreadFactory(
          "transcoder", daemon), new ThreadPoolExecutor.DiscardPolicy());
  }

  /**
   * Perform a decode.
   */
  public <T> Future<T> decode(final Transcoder<T> tc,
      final CachedData cachedData) {

    assert !pool.isShutdown() : "Pool has already shut down.";

    TranscodeService.Task<T> task =
        new TranscodeService.Task<T>(new Callable<T>() {
          public T call() {
            return tc.decode(cachedData);
          }
        });

    if (tc.asyncDecode(cachedData)) {
      this.pool.execute(task);
    }
    return task;
  }

  /**
   * Shut down the pool.
   */
  public void shutdown() {
    pool.shutdown();
  }

  /**
   * Ask whether this service has been shut down.
   */
  public boolean isShutdown() {
    return pool.isShutdown();
  }

  private static class Task<T> extends FutureTask<T> {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public Task(Callable<T> callable) {
      super(callable);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      this.run();
      return super.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException {
      this.run();
      return super.get(timeout, unit);
    }

    @Override
    public void run() {
      if (this.isRunning.compareAndSet(false, true)) {
        super.run();
      }
    }
  }
}
