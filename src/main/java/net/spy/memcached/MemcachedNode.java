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

package net.spy.memcached;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import net.spy.memcached.ops.Operation;

/**
 * Interface defining a connection to a memcached server.
 */
public interface MemcachedNode {

  /**
   * Move all of the operations delivered via addOperation into the internal
   * write queue.
   */
  void copyInputQueue();

  /**
   * Extract all queued items for this node destructively.
   *
   * This is useful for redistributing items.
   */
  Collection<Operation> destroyInputQueue();

  /**
   * Clear the queue of currently processing operations by either cancelling
   * them or setting them up to be reapplied after a reconnect.
   */
  void setupResend();

  /**
   * Fill the write buffer with data from the next operations in the queue.
   *
   * @param optimizeGets if true, combine sequential gets into a single
   *          multi-key get
   */
  void fillWriteBuffer(boolean optimizeGets);

  /**
   * Transition the current write item into a read state.
   */
  void transitionWriteItem();

  /**
   * Get the operation at the top of the queue that is requiring input.
   */
  Operation getCurrentReadOp();

  /**
   * Remove the operation at the top of the queue that is requiring input.
   */
  Operation removeCurrentReadOp();

  /**
   * Get the operation at the top of the queue that has information available to
   * write.
   */
  Operation getCurrentWriteOp();

  /**
   * Remove the operation at the top of the queue that has information available
   * to write.
   */
  Operation removeCurrentWriteOp();

  /**
   * True if an operation is available to read.
   */
  boolean hasReadOp();

  /**
   * True if an operation is available to write.
   */
  boolean hasWriteOp();

  /**
   * Add an operation to the queue. Authentication operations should never be
   * added to the queue, but this is not checked.
   */
  void addOp(Operation op);

  /**
   * Insert an operation to the beginning of the queue.
   *
   * This method is meant to be invoked rarely.
   */
  void insertOp(Operation o);

  /**
   * Compute the appropriate selection operations for the channel this
   * MemcachedNode holds to the server.
   */
  int getSelectionOps();

  /**
   * Get the buffer used for reading data from this node.
   */
  ByteBuffer getRbuf();

  /**
   * Get the buffer used for writing data to this node.
   */
  ByteBuffer getWbuf();

  /**
   * Get the SocketAddress of the server to which this node is connected.
   */
  SocketAddress getSocketAddress();

  /**
   * True if this node is <q>active.</q> i.e. is is currently connected and
   * expected to be able to process requests
   */
  boolean isActive();

  /**
   * True if this node is <q>authenticated.</q>
   */
  boolean isAuthenticated();

  /**
   * Milliseconds since last successful read.
   */
  long lastReadDelta();

  /**
   * Notify node of successful read.
   *
   * This is used so the node can keep track of any internal debugging or
   * state it cares about on read.
   */
  void completedRead();

  /**
   * Notify this node that it will be reconnecting.
   */
  void reconnecting();

  /**
   * Notify this node that it has reconnected.
   */
  void connected();

  /**
   * Get the current reconnect count.
   */
  int getReconnectCount();

  /**
   * Register a channel with this node.
   */
  void registerChannel(SocketChannel ch, SelectionKey selectionKey);

  /**
   * Set the SocketChannel this node uses.
   */
  void setChannel(SocketChannel to);

  /**
   * Get the SocketChannel for this connection.
   */
  SocketChannel getChannel();

  /**
   * Set the selection key for this node.
   */
  void setSk(SelectionKey to);

  /**
   * Get the selection key from this node.
   */
  SelectionKey getSk();

  /**
   * Get the number of bytes remaining to write.
   */
  int getBytesRemainingToWrite();

  /**
   * Write some bytes and return the number of bytes written.
   *
   * @return the number of bytes written
   * @throws IOException if there's a problem writing
   */
  int writeSome() throws IOException;

  /**
   * Fix up the selection ops on the selection key.
   */
  void fixupOps();

  /**
   * Let the node know that auth is complete. Typically this would mean the node
   * can start processing and accept new operations to its input queue.
   */
  void authComplete();

  /**
   * Tell a node to set up for authentication. Typically this would mean
   * blocking additions to the queue. In a reconnect situation this may mean
   * putting any queued operations on hold to get to an auth complete state.
   */
  void setupForAuth();

  /**
   * Count 'time out' exceptions to drop connections that fail perpetually.
   *
   * @param timedOut
   */
  void setContinuousTimeout(boolean timedOut);

  int getContinuousTimeout();

  MemcachedConnection getConnection();

  void setConnection(MemcachedConnection connection);
}
