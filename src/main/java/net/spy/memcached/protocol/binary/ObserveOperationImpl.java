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

package net.spy.memcached.protocol.binary;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ObserveResponse;
import net.spy.memcached.ops.ObserveOperation;
import net.spy.memcached.ops.OperationCallback;

class ObserveOperationImpl extends SingleKeyOperationImpl implements
    ObserveOperation {

  private static final byte CMD = (byte) 0x92;

  private final long cas;
  private final int index;
  private byte keystate = (byte)0xff;
  private long retCas = 0;

  public ObserveOperationImpl(String k, long c, int i,
          OperationCallback cb) {
    super(CMD, generateOpaque(), k, cb);
    cas = c;
    index = i;
  }

  @Override
  public void initialize() {
    byte[] keyBytes = KeyUtil.getKeyBytes(key);
    prepareBuffer("", 0x0, EMPTY_BYTES, (short) index,
      (short) keyBytes.length, keyBytes);
  }

  @Override
  public String toString() {
    return super.toString() + " Cas: " + cas;
  }

  @Override
  protected void decodePayload(byte[] pl) {
    final short  keylen = (short) decodeShort(pl, 2);
    keystate = (byte) decodeByte(pl, keylen+4);
    retCas = (long) decodeLong(pl, keylen+5);
    ObserveResponse r = ObserveResponse.valueOf(keystate);
    ((ObserveOperation.Callback) getCallback()).gotData(key, retCas,
        getHandlingNode(), r);
    getCallback().receivedStatus(STATUS_OK);
  }
}
