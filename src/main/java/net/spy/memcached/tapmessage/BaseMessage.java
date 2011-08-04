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

import java.nio.ByteBuffer;

import net.spy.memcached.compat.SpyObject;

/**
 * The HeaderMessage implements the header of a tap message. This class cannot
 * be instantiated. Tap stream messages are created with the RequestMessage and
 * ResponseMessage classes. Users who want to take advantage of customizing
 * their own tap messages should use the CustomTapStream class since it provides
 * flexibility to create all valid tap messages.
 */
public class BaseMessage extends SpyObject {
  /**
   * The index of the magic field in a tap header.
   */
  public static final int MAGIC_INDEX = 0;

  /**
   * The length of the magic field in a tap header.
   */
  public static final int MAGIC_FIELD_LENGTH = 1;

  /**
   * The index of the opcode field in a tap header.
   */
  public static final int OPCODE_INDEX = 1;

  /**
   * The length of the opcode field in a tap header.
   */
  public static final int OPCODE_FIELD_LENGTH = 1;

  /**
   * The index of the key length field in a tap header.
   */
  public static final int KEY_LENGTH_INDEX = 2;

  /**
   * The length of the key length field in a tap header.
   */
  public static final int KEY_LENGTH_FIELD_LENGTH = 2;

  /**
   * The index of the extra length field in the tap header.
   */
  public static final int EXTRA_LENGTH_INDEX = 4;

  /**
   * The length of the extra length field in a tap header.
   */
  public static final int EXTRA_LENGTH_FIELD_LENGTH = 1;

  /**
   * The index of the data type field in the tap header.
   */
  public static final int DATA_TYPE_INDEX = 5;

  /**
   * The length of the data type field in a tap header.
   */
  public static final int DATA_TYPE_FIELD_LENGTH = 1;

  /**
   * The index of the vbucket field in the tap header.
   */
  public static final int VBUCKET_INDEX = 6;

  /**
   * The length of the vbucket field in a tap header.
   */
  public static final int VBUCKET_FIELD_LENGTH = 2;

  /**
   * The index of the total body field in the tap header.
   */
  public static final int TOTAL_BODY_INDEX = 8;

  /**
   * The length of the total body field in the tap header.
   */
  public static final int TOTAL_BODY_FIELD_LENGTH = 4;

  /**
   * The index of the opaque field in the tap header.
   */
  public static final int OPAQUE_INDEX = 12;

  /**
   * The length of the opaque field in the tap header.
   */
  public static final int OPAQUE_FIELD_LENGTH = 4;

  /**
   * The index of the cas field in the tap header.
   */
  public static final int CAS_INDEX = 16;

  /**
   * The length of the cas field in the tap header.
   */
  public static final int CAS_FIELD_LENGTH = 8;

  /**
   * The header length.
   */
  public static final int HEADER_LENGTH = 24;

  /**
   * Holds the binary data for the header field.
   */
  protected byte[] mbytes;

  /**
   * Instantiates a tap header.
   */
  protected BaseMessage() {
    mbytes = new byte[HEADER_LENGTH];
  }

  /**
   * Sets the value of the tap messages magic field.
   *
   * @param m The new value for the magic field.
   */
  public final void setMagic(TapMagic m) {
    mbytes[MAGIC_INDEX] = m.getMagic();
  }

  /**
   * Gets the value of the tap messages magic field.
   *
   * @return The value of the magic field.
   */
  public final int getMagic() {
    return mbytes[MAGIC_INDEX];
  }

  /**
   * Sets the value of the tap messages opcode field.
   *
   * @param o The new value of the opcode field.
   */
  public final void setOpcode(TapOpcode o) {
    mbytes[OPCODE_INDEX] = o.getOpcode();
  }

  /**
   * Gets the value of the tap messages opaque field.
   *
   * @return The value of the opaque field.
   */
  public final TapOpcode getOpcode() {
    return TapOpcode.getOpcodeByByte(mbytes[OPCODE_INDEX]);
  }

