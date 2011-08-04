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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.vbucket.config.Config;
import net.spy.memcached.vbucket.config.ConfigDifference;

/**
 * Implementation of the {@link NodeLocator} interface that contains vbucket
 * hashing methods.
 */
public class VBucketNodeLocator extends SpyObject implements NodeLocator {

  private final AtomicReference<TotalConfig> fullConfig;

  /**
   * Construct a VBucketNodeLocator over the given JSON configuration string.
   *
   * @param nodes
   * @param jsonConfig
   */
  public VBucketNodeLocator(List<MemcachedNode> nodes, Config jsonConfig) {
    super();
    fullConfig = new AtomicReference<TotalConfig>();
    fullConfig.set(new TotalConfig(jsonConfig, fillNodesEntries(nodes)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MemcachedNode getPrimary(String k) {
    TotalConfig totConfig = fullConfig.get();
    Config config = totConfig.getConfig();
    Map<String, MemcachedNode> nodesMap = totConfig.getNodesMap();
    int vbucket = config.getVbucketByKey(k);
    int serverNumber = config.getMaster(vbucket);
    String server = config.getServer(serverNumber);
    // choose appropriate MemcachedNode according to config data
    MemcachedNode pNode = nodesMap.get(server);
    if (pNode == null) {
      getLogger().error("The node locator does not have a primary for key"
        + " %s.  Wanted vbucket %s which should be on server %s.", k,
        vbucket, server);
      getLogger().error("List of nodes has %s entries:", nodesMap.size());
      Set<String> keySet = nodesMap.keySet();
      Iterator<String> iterator = keySet.iterator();
      while (iterator.hasNext()) {
        String anode = iterator.next();
        getLogger().error("MemcachedNode for %s is %s", anode,
          nodesMap.get(anode));
      }
      Collection<MemcachedNode> nodes = nodesMap.values();
      for (MemcachedNode node : nodes) {
        getLogger().error(node);
      }
    }
    assert (pNode != null);
    return pNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<MemcachedNode> getSequence(String k) {
    return new NullIterator<MemcachedNode>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<MemcachedNode> getAll() {
    Map<String, MemcachedNode> nodesMap = fullConfig.get().getNodesMap();
    return nodesMap.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeLocator getReadonlyCopy() {
    return this;
  }

  public void updateLocator(final List<MemcachedNode> nodes,
      final Config newconf) {
    // we'll get a new config for various reasons we don't care about, so check
    // if we do care
    Config current = fullConfig.get().getConfig();
    ConfigDifference compareTo = current.compareTo(newconf);
    if (compareTo.isSequenceChanged() || compareTo.getVbucketsChanges() > 0) {
      getLogger().debug("Updating configuration, received updated configuration"
        + " with significant changes.");
      fullConfig.set(new TotalConfig(newconf, fillNodesEntries(nodes)));
    } else {
      getLogger().debug("Received updated configuration with insignificant "
        + "changes.");
    }
  }

  /**
   * Returns a vbucket index for the given key.
   *
   * @param key the key
   * @return vbucket index
   */
  public int getVBucketIndex(String key) {
    Config config = fullConfig.get().getConfig();
    return config.getVbucketByKey(key);
  }

  private Map<String, MemcachedNode> fillNodesEntries(
      Collection<MemcachedNode> nodes) {
    HashMap<String, MemcachedNode> vbnodesMap =
        new HashMap<String, MemcachedNode>();
    getLogger().debug("Updating nodesMap in VBucketNodeLocator.");
    for (MemcachedNode node : nodes) {
      InetSocketAddress addr = (InetSocketAddress) node.getSocketAddress();
      String address = addr.getAddress().getHostName() + ":" + addr.getPort();
      String hostname = addr.getAddress().getHostAddress() + ":"
        + addr.getPort();
      getLogger().debug("Adding node with hostname %s and address %s.",
          hostname, address);
      getLogger().debug("Node added is %s.", node);
      vbnodesMap.put(address, node);
      vbnodesMap.put(hostname, node);
    }

    return Collections.unmodifiableMap(vbnodesMap);
  }

  /**
   * Method returns the node that is not contained in the specified collection
   * of the failed nodes.
   *
   * @param k the key
   * @param notMyVbucketNodes a collection of the nodes are excluded
   * @return The first MemcachedNode which meets requirements
   */
  public MemcachedNode getAlternative(String k,
      Collection<MemcachedNode> notMyVbucketNodes) {
    // it's safe to only copy the map here, only removing references found to be
    // incorrect, and trying remaining
    Map<String, MemcachedNode> nodesMap =
        new HashMap<String, MemcachedNode>(fullConfig.get().getNodesMap());
    Collection<MemcachedNode> nodes = nodesMap.values();
    nodes.removeAll(notMyVbucketNodes);
    if (nodes.isEmpty()) {
      return null;
    } else {
      return nodes.iterator().next();
    }
  }

  private class TotalConfig {
    private Config config;
    private Map<String, MemcachedNode> nodesMap;

    public TotalConfig(Config newConfig, Map<String, MemcachedNode> newMap) {
      config = newConfig;
      nodesMap = Collections.unmodifiableMap(newMap);
    }

    protected Config getConfig() {
      return config;
    }

    protected Map<String, MemcachedNode> getNodesMap() {
      return nodesMap;
    }
  }

  class NullIterator<E> implements Iterator<MemcachedNode> {

    public boolean hasNext() {
      return false;
    }

    public MemcachedNode next() {
      throw new NoSuchElementException(
          "VBucketNodeLocators have no alternate nodes.");
    }

    public void remove() {
      throw new UnsupportedOperationException(
          "VBucketNodeLocators have no alternate nodes; cannot remove.");
    }
  }
}
