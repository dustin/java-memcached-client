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

/**
 * A value with a CAS identifier.
 */
public class CASValue<T> {
  private final long cas;
  private final T value;

  /**
   * Construct a new CASValue with the given identifer and value.
   *
   * @param c the CAS identifier
   * @param v the value
   */
  public CASValue(long c, T v) {
    super();
    cas = c;
    value = v;
  }

  /**
   * Get the CAS identifier.
   */
  public long getCas() {
    return cas;
  }

  /**
   * Get the object value.
   */
  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "{CasValue " + cas + "/" + value + "}";
  }
}
