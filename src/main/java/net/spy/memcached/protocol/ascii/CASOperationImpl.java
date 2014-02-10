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
import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.CASResponse;
import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.CASOperationStatus;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.ops.StoreType;

class CASOperationImpl extends OperationImpl implements CASOperation {

  // Overhead storage stuff to make sure the buffer pushes out far enough.
  // This is "cas" + length(flags) + length(length(data)) + length(cas id)
  // + spaces
  private static final int OVERHEAD = 64;

  private static final OperationStatus STORED = new CASOperationStatus(true,
      "STORED", CASResponse.OK, StatusCode.SUCCESS);
  private static final OperationStatus NOT_FOUND = new CASOperationStatus(
      false, "NOT_FOUND", CASResponse.NOT_FOUND, StatusCode.ERR_NOT_FOUND);
  private static final OperationStatus EXISTS = new CASOperationStatus(false,
      "EXISTS", CASResponse.EXISTS, StatusCode.ERR_EXISTS);

  private final String key;
  private final long casValue;
  private final int flags;
  private final int exp;
  private final byte[] data;

  public CASOperationImpl(String k, long c, int f, int e, byte[] d,
      OperationCallback cb) {
    super(cb);
    key = k;
    casValue = c;
    flags = f;
    exp = e;
    data = d;
  }

  @Override
  public void handleLine(String line) {
    assert getState() == OperationState.READING : "Read ``" + line
        + "'' when in " + getState() + " state";
    getCallback().receivedStatus(matchStatus(line, STORED, NOT_FOUND, EXISTS));
    transitionState(OperationState.COMPLETE);
  }

  @Override
  public void initialize() {
    ByteBuffer bb = ByteBuffer.allocate(data.length
        + KeyUtil.getKeyBytes(key).length + OVERHEAD);
    setArguments(bb, "cas", key, flags, exp, data.length, casValue);
    assert bb.remaining() >= data.length + 2 : "Not enough room in buffer,"
        + " need another " + (2 + data.length - bb.remaining());
    bb.put(data);
    bb.put(CRLF);
    bb.flip();
    setBuffer(bb);
  }

  public Collection<String> getKeys() {
    return Collections.singleton(key);
  }

  public byte[] getData() {
    return data;
  }

  public long getCasValue() {
    return casValue;
  }

  public int getExpiration() {
    return exp;
  }

  public int getFlags() {
    return flags;
  }

  public StoreType getStoreType() {
    return StoreType.set;
  }

  @Override
  public String toString() {
    return "Cmd: cas Key: " + key + " Cas Value: " + casValue + " Flags: "
      + flags + " Exp: " + exp + " Data Length: " + data.length;
  }
}
