/**
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

package net.spy.memcached.tapmessage;

/**
 * A representation of a tap stream message sent from a tap stream server.
 */
public class ResponseMessage extends BaseMessage {
  // Offsets are given from the end of the header
  private static final int ENGINE_PRIVATE_OFFSET = 0;
  private static final int ENGINE_PRIVATE_FIELD_LENGTH = 2;
  private static final int FLAGS_OFFSET = 2;
  private static final int FLAGS_FIELD_LENGTH = 2;
  private static final int TTL_OFFSET = 3;
  private static final int TTL_FIELD_LENGTH = 1;
  private static final int RESERVED1_OFFSET = 4;
  private static final int RESERVED1_FIELD_LENGTH = 1;
  private static final int RESERVED2_OFFSET = 5;
  private static final int RESERVED2_FIELD_LENGTH = 1;
  private static final int RESERVED3_OFFSET = 6;
  private static final int RESERVED3_FIELD_LENGTH = 1;
  private static final int ITEM_FLAGS_OFFSET = 7;
  private static final int ITEM_FLAGS_FIELD_LENGTH = 4;
  private static final int ITEM_EXPIRY_OFFSET = 11;
  private static final int ITEM_EXPIRY_FIELD_LENGTH = 5;

  /**
   * Creates a ResponseMessage from binary data.
   *
   * @param buffer The binary data sent from the tap stream server.
   */
  public ResponseMessage(byte[] buffer) {
    mbytes = buffer;
  }

  /**
   * Gets the value of the engine private field if the field exists in the
   * message.
   *
   * @return The engine private data.
   */
  public long getEnginePrivate() {
    if (ENGINE_PRIVATE_OFFSET + ENGINE_PRIVATE_FIELD_LENGTH
        > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + ENGINE_PRIVATE_OFFSET;
    return Util.fieldToValue(mbytes, offset, ENGINE_PRIVATE_FIELD_LENGTH);
  }

  /**
   * Gets the value of the flags field if the field exists in the message.
   *
   * @return The flags data.
   */
  public int getFlags() {
    if (FLAGS_OFFSET + FLAGS_FIELD_LENGTH > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + FLAGS_OFFSET;
    return (int) Util.fieldToValue(mbytes, offset, FLAGS_FIELD_LENGTH);
  }

  /**
   * Gets the value of the time to live field if the field exists in the
   * message.
   *
   * @return The time to live value;
   */
  public int getTTL() {
    if (TTL_OFFSET + TTL_FIELD_LENGTH > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + TTL_OFFSET;
    return (int) Util.fieldToValue(mbytes, offset, TTL_FIELD_LENGTH);
  }

  /**
   * Gets the value of the reserved1 field if the field exists in the message.
   *
   * @return The reserved1 data.
   */
  public int getReserved1() {
    if (RESERVED1_OFFSET + RESERVED1_FIELD_LENGTH > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + RESERVED1_OFFSET;
    return (int) Util.fieldToValue(mbytes, offset, RESERVED1_FIELD_LENGTH);
  }

  /**
   * Gets the value of the reserved2 field if the field exists in the message.
   *
   * @return The reserved2 data.
   */
  public int getReserved2() {
    if (RESERVED2_OFFSET + RESERVED2_FIELD_LENGTH > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + RESERVED2_OFFSET;
    return (int) Util.fieldToValue(mbytes, offset, RESERVED2_FIELD_LENGTH);
  }

  /**
   * Gets the value of the reserved3 field if the field exists in the message.
   *
   * @return The reserved3 data.
   */
  public int getReserved3() {
    if (RESERVED3_OFFSET + RESERVED3_FIELD_LENGTH > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + RESERVED3_OFFSET;
    return (int) Util.fieldToValue(mbytes, offset, RESERVED3_FIELD_LENGTH);
  }

  /**
   * Gets the value of the items flag field if the field exists in the message.
   *
   * @return The items flag data.
   */
  public int getItemFlags() {
    if (ITEM_FLAGS_OFFSET + ITEM_FLAGS_FIELD_LENGTH > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + ITEM_FLAGS_OFFSET;
    return (int) Util.fieldToValue(mbytes, offset, ITEM_FLAGS_FIELD_LENGTH);
  }

  /**
   * Gets the value of the item expiry field if the field exists in the message.
   *
   * @return The item expiry data.
   */
  public long getItemExpiry() {
    if (ITEM_EXPIRY_OFFSET + ITEM_EXPIRY_FIELD_LENGTH > getExtralength()) {
      return 0;
    }
    int offset = HEADER_LENGTH + ITEM_EXPIRY_OFFSET;
    return Util.fieldToValue(mbytes, offset, ITEM_EXPIRY_FIELD_LENGTH);
  }

  /**
   * Gets the value of the key field if the field exists in the message.
   *
   * @return The key data.
   */
  public String getKey() {
    if (getExtralength() >= getTotalbody()) {
      return new String();
    }
    int offset = (int) (HEADER_LENGTH + getExtralength());
    return new String(mbytes, offset, getKeylength());
  }

  /**
   * Gets the value of the value field if the field exists in the message.
   *
   * @return The value data.
   */
  public byte[] getValue() {
    if (getExtralength() + getKeylength() >= getTotalbody()) {
      return new byte[0];
    }
    int offset = (int) (HEADER_LENGTH + getExtralength() + getKeylength());
    int length = (int) (getTotalbody() - getKeylength() - getExtralength());
    byte[] value = new byte[length];
    System.arraycopy(mbytes, offset, value, 0, length);
    return value;
  }
}
