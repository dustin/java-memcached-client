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

import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.MutatorOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;

class MutatorOperationImpl extends SingleKeyOperationImpl implements
    MutatorOperation {

  private static final byte CMD_INCR = 0x05;
  private static final byte CMD_DECR = 0x06;

  private final Mutator mutator;
  private final long by;
  private final int exp;
  private final long def;

  public MutatorOperationImpl(Mutator m, String k, long b, long d, int e,
      OperationCallback cb) {
    super(m == Mutator.incr ? CMD_INCR : CMD_DECR, generateOpaque(), k, cb);
    assert d >= 0 : "Default value is below zero";
    mutator = m;
    by = b;
    exp = e;
    def = d;
  }

  @Override
  public void initialize() {
    // We're passing around a long so we can cover an unsigned integer.
    byte[] defBytes = new byte[8];
    defBytes[0] = (byte) ((def >> 56) & 0xff);
    defBytes[1] = (byte) ((def >> 48) & 0xff);
    defBytes[2] = (byte) ((def >> 40) & 0xff);
    defBytes[3] = (byte) ((def >> 32) & 0xff);
    defBytes[4] = (byte) ((def >> 24) & 0xff);
    defBytes[5] = (byte) ((def >> 16) & 0xff);
    defBytes[6] = (byte) ((def >> 8) & 0xff);
    defBytes[7] = (byte) (def & 0xff);
    prepareBuffer(key, 0, EMPTY_BYTES, by, defBytes, exp);
  }

  @Override
  protected void decodePayload(byte[] pl) {
    getCallback().receivedStatus(new OperationStatus(true,
      String.valueOf(decodeLong(pl, 0)), StatusCode.SUCCESS));
  }

  public long getBy() {
    return (int) by;
  }

  public long getDefault() {
    return def;
  }

  public int getExpiration() {
    return exp;
  }

  public Mutator getType() {
    return mutator;
  }

  @Override
  public String toString() {
    return super.toString() + " Amount: " + by + " Default: " + def + " Exp: "
      + exp;
  }
}
