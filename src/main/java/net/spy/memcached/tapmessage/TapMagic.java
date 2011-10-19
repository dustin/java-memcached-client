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
 * The Magic enum contains a list all of the different magic that can be passed
 * in a tap message in the flag field.
 */
public enum TapMagic {
  /**
   * Defines a tap binary request packet.
   */
  PROTOCOL_BINARY_REQ((byte) 0x80),

  /**
   * Defines a tap binary response packet.
   */
  PROTOCOL_BINARY_RES((byte) 0x81);

  /**
   * The magic value.
   */
  private byte magic;

  /**
   * Defines the magic value.
   *
   * @param magic - The new magic value
   */
  TapMagic(byte magic) {
    this.magic = magic;
  }

  public static TapMagic getMagicByByte(byte b) {
    if (b == PROTOCOL_BINARY_REQ.magic) {
      return TapMagic.PROTOCOL_BINARY_REQ;
    } else if (b == PROTOCOL_BINARY_RES.magic) {
      return TapMagic.PROTOCOL_BINARY_RES;
    } else {
      throw new IllegalArgumentException("Bad magic value");
    }
  }

  public byte getMagic() {
    return magic;
  }
}
