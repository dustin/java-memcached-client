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

package net.spy.memcached.protocol.binary;

import java.util.Collections;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.VBucketAware;
import net.spy.memcached.protocol.ProxyCallback;

/**
 * Optimized Get operation for folding a bunch of gets together.
 */
final class OptimizedGetImpl extends MultiGetOperationImpl {

  private final ProxyCallback pcb;

  /**
   * Construct an optimized get starting with the given get operation.
   */
  public OptimizedGetImpl(GetOperation firstGet) {
    super(Collections.<String>emptySet(), new ProxyCallback());
    pcb = (ProxyCallback) getCallback();
    addOperation(firstGet);
  }

  /**
   * Add a new GetOperation to get.
   */
  public void addOperation(GetOperation o) {
    pcb.addCallbacks(o);
    for (String k : o.getKeys()) {
      addKey(k);
      setVBucket(k, ((VBucketAware) o).getVBucket(k));
    }
  }

  public int size() {
    return pcb.numKeys();
  }
}
