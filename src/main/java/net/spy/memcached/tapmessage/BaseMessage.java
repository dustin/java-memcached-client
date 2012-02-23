/**
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

package net.spy.memcached.tapmessage;

import java.nio.ByteBuffer;

import net.spy.memcached.compat.SpyObject;

/**
 * The BaseMessage implements the header of a tap message. This class cannot be
 * instantiated.  Tap stream messages are created with the RequestMessage and
 * ResponseMessage classes.
 */
public abstract class BaseMessage extends SpyObject {
  private static final int MAGIC_OFFSET = 0;
  private static final int OPCODE_OFFSET = 1;
  private static final int KEYLENGTH_OFFSET = 2;
  private static final int EXTRALENGTH_OFFSET = 4;
  private static final int DATATYPE_OFFSET = 5;
  private static final int VBUCKET_OFFSET = 6;
  private static final int TOTALBODY_OFFSET = 8;
  private static final int OPAQUE_OFFSET = 12;
  private static final int CAS_OFFSET = 16;
  public static final int HEADER_LENGTH = 24;
  protected TapMagic magic;
  protected TapOpcode opcode;
  protected short keylength;
  protected byte extralength;
  protected byte datatype;
  protected short vbucket;
  protected int totalbody;
  protected int opaque;
  protected long cas;

  protected BaseMessage() {
    // Empty
  }

  protected BaseMessage(byte[] b) {
    magic = TapMagic.getMagicByByte(b[MAGIC_OFFSET]);
    opcode = TapOpcode.getOpcodeByByte(b[OPCODE_OFFSET]);
    keylength = decodeShort(b, KEYLENGTH_OFFSET);
    extralength = b[EXTRALENGTH_OFFSET];
    datatype = b[DATATYPE_OFFSET];
    vbucket = decodeShort(b, VBUCKET_OFFSET);
    totalbody = decodeInt(b, TOTALBODY_OFFSET);
    opaque = decodeInt(b, OPAQUE_OFFSET);
    cas = decodeLong(b, CAS_OFFSET);
  }

  /**
   * Sets the value of the tap messages magic field.
   *
   * @param m The new value for the magic field.
   */
  public final void setMagic(TapMagic m) {
    magic = m;
  }

  /**
   * Gets the value of the tap messages magic field.
   *
   * @return The value of the magic field.
   */
  public final TapMagic getMagic() {
    return magic;
  }

  /**
   * Sets the value of the tap messages opcode field.
   *
   * @param o The new value of the opcode field.
   */
  public final void setOpcode(TapOpcode o) {
    opcode = o;
  }

  /**
   * Gets the value of the tap messages opaque field.
   *
   * @return The value of the opaque field.
   */
  public final TapOpcode getOpcode() {
    return opcode;
  }

  /**
   * Gets the value of the tap messages key length field.
   *
   * @return The value of the key length field.
   */
  public final short getKeylength() {
    return keylength;
  }

  /**
   * Sets the value of the tap messages data type field.
   *
   * @param d The new value for the data type field.
   */
  public final void setDatatype(byte d) {
    datatype = d;
  }

  /**
   * Gets the value of the tap messages data type field.
   *
   * @return The value of the data type field.
   */
  public final byte getDatatype() {
    return datatype;
  }

  /**
   * Sets the value of the tap messages extra length field.
   *
   * @param e The new value for the extra length field.
   */
  public final void setExtralength(byte e) {
    extralength = e;
  }

  /**
   * Gets the value of the tap messages extra length field.
   *
   * @return The value of the extra length field.
   */
  public final byte getExtralength() {
    return extralength;
  }

  /**
   * Sets the value of the tap messages vbucket field.
   *
   * @param vb The new value for the vbucket field.
   */
  public final void setVbucket(short vb) {
    vbucket = vb;
  }

  /**
   * Gets the value of the tap messages vbucket field.
   *
   * @return The value of the vbucket field.
   */
  public final short getVbucket() {
    return vbucket;
  }

  /**
   * Sets the value of the tap messages total body field.
   *
   * @param t The new value for the total body field.
   */
  public final void setTotalbody(int t) {
    totalbody = t;
  }

  /**
   * Gets the value of the tap messages total body field.
   *
   * @return The value of the total body field.
   */
  public final int getTotalbody() {
    return totalbody;
  }

  /**
   * Sets the value of the tap messages opaque field.
   *
   * @param op The new value for the opaque field.
   */
  public final void setOpaque(int op) {
    opaque = op;
  }

  /**
   * Gets the value of the tap messages opaque field.
   *
   * @return The value of the opaque field.
   */
  public final int getOpaque() {
    return opaque;
  }

  /**
   * Sets the value of the tap messages cas field.
   *
   * @param c The new value for the cas field.
   */
  public final void setCas(long c) {
    cas = c;
  }

  /**
   * Gets the value of the tap messages cas field.
   *
   * @return The value of the cas field.
   */
  public final long getCas() {
    return cas;
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
  public abstract ByteBuffer getBytes();

  protected short decodeShort(byte[] data, int i) {
    return (short) ((data[i] & 0xff) << 8 | (data[i + 1] & 0xff));
  }

  protected int decodeInt(byte[] data, int i) {
    return (data[i] & 0xff) << 24
      | (data[i + 1] & 0xff) << 16
      | (data[i + 2] & 0xff) << 8
      | (data[i + 3] & 0xff);
  }

  protected int decodeIntHostOrder(byte[] data, int i) {
    return (data[i] & 0xff)
      | (data[i + 1] & 0xff) << 8
      | (data[i + 2] & 0xff) << 16
      | (data[i + 3] & 0xff) << 24;
  }

  protected long decodeLong(byte[] data, int i) {
    return (data[i] & 0xffL) << 56
      | (data[i + 1] & 0xffL) << 48
      | (data[i + 2] & 0xffL) << 40
      | (data[i + 3] & 0xffL) << 32
      | (data[i + 4] & 0xffL) << 24
      | (data[i + 5] & 0xffL) << 16
      | (data[i + 6] & 0xffL) << 8
      | (data[i + 7] & 0xffL);
  }
}
