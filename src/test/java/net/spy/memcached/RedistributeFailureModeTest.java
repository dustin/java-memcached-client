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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A RedistributeFailoverModeTest.
 */
public class RedistributeFailureModeTest extends ClientBaseCase {

  private String serverList;

  @Override
  protected void setUp() throws Exception {
    serverList =
        TestConfig.IPV4_ADDR + ":" + TestConfig.PORT_NUMBER + " "
            + TestConfig.IPV4_ADDR + ":11311";
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
        return FailureMode.Redistribute;
      }
    });
  }

  @Override
  protected void flushPause() throws InterruptedException {
    Thread.sleep(100);
  }

  // Just to make sure the sequence is being handled correctly
  public void testMixedSetsAndUpdates() throws Exception {
    Collection<Future<CASResponse>> futures = new ArrayList<Future<CASResponse>>();
    Collection<String> keys = new ArrayList<String>();
    Thread.sleep(100);
    for (int i = 0; i < 100; i++) {
      String key = "k" + i;
      futures.add(client.set(key, 10, key));
      futures.add(client.add(key, 10, "a" + i));
      keys.add(key);
    }
    Map<String, Object> m = client.getBulk(keys);
    assertEquals(100, m.size());
    for (Map.Entry<String, Object> me : m.entrySet()) {
      assertEquals(me.getKey(), me.getValue());
    }
    for (Iterator<Future<CASResponse>> i = futures.iterator(); i.hasNext();) {
      assertTrue(i.next().get(10, TimeUnit.MILLISECONDS).type == CASResponseType.OK);
      assertFalse(i.next().get(10, TimeUnit.MILLISECONDS).type == CASResponseType.OK);
    }
    System.err.println(getName() + " complete.");
  }
}
