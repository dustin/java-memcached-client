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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Interface for locating a node by hash value.
 */
public interface NodeLocator {

  /**
   * Get the primary location for the given key.
   *
   * @param k the object key
   * @return the QueueAttachment containing the primary storage for a key
   */
  MemcachedNode getPrimary(String k);

  /**
   * Get an iterator over the sequence of nodes that make up the backup
   * locations for a given key.
   *
   * @param k the object key
   * @return the sequence of backup nodes.
   */
  Iterator<MemcachedNode> getSequence(String k);

  /**
   * Get all memcached nodes. This is useful for broadcasting messages.
   */
  Collection<MemcachedNode> getAll();

  /**
   * Create a read-only copy of this NodeLocator.
   */
  NodeLocator getReadonlyCopy();

  /**
   * Update locator status.
   *
   * @param nodes New locator nodes.
   */
  void updateLocator(final List<MemcachedNode> nodes);
}
