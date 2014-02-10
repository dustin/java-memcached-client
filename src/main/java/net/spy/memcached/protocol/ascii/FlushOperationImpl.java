/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;

/**
 * Memcached flush_all operation.
 */
final class FlushOperationImpl extends OperationImpl implements FlushOperation {

  private static final byte[] FLUSH = "flush_all\r\n".getBytes();

  private static final OperationStatus OK = new OperationStatus(true, "OK",
    StatusCode.SUCCESS);

  private final int delay;

  public FlushOperationImpl(int d, OperationCallback cb) {
    super(cb);
    delay = d;
  }

  @Override
  public void handleLine(String line) {
    getLogger().debug("Flush completed successfully");
    getCallback().receivedStatus(matchStatus(line, OK));
    transitionState(OperationState.COMPLETE);
  }

  @Override
  public void initialize() {
    ByteBuffer b = null;
    if (delay == -1) {
      b = ByteBuffer.wrap(FLUSH);
    } else {
      b = ByteBuffer.allocate(32);
      b.put(("flush_all " + delay + "\r\n").getBytes());
      b.flip();
    }
    setBuffer(b);
  }

  @Override
  public String toString() {
    return "Cmd: flush_all Delay: " + delay;
  }
}
