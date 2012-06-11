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

package net.spy.memcached.util;

import java.util.Collection;
import java.util.Iterator;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.MemcachedClientIF;

/**
 * Some String utilities.
 */
public final class StringUtils {

  private StringUtils() {
    // Empty
  }

  public static String join(Collection<String> keys, String delimiter) {
    StringBuilder sb = new StringBuilder();
    if (!keys.isEmpty()) {
      Iterator<String> itr = keys.iterator();
      sb.append(itr.next());
      while (itr.hasNext()) {
        sb.append(delimiter);
        sb.append(itr.next());
      }
    }
    return sb.toString();
  }

  public static boolean isJsonObject(String s) {
    if (s.startsWith("{") || s.startsWith("[") || s.equals("true")
        || s.equals("false") || s.equals("null")) {
      return true;
    }
    try {
      new Integer(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static void validateKey(String key) {
    byte[] keyBytes = KeyUtil.getKeyBytes(key);
    if (keyBytes.length > MemcachedClientIF.MAX_KEY_LENGTH) {
      throw new IllegalArgumentException("Key is too long (maxlen = "
          + MemcachedClientIF.MAX_KEY_LENGTH + ")");
    }
    if (keyBytes.length == 0) {
      throw new IllegalArgumentException(
          "Key must contain at least one character.");
    }
    // Validate the key
    for (byte b : keyBytes) {
      if (b == ' ' || b == '\n' || b == '\r' || b == 0) {
        throw new IllegalArgumentException(
            "Key contains invalid characters:  ``" + key + "''");
      }
    }
  }
}
