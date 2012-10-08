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
 * PersistTo codes for a Observe operation.
 */
public enum PersistTo {

  /**
   * Don't wait for persistence on any nodes.
   */
  ZERO(0),
  /**
   * Persist to the Master. ONE implies MASTER.
   */
  MASTER(1),
  /**
   * ONE implies MASTER.
   */
  ONE(1),
  /**
   * Persist to at least two nodes including Master.
   */
  TWO(2),
  /**
   * Persist to at least three nodes including Master.
   */
  THREE(3),
  /**
   * Persist to at least four nodes including Master.
   */
  FOUR(4);

  private final int value;

  PersistTo(int val) {
    value = val;
  }

  public int getValue() {
    return value;
  }
}
