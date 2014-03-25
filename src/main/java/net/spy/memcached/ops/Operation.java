/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.ops;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.spy.memcached.MemcachedNode;

/**
 * Base interface for all operations.
 */
public interface Operation {

  /**
   * Has this operation been cancelled?
   */
  boolean isCancelled();

  /**
   * True if an error occurred while processing this operation.
   */
  boolean hasErrored();

  /**
   * Get the exception that occurred (or null if no exception occurred).
   */
  OperationException getException();

  /**
   * Get the callback for this get operation.
   */
  OperationCallback getCallback();

  /**
   * Cancel this operation.
   */
  void cancel();

  /**
   * Get the current state of this operation.
   */
  OperationState getState();

  /**
   * Get the write buffer for this operation.
   */
  ByteBuffer getBuffer();

  /**
   * Invoked when we start writing all of the bytes from this operation to
   * the sockets write buffer.
   */
  void writing();

  /**
   * Invoked after having written all of the bytes from the supplied output
   * buffer.
   */
  void writeComplete();

  /**
   * Initialize this operation. This is used to prepare output byte buffers and
   * stuff.
   */
  void initialize();

  /**
   * Read data from the given byte buffer and dispatch to the appropriate read
   * mechanism.
   */
  void readFromBuffer(ByteBuffer data) throws IOException;

  /**
   * Handle a raw data read.
   */
  void handleRead(ByteBuffer data);

  /**
   * Get the node that should've been handling this operation.
   */
  MemcachedNode getHandlingNode();

  /**
   * Set a reference to the node that will be/is handling this operation.
   *
   * @param to a memcached node
   */
  void setHandlingNode(MemcachedNode to);

  /**
   * Mark this operation as one which has exceeded its timeout value.
   */
  void timeOut();

  /**
   * True if the operation has timed out.
   *
   * <p>
   * A timed out operation may or may not have been sent to the server already,
   * but it exceeded either the specified or the default timeout value.
   */
  boolean isTimedOut();

  /**
   * True if the operation has timed out.
   *
   * The ttl allows the caller to specify how long the operation should have
   * been given since its creation, returning true if the operation has exceeded
   * that time period.
   *
   * <p>
   * A timed out operation may or may not have been sent to the server already,
   * but it exceeded either the specified or the default timeout value.
   *
   * <p>
   * In the rare case this may be called with a longer timeout value after
   * having been called with a shorter value that caused the operation to be
   * timed out, an IllegalArgumentException may be thrown.
   *
   * @param ttlMillis the max amount of time an operation may have existed since
   *          its creation in milliseconds.
   */
  boolean isTimedOut(long ttlMillis);

  /**
   * True if the operation has timed out and has not been sent.
   *
   * If the client has timed out this operation and knows it has not been
   * written to the network, this will be true.
   */
  boolean isTimedOutUnsent();

  /**
   * Returns the timestamp from the point where the WRITE was completed.
   *
   * This can be used to calculate the roundtrip time of the operation.
   */
  long getWriteCompleteTimestamp();

  /**
   * Returns the raw bytes of the error message content.
   *
   * @return the raw error message content.
   */
  byte[] getErrorMsg();

  /**
   * Add the clone from this operation.
   *
   * @param op the cloned operation.
   */
  void addClone(Operation op);

  /**
   * Returns the number of times this operation has been cloned.
   *
   * @return the number of clones.
   */
  int getCloneCount();

  /**
   * Sets the clone count for this operation.
   */
  void setCloneCount(int count);
}
