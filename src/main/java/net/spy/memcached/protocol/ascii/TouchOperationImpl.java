/**
 * Copyright (C) 2013 SoundCloud, Ltd.
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

import net.spy.memcached.ops.TouchOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.KeyUtil;

/**
 * Memcached touch operation.
 */
final class TouchOperationImpl extends SingleKeyOperationImpl implements TouchOperation {

  // TODO: check real overhead
  private static final int OVERHEAD = 9;

  private static final OperationStatus OK = new OperationStatus(true, "TOUCHED");

  private final String key;
  private final long exp;

  public TouchOperationImpl(String k, long t, OperationCallback cb) {
    super(k, cb);
    key = k;
    exp = t;
  }

  @Override
  public void handleLine(String line) {
    getLogger().debug("Touch completed successfully");
    getCallback().receivedStatus(matchStatus(line, OK));
    transitionState(OperationState.COMPLETE);
  }

  @Override
  public void initialize() {
    ByteBuffer b = null;
    b = ByteBuffer.allocate(KeyUtil.getKeyBytes(key).length + String.valueOf(exp).length() + OVERHEAD);
    b.put(("touch " + key + " " + exp + "\r\n").getBytes());
    b.flip();
    setBuffer(b);
  }

  @Override
  public String toString() {
    return "Cmd: touch key: " + key + " exp: " + exp;
  }
}
