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

package net.spy.memcached;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import net.spy.memcached.compat.SpyObject;

/**
 * Implements an Iterator which the KetamaNodeLoctaor may return to a client for
 * iterating through alternate nodes for a given key.
 */
class KetamaIterator extends SpyObject implements Iterator<MemcachedNode> {

  private final String key;
  private long hashVal;
  private int remainingTries;
  private int numTries = 0;
  private final HashAlgorithm hashAlg;
  private final TreeMap<Long, MemcachedNode> ketamaNodes;

  /**
   * Create a new KetamaIterator to be used by a client for an operation.
   *
   * @param k the key to iterate for
   * @param t the number of tries until giving up
   * @param ketamaNodes the continuum in the form of a TreeMap to be used when
   *          selecting a node
   * @param hashAlg the hash algorithm to use when selecting within the
   *          continuumq
   */
  protected KetamaIterator(final String k, final int t,
      TreeMap<Long, MemcachedNode> ketamaNodes, final HashAlgorithm hashAlg) {
    super();
    this.ketamaNodes = ketamaNodes;
    this.hashAlg = hashAlg;
    hashVal = hashAlg.hash(k);
    remainingTries = t;
    key = k;
  }

  private void nextHash() {
    // this.calculateHash(Integer.toString(tries)+key).hashCode();
    long tmpKey = hashAlg.hash((numTries++) + key);
    // This echos the implementation of Long.hashCode()
    hashVal += (int) (tmpKey ^ (tmpKey >>> 32));
    hashVal &= 0xffffffffL; /* truncate to 32-bits */
    remainingTries--;
  }

  public boolean hasNext() {
    return remainingTries > 0;
  }

  public MemcachedNode next() {
    try {
      return getNodeForKey(hashVal);
    } finally {
      nextHash();
    }
  }

  public void remove() {
    throw new UnsupportedOperationException("remove not supported");
  }

  private MemcachedNode getNodeForKey(long hash) {
    final MemcachedNode rv;
    if (!ketamaNodes.containsKey(hash)) {
      // Java 1.6 adds a ceilingKey method, but I'm still stuck in 1.5
      // in a lot of places, so I'm doing this myself.
      SortedMap<Long, MemcachedNode> tailMap = ketamaNodes.tailMap(hash);
      if (tailMap.isEmpty()) {
        hash = ketamaNodes.firstKey();
      } else {
        hash = tailMap.firstKey();
      }
    }
    rv = ketamaNodes.get(hash);
    return rv;
  }
}
