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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.ops.VBucketAware;

/**
 * Optimized Set operation for folding a bunch of sets together.
 */
public class OptimizedSetImpl extends MultiKeyOperationImpl {

  private static final OperationCallback NOOP_CALLBACK = new NoopCallback();

  private final int terminalOpaque = generateOpaque();
  private final Map<Integer, OperationCallback> callbacks =
      new HashMap<Integer, OperationCallback>();
  private final List<CASOperation> ops = new ArrayList<CASOperation>();

  // If nothing else, this will be a NOOP.
  private int byteCount = MIN_RECV_PACKET;

  /**
   * Construct an optimized get starting with the given get operation.
   */
  public OptimizedSetImpl(CASOperation firstStore) {
    super(DUMMY_OPCODE, -1, NOOP_CALLBACK);
    addOperation(firstStore);
  }

  public void addOperation(CASOperation op) {
    ops.add(op);

    // Count the bytes required by this operation.
    Iterator<String> is = op.getKeys().iterator();
    String k = is.next();
    int keylen = KeyUtil.getKeyBytes(k).length;

    byteCount += MIN_RECV_PACKET + StoreOperationImpl.EXTRA_LEN + keylen
      + op.getData().length;
  }

  public int size() {
    return ops.size();
  }

  public int bytes() {
    return byteCount;
  }

  @Override
  public void initialize() {
    // Now create a buffer.
    ByteBuffer bb = ByteBuffer.allocate(byteCount);
    for (CASOperation so : ops) {
      Iterator<String> is = so.getKeys().iterator();
      String k = is.next();
      byte[] keyBytes = KeyUtil.getKeyBytes(k);
      assert !is.hasNext();

      int myOpaque = generateOpaque();
      callbacks.put(myOpaque, so.getCallback());
      byte[] data = so.getData();

      // Custom header
      bb.put(REQ_MAGIC);
      bb.put(cmdMap(so.getStoreType()));
      bb.putShort((short) keyBytes.length);
      bb.put((byte) StoreOperationImpl.EXTRA_LEN); // extralen
      bb.put((byte) 0); // data type
      bb.putShort(((VBucketAware) so).getVBucket(k)); // vbucket
      bb.putInt(keyBytes.length + data.length + StoreOperationImpl.EXTRA_LEN);
      bb.putInt(myOpaque);
      bb.putLong(so.getCasValue()); // cas
      // Extras
      bb.putInt(so.getFlags());
      bb.putInt(so.getExpiration());
      // the actual key
      bb.put(keyBytes);
      // And the value
      bb.put(data);
    }
    // Add the noop
    bb.put(REQ_MAGIC);
    bb.put((byte) NoopOperationImpl.CMD);
    bb.putShort((short) 0);
    bb.put((byte) 0); // extralen
    bb.put((byte) 0); // data type
    bb.putShort((short) 0); // reserved
    bb.putInt(0);
    bb.putInt(terminalOpaque);
    bb.putLong(0); // cas

    bb.flip();
    setBuffer(bb);
  }

  private static byte cmdMap(StoreType t) {
    byte rv;
    switch (t) {
    case set:
      rv = StoreOperationImpl.SETQ;
      break;
    case add:
      rv = StoreOperationImpl.ADDQ;
      break;
    case replace:
      rv = StoreOperationImpl.REPLACEQ;
      break;
    default:
      rv = DUMMY_OPCODE;
    }
    // Check fall-through.
    assert rv != DUMMY_OPCODE : "Unhandled store type:  " + t;
    return rv;
  }

  @Override
  protected void finishedPayload(byte[] pl) throws IOException {
    if (responseOpaque == terminalOpaque) {
      for (OperationCallback cb : callbacks.values()) {
        cb.receivedStatus(STATUS_OK);
        cb.complete();
      }
      transitionState(OperationState.COMPLETE);
    } else {
      OperationCallback cb = callbacks.remove(responseOpaque);
      assert cb != null : "No callback for " + responseOpaque;
      assert errorCode != 0 : "Got no error on a quiet mutation.";
      super.finishedPayload(pl);
    }
    resetInput();
  }

  @Override
  protected boolean opaqueIsValid() {
    return responseOpaque == terminalOpaque
        || callbacks.containsKey(responseOpaque);
  }

  static class NoopCallback implements OperationCallback {

    public void complete() {
      // noop
    }

    public void receivedStatus(OperationStatus status) {
      // noop
    }
  }
}
