/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.ops.UnlockOperation;

/**
 * Operation to delete an item from the cache.
 */
final class UnlockOperationImpl extends OperationImpl implements
    UnlockOperation {

  private static final int OVERHEAD = 32;

  private static final OperationStatus UNLOCKED = new OperationStatus(true,
      "UNLOCKED", StatusCode.SUCCESS);
  private static final OperationStatus NOT_FOUND = new OperationStatus(false,
      "NOT_FOUND", StatusCode.ERR_NOT_FOUND);

  private static final String CMD = "unl";
  private final String key;
  private final long cas;

  public UnlockOperationImpl(String k, long casId,
          OperationCallback cb) {
    super(cb);
    key = k;
    cas = casId;
  }

  @Override
  public void handleLine(String line) {
    getLogger().debug("Unlock of %s returned %s", key, line);
    getCallback().receivedStatus(matchStatus(line, UNLOCKED, NOT_FOUND));
    transitionState(OperationState.COMPLETE);
  }

  @Override
  public void initialize() {
    ByteBuffer b = ByteBuffer.allocate(KeyUtil.getKeyBytes(key).length
        + OVERHEAD);
    setArguments(b, CMD, key, cas);
    b.flip();
    setBuffer(b);
  }

  @Override
  public Collection<String> getKeys() {
    return Collections.singleton(key);
  }

  @Override
  public String toString() {
    return "Cmd: " + CMD + " Key: " + key + " Cas Value: " + cas;
  }
}
