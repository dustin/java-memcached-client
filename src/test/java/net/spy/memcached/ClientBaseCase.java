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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

/**
 * A ClientBaseCase.
 */
public abstract class ClientBaseCase extends TestCase {

  protected MemcachedClient client = null;
  protected Boolean membase;
  protected Boolean moxi;

  protected void initClient() throws Exception {
    initClient(new DefaultConnectionFactory() {
      @Override
      public long getOperationTimeout() {
        return 15000;
      }

      @Override
      public FailureMode getFailureMode() {
        return FailureMode.Retry;
      }
    });
  }

  protected void initClient(ConnectionFactory cf) throws Exception {
    client = new MemcachedClient(cf, AddrUtil.getAddresses(TestConfig.IPV4_ADDR
        + ":" + TestConfig.PORT_NUMBER));
  }

  protected Collection<String> stringify(Collection<?> c) {
    Collection<String> rv = new ArrayList<String>();
    for (Object o : c) {
      rv.add(String.valueOf(o));
    }
    return rv;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    initClient();
  }

  @Override
  protected void tearDown() throws Exception {
    // Shut down, start up, flush, and shut down again. Error tests have
    // unpredictable timing issues.
    client.shutdown();
    client = null;
    initClient();
    flushPause();
    assertTrue(client.flush().get());
    client.shutdown();
    client = null;
    super.tearDown();
  }

  protected void flushPause() throws InterruptedException {
    // nothing useful
  }

  protected boolean isMoxi() {
    if (moxi != null) {
      return moxi.booleanValue();
    }
    // some tests are invalid if using moxi

    Map<SocketAddress, Map<String, String>> stats = client.getStats("proxy");
    for (Map<String, String> node : stats.values()) {
      if (node.get("basic:version") != null) {
        moxi = true;
        System.err.println("Using proxy");
        break;
      } else {
        moxi = false;
        System.err.println("Not using proxy");
      }
    }
    return moxi.booleanValue();
  }
}
