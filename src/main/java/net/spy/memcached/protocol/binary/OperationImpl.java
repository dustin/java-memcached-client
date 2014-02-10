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
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

import net.spy.memcached.CASResponse;
import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.CASOperationStatus;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.protocol.BaseOperationImpl;

/**
 * Base class for binary operations.
 */
public  abstract class OperationImpl extends BaseOperationImpl
  implements Operation {

  protected static final byte REQ_MAGIC = (byte) 0x80;
  protected static final byte RES_MAGIC = (byte) 0x81;
  protected static final byte DUMMY_OPCODE = (byte)0xff;
  protected static final int MIN_RECV_PACKET = 24;

  /**
   * Error code for operations.
   */
  protected static final int SUCCESS = 0x00;
  protected static final int ERR_NOT_FOUND = 0x01;
  protected static final int ERR_EXISTS = 0x02;
  protected static final int ERR_2BIG = 0x03;
  protected static final int ERR_INVAL = 0x04;
  protected static final int ERR_NOT_STORED = 0x05;
  protected static final int ERR_DELTA_BADVAL = 0x06;
  protected static final int ERR_NOT_MY_VBUCKET = 0x07;
  protected static final int ERR_UNKNOWN_COMMAND = 0x81;
  protected static final int ERR_NO_MEM = 0x82;
  protected static final int ERR_NOT_SUPPORTED = 0x83;
  protected static final int ERR_INTERNAL = 0x84;
  protected static final int ERR_BUSY = 0x85;
  protected static final int ERR_TEMP_FAIL = 0x86;

  protected static final byte[] EMPTY_BYTES = new byte[0];

  protected static final OperationStatus STATUS_OK = new CASOperationStatus(
      true, "OK", CASResponse.OK, StatusCode.SUCCESS);

  private static final AtomicInteger SEQ_NUMBER = new AtomicInteger(0);

  // request header fields
  private final byte cmd;
  protected short vbucket = 0;
  protected final int opaque;

  private final byte[] header = new byte[MIN_RECV_PACKET];
  private int headerOffset = 0;
  private byte[] payload = null;
  private byte[] errorMsg = null;

  // Response header fields
  protected int keyLen;
  protected byte responseCmd;
  protected int errorCode;
  protected int responseOpaque;
  protected long responseCas;

  private int payloadOffset = 0;

  /**
   * Construct with opaque.
   *
   * @param o the opaque value.
   * @param cb
   */
  protected OperationImpl(byte c, int o, OperationCallback cb) {
    super();
    cmd = c;
    opaque = o;
    setCallback(cb);
  }

  protected void resetInput() {
    payload = null;
    payloadOffset = 0;
    headerOffset = 0;
  }

  /**
   * Read from the incoming {@link ByteBuffer}.
   *
   * Reading from the buffer is done in stages, depending on how much data
   * can be read at once. First, the header is read and then parsed (24
   * bytes, indicated by {@link #MIN_RECV_PACKET}). Then, the payload is read
   * (if one is available for this operation and can be loaded fully).
   *
   * @param buffer the buffer to read from.
   * @throws IOException if an error happened during parsing/reading.
   */
  @Override
  public void readFromBuffer(final ByteBuffer buffer) throws IOException {
    if (headerOffset < MIN_RECV_PACKET) {
      readHeaderFromBuffer(buffer);
      if (headerOffset == MIN_RECV_PACKET) {
        parseHeaderFromBuffer();
      }
    }

    if (headerOffset >= MIN_RECV_PACKET && payload == null) {
      finishedPayload(EMPTY_BYTES);
    } else if (payload != null) {
      readPayloadFromBuffer(buffer);
    } else {
      getLogger().debug("Only read %d of the %d needed to fill a header",
        headerOffset, MIN_RECV_PACKET);
    }

  }

  /**
   * Read the header bytes from the incoming {@link ByteBuffer}.
   *
   * @param buffer the buffer to read from.
   */
  private void readHeaderFromBuffer(final ByteBuffer buffer) {
    int toRead = MIN_RECV_PACKET - headerOffset;
    int available = buffer.remaining();
    toRead = Math.min(toRead, available);
    getLogger().debug("Reading %d header bytes", toRead);
    buffer.get(header, headerOffset, toRead);
    headerOffset += toRead;
  }

  /**
   * Parse the header info out of the buffer.
   */
  private void parseHeaderFromBuffer() {
    int magic = header[0];
    assert magic == RES_MAGIC : "Invalid magic:  " + magic;
    responseCmd = header[1];
    assert cmd == DUMMY_OPCODE || responseCmd == cmd
      : "Unexpected response command value";
    keyLen = decodeShort(header, 2);
    errorCode = decodeShort(header, 6);
    int bytesToRead = decodeInt(header, 8);
    payload = new byte[bytesToRead];
    responseOpaque = decodeInt(header, 12);
    responseCas = decodeLong(header, 16);
    assert opaqueIsValid() : "Opaque is not valid";
  }

  /**
   * Read the payload from the buffer.
   *
   * @param buffer the buffer to read from.
   * @throws IOException if an error occures during payload finishing.
   */
  private void readPayloadFromBuffer(final ByteBuffer buffer)
    throws IOException {
    int toRead = payload.length - payloadOffset;
    int available = buffer.remaining();
    toRead = Math.min(toRead, available);
    getLogger().debug("Reading %d payload bytes", toRead);
    buffer.get(payload, payloadOffset, toRead);
    payloadOffset += toRead;


    if (payloadOffset == payload.length) {
      finishedPayload(payload);
    }
  }

  protected void finishedPayload(byte[] pl) throws IOException {
    OperationStatus status = getStatusForErrorCode(errorCode, pl);

    if (status == null) {
      handleError(OperationErrorType.SERVER, new String(pl));
    } else if (errorCode == SUCCESS) {
      decodePayload(pl);
      transitionState(OperationState.COMPLETE);
    } else if (errorCode == ERR_NOT_MY_VBUCKET
        && !getState().equals(OperationState.COMPLETE)) {
      transitionState(OperationState.RETRY);
    } else {
      getCallback().receivedStatus(status);
      transitionState(OperationState.COMPLETE);
    }
  }

  /**
   * Get the OperationStatus object for the given error code.
   *
   * @param errCode the error code
   * @return the status to return, or null if this is an exceptional case
   */
  protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl)
    throws IOException {
    errorMsg = new byte[errPl.length];
    errorMsg = errPl.clone();

    StatusCode statusCode = StatusCode.fromBinaryCode(errCode);

    switch (errCode) {
    case SUCCESS:
      return STATUS_OK;
    case ERR_NOT_FOUND:
      return new CASOperationStatus(false, new String(errPl),
          CASResponse.NOT_FOUND, statusCode);
    case ERR_EXISTS:
      return new CASOperationStatus(false, new String(errPl),
          CASResponse.EXISTS, statusCode);
    case ERR_NOT_STORED:
      return new CASOperationStatus(false, new String(errPl),
          CASResponse.NOT_FOUND, statusCode);
    case ERR_2BIG:
    case ERR_INTERNAL:
      handleError(OperationErrorType.SERVER, new String(errPl));
    case ERR_INVAL:
    case ERR_DELTA_BADVAL:
    case ERR_NOT_MY_VBUCKET:
    case ERR_UNKNOWN_COMMAND:
    case ERR_NO_MEM:
    case ERR_NOT_SUPPORTED:
    case ERR_BUSY:
    case ERR_TEMP_FAIL:
      return new OperationStatus(false, new String(errPl), statusCode);
    default:
      return null;
    }
  }

  /**
   * Decode the given payload for this command.
   *
   * @param pl the payload.
   */
  protected void decodePayload(byte[] pl) {
    assert pl.length == 0 : "Payload has bytes, but decode isn't overridden";
    getCallback().receivedStatus(STATUS_OK);
  }

  /**
   * Validate an opaque value from the header. This may be overridden from a
   * subclass where the opaque isn't expected to always be the same as the
   * request opaque.
   */
  protected boolean opaqueIsValid() {
    if (responseOpaque != opaque) {
      getLogger().warn("Expected opaque:  %d, got opaque:  %d\n",
          responseOpaque, opaque);
    }
    return responseOpaque == opaque;
  }

  static int decodeShort(byte[] data, int i) {
    return (data[i] & 0xff) << 8 | (data[i + 1] & 0xff);
  }

  static int decodeByte(byte[] data, int i) {
    return (data[i] & 0xff);
  }
  static int decodeInt(byte[] data, int i) {
    return (data[i] & 0xff) << 24
      | (data[i + 1] & 0xff) << 16
      | (data[i + 2] & 0xff) << 8
      | (data[i + 3] & 0xff);
  }

  static long decodeUnsignedInt(byte[] data, int i) {
    return ((long) (data[i] & 0xff) << 24)
      | ((data[i + 1] & 0xff) << 16)
      | ((data[i + 2] & 0xff) << 8)
      | (data[i + 3] & 0xff);
  }

  static long decodeLong(byte[] data, int i) {
    return (data[i] & 0xffL) << 56
      | (data[i + 1] & 0xffL) << 48
      | (data[i + 2] & 0xffL) << 40
      | (data[i + 3] & 0xffL) << 32
      | (data[i + 4] & 0xffL) << 24
      | (data[i + 5] & 0xffL) << 16
      | (data[i + 6] & 0xffL) << 8
      | (data[i + 7] & 0xffL);
  }

  /**
   * Prepare the buffer for sending.
   *
   * @param key the key (for keyed ops).
   * @param cas the cas value.
   * @param val the data payload.
   * @param extraHeaders any additional headers that need to be sent.
   */
  protected void prepareBuffer(final String key, final long cas,
    final byte[] val, final Object... extraHeaders) {
    int extraLen = 0;
    int extraHeadersLength = extraHeaders.length;

    if (extraHeadersLength > 0) {
      extraLen = calculateExtraLength(extraHeaders);
    }

    final byte[] keyBytes = KeyUtil.getKeyBytes(key);
    int bufSize = MIN_RECV_PACKET + keyBytes.length + val.length;

    ByteBuffer bb = ByteBuffer.allocate(bufSize + extraLen);
    assert bb.order() == ByteOrder.BIG_ENDIAN;
    bb.put(REQ_MAGIC);
    bb.put(cmd);
    bb.putShort((short) keyBytes.length);
    bb.put((byte) extraLen);
    bb.put((byte) 0);
    bb.putShort(vbucket);
    bb.putInt(keyBytes.length + val.length + extraLen);
    bb.putInt(opaque);
    bb.putLong(cas);

    if (extraHeadersLength > 0) {
      addExtraHeaders(bb, extraHeaders);
    }

    bb.put(keyBytes);
    bb.put(val);

    bb.flip();
    setBuffer(bb);
  }

  /**
   * Add the extra headers to the write {@link ByteBuffer}.
   *
   * @param bb the buffer where to append.
   * @param extraHeaders the headers to append.
   */
  private void addExtraHeaders(final ByteBuffer bb,
    final Object... extraHeaders) {
    for (Object o : extraHeaders) {
      if (o instanceof Integer) {
        bb.putInt((Integer) o);
      } else if (o instanceof byte[]) {
        bb.put((byte[]) o);
      } else if (o instanceof Long) {
        bb.putLong((Long) o);
      } else if (o instanceof Short) {
        bb.putShort((Short) o);
      } else {
        assert false : "Unhandled extra header type:  " + o.getClass();
      }
    }
  }

  /**
   * Calculate the length of all extra headers.
   *
   * @param extraHeaders the list of extra headers to count.
   * @return the length of the extra headers.
   */
  private int calculateExtraLength(final Object... extraHeaders) {
    int extraLen = 0;
    for (Object o : extraHeaders) {
      if (o instanceof Integer) {
        extraLen += 4;
      } else if (o instanceof byte[]) {
        extraLen += ((byte[]) o).length;
      } else if (o instanceof Long) {
        extraLen += 8;
      } else  if (o instanceof Short) {
        extraLen += 2;
      } else {
        assert false : "Unhandled extra header type:  " + o.getClass();
      }
    }
    return extraLen;
  }

  /**
   * Generate an opaque ID.
   */
  static int generateOpaque() {
    int rv = SEQ_NUMBER.incrementAndGet();
    while (rv < 0) {
      SEQ_NUMBER.compareAndSet(rv, 0);
      rv = SEQ_NUMBER.incrementAndGet();
    }
    return rv;
  }

  @Override
  public String toString() {
    return "Cmd: " + cmd + " Opaque: " + opaque;
  }

  @Override
  public byte[] getErrorMsg() {
    return errorMsg;
  }

}
