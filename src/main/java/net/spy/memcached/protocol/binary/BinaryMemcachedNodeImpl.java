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

package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;

import net.spy.memcached.MemcachedNodeStats;
import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.protocol.ProxyCallback;
import net.spy.memcached.protocol.TCPMemcachedNodeImpl;

/**
 * Implementation of MemcachedNode for speakers of the binary protocol.
 */
public class BinaryMemcachedNodeImpl extends TCPMemcachedNodeImpl {

  private static final int MAX_GET_OPTIMIZATION_COUNT = 4096;
  private static final int MAX_SET_OPTIMIZATION_COUNT = 65535;
  private static final int MAX_SET_OPTIMIZATION_BYTES = 2 * 1024 * 1024;

  public BinaryMemcachedNodeImpl(SocketAddress sa,
                                 int bufSize,
                                 BlockingQueue<Operation> rq,
                                 BlockingQueue<Operation> wq,
                                 BlockingQueue<Operation> iq,
                                 long opQueueMaxBlockTime,
                                 boolean waitForAuth,
                                 long dt,
                                 final MemcachedNodeStats stats) throws IOException {
    super(sa, bufSize, rq, wq, iq, opQueueMaxBlockTime, waitForAuth, dt, stats);
  }

  @Override
  protected void optimize() {
    Operation firstOp = getWriteOpInQueue();
    if (firstOp instanceof GetOperation) {
      optimizeGets();
    } else if (firstOp instanceof CASOperation) {
      optimizeSets();
    }
  }

  private void optimizeGets() {
    // make sure there are at least two get operations in a row before
    // attempting to optimize them.
    optimizedOp = removeWriteOpFromQueue();
    if (getWriteOpInQueue() instanceof GetOperation) {
      OptimizedGetImpl og = new OptimizedGetImpl((GetOperation) optimizedOp);
      optimizedOp = og;
      getStats().optimizedGets(1);
      while (getWriteOpInQueue() instanceof GetOperation
          && og.size() < MAX_GET_OPTIMIZATION_COUNT) {
        GetOperation o = (GetOperation) removeWriteOpFromQueue();
        if (!o.isCancelled()) {
          og.addOperation(o);
          getStats().optimizedGets(1);
        }
      }

      // Initialize the new mega get
      optimizedOp.initialize();
      assert optimizedOp.getState() == OperationState.WRITE_QUEUED;
      ProxyCallback pcb = (ProxyCallback) og.getCallback();
      getLogger().trace("Set up %s with %s keys and %s callbacks", this,
          pcb.numKeys(), pcb.numCallbacks());
    }
  }

  private void optimizeSets() {
    // make sure there are at least two get operations in a row before
    // attempting to optimize them.
    optimizedOp = removeWriteOpFromQueue();
    if (getWriteOpInQueue() instanceof CASOperation) {
      OptimizedSetImpl og = new OptimizedSetImpl((CASOperation) optimizedOp);
      optimizedOp = og;
      getStats().optimizedSets(1);
      while (getWriteOpInQueue() instanceof StoreOperation
          && og.size() < MAX_SET_OPTIMIZATION_COUNT
          && og.bytes() < MAX_SET_OPTIMIZATION_BYTES) {
        CASOperation o = (CASOperation) removeWriteOpFromQueue();
        if (!o.isCancelled()) {
          og.addOperation(o);
          getStats().optimizedSets(1);
        }
      }

      // Initialize the new mega set
      optimizedOp.initialize();
      assert optimizedOp.getState() == OperationState.WRITE_QUEUED;
    }
  }
}
