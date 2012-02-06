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

package net.spy.memcached.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.CancelledOperationStatus;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.TimedOutOperationStatus;

/**
 * Base class for protocol-specific operation implementations.
 */
public abstract class BaseOperationImpl extends SpyObject implements Operation {

  /**
   * Status object for canceled operations.
   */
  public static final OperationStatus CANCELLED =
      new CancelledOperationStatus();
  public static final OperationStatus TIMED_OUT=
      new TimedOutOperationStatus();
  private OperationState state = OperationState.WRITE_QUEUED;
  private ByteBuffer cmd = null;
  private boolean cancelled = false;
  private OperationException exception = null;
  protected OperationCallback callback = null;
  private volatile MemcachedNode handlingNode = null;
  private boolean timedout;
  private long creationTime;
  private boolean timedOutUnsent = false;
  protected Collection<MemcachedNode> notMyVbucketNodes =
      new HashSet<MemcachedNode>();

  public BaseOperationImpl() {
    super();
    creationTime = System.nanoTime();
  }

  /**
   * Get the operation callback associated with this operation.
   */
  public synchronized final OperationCallback getCallback() {
    return callback;
  }

  /**
   * Set the callback for this instance.
   */
  protected synchronized void setCallback(OperationCallback to) {
    callback = to;
  }

  public final synchronized boolean isCancelled() {
    return cancelled;
  }

  public final boolean hasErrored() {
    return exception != null;
  }

  public final OperationException getException() {
    return exception;
  }

  public final synchronized void cancel() {
    cancelled = true;
    wasCancelled();
    callback.receivedStatus(CANCELLED);
    callback.complete();
  }

  /**
   * This is called on each subclass whenever an operation was cancelled.
   */
  protected void wasCancelled() {
    getLogger().debug("was cancelled.");
  }

  public final synchronized OperationState getState() {
    return state;
  }

  public final synchronized ByteBuffer getBuffer() {
    return cmd;
  }

  /**
   * Set the write buffer for this operation.
   */
  protected final synchronized void setBuffer(ByteBuffer to) {
    assert to != null : "Trying to set buffer to null";
    cmd = to;
    cmd.mark();
  }

  /**
   * Transition the state of this operation to the given state.
   */
  protected final synchronized void transitionState(OperationState newState) {
    getLogger().debug("Transitioned state from %s to %s", state, newState);
    state = newState;
    // Discard our buffer when we no longer need it.
    if(state != OperationState.WRITE_QUEUED
        && state != OperationState.WRITING) {
      cmd = null;
    }
    if (state == OperationState.COMPLETE) {
      callback.complete();
    }
  }

  public final void writing() {
    transitionState(OperationState.WRITING);
  }

  public final void writeComplete() {
    transitionState(OperationState.READING);
  }

  public abstract void initialize();

  public abstract void readFromBuffer(ByteBuffer data) throws IOException;

  protected synchronized void handleError(OperationErrorType eType, String line)
    throws IOException {
    getLogger().error("Error:  %s", line);
    switch (eType) {
    case GENERAL:
      exception = new OperationException();
      break;
    case SERVER:
      exception = new OperationException(eType, line);
      break;
    case CLIENT:
      exception = new OperationException(eType, line);
      break;
    default:
      assert false;
    }
    callback.receivedStatus(new OperationStatus(false,
        exception.getMessage()));
    transitionState(OperationState.COMPLETE);
    throw exception;
  }

  public void handleRead(ByteBuffer data) {
    assert false;
  }

  public MemcachedNode getHandlingNode() {
    return handlingNode;
  }

  public void setHandlingNode(MemcachedNode to) {
    handlingNode = to;
  }

  @Override
  public synchronized void timeOut() {
    timedout = true;
    callback.receivedStatus(TIMED_OUT);
    callback.complete();
  }

  @Override
  public synchronized boolean isTimedOut() {
    return timedout;
  }

  @Override
  public synchronized boolean isTimedOut(long ttlMillis) {
    long elapsed = System.nanoTime();
    long ttlNanos = ttlMillis * 1000 * 1000;
    if (elapsed - creationTime > ttlNanos) {
      timedOutUnsent = true;
      timedout = true;
      callback.receivedStatus(TIMED_OUT);
      callback.complete();
    } else {
      // timedout would be false, but we cannot allow you to untimeout an
      // operation
      if (timedout) {
        throw new IllegalArgumentException("Operation has already timed out;"
            + " ttl specified would allow it to be valid.");
      }
    }
    return timedout;
  }

  @Override
  public boolean isTimedOutUnsent() {
    return timedOutUnsent;
  }
}
