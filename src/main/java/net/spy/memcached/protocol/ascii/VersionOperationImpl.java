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
import java.nio.charset.Charset;

import net.spy.memcached.ops.NoopOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.ops.VersionOperation;

/**
 * Operation to request the version of a memcached server.
 */
final class VersionOperationImpl extends OperationImpl implements
    VersionOperation, NoopOperation {

  private static final byte[] REQUEST = "version\r\n".getBytes(Charset.forName("UTF-8"));

  public VersionOperationImpl(OperationCallback c) {
    super(c);
  }

  @Override
  public void handleLine(String line) {
    assert line.startsWith("VERSION ");
    getCallback().receivedStatus(new OperationStatus(true,
      line.substring("VERSION ".length()), StatusCode.SUCCESS));
    transitionState(OperationState.COMPLETE);
  }

  @Override
  public void initialize() {
    setBuffer(ByteBuffer.wrap(REQUEST));
  }

  @Override
  public String toString() {
    return "Cmd: version";
  }
}
