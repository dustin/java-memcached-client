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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.util.DefaultKetamaNodeLocatorConfiguration;
import net.spy.memcached.util.KetamaNodeLocatorConfiguration;

/**
 * This is an implementation of the Ketama consistent hash strategy from
 * last.fm. This implementation may not be compatible with libketama as hashing
 * is considered separate from node location.
 *
 * Note that this implementation does not currently supported weighted nodes.
 *
 * @see <a href="http://www.last.fm/user/RJ/journal/2007/04/10/392555/">RJ's
 *      blog post</a>
 */
public final class KetamaNodeLocator extends SpyObject implements NodeLocator {

  private volatile TreeMap<Long, MemcachedNode> ketamaNodes;
  private volatile Collection<MemcachedNode> allNodes;

  private final HashAlgorithm hashAlg;
  private final KetamaNodeLocatorConfiguration config;

  /**
   * Create a new KetamaNodeLocator using specified nodes and the specifed hash
   * algorithm.
   *
   * @param nodes The List of nodes to use in the Ketama consistent hash
   *          continuum
   * @param alg The hash algorithm to use when choosing a node in the Ketama
   *          consistent hash continuum
   */
  public KetamaNodeLocator(List<MemcachedNode> nodes, HashAlgorithm alg) {
    this(nodes, alg, new DefaultKetamaNodeLocatorConfiguration());
  }

  /**
   * Create a new KetamaNodeLocator using specified nodes and the specifed hash
   * algorithm and configuration.
   *
   * @param nodes The List of nodes to use in the Ketama consistent hash
   *          continuum
   * @param alg The hash algorithm to use when choosing a node in the Ketama
   *          consistent hash continuum
   * @param conf
   */
  public KetamaNodeLocator(List<MemcachedNode> nodes, HashAlgorithm alg,
      KetamaNodeLocatorConfiguration conf) {
    super();
    allNodes = nodes;
    hashAlg = alg;
    config = conf;
    setKetamaNodes(nodes);
  }

  private KetamaNodeLocator(TreeMap<Long, MemcachedNode> smn,
      Collection<MemcachedNode> an, HashAlgorithm alg,
      KetamaNodeLocatorConfiguration conf) {
    super();
    ketamaNodes = smn;
    allNodes = an;
    hashAlg = alg;
    config = conf;
  }

  public Collection<MemcachedNode> getAll() {
    return allNodes;
  }

  public MemcachedNode getPrimary(final String k) {
    MemcachedNode rv = getNodeForKey(hashAlg.hash(k));
    assert rv != null : "Found no node for key " + k;
    return rv;
  }

  long getMaxKey() {
    return getKetamaNodes().lastKey();
  }

  MemcachedNode getNodeForKey(long hash) {
    final MemcachedNode rv;
    if (!ketamaNodes.containsKey(hash)) {
      // Java 1.6 adds a ceilingKey method, but I'm still stuck in 1.5
      // in a lot of places, so I'm doing this myself.
      SortedMap<Long, MemcachedNode> tailMap = getKetamaNodes().tailMap(hash);
      if (tailMap.isEmpty()) {
        hash = getKetamaNodes().firstKey();
      } else {
        hash = tailMap.firstKey();
      }
    }
    rv = getKetamaNodes().get(hash);
    return rv;
  }

  public Iterator<MemcachedNode> getSequence(String k) {
    // Seven searches gives us a 1 in 2^7 chance of hitting the
    // same dead node all of the time.
    return new KetamaIterator(k, 7, getKetamaNodes(), hashAlg);
  }

  public NodeLocator getReadonlyCopy() {
    TreeMap<Long, MemcachedNode> smn =
        new TreeMap<Long, MemcachedNode>(getKetamaNodes());
    Collection<MemcachedNode> an =
        new ArrayList<MemcachedNode>(allNodes.size());

    // Rewrite the values a copy of the map.
    for (Map.Entry<Long, MemcachedNode> me : smn.entrySet()) {
      me.setValue(new MemcachedNodeROImpl(me.getValue()));
    }

    // Copy the allNodes collection.
    for (MemcachedNode n : allNodes) {
      an.add(new MemcachedNodeROImpl(n));
    }

    return new KetamaNodeLocator(smn, an, hashAlg, config);
  }

  @Override
  public void updateLocator(List<MemcachedNode> nodes) {
    allNodes = nodes;
    setKetamaNodes(nodes);
  }

  /**
   * @return the ketamaNodes
   */
  protected TreeMap<Long, MemcachedNode> getKetamaNodes() {
    return ketamaNodes;
  }

  /**
   * Setup the KetamaNodeLocator with the list of nodes it should use.
   *
   * @param nodes a List of MemcachedNodes for this KetamaNodeLocator to use in
   *          its continuum
   */
  protected void setKetamaNodes(List<MemcachedNode> nodes) {
    TreeMap<Long, MemcachedNode> newNodeMap =
        new TreeMap<Long, MemcachedNode>();
    int numReps = config.getNodeRepetitions();
    for (MemcachedNode node : nodes) {
      // Ketama does some special work with md5 where it reuses chunks.
      if (hashAlg == DefaultHashAlgorithm.KETAMA_HASH) {
        for (int i = 0; i < numReps / 4; i++) {
          byte[] digest =
              DefaultHashAlgorithm.computeMd5(config.getKeyForNode(node, i));
          for (int h = 0; h < 4; h++) {
            Long k = ((long) (digest[3 + h * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + h * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + h * 4] & 0xFF) << 8)
                    | (digest[h * 4] & 0xFF);
            newNodeMap.put(k, node);
            getLogger().debug("Adding node %s in position %d", node, k);
          }
        }
      } else {
        for (int i = 0; i < numReps; i++) {
          newNodeMap.put(hashAlg.hash(config.getKeyForNode(node, i)), node);
        }
      }
    }
    assert newNodeMap.size() == numReps * nodes.size();
    ketamaNodes = newNodeMap;
  }
}
