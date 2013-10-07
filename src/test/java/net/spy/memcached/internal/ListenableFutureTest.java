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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.DefaultConnectionFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Verifies the correct functionality of a {@link ListenableFuture}.
 */
public class ListenableFutureTest {

  /**
   * Use an unbounded thread pool for testing, which is very similar to
   * the one used in the {@link DefaultConnectionFactory}.
   */
  private ExecutorService service = Executors.newCachedThreadPool();

  @Test
  public void verifyOnComplete() throws Exception {
    DummyListenableFuture<String> future =
      new DummyListenableFuture<String>(false, service);

    final CountDownLatch latch = new CountDownLatch(1);
    future.addListener(new GenericCompletionListener() {
      @Override
      public void onComplete(Future future) throws Exception {
        assertEquals("Hello World", (String) future.get());
        latch.countDown();
      }
    });

    future.set("Hello World");
    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

  @Test
  public void verifyOnCompleteWhenAlreadyDone() throws Exception {
    DummyListenableFuture<String> future =
      new DummyListenableFuture<String>(true, service);

    final CountDownLatch latch = new CountDownLatch(1);
    future.addListener(new GenericCompletionListener() {
      @Override
      public void onComplete(Future future) throws Exception {
        latch.countDown();
      }
    });

    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

  @Test
  public void verifyOnCompleteWhenCancelled() throws Exception {
    DummyListenableFuture<String> future =
      new DummyListenableFuture<String>(false, service);

    final CountDownLatch latch = new CountDownLatch(1);
    future.addListener(new GenericCompletionListener() {
      @Override
      public void onComplete(Future future) throws Exception {
        assertTrue(future.isCancelled());
        latch.countDown();
      }
    });

    future.cancel(true);

    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

  @Test
  public void verifyRemoval() throws Exception {
    DummyListenableFuture<String> future =
      new DummyListenableFuture<String>(false, service);

    final CountDownLatch latch = new CountDownLatch(1);
    final GenericCompletionListener listener = new GenericCompletionListener() {
      @Override
      public void onComplete(Future future) throws Exception {
        latch.countDown();
      }
    };

    future.addListener(listener);
    future.removeListener(listener);

    Thread.sleep(500);
    assertEquals(1, latch.getCount());
  }

  @Test
  public void verifyMultipleListeners() throws Exception {
    DummyListenableFuture<String> future =
      new DummyListenableFuture<String>(false, service);

    final CountDownLatch latch = new CountDownLatch(2);
    final GenericCompletionListener listener1 =
      new GenericCompletionListener() {
        @Override
        public void onComplete(Future future) throws Exception {
          latch.countDown();
        }
      };

    final GenericCompletionListener listener2 =
      new GenericCompletionListener() {
        @Override
        public void onComplete(Future future) throws Exception {
          latch.countDown();
        }
      };

    future.addListener(listener1);
    future.addListener(listener2);

    future.set("Hello World");
    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

}
