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

package net.spy.memcached.ops;

/**
 * An operation callback that will capture receivedStatus and complete
 * invocations and dispatch to a single callback.
 *
 * <p>
 * This is useful for the cases where a single request gets split into multiple
 * requests and the callback needs to not know the difference.
 * </p>
 */
public abstract class MultiOperationCallback implements OperationCallback {

  private OperationStatus mostRecentStatus = null;
  private int remaining = 0;
  protected final OperationCallback originalCallback;

  /**
   * Get a MultiOperationCallback over the given callback for the specified
   * number of replicates.
   *
   * @param original the original callback
   * @param todo how many complete() calls we expect before dispatching.
   */
  public MultiOperationCallback(OperationCallback original, int todo) {
    if (original instanceof MultiOperationCallback) {
        original = ((MultiOperationCallback) original).originalCallback;
    }

    originalCallback = original;
    remaining = todo;
  }

  public void complete() {
    if (--remaining == 0) {
      originalCallback.receivedStatus(mostRecentStatus);
      originalCallback.complete();
    }
  }

  public void receivedStatus(OperationStatus status) {
    mostRecentStatus = status;
  }
}
