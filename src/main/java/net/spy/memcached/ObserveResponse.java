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

package net.spy.memcached;

/**
 * Response codes for a Observe operation.
 */
public enum ObserveResponse {
  /**
   * Response indicating the key was uninitialized.
   */
  UNINITIALIZED((byte) 0xff),
  /**
   * Response indicating the key was modified.
   */
  MODIFIED((byte) 0xfe),
  /**
   * Response indicating the key was persisted.
   */
  FOUND_PERSISTED((byte) 0x01),
  /**
   * Response indicating the key was found but not persisted.
   */
  FOUND_NOT_PERSISTED((byte) 0x00),
  /**
   * Response indicating the key was not found and persisted, as in
   * the case of deletes - a real delete.
   */
  NOT_FOUND_PERSISTED((byte) 0x80),
  /**
   * Response indicating the key was not found and not
   * persisted, as in the case of deletes - a logical delete.
   */
  NOT_FOUND_NOT_PERSISTED((byte) 0x81);

  private final byte value;

  ObserveResponse(byte b) {
    value = b;
  }

  public static ObserveResponse valueOf(byte b) {
    switch (b) {
    case (byte) 0x00:
      return ObserveResponse.FOUND_NOT_PERSISTED;
    case (byte) 0x01:
      return ObserveResponse.FOUND_PERSISTED;
    case (byte) 0x80:
      return ObserveResponse.NOT_FOUND_PERSISTED;
    case (byte) 0x81:
      return ObserveResponse.NOT_FOUND_NOT_PERSISTED;
    case (byte) 0xfe:
      return ObserveResponse.MODIFIED;
    default:
      return ObserveResponse.UNINITIALIZED;
    }
  }

  public byte getResponse() {
    return value;
  }
}
