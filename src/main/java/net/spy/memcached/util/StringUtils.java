/**
 * Copyright (C) 2009-2014 Couchbase, Inc.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.MemcachedClientIF;

/**
 * Utility methods on string objects.
 */
public final class StringUtils {

  /**
   * A pattern to match on a signed integer value.
   */
  private static final Pattern decimalPattern = Pattern.compile("^-?\\d+$");

  /**
   * The matcher for the decimal pattern regex.
   */
  private static final Matcher decimalMatcher = decimalPattern.matcher("");

  /**
   * Maximum supported key length.
   */
  private static final int MAX_KEY_LENGTH = MemcachedClientIF.MAX_KEY_LENGTH;

  /**
   * Exception thrown if the input key is too long.
   */
  private static final IllegalArgumentException KEY_TOO_LONG_EXCEPTION =
    new IllegalArgumentException("Key is too long (maxlen = "
      + MAX_KEY_LENGTH + ")");

  /**
   * Exception thrown if the input key is empty.
   */
  private static final IllegalArgumentException KEY_EMPTY_EXCEPTION =
    new IllegalArgumentException("Key must contain at least one character.");

  /**
   * Preset the stack traces for the static exceptions.
   */
  static {
    KEY_TOO_LONG_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    KEY_EMPTY_EXCEPTION.setStackTrace(new StackTraceElement[0]);
  }

  /**
   * Private constructor, since this is a purely static class.
   */
  private StringUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Join a collection of strings together into one.
   *
   * @param chunks the chunks to join.
   * @param delimiter the delimiter between the keys.
   * @return the fully joined string.
   */
  public static String join(final Collection<String> chunks,
    final String delimiter) {
    StringBuilder sb = new StringBuilder();
    if (!chunks.isEmpty()) {
      Iterator<String> itr = chunks.iterator();
      sb.append(itr.next());
      while (itr.hasNext()) {
        sb.append(delimiter);
        sb.append(itr.next());
      }
    }
    return sb.toString();
  }

  /**
   * Check if a given string is a JSON object.
   *
   * @param s the input string.
   * @return true if it is a JSON object, false otherwise.
   */
  public static boolean isJsonObject(final String s) {
    if (s.startsWith("{") || s.startsWith("[")
      || "true".equals(s) || "false".equals(s)
      || "null".equals(s) || decimalMatcher.reset(s).matches()) {
      return true;
    }

    return false;
  }

  /**
   * Check if a given key is valid to transmit.
   *
   * @param key the key to check.
   * @param binary if binary protocol is used.
   */
  public static void validateKey(final String key, final boolean binary) {
    byte[] keyBytes = KeyUtil.getKeyBytes(key);
    int keyLength = keyBytes.length;

    if (keyLength > MAX_KEY_LENGTH) {
      throw KEY_TOO_LONG_EXCEPTION;
    }

    if (keyLength == 0) {
      throw KEY_EMPTY_EXCEPTION;
    }

    if(!binary) {
      for (byte b : keyBytes) {
        if (b == ' ' || b == '\n' || b == '\r' || b == 0) {
          throw new IllegalArgumentException(
              "Key contains invalid characters:  ``" + key + "''");
        }
      }
    }

  }
}
