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

package net.spy.memcached.protocol.binary;

import java.io.IOException;

import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatsOperation;

/**
 * A StatsOperationImpl.
 */
public class StatsOperationImpl extends OperationImpl
  implements StatsOperation {

  private static final byte CMD = 0x10;
  private final String key;

  public StatsOperationImpl(String arg, StatsOperation.Callback c) {
    super(CMD, generateOpaque(), c);
    key = (arg == null) ? "" : arg;
  }

  @Override
  public void initialize() {
    prepareBuffer(key, 0, EMPTY_BYTES);
  }

  @Override
  protected void finishedPayload(byte[] pl) throws IOException {
    if (keyLen > 0) {
      final byte[] keyBytes = new byte[keyLen];
      final byte[] data = new byte[pl.length - keyLen];
      System.arraycopy(pl, 0, keyBytes, 0, keyLen);
      System.arraycopy(pl, keyLen, data, 0, pl.length - keyLen);
      Callback cb = (Callback) getCallback();
      cb.gotStat(new String(keyBytes, "UTF-8"), new String(data, "UTF-8"));
    } else {
      OperationStatus status = getStatusForErrorCode(errorCode, pl);
      getCallback().receivedStatus(status);
      transitionState(OperationState.COMPLETE);
    }
    resetInput();
  }
}
