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

import java.util.List;

import junit.framework.TestCase;

import net.spy.memcached.vbucket.config.Config;
import net.spy.memcached.vbucket.config.ConfigFactory;
import net.spy.memcached.vbucket.config.DefaultConfigFactory;

/**
 * A VBucketCacheNodeLocatorTest.
 */
public class VBucketCacheNodeLocatorTest extends TestCase {

  private static final String CONFIG_IN_ENVELOPE = "{"
      + "    \"authType\": \"sasl\", "
      + "    \"basicStats\": {"
      + "        \"diskUsed\": 0, "
      + "        \"hitRatio\": 0, "
      + "        \"itemCount\": 10001, "
      + "        \"memUsed\": 27007687, "
      + "        \"opsPerSec\": 0, "
      + "        \"quotaPercentUsed\": 10.061147436499596"
      + "    }, "
      + "    \"bucketType\": \"memcached\", "
      + "    \"flushCacheUri\": \"/pools/default/buckets/default/controller"
      + "/doFlush\", "
      + "    \"name\": \"default\", "
      + "    \"nodeLocator\": \"ketama\", "
      + "    \"nodes\": ["
      + "        {"
      + "            \"clusterCompatibility\": 1, "
      + "            \"clusterMembership\": \"active\", "
      + "            \"hostname\": \"127.0.0.1:8091\", "
      + "            \"mcdMemoryAllocated\": 2985, "
      + "            \"mcdMemoryReserved\": 2985, "
      + "            \"memoryFree\": 285800000, "
      + "            \"memoryTotal\": 3913584000.0, "
      + "            \"os\": \"i386-apple-darwin9.8.0\", "
      + "            \"ports\": {"
      + "                \"direct\": 11210, "
      + "                \"proxy\": 11211"
      + "            }, "
      + "            \"replication\": 1.0, "
      + "            \"status\": \"unhealthy\", "
      + "            \"uptime\": \"4204\", "
      + "            \"version\": \"1.6.5\""
      + "        }"
      + "    ], "
      + "    \"proxyPort\": 0, "
      + "    \"quota\": {"
      + "        \"ram\": 268435456, "
      + "        \"rawRAM\": 268435456"
      + "    }, "
      + "    \"replicaNumber\": 0, "
      + "    \"saslPassword\": \"\", "
      + "    \"stats\": {"
      + "        \"uri\": \"/pools/default/buckets/default/stats\""
      + "    }, "
      + "    \"streamingUri\": \"/pools/default/bucketsStreaming/default"
      + "\", "
      + "    \"uri\": \"/pools/default/buckets/default\"" + "}";

  public void testGetPrimary() {
    /*
     * MemcachedNode node1 = createMock(MemcachedNode.class); MemcachedNode
     * node2 = createMock(MemcachedNode.class); MemcachedNode node3 =
     * createMock(MemcachedNode.class); InetSocketAddress address1 = new
     * InetSocketAddress("127.0.0.1", 11211); InetSocketAddress address2 = new
     * InetSocketAddress("127.0.0.1", 11210); InetSocketAddress address3 = new
     * InetSocketAddress("127.0.0.1", 11211);
     *
     * expect(node1.getSocketAddress()).andReturn(address1);
     * expect(node2.getSocketAddress()).andReturn(address2);
     * expect(node3.getSocketAddress()).andReturn(address3);
     *
     * replay(node1, node2, node3);
     *
     * ConfigFactory factory = new DefaultConfigFactory(); Config config =
     * factory.createConfigFromString(configInEnvelope);
     *
     * VBucketNodeLocator locator = new VBucketNodeLocator(Arrays.asList(node1,
     * node2, node3), config); MemcachedNode resultNode =
     * locator.getPrimary("key1"); //assertEquals(node1, resultNode);
     *
     * verify(node1, node2, node3);
     */
  }

  public void testGetConfig() {
    ConfigFactory configFactory = new DefaultConfigFactory();
    Config config = configFactory.create(CONFIG_IN_ENVELOPE);
    config.getServersCount();
    List<String> servers = config.getServers();
    System.out.println(servers);
  }
}
