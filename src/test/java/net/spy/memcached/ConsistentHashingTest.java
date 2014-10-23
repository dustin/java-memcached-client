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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import junit.framework.TestCase;

/**
 */
public class ConsistentHashingTest extends TestCase {

  public void testSmallSet() {
    runThisManyNodes(3);
  }

  public void testLargeSet() {
    runThisManyNodes(100);
  }

  /**
   * Simulate dropping from (totalNodes) to (totalNodes-1). Ensure hashing is
   * consistent between the the two scenarios.
   *
   * @param totalNodes
   */
  private void runThisManyNodes(final int totalNodes) {
    final String[] stringNodes = generateAddresses(totalNodes);

    List<MemcachedNode> smaller =
        createNodes(AddrUtil.getAddresses(stringNodes[0]));
    List<MemcachedNode> larger =
        createNodes(AddrUtil.getAddresses(stringNodes[1]));

    assertTrue(larger.containsAll(smaller));
    MemcachedNode oddManOut = larger.get(larger.size() - 1);
    assertFalse(smaller.contains(oddManOut));

    KetamaNodeLocator lgLocator =
        new KetamaNodeLocator(larger, DefaultHashAlgorithm.KETAMA_HASH);
    KetamaNodeLocator smLocator =
        new KetamaNodeLocator(smaller, DefaultHashAlgorithm.KETAMA_HASH);

    SortedMap<Long, MemcachedNode> lgMap = lgLocator.getKetamaNodes();
    SortedMap<Long, MemcachedNode> smMap = smLocator.getKetamaNodes();

    // Verify that EVERY entry in the smaller map has an equivalent
    // mapping in the larger map.
    boolean failed = false;
    for (final Long key : smMap.keySet()) {
      final MemcachedNode largeNode = lgMap.get(key);
      final MemcachedNode smallNode = smMap.get(key);
      if (!largeNode.equals(smallNode)) {
        failed = true;
        System.out.println("---------------");
        System.out.println("Key: " + key);
        System.out.println("Small: " + smallNode.getHostPort());
        System.out.println("Large: " + largeNode.getHostPort());
      }
    }
    assertFalse(failed);

    for (final Map.Entry<Long, MemcachedNode> entry : lgMap.entrySet()) {
      final Long key = entry.getKey();
      final MemcachedNode node = entry.getValue();
      if (node.equals(oddManOut)) {
        final MemcachedNode newNode = smLocator.getNodeForKey(key);
        if (!smaller.contains(newNode)) {
          System.out.println("Error - " + key + " -> "
              + newNode.getHostPort());
          failed = true;
        }
      }
    }
    assertFalse(failed);
  }

  private String[] generateAddresses(final int maxSize) {
    final String[] results = new String[2];

    // Generate a pseudo-random set of addresses.
    long now = new Date().getTime();
    int first = (int) ((now % 250) + 3);

    int second = (int) (((now / 250) % 250) + 3);

    String port = ":" + TestConfig.PORT_NUMBER + " ";
    int last = (int) ((now % 100) + 3);

    StringBuffer prefix = new StringBuffer();
    prefix.append(first);
    prefix.append(".");
    prefix.append(second);
    prefix.append(".1.");

    // Don't protect the possible range too much, as we are our own client.
    StringBuffer buf = new StringBuffer();
    for (int ix = 0; ix < maxSize - 1; ix++) {
      buf.append(prefix);
      buf.append(last + ix);
      buf.append(port);
    }

    results[0] = buf.toString();

    buf.append(prefix);
    buf.append(last + maxSize - 1);
    buf.append(port);

    results[1] = buf.toString();

    return results;
  }

  private List<MemcachedNode> createNodes(List<HostPort> addresses) {
    List<MemcachedNode> results = new ArrayList<MemcachedNode>();

    for (HostPort addr : addresses) {
      results.add(new MockMemcachedNode(addr));
    }

    return results;
  }
}
