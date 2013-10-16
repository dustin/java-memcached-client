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

import net.spy.memcached.ops.GetAndTouchOperation;

/**
 * Implementation of the get and touch operation.
 */
public class GetAndTouchOperationImpl extends SingleKeyOperationImpl implements
    GetAndTouchOperation {

  static final byte GAT_CMD = 0x1d;

  /**
   * Length of the extra header stuff for a GET response.
   */
  static final int EXTRA_HDR_LEN = 4;

  private final int exp;

  public GetAndTouchOperationImpl(String k, int e,
      GetAndTouchOperation.Callback cb) {
    super(GAT_CMD, generateOpaque(), k, cb);
    exp = e;
  }

  @Override
  public void initialize() {
    prepareBuffer(key, 0, EMPTY_BYTES, exp);
  }

  @Override
  protected void decodePayload(byte[] pl) {
    final int flags = decodeInt(pl, 0);
    final byte[] data = new byte[pl.length - EXTRA_HDR_LEN];
    System.arraycopy(pl, EXTRA_HDR_LEN, data, 0, pl.length - EXTRA_HDR_LEN);
    GetAndTouchOperation.Callback gcb =
        (GetAndTouchOperation.Callback) getCallback();
    gcb.gotData(key, flags, responseCas, data);
    getCallback().receivedStatus(STATUS_OK);
  }

  @Override
  public String toString() {
    return super.toString() + " Exp: " + exp;
  }

  @Override
  public int getExpiration() {
    return exp;
  }

}
