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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Test the various memcached client constructors.
 */
public class MemcachedClientConstructorTest extends TestCase {

  private MemcachedClient client = null;

  @Override
  protected void tearDown() throws Exception {
    if (client != null) {
      try {
        client.shutdown();
      } catch (NullPointerException e) {
        // This is a workaround for a disagreement betweewn how things
        // should work in eclipse and buildr. My plan is to upgrade to
        // junit4 all around and write some tests that are a bit easier
        // to follow.

        // The actual problem here is a client that isn't properly
        // initialized is attempting to be shut down.
      }
    }
    super.tearDown();
  }

  private void assertWorking() throws Exception {
    Map<SocketAddress, String> versions = client.getVersions();
    assertEquals("/" + TestConfig.IPV4_ADDR + ":11211",
        versions.keySet().iterator().next().toString());
  }

  private void assertArgRequired(IllegalArgumentException e) {
    assertEquals("You must have at least one server to connect to",
        e.getMessage());
  }

  public void testVarargConstructor() throws Exception {
    client =
        new MemcachedClient(new InetSocketAddress(
            InetAddress.getByName(TestConfig.IPV4_ADDR), 11211));
    assertWorking();
  }

  public void testEmptyVarargConstructor() throws Exception {
    try {
      client = new MemcachedClient();
      fail("Expected illegal arg exception, got " + client);
    } catch (IllegalArgumentException e) {
      assertArgRequired(e);
    }
  }

  public void testNulListConstructor() throws Exception {
    try {
      List<InetSocketAddress> l = null;
      client = new MemcachedClient(l);
      fail("Expected null pointer exception, got " + client);
    } catch (NullPointerException e) {
      assertEquals("Server list required", e.getMessage());
    }
  }

  public void testEmptyListConstructor() throws Exception {
    try {
      client = new MemcachedClient(Collections.<InetSocketAddress>emptyList());
      fail("Expected illegal arg exception, got " + client);
    } catch (IllegalArgumentException e) {
      assertArgRequired(e);
    }
  }

  public void testNullFactoryConstructor() throws Exception {
    try {
      client =
          new MemcachedClient(null, AddrUtil.getAddresses(TestConfig.IPV4_ADDR
              + ":11211"));
      fail("Expected null pointer exception, got " + client);
    } catch (NullPointerException e) {
      assertEquals("Connection factory required", e.getMessage());
    }
  }

  public void testNegativeTimeout() throws Exception {
    try {
      client = new MemcachedClient(new DefaultConnectionFactory() {
        @Override
        public long getOperationTimeout() {
          return -1;
        }
      }, AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11211"));
      fail("Expected null pointer exception, got " + client);
    } catch (IllegalArgumentException e) {
      assertEquals("Operation timeout must be positive.", e.getMessage());
    }
  }

  public void testZeroTimeout() throws Exception {
    try {
      client = new MemcachedClient(new DefaultConnectionFactory() {
        @Override
        public long getOperationTimeout() {
          return 0;
        }
      }, AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11211"));
      fail("Expected null pointer exception, got " + client);
    } catch (IllegalArgumentException e) {
      assertEquals("Operation timeout must be positive.", e.getMessage());
    }
  }

  public void testConnFactoryWithoutOpFactory() throws Exception {
    try {
      client = new MemcachedClient(new DefaultConnectionFactory() {
        @Override
        public OperationFactory getOperationFactory() {
          return null;
        }
      }, AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11211"));
    } catch (AssertionError e) {
      assertEquals("Connection factory failed to make op factory",
          e.getMessage());
    }
  }

  public void testConnFactoryWithoutConns() throws Exception {
    try {
      client = new MemcachedClient(new DefaultConnectionFactory() {
        @Override
        public MemcachedConnection createConnection(
            List<InetSocketAddress> addrs) throws IOException {
          return null;
        }
      }, AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11211"));
    } catch (AssertionError e) {
      assertEquals("Connection factory failed to make a connection",
          e.getMessage());
    }
  }

  public void testArraymodNodeLocatorAccessor() throws Exception {
    client =
        new MemcachedClient(AddrUtil.getAddresses(TestConfig.IPV4_ADDR
            + ":11211"));
    assertTrue(client.getNodeLocator() instanceof ArrayModNodeLocator);
    assertTrue(client.getNodeLocator().getPrimary("x")
        instanceof MemcachedNodeROImpl);
  }

  public void testKetamaNodeLocatorAccessor() throws Exception {
    client =
        new MemcachedClient(new KetamaConnectionFactory(),
            AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11211"));
    assertTrue(client.getNodeLocator() instanceof KetamaNodeLocator);
    assertTrue(client.getNodeLocator().getPrimary("x")
        instanceof MemcachedNodeROImpl);
  }
}
