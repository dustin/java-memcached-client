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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * NodeLocator implementation for dealing with simple array lookups using a
 * modulus of the hash code and node list length.
 */
public final class ArrayModNodeLocator implements NodeLocator {

  private final HashAlgorithm hashAlg;

  private MemcachedNode[] nodes;

  /**
   * Construct an ArraymodNodeLocator over the given array of nodes and using
   * the given hash algorithm.
   *
   * @param n the array of nodes
   * @param alg the hash algorithm
   */
  public ArrayModNodeLocator(List<MemcachedNode> n, HashAlgorithm alg) {
    super();
    nodes = n.toArray(new MemcachedNode[n.size()]);
    hashAlg = alg;
  }

  private ArrayModNodeLocator(MemcachedNode[] n, HashAlgorithm alg) {
    super();
    nodes = n;
    hashAlg = alg;
  }

  public Collection<MemcachedNode> getAll() {
    return Arrays.asList(nodes);
  }

  public MemcachedNode getPrimary(String k) {
    return nodes[getServerForKey(k)];
  }

  public Iterator<MemcachedNode> getSequence(String k) {
    return new NodeIterator(getServerForKey(k));
  }

  public NodeLocator getReadonlyCopy() {
    MemcachedNode[] n = new MemcachedNode[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      n[i] = new MemcachedNodeROImpl(nodes[i]);
    }
    return new ArrayModNodeLocator(n, hashAlg);
  }

  @Override
  public void updateLocator(List<MemcachedNode> newNodes) {
    this.nodes = newNodes.toArray(new MemcachedNode[newNodes.size()]);
  }

  private int getServerForKey(String key) {
    int rv = (int) (hashAlg.hash(key) % nodes.length);
    assert rv >= 0 : "Returned negative key for key " + key;
    assert rv < nodes.length : "Invalid server number " + rv + " for key "
        + key;
    return rv;
  }

  class NodeIterator implements Iterator<MemcachedNode> {

    private final int start;
    private int next = 0;

    public NodeIterator(int keyStart) {
      start = keyStart;
      next = start;
      computeNext();
      assert next >= 0 || nodes.length == 1 : "Starting sequence at " + start
          + " of " + nodes.length + " next is " + next;
    }

    public boolean hasNext() {
      return next >= 0;
    }

    private void computeNext() {
      if (++next >= nodes.length) {
        next = 0;
      }
      if (next == start) {
        next = -1;
      }
    }

    public MemcachedNode next() {
      try {
        return nodes[next];
      } finally {
        computeNext();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("Can't remove a node");
    }
  }
}
