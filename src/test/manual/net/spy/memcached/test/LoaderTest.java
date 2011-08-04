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

package net.spy.memcached.test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.util.CacheLoader;

/**
 * Loader performance test.
 */
public class LoaderTest extends SpyObject implements Runnable {

  private final int count;
  private MemcachedClient client;

  public LoaderTest(int c) {
    count = c;
  }

  public void init() throws Exception {
    client = new MemcachedClient(new ConnectionFactoryBuilder()
        .setProtocol(Protocol.BINARY).setOpQueueMaxBlockTime(1000).build(),
        AddrUtil.getAddresses("localhost:11211"));
  }

  public void shutdown() throws Exception {
    client.shutdown();
  }

  public void run() {
    CacheLoader cl = new CacheLoader(client);

    Future<Boolean> f = null;
    for (int i = 0; i < count; i++) {
      f = cl.push("k" + i, "some value");
    }
    if (f != null) {
      try {
        f.get(1, TimeUnit.MINUTES);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] a) throws Exception {
    LoaderTest lt = new LoaderTest(1000000);
    lt.init();
    long start = System.currentTimeMillis();
    try {
      lt.run();
    } finally {
      lt.shutdown();
    }
    long end = System.currentTimeMillis();
    System.out.println("Runtime:  " + (end - start) + "ms");
  }
}
