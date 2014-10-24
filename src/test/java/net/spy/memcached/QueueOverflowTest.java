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

package net.spy.memcached;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.Operation;

/**
 * Test queue overflow.
 */
public class QueueOverflowTest extends ClientBaseCase {

  @Override
  protected void initClient() throws Exception {

    // We're creating artificially constrained queues with the explicit
    // goal of overrunning them to verify the client will still be
    // functional after such conditions occur.
    initClient(new DefaultConnectionFactory(5, 1024) {
      @Override
      public MemcachedConnection
      createConnection(List<HostPort> addrs) throws IOException {
        MemcachedConnection rv = super.createConnection(addrs);
        return rv;
      }

      @Override
      public long getOperationTimeout() {
        return 5000;
      }

      @Override
      public BlockingQueue<Operation> createOperationQueue() {
        return new ArrayBlockingQueue<Operation>(getOpQueueLen());
      }

      @Override
      public BlockingQueue<Operation> createReadOperationQueue() {
        return new ArrayBlockingQueue<Operation>((int) (getOpQueueLen() * 1.1));
      }

      @Override
      public BlockingQueue<Operation> createWriteOperationQueue() {
        return createOperationQueue();
      }

      @Override
      public boolean shouldOptimize() {
        return false;
      }

      @Override
      public long getOpQueueMaxBlockTime() {
        return 0;
      }
    });
  }

  private void runOverflowTest(byte[] b) throws Exception {
    Collection<Future<Boolean>> c = new ArrayList<Future<Boolean>>();
    try {
      for (int i = 0; i < 1000; i++) {
        c.add(client.set("k" + i, 0, b));
      }
      fail("Didn't catch an illegal state exception");
    } catch (IllegalStateException e) {
      // expected
    }
    try {
      Thread.sleep(50);
      for (Future<Boolean> f : c) {
        f.get(1, TimeUnit.SECONDS);
      }
    } catch (TimeoutException e) {
      // OK, at least we got one back.
    } catch (ExecutionException e) {
      // OK, at least we got one back.
    }
    Thread.sleep(500);
    assertTrue("Was not able to set a key after failure.",
        client.set("kx", 0, "woo").get(10, TimeUnit.SECONDS));
  }

  public void testOverflowingWriteQueue() throws Exception {
    byte[] b = new byte[8192];
    Random r = new Random();

    // Do not execute this for CI
    if (TestConfig.isCITest()) {
      return;
    }
    r.nextBytes(b);
    runOverflowTest(b);
  }

  public void testOverflowingReadQueue() throws Exception {
    byte[] b = new byte[8192];
    Random r = new Random();

    // Do not execute this for CI
    if (TestConfig.isCITest()) {
      return;
    }
    r.nextBytes(b);
    client.set("x", 0, b);

    Collection<Future<Object>> c = new ArrayList<Future<Object>>();
    try {
      for (int i = 0; i < 1000; i++) {
        c.add(client.asyncGet("x"));
      }
      fail("Didn't catch an illegal state exception");
    } catch (IllegalStateException e) {
      // expected
    }
    Thread.sleep(100);
    for (Future<Object> f : c) {
      try {
        f.get(1, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        // OK, just want to make sure the client doesn't crash
      } catch (ExecutionException e) {
        // OK, at least we got one back.
      }
    }
    Thread.sleep(1000);
    assertTrue(client.set("kx", 0, "woo").get(5, TimeUnit.SECONDS));
  }
}
