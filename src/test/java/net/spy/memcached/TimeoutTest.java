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

/**
 * A TimeoutTest.
 */
public class TimeoutTest extends ClientBaseCase {

  @Override
  protected void tearDown() throws Exception {
    // override teardown to avoid the flush phase
    client.shutdown();
  }

  @Override
  protected void initClient() throws Exception {
    client = new MemcachedClient(new DefaultConnectionFactory() {
      @Override
      public long getOperationTimeout() {
        return 20;
      }

      @Override
      public FailureMode getFailureMode() {
        return FailureMode.Retry;
      }
    }, AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":64213"));
  }

  private void tryTimeout(String name, Runnable r) {
    try {
      r.run();
      fail("Expected timeout in " + name);
    } catch (OperationTimeoutException e) {
      // pass
    }
  }

  public void testCasTimeout() {
    tryTimeout("cas", new Runnable() {
      public void run() {
        client.cas("k", 1, "blah");
      }
    });
  }

  public void testGetsTimeout() {
    tryTimeout("gets", new Runnable() {
      public void run() {
        client.gets("k");
      }
    });
  }

  public void testGetTimeout() {
    tryTimeout("get", new Runnable() {
      public void run() {
        client.get("k");
      }
    });
  }

  public void testGetBulkTimeout() {
    tryTimeout("getbulk", new Runnable() {
      public void run() {
        client.getBulk("k", "k2");
      }
    });
  }

  public void testIncrTimeout() {
    tryTimeout("incr", new Runnable() {
      public void run() {
        client.incr("k", 1);
      }
    });
  }

  public void testIncrWithDefTimeout() {
    tryTimeout("incrWithDef", new Runnable() {
      public void run() {
        client.incr("k", 1, 5);
      }
    });
  }
}
