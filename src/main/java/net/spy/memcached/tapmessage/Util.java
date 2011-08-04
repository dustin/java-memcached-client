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
 * The Util class provides utility functions for converting portions of byte
 * arrays to values and putting values into byte arrays.
 */
public final class Util {

  private Util() {
    // Empty
  }

  /**
   * Converts a field in a byte array into a value.
   *
   * @param buffer The byte array that contains the value
   * @param offset The offset of where the value begins in the byte array
   * @param length The length of the field to be converted
   * @return A long that represent the value of the field
   */
  public static long fieldToValue(byte[] buffer, int offset, int length) {
    long total = 0;
    long val = 0;
    for (int i = 0; i < length; i++) {
      val = buffer[offset + i];
      if (val < 0) {
        val = val + 256;
      }
      total += (long) Math.pow(256.0, (double) (length - 1 - i)) * val;
    }
    return total;
  }

  /**
   * Puts a value into a specific location in a byte buffer.
   *
   * @param buffer The buffer that the value will be written to.
   * @param offset The offset for where the value begins in the buffer.
   * @param length The length of the field in the array
   * @param l The value to be encoded into the byte array
   */
  public static void valueToFieldOffest(byte[] buffer, int offset, int length,
      long l) {
    long divisor;
    for (int i = 0; i < length; i++) {
      divisor = (long) Math.pow(256.0, (double) (length - 1 - i));
      buffer[offset + i] = (byte) (l / divisor);
      l = l % divisor;
    }
  }
}
