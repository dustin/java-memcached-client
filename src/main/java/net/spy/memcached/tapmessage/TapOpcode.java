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

/**
 * The Opcode enum contains a list all of the different opcodes that can be
 * passed in a tap message in the flag field.
 */
public enum TapOpcode {
  /**
   * Defines a tap no-op message.
   */
  NOOP((byte) 0x0a),

  /**
   * Defines a SASL list mechanism message.
   */
  SASLLIST((byte) 0x20),

  /**
   * Defines a SASL authorization message.
   */
  SASLAUTH((byte) 0x21),

  /**
   * Defines a request message to open a tap connection.
   */
  REQUEST((byte) 0x40),

  /**
   * Defines a key-value mutation message to specify a key-value has changed.
   */
  MUTATION((byte) 0x41),

  /**
   * Defines a delete message to specify a key has been deleted.
   */
  DELETE((byte) 0x42),

  /**
   * Defines a tap flush message.
   */
  FLUSH((byte) 0x43),

  /**
   * Defines a opaque message to send control data to the consumer.
   */
  OPAQUE((byte) 0x44),

  /**
   * Defines a vBucket set message to set the state of a vBucket in the
   * consumer.
   */
  VBUCKETSET((byte) 0x45),

  /**
   * Defines the start of a checkpoint.
   */
  START_CHECKPOINT((byte) 0x46),

  /**
   * Defines the end of a checkpoint.
   */
  END_CHECKPOINT((byte) 0x47);

  /**
   * The opcode value.
   */
  private byte opcode;

  /**
   * Defines the magic value.
   *
   * @param magic - The new magic value
   */
  TapOpcode(byte opcode) {
    this.opcode = opcode;
  }

  public byte getOpcode() {
    return opcode;
  }

  public static TapOpcode getOpcodeByByte(byte b) {
    if (b == TapOpcode.DELETE.opcode) {
      return TapOpcode.DELETE;
    } else if (b == TapOpcode.FLUSH.opcode) {
      return TapOpcode.DELETE;
    } else if (b == TapOpcode.MUTATION.opcode) {
      return TapOpcode.MUTATION;
    } else if (b == TapOpcode.NOOP.opcode) {
      return TapOpcode.NOOP;
    } else if (b == TapOpcode.OPAQUE.opcode) {
      return TapOpcode.OPAQUE;
    } else if (b == TapOpcode.REQUEST.opcode) {
      return TapOpcode.REQUEST;
    } else if (b == TapOpcode.SASLAUTH.opcode) {
      return TapOpcode.SASLAUTH;
    } else if (b == TapOpcode.SASLLIST.opcode) {
      return TapOpcode.SASLLIST;
    } else if (b == TapOpcode.VBUCKETSET.opcode) {
      return TapOpcode.VBUCKETSET;
    } else if (b == TapOpcode.START_CHECKPOINT.opcode) {
      return TapOpcode.START_CHECKPOINT;
    } else if (b == TapOpcode.END_CHECKPOINT.opcode) {
      return TapOpcode.END_CHECKPOINT;
    } else {
      return null;
    }
  }
}
