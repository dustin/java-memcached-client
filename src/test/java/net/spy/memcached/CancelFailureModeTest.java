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
 * A CancelFailureModeTest.
 */
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A CancellationFailureModeTest.
 */
public class CancelFailureModeTest extends ClientBaseCase {
  private String serverList;

  @Override
  protected void setUp() throws Exception {
    serverList = TestConfig.IPV4_ADDR + ":" + TestConfig.PORT_NUMBER
        + " " + TestConfig.IPV4_ADDR
        + ":11311";
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    serverList = TestConfig.IPV4_ADDR + ":" + TestConfig.PORT_NUMBER;
    super.tearDown();
  }

  @Override
  protected void initClient(ConnectionFactory cf) throws Exception {
    client = new MemcachedClient(cf, AddrUtil.getAddresses(serverList));
  }

  @Override
  protected void initClient() throws Exception {
    initClient(new DefaultConnectionFactory() {
      @Override
      public FailureMode getFailureMode() {
        return FailureMode.CANCEL;
      }
    });
  }

  @Override
  protected void flushPause() throws InterruptedException {
    Thread.sleep(100);
  }

  public void testQueueingToDownServer() throws Exception {
    Future<Boolean> f = client.add("someKey", 0, "some object");
    try {
      boolean b = f.get();
      fail("Should've thrown an exception, returned " + b);
    } catch (ExecutionException e) {
      // probably OK
    }
    assertTrue(f.isCancelled());
  }
}