  /**
   * Sets the key length for this message. This function should never be called
   * by the user since changes to fields that affect key length call this
   * function automatically.
   *
   * @param l The new value for the key length field.
   */
  protected final void setKeylength(long l) {
    Util.valueToFieldOffest(mbytes, KEY_LENGTH_INDEX, KEY_LENGTH_FIELD_LENGTH,
        l);
  }

  /**
   * Gets the value of the tap messages key length field.
   *
   * @return The value of the key length field.
   */
  public final int getKeylength() {
    return (int) Util.fieldToValue(mbytes, KEY_LENGTH_INDEX,
        KEY_LENGTH_FIELD_LENGTH);
  }

  /**
   * Sets the value of the tap messages data type field.
   *
   * @param b The new value for the data type field.
   */
  public final void setDatatype(byte b) {
    mbytes[DATA_TYPE_INDEX] = b;
  }

  /**
   * Gets the value of the tap messages data type field.
   *
   * @return The value of the data type field.
   */
  public final byte getDatatype() {
    return mbytes[DATA_TYPE_INDEX];
  }

  /**
   * Sets the value of the tap messages extra length field.
   *
   * @param i The new value for the extra length field.
   */
  public final void setExtralength(int i) {
    mbytes[EXTRA_LENGTH_INDEX] = (byte) i;
  }

  /**
   * Gets the value of the tap messages extra length field.
   *
   * @return The value of the extra length field.
   */
  public final int getExtralength() {
    return mbytes[EXTRA_LENGTH_INDEX];
  }

  /**
   * Sets the value of the tap messages vbucket field.
   *
   * @param vb The new value for the vbucket field.
   */
  public final void setVbucket(int vb) {
    Util.valueToFieldOffest(mbytes, VBUCKET_INDEX, VBUCKET_FIELD_LENGTH, vb);
  }

  /**
   * Gets the value of the tap messages vbucket field.
   *
   * @return The value of the vbucket field.
   */
  public final int getVbucket() {
    return (int) Util.fieldToValue(mbytes, VBUCKET_INDEX, VBUCKET_FIELD_LENGTH);
  }

  /**
   * Sets the value of the tap messages total body field.
   *
   * @param l The new value for the total body field.
   */
  public final void setTotalbody(long l) {
    Util.valueToFieldOffest(mbytes, TOTAL_BODY_INDEX, TOTAL_BODY_FIELD_LENGTH,
        l);
  }

  /**
   * Gets the value of the tap messages total body field.
   *
   * @return The value of the total body field.
   */
  public final int getTotalbody() {
    return (int) Util.fieldToValue(mbytes, TOTAL_BODY_INDEX,
        TOTAL_BODY_FIELD_LENGTH);
  }

  /**
   * Sets the value of the tap messages opaque field.
   *
   * @param op The new value for the opaque field.
   */
  public final void setOpaque(int op) {
    Util.valueToFieldOffest(mbytes, OPAQUE_INDEX, OPAQUE_FIELD_LENGTH, op);
  }

  /**
   * Gets the value of the tap messages opaque field.
   *
   * @return The value of the opaque field.
   */
  public final int getOpaque() {
    return (int) Util.fieldToValue(mbytes, OPAQUE_INDEX, OPAQUE_FIELD_LENGTH);
  }

  /**
   * Sets the value of the tap messages cas field.
   *
   * @param cas The new value for the cas field.
   */
  public final void setCas(long cas) {
    Util.valueToFieldOffest(mbytes, CAS_INDEX, CAS_FIELD_LENGTH, cas);
  }

  /**
   * Gets the value of the tap messages cas field.
   *
   * @return The value of the cas field.
   */
  public final long getCas() {
    return Util.fieldToValue(mbytes, CAS_INDEX, CAS_FIELD_LENGTH);
  }

  /**
   * Gets the length of the entire message.
   *
   * @return The length of the message.
   */
  public final int getMessageLength() {
    return HEADER_LENGTH + getTotalbody();
  }

  /**
   * Creates a ByteBuffer representation of the message.
   *
   * @return The ByteBuffer representation of the message.
   */
  public final ByteBuffer getBytes() {
    return ByteBuffer.wrap(mbytes);
  }
}
