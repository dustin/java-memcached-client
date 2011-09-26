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

package net.spy.memcached.vbucket;

import java.net.InetSocketAddress;
import java.util.Arrays;

import junit.framework.TestCase;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.vbucket.config.Config;
import net.spy.memcached.vbucket.config.ConfigFactory;
import net.spy.memcached.vbucket.config.DefaultConfigFactory;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * A VBucketNodeLocatorTest.
 */
public class VBucketNodeLocatorTest extends TestCase {

  private static final String CONFIG_IN_ENVELOPE =
      "{ \"otherKeyThatIsIgnored\": 12345,\n"
      + "\"nodes\": [\n"
      + "{\n"
      + "\"clusterCompatibility\": 1,\n"
      + "\"clusterMembership\": \"active\"\n,"
      + "\"couchApiBase\": \"http://10.2.1.67:5984/\"\n"
      + "}\n"
      + "],\n"
      + "\"vBucketServerMap\": \n"
      + "{\n"
      + "  \"hashAlgorithm\": \"CRC\",\n"
      + "  \"numReplicas\": 2,\n"
      + "  \"serverList\": [\"127.0.0.1:11211\", \"127.0.0.1:11210\", \""
      + "127.0.0.1:11212\"],\n"
      + "  \"vBucketMap\":\n" + "    [\n" + "      [0, 1, 2],\n"
      + "      [1, 2, 0],\n" + "      [2, 1, -1],\n" + "      [1, 2, 0]\n"
      + "    ]\n" + "}" + "}";

  public void testGetPrimary() {
    MemcachedNode node1 = createMock(MemcachedNode.class);
    MemcachedNode node2 = createMock(MemcachedNode.class);
    MemcachedNode node3 = createMock(MemcachedNode.class);
    InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 11211);
    InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 11210);
    InetSocketAddress address3 = new InetSocketAddress("127.0.0.1", 11212);

    expect(node1.getSocketAddress()).andReturn(address1);
    expect(node2.getSocketAddress()).andReturn(address2);
    expect(node3.getSocketAddress()).andReturn(address3);

    replay(node1, node2, node3);

    ConfigFactory factory = new DefaultConfigFactory();
    Config config = factory.create(CONFIG_IN_ENVELOPE);

    VBucketNodeLocator locator =
        new VBucketNodeLocator(Arrays.asList(node1, node2, node3), config);
    MemcachedNode resultNode = locator.getPrimary("key1");
    assertEquals(node1, resultNode);

    verify(node1, node2, node3);
  }

  public void testGetAlternative() {
    MemcachedNodeMockImpl node1 = new MemcachedNodeMockImpl();
    MemcachedNodeMockImpl node2 = new MemcachedNodeMockImpl();
    MemcachedNodeMockImpl node3 = new MemcachedNodeMockImpl();
    node1.setSocketAddress(new InetSocketAddress("127.0.0.1", 11211));
    node2.setSocketAddress(new InetSocketAddress("127.0.0.1", 11210));
    node3.setSocketAddress(new InetSocketAddress("127.0.0.1", 11212));
    ConfigFactory configFactory = new DefaultConfigFactory();
    Config config = configFactory.create(CONFIG_IN_ENVELOPE);
    VBucketNodeLocator locator =
        new VBucketNodeLocator(Arrays.asList((MemcachedNode) node1, node2,
            node3), config);
    MemcachedNode primary = locator.getPrimary("k1");
    MemcachedNode alternative =
        locator.getAlternative("k1", Arrays.asList(primary));
    alternative.getSocketAddress();
  }
}
