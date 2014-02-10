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

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.GetAndTouchOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetlOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.util.StringUtils;

/**
 * Base class for get and gets handlers.
 */
abstract class BaseGetOpImpl extends OperationImpl {

  private static final OperationStatus END = new OperationStatus(true, "END",
    StatusCode.SUCCESS);
  private static final OperationStatus NOT_FOUND = new OperationStatus(false,
      "NOT_FOUND", StatusCode.ERR_NOT_FOUND);
  private static final OperationStatus LOCK_ERROR = new OperationStatus(false,
      "LOCK_ERROR", StatusCode.ERR_TEMP_FAIL);
  private static final byte[] RN_BYTES = "\r\n".getBytes();
  private final String cmd;
  private final Collection<String> keys;
  private String currentKey = null;
  protected final int exp;
  private final boolean hasExp;
  private long casValue = 0;
  private int currentFlags = 0;
  private byte[] data = null;
  private int readOffset = 0;
  private byte lookingFor = '\0';
  private boolean hasValue;

  public BaseGetOpImpl(String c, OperationCallback cb, Collection<String> k) {
    super(cb);
    cmd = c;
    keys = k;
    exp = 0;
    hasExp = false;
    hasValue = false;
  }

  public BaseGetOpImpl(String c, int e, OperationCallback cb, String k) {
    super(cb);
    cmd = c;
    keys = Collections.singleton(k);
    exp = e;
    hasExp = true;
    hasValue = false;
  }

  /**
   * Get the keys this GetOperation is looking for.
   */
  public final Collection<String> getKeys() {
    return keys;
  }

  @Override
  public final void handleLine(String line) {
    if (line.equals("END")) {
      getLogger().debug("Get complete!");
      if (hasValue) {
        getCallback().receivedStatus(END);
      } else {
        getCallback().receivedStatus(NOT_FOUND);
      }
      transitionState(OperationState.COMPLETE);
      data = null;
    } else if (line.startsWith("VALUE ")) {
      getLogger().debug("Got line %s", line);
      String[] stuff = line.split(" ");
      assert stuff[0].equals("VALUE");
      currentKey = stuff[1];
      currentFlags = Integer.parseInt(stuff[2]);
      data = new byte[Integer.parseInt(stuff[3])];
      if (stuff.length > 4) {
        casValue = Long.parseLong(stuff[4]);
      }
      readOffset = 0;
      hasValue = true;
      getLogger().debug("Set read type to data");
      setReadType(OperationReadType.DATA);
    } else if (line.equals("LOCK_ERROR")) {
      getCallback().receivedStatus(LOCK_ERROR);
      transitionState(OperationState.COMPLETE);
    } else {
      assert false : "Unknown line type: " + line;
    }
  }

  @Override
  public final void handleRead(ByteBuffer b) {
    assert currentKey != null;
    assert data != null;
    // This will be the case, because we'll clear them when it's not.
    assert readOffset <= data.length : "readOffset is " + readOffset
        + " data.length is " + data.length;

    getLogger().debug("readOffset: %d, length: %d", readOffset, data.length);
    // If we're not looking for termination, we're still looking for data
    if (lookingFor == '\0') {
      int toRead = data.length - readOffset;
      int available = b.remaining();
      toRead = Math.min(toRead, available);
      getLogger().debug("Reading %d bytes", toRead);
      b.get(data, readOffset, toRead);
      readOffset += toRead;
    }
    // Transition us into a ``looking for \r\n'' kind of state if we've
    // read enough and are still in a data state.
    if (readOffset == data.length && lookingFor == '\0') {
      // The callback is most likely a get callback. If it's not, then
      // it's a gets callback.
      OperationCallback cb = getCallback();
      if (cb instanceof GetOperation.Callback) {
        GetOperation.Callback gcb = (GetOperation.Callback) cb;
        gcb.gotData(currentKey, currentFlags, data);
      } else if (cb instanceof GetsOperation.Callback) {
        GetsOperation.Callback gcb = (GetsOperation.Callback) cb;
        gcb.gotData(currentKey, currentFlags, casValue, data);
      } else if (cb instanceof GetlOperation.Callback) {
        GetlOperation.Callback gcb = (GetlOperation.Callback) cb;
        gcb.gotData(currentKey, currentFlags, casValue, data);
      } else if (cb instanceof GetAndTouchOperation.Callback) {
        GetAndTouchOperation.Callback gcb = (GetAndTouchOperation.Callback) cb;
        gcb.gotData(currentKey, currentFlags, casValue, data);
      } else {
        throw new ClassCastException("Couldn't convert " + cb
            + "to a relevent op");
      }
      lookingFor = '\r';
    }
    // If we're looking for an ending byte, let's go find it.
    if (lookingFor != '\0' && b.hasRemaining()) {
      do {
        byte tmp = b.get();
        assert tmp == lookingFor : "Expecting " + lookingFor + ", got "
            + (char) tmp;
        switch (lookingFor) {
        case '\r':
          lookingFor = '\n';
          break;
        case '\n':
          lookingFor = '\0';
          break;
        default:
          assert false : "Looking for unexpected char: " + (char) lookingFor;
        }
      } while (lookingFor != '\0' && b.hasRemaining());
      // Completed the read, reset stuff.
      if (lookingFor == '\0') {
        currentKey = null;
        data = null;
        readOffset = 0;
        currentFlags = 0;
        getLogger().debug("Setting read type back to line.");
        setReadType(OperationReadType.LINE);
      }
    }
  }

  @Override
  public final void initialize() {
    // Figure out the length of the request
    int size = 6; // Enough for gets\r\n
    Collection<byte[]> keyBytes = KeyUtil.getKeyBytes(keys);
    for (byte[] k : keyBytes) {
      size += k.length;
      size++;
    }
    byte[] e = String.valueOf(exp).getBytes();
    if (hasExp) {
      size += e.length + 1;
    }
    ByteBuffer b = ByteBuffer.allocate(size);
    b.put(cmd.getBytes());
    for (byte[] k : keyBytes) {
      b.put((byte) ' ');
      b.put(k);
    }
    if (hasExp) {
      b.put((byte) ' ');
      b.put(e);
    }
    b.put(RN_BYTES);
    b.flip();
    setBuffer(b);
  }

  @Override
  protected final void wasCancelled() {
    getCallback().receivedStatus(CANCELLED);
  }

  @Override
  public String toString() {
    return "Cmd: " + cmd + " Keys: " + StringUtils.join(keys, " ") + "Exp: "
      + exp;
  }
}
