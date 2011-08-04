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
 * Failure modes for node failures.
 */
public enum FailureMode {

  /**
   * Move on to functional nodes when nodes fail.
   *
   * <p>
   * In this failure mode, the failure of a node will cause its current queue
   * and future requests to move to the next logical node in the cluster for a
   * given key.
   * </p>
   */
  Redistribute,
  /**
   * Continue to retry a failing node until it comes back up.
   *
   * <p>
   * This failure mode is appropriate when you have a rare short downtime of a
   * memcached node that will be back quickly, and your app is written to not
   * wait very long for async command completion.
   * </p>
   */
  Retry,

  /**
   * Automatically cancel all operations heading towards a downed node.
   */
  Cancel
}
