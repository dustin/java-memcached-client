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

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.VBucketAware;

/**
 * Binary operations that contain a single key and are VBucket aware operations
 * should extend this class.
 */
abstract class SingleKeyOperationImpl extends OperationImpl implements
    VBucketAware, KeyedOperation {

  protected final String key;

  protected SingleKeyOperationImpl(byte c, int o, String k,
      OperationCallback cb) {
    super(c, o, cb);
    key = k;
  }

  public Collection<String> getKeys() {
    return Collections.singleton(key);
  }

  public Collection<MemcachedNode> getNotMyVbucketNodes() {
    return notMyVbucketNodes;
  }

  public void addNotMyVbucketNode(MemcachedNode node) {
    notMyVbucketNodes.add(node);
  }

  public void setNotMyVbucketNodes(Collection<MemcachedNode> nodes) {
    notMyVbucketNodes = nodes;
  }

  public void setVBucket(String k, short vb) {
    assert k.equals(key) : k + " doesn't match the key " + key
        + " for this operation";
    vbucket = vb;
  }

  public short getVBucket(String k) {
    assert k.equals(key) : k + " doesn't match the key " + key
        + " for this operation";
    return vbucket;
  }

  @Override
  public String toString() {
    return super.toString() + " Key: " + key;
  }
}
