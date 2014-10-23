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

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.HostPort;
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

  public BinaryMemcachedNodeImpl(
      HostPort hp,
      SocketChannel c,
      int bufSize,
      BlockingQueue<Operation> rq,
      BlockingQueue<Operation> wq,
      BlockingQueue<Operation> iq,
      Long opQueueMaxBlockTimeNs,
      boolean waitForAuth,
      long dt,
      long at,
      ConnectionFactory fa) {
    super(hp, c, bufSize, rq, wq, iq, opQueueMaxBlockTimeNs, waitForAuth, dt, at, fa);
  }

  @Override
  protected void optimize() {
    Operation firstOp = writeQ.peek();
    if (firstOp instanceof GetOperation) {
      optimizeGets();
    } else if (firstOp instanceof CASOperation) {
      optimizeSets();
    }
  }

  private void optimizeGets() {
    // make sure there are at least two get operations in a row before
    // attempting to optimize them.
    optimizedOp = writeQ.remove();
    if (writeQ.peek() instanceof GetOperation) {
      OptimizedGetImpl og = new OptimizedGetImpl((GetOperation) optimizedOp);
      optimizedOp = og;

      while (writeQ.peek() instanceof GetOperation
          && og.size() < MAX_GET_OPTIMIZATION_COUNT) {
        GetOperation o = (GetOperation) writeQ.remove();
        if (!o.isCancelled()) {
          og.addOperation(o);
        }
      }

      // Initialize the new mega get
      optimizedOp.initialize();
      assert optimizedOp.getState() == OperationState.WRITE_QUEUED;
      ProxyCallback pcb = (ProxyCallback) og.getCallback();
      getLogger().debug("Set up %s with %s keys and %s callbacks", this,
          pcb.numKeys(), pcb.numCallbacks());
    }
  }

  private void optimizeSets() {
    // make sure there are at least two get operations in a row before
    // attempting to optimize them.
    optimizedOp = writeQ.remove();
    if (writeQ.peek() instanceof CASOperation) {
      OptimizedSetImpl og = new OptimizedSetImpl((CASOperation) optimizedOp);
      optimizedOp = og;

      while (writeQ.peek() instanceof StoreOperation
          && og.size() < MAX_SET_OPTIMIZATION_COUNT
          && og.bytes() < MAX_SET_OPTIMIZATION_BYTES) {
        CASOperation o = (CASOperation) writeQ.remove();
        if (!o.isCancelled()) {
          og.addOperation(o);
        }
      }

      // Initialize the new mega set
      optimizedOp.initialize();
      assert optimizedOp.getState() == OperationState.WRITE_QUEUED;
    }
  }
}
