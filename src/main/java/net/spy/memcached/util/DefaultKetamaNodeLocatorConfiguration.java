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

package net.spy.memcached.util;

import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.MemcachedNode;

/**
 * A Default implementation of the configuration required for the
 * KetamaNodeLocator algorithm to run.
 */
public class DefaultKetamaNodeLocatorConfiguration implements
    KetamaNodeLocatorConfiguration {

  private final int numReps = 160;

  // Internal lookup map to try to carry forward the optimisation that was
  // previously in KetamaNodeLocator
  protected Map<MemcachedNode, String> socketAddresses =
      new HashMap<MemcachedNode, String>();

  /**
   * Returns the socket address of a given MemcachedNode.
   *
   * @param node The node which we're interested in
   * @return String the socket address of that node.
   */
  protected String getSocketAddressForNode(MemcachedNode node) {
    // Using the internal map retrieve the socket addresses
    // for given nodes.
    // I'm aware that this code is inherently thread-unsafe as
    // I'm using a HashMap implementation of the map, but the worst
    // case ( I believe) is we're slightly in-efficient when
    // a node has never been seen before concurrently on two different
    // threads, so it the socketaddress will be requested multiple times!
    // all other cases should be as fast as possible.
    String result = socketAddresses.get(node);
    if (result == null) {
      result = String.valueOf(node.getSocketAddress());
      if (result.startsWith("/")) {
        result = result.substring(1);
      }
      socketAddresses.put(node, result);
    }
    return result;
  }

  /**
   * Returns the number of discrete hashes that should be defined for each node
   * in the continuum.
   *
   * @return NUM_REPS repetitions.
   */
  public int getNodeRepetitions() {
    return numReps;
  }

  /**
   * Returns a uniquely identifying key, suitable for hashing by the
   * KetamaNodeLocator algorithm.
   *
   * <p>
   * This default implementation uses the socket-address of the MemcachedNode
   * and concatenates it with a hyphen directly against the repetition number
   * for example a key for a particular server's first repetition may look like:
   * <p>
   *
   * <p>
   * <code>myhost/10.0.2.1-0</code>
   * </p>
   *
   * <p>
   * for the second repetition
   * </p>
   *
   * <p>
   * <code>myhost/10.0.2.1-1</code>
   * </p>
   *
   * <p>
   * for a server where reverse lookups are failing the returned keys may look
   * like
   * </p>
   *
   * <p>
   * <code>/10.0.2.1-0</code> and <code>/10.0.2.1-1</code>
   * </p>
   *
   * @param node The MemcachedNode to use to form the unique identifier
   * @param repetition The repetition number for the particular node in question
   *          (0 is the first repetition)
   * @return The key that represents the specific repetition of the node
   */
  public String getKeyForNode(MemcachedNode node, int repetition) {
    return getSocketAddressForNode(node) + "-" + repetition;
  }
}
