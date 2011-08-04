/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Utilities for processing key values.
 */
public final class KeyUtil {

  private KeyUtil() {
    // Empty
  }

  /**
   * Get the bytes for a key.
   *
   * @param k the key
   * @return the bytes
   */
  public static byte[] getKeyBytes(String k) {
    try {
      return k.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the keys in byte form for all of the string keys.
   *
   * @param keys a collection of keys
   * @return return a collection of the byte representations of keys
   */
  public static Collection<byte[]> getKeyBytes(Collection<String> keys) {
    Collection<byte[]> rv = new ArrayList<byte[]>(keys.size());
    for (String s : keys) {
      rv.add(getKeyBytes(s));
    }
    return rv;
  }
}
