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

package net.spy.memcached.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.compat.BaseMockCase;
import net.spy.memcached.internal.ImmediateFuture;

import org.jmock.Mock;

/**
 * Test the cache loader.
 */
public class CacheLoaderTest extends BaseMockCase {

  private ExecutorService es = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    BlockingQueue<Runnable> wq = new LinkedBlockingQueue<Runnable>();
    es = new ThreadPoolExecutor(10, 10, 5 * 60, TimeUnit.SECONDS, wq);
  }

  @Override
  protected void tearDown() throws Exception {
    es.shutdownNow();
    super.tearDown();
  }

  public void testSimpleLoading() throws Exception {
    Mock m = mock(MemcachedClientIF.class);

    LoadCounter sl = new LoadCounter();
    CacheLoader cl = new CacheLoader((MemcachedClientIF) m.proxy(), es, sl, 0);

    m.expects(once()).method("set").with(eq("a"), eq(0),
        eq(1)).will(returnValue(new ImmediateFuture(true)));
    m.expects(once()).method("set").with(eq("a"), eq(0),
        eq(1)).will(throwException(new IllegalStateException("Full up")));
    m.expects(once()).method("set").with(eq("b"), eq(0), eq(2))
        .will(returnValue(new ImmediateFuture(new RuntimeException("blah"))));
    m.expects(once()).method("set").with(eq("c"), eq(0),
        eq(3)).will(returnValue(new ImmediateFuture(false)));

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);

    // Load the cache and wait for it to finish.
    cl.loadData(map).get();
    es.shutdown();
    es.awaitTermination(1, TimeUnit.SECONDS);

    assertEquals(1, sl.success.get());
    assertEquals(1, sl.exceptions.get());
    assertEquals(1, sl.failure.get());
  }

  static class LoadCounter implements CacheLoader.StorageListener {

    private AtomicInteger exceptions = new AtomicInteger(0);
    private AtomicInteger success = new AtomicInteger(0);
    private AtomicInteger failure = new AtomicInteger(0);

    public void errorStoring(String k, Exception e) {
      exceptions.incrementAndGet();
    }

    public void storeResult(String k, boolean result) {
      if (result) {
        success.incrementAndGet();
      } else {
        failure.incrementAndGet();
      }
    }
  }
}
