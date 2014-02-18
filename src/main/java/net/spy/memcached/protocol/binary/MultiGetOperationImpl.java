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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;

import static net.spy.memcached.protocol.binary.GetOperationImpl.EXTRA_HDR_LEN;

public class MultiGetOperationImpl extends MultiKeyOperationImpl implements
    GetOperation {

  private static final byte CMD_GETQ = 0x09;

  private final Map<Integer, String> keys = new HashMap<Integer, String>();
  private final Map<Integer, byte[]> bkeys = new HashMap<Integer, byte[]>();
  private final Map<String, Integer> rkeys = new HashMap<String, Integer>();

  private final int terminalOpaque = generateOpaque();
  private final List<String> retryKeys = new ArrayList<String>();

  public MultiGetOperationImpl(Collection<String> k, OperationCallback cb) {
    super(DUMMY_OPCODE, -1, cb);
    for (String s : new HashSet<String>(k)) {
      addKey(s);
    }
  }

  /**
   * Add a key (and return its new opaque value).
   */
  protected int addKey(String k) {
    Integer rv = rkeys.get(k);
    if (rv == null) {
      rv = generateOpaque();
      keys.put(rv, k);
      bkeys.put(rv, KeyUtil.getKeyBytes(k));
      rkeys.put(k, rv);
      synchronized (vbmap) {
        vbmap.put(k, new Short((short) 0));
      }
    }
    return rv;
  }

  @Override
  public void initialize() {
    int size = (1 + keys.size()) * MIN_RECV_PACKET;
    for (byte[] b : bkeys.values()) {
      size += b.length;
    }
    // set up the initial header stuff
    ByteBuffer bb = ByteBuffer.allocate(size);
    for (Map.Entry<Integer, byte[]> me : bkeys.entrySet()) {
      final byte[] keyBytes = me.getValue();
      final String key = keys.get(me.getKey());

      // Custom header
      bb.put(REQ_MAGIC);
      bb.put(CMD_GETQ);
      bb.putShort((short) keyBytes.length);
      bb.put((byte) 0); // extralen
      bb.put((byte) 0); // data type
      bb.putShort(vbmap.get(key).shortValue()); // vbucket
      bb.putInt(keyBytes.length);
      bb.putInt(me.getKey());
      bb.putLong(0); // cas
      // the actual key
      bb.put(keyBytes);
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

  @Override
  protected void finishedPayload(byte[] pl) throws IOException {
    getStatusForErrorCode(errorCode, pl);

    if (responseOpaque == terminalOpaque) {
      if (retryKeys.size() > 0) {
        transitionState(OperationState.RETRY);
        OperationStatus retryStatus = new OperationStatus(true,
          Integer.toString(retryKeys.size()), StatusCode.ERR_NOT_MY_VBUCKET);
        getCallback().receivedStatus(retryStatus);
        getCallback().complete();
      } else {
        getCallback().receivedStatus(STATUS_OK);
        transitionState(OperationState.COMPLETE);
      }
    } else if (errorCode == ERR_NOT_MY_VBUCKET) {
      retryKeys.add(keys.get(responseOpaque));
    } else if (errorCode != SUCCESS) {
      getLogger().warn("Error on key %s:  %s (%d)", keys.get(responseOpaque),
          new String(pl), errorCode);
    } else {
      final int flags = decodeInt(pl, 0);
      final byte[] data = new byte[pl.length - EXTRA_HDR_LEN];
      System.arraycopy(pl, EXTRA_HDR_LEN, data, 0, pl.length - EXTRA_HDR_LEN);
      Callback cb = (Callback) getCallback();
      cb.gotData(keys.get(responseOpaque), flags, data);
    }
    resetInput();
  }

  @Override
  protected boolean opaqueIsValid() {
    return responseOpaque == terminalOpaque || keys.containsKey(responseOpaque);
  }

  /**
   * Returns the keys to redistribute.
   *
   * @return the keys to retry.
   */
  public List<String> getRetryKeys() {
    return retryKeys;
  }

}
