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

import net.spy.memcached.KetamaNodeKeyFormatter;
import net.spy.memcached.MemcachedNode;

/**
 * A Default implementation of the configuration required for the
 * KetamaNodeLocator algorithm to run.
 */
public class DefaultKetamaNodeLocatorConfiguration implements
    KetamaNodeLocatorConfiguration {

  private final int numReps = 160;
  private final KetamaNodeKeyFormatter ketamaNodeKeyFormatter;

  /**
   * Create a KetamaNodeLocatorConfiguraiton with the default SPYMEMCACHED node
   * key format
   */
  public DefaultKetamaNodeLocatorConfiguration() {
      ketamaNodeKeyFormatter = new KetamaNodeKeyFormatter();
  }

  /**
   * Create a KetamaNodeLocatorConfiguraiton
   *
   * @param nodeKeyFormatter Ketama node key format, either SPYMEMCACHED or LIBMEMCACHED
   */

  public DefaultKetamaNodeLocatorConfiguration(KetamaNodeKeyFormatter nodeKeyFormatter) {
      ketamaNodeKeyFormatter = nodeKeyFormatter;
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
   * Delegates to the KetamaNodeKeyFormatter
   */
  public String getKeyForNode(MemcachedNode node, int repetition) {
    return ketamaNodeKeyFormatter.getKeyForNode(node, repetition);
  }
}
