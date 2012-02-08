/**
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

package net.spy.memcached.tapmessage;

import java.util.LinkedList;
import java.util.List;

import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.TapOperation;

/**
 * Holds multiple operations put together for a tap stream.
 *
 * A TapStream handles the series of messages coming back from a given node
 * which is sending data over TAP.  The TapClient will typically manage a
 * set of TapStreams.
 */
public class TapStream {
  private final List<TapOperation> ops;

  /**
   * Creates a new TapStream which will be used by a TapClient to handle
   * incoming TAP operations.
   *
   */
  public TapStream() {
    ops = new LinkedList<TapOperation>();
  }

  /**
   * Cancels all operations still waiting on an existing TapStream.
   */
  public void cancel() {
    for (TapOperation op : ops) {
      op.cancel();
    }
  }

  /**
   * Check if all operations in the TapStream are completed.
   *
   * @return true if all operations currently in the TapStream are completed
   */
  public boolean isCompleted() {
    for (TapOperation op : ops) {
      if (!op.getState().equals(OperationState.COMPLETE)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine if messages sent to this server and recorded on this TapStream
   * have been canceled.
   *
   * @return true if all operations on this TapStream have been canceled
   */
  public boolean isCancelled() {
    for (TapOperation op : ops) {
      if (!op.isCancelled()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine if messages sent to this server and recorded on this TapStream
   * have been determined to be in error.
   *
   * @return true if all operations on this TapStream are in an error state
   */
  public boolean hasErrored() {
    for (TapOperation op : ops) {
      if (!op.hasErrored()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Add an operation to the stream of TAP messages which have been sent to the
   * server.
   *
   * Note this does not represent all messages, just a select few worth keeping
   * track of.
   *
   * While a public method, this is not intended for general use but rather only
   * for use when extending the TapClient which manages this TapStream.
   *
   * @param op
   */
  public void addOp(TapOperation op) {
    ops.add(op);
  }
}
