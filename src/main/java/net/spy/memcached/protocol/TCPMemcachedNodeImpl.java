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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;


import net.spy.memcached.LocalStatType;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.protocol.binary.TapAckOperationImpl;

/**
 * Represents a node with the memcached cluster, along with buffering and
 * operation queues.
 */
public abstract class TCPMemcachedNodeImpl extends SpyObject implements
    MemcachedNode {

  private final SocketAddress socketAddress;
  private final ByteBuffer rbuf;
  private final ByteBuffer wbuf;
  protected final BlockingQueue<Operation> writeQ;
  private final BlockingQueue<Operation> readQ;
  private final BlockingQueue<Operation> inputQueue;
  private final long opQueueMaxBlockTime;
  // This has been declared volatile so it can be used as an availability
  // indicator.
  private volatile int reconnectAttempt = 1;
  private SocketChannel channel;
  private int toWrite = 0;
  protected Operation optimizedOp = null;
  private volatile SelectionKey sk = null;
  private boolean shouldAuth = false;
  private CountDownLatch authLatch;
  private ArrayList<Operation> reconnectBlocked;
  private long defaultOpTimeout;

  // operation Future.get timeout counter
  private final AtomicInteger continuousTimeout = new AtomicInteger(0);

  public TCPMemcachedNodeImpl(SocketAddress sa, SocketChannel c, int bufSize,
      BlockingQueue<Operation> rq, BlockingQueue<Operation> wq,
      BlockingQueue<Operation> iq, long opQueueMaxBlockTime,
      boolean waitForAuth, long dt) {
    super();
    assert sa != null : "No SocketAddress";
    assert c != null : "No SocketChannel";
    assert bufSize > 0 : "Invalid buffer size: " + bufSize;
    assert rq != null : "No operation read queue";
    assert wq != null : "No operation write queue";
    assert iq != null : "No input queue";
    socketAddress = sa;
    setChannel(c);
    // Since these buffers are allocated rarely (only on client creation
    // or reconfigure), and are passed to Channel.read() and Channel.write(),
    // use direct buffers to avoid
    //   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6214569
    rbuf = ByteBuffer.allocateDirect(bufSize);
    wbuf = ByteBuffer.allocateDirect(bufSize);
    getWbuf().clear();
    readQ = rq;
    writeQ = wq;
    inputQueue = iq;
    this.opQueueMaxBlockTime = opQueueMaxBlockTime;
    shouldAuth = waitForAuth;
    defaultOpTimeout = dt;
    setupForAuth();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#copyInputQueue()
   */
  public final void copyInputQueue() {
    Collection<Operation> tmp = new ArrayList<Operation>();

    // don't drain more than we have space to place
    inputQueue.drainTo(tmp, writeQ.remainingCapacity());
    writeQ.addAll(tmp);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#destroyInputQueue()
   */
  public Collection<Operation> destroyInputQueue() {
    Collection<Operation> rv = new ArrayList<Operation>();
    inputQueue.drainTo(rv);
    return rv;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#setupResend()
   */
  public final void setupResend() {
    // First, reset the current write op, or cancel it if we should
    // be authenticating
    Operation op = getCurrentWriteOp();
    if (shouldAuth && op != null) {
      op.cancel();
    } else if (op != null) {
      ByteBuffer buf = op.getBuffer();
      if (buf != null) {
        buf.reset();
      } else {
        getLogger().info("No buffer for current write op, removing");
        removeCurrentWriteOp();
      }
    }
    // Now cancel all the pending read operations. Might be better to
    // to requeue them.
    while (hasReadOp()) {
      op = removeCurrentReadOp();
      if (op != getCurrentWriteOp()) {
        getLogger().warn("Discarding partially completed op: %s", op);
        op.cancel();
      }
    }

    while (shouldAuth && hasWriteOp()) {
      op = removeCurrentWriteOp();
      getLogger().warn("Discarding partially completed op: %s", op);
      op.cancel();
    }

    getWbuf().clear();
    getRbuf().clear();
    toWrite = 0;
  }

  // Prepare the pending operations. Return true if there are any pending
  // ops
  private boolean preparePending() {
    // Copy the input queue into the write queue.
    copyInputQueue();

    // Now check the ops
    Operation nextOp = getCurrentWriteOp();
    while (nextOp != null && nextOp.isCancelled()) {
      getLogger().info("Removing cancelled operation: %s", nextOp);
      removeCurrentWriteOp();
      nextOp = getCurrentWriteOp();
    }
    return nextOp != null;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#fillWriteBuffer(boolean)
   */
  public final void fillWriteBuffer(boolean shouldOptimize) {
    if (toWrite == 0 && readQ.remainingCapacity() > 0) {
      getWbuf().clear();
      Operation o=getNextWritableOp();

      while(o != null && toWrite < getWbuf().capacity()) {
        synchronized(o) {
          assert o.getState() == OperationState.WRITING;

          ByteBuffer obuf = o.getBuffer();
          assert obuf != null : "Didn't get a write buffer from " + o;
          int bytesToCopy = Math.min(getWbuf().remaining(), obuf.remaining());
          byte[] b = new byte[bytesToCopy];
          obuf.get(b);
          getWbuf().put(b);
          getLogger().debug("After copying stuff from %s: %s", o, getWbuf());
          if (!o.getBuffer().hasRemaining()) {
            o.writeComplete();
            transitionWriteItem();

            preparePending();
            if (shouldOptimize) {
              optimize();
            }

            o=getNextWritableOp();
          }
          toWrite += bytesToCopy;
        }
      }
      getWbuf().flip();
      assert toWrite <= getWbuf().capacity() : "toWrite exceeded capacity: "
          + this;
      assert toWrite == getWbuf().remaining() : "Expected " + toWrite
          + " remaining, got " + getWbuf().remaining();
    } else {
      getLogger().debug("Buffer is full, skipping");
    }
  }


  private Operation getNextWritableOp() {
    Operation o = getCurrentWriteOp();
    while (o != null && o.getState() == OperationState.WRITE_QUEUED) {
      synchronized(o) {
        if (o.isCancelled()) {
          getLogger().debug("Not writing cancelled op.");
          Operation cancelledOp = removeCurrentWriteOp();
          assert o == cancelledOp;
        } else if (o.isTimedOut(defaultOpTimeout)) {
          getLogger().debug("Not writing timed out op.");
          Operation timedOutOp = removeCurrentWriteOp();
          assert o == timedOutOp;
        } else {
          o.writing();
          if (!(o instanceof TapAckOperationImpl)) {
            readQ.add(o);
          }
          return o;
        }
        o = getCurrentWriteOp();
      }
    }
    return o;
  }

  /* (non-Javadoc)
   * @see net.spy.memcached.MemcachedNode#transitionWriteItem()
   */
  public final void transitionWriteItem() {
    Operation op = removeCurrentWriteOp();
    assert op != null : "There is no write item to transition";
    getLogger().debug("Finished writing %s", op);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#optimize()
   */
  protected abstract void optimize();

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getCurrentReadOp()
   */
  public final Operation getCurrentReadOp() {
    return readQ.peek();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#removeCurrentReadOp()
   */
  public final Operation removeCurrentReadOp() {
    return readQ.remove();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getCurrentWriteOp()
   */
  public final Operation getCurrentWriteOp() {
    return optimizedOp == null ? writeQ.peek() : optimizedOp;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#removeCurrentWriteOp()
   */
  public final Operation removeCurrentWriteOp() {
    Operation rv = optimizedOp;
    if (rv == null) {
      rv = writeQ.remove();
    } else {
      optimizedOp = null;
    }
    return rv;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#hasReadOp()
   */
  public final boolean hasReadOp() {
    return !readQ.isEmpty();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#hasWriteOp()
   */
  public final boolean hasWriteOp() {
    return !(optimizedOp == null && writeQ.isEmpty());
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#addOp(net.spy.memcached.ops.Operation)
   */
  public final void addOp(Operation op) {
    try {
      if (!authLatch.await(1, TimeUnit.SECONDS)) {
        op.cancel();
        getLogger().warn("Operation canceled because authentication "
                + "or reconnection and authentication has "
                + "taken more than one second to complete.");
        getLogger().debug("Canceled operation %s", op.toString());
        return;
      }
      if (!inputQueue.offer(op, opQueueMaxBlockTime, TimeUnit.MILLISECONDS)) {
        throw new IllegalStateException("Timed out waiting to add " + op
            + "(max wait=" + opQueueMaxBlockTime + "ms)");
      }
    } catch (InterruptedException e) {
      // Restore the interrupted status
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to add " + op);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * net.spy.memcached.MemcachedNode#insertOp(net.spy.memcached.ops.Operation)
   */
  public final void insertOp(Operation op) {
    ArrayList<Operation> tmp = new ArrayList<Operation>(inputQueue.size() + 1);
    tmp.add(op);
    inputQueue.drainTo(tmp);
    inputQueue.addAll(tmp);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getSelectionOps()
   */
  public final int getSelectionOps() {
    int rv = 0;
    if (getChannel().isConnected()) {
      if (hasReadOp()) {
        rv |= SelectionKey.OP_READ;
      }
      if (toWrite > 0 || hasWriteOp()) {
        rv |= SelectionKey.OP_WRITE;
      }
    } else {
      rv = SelectionKey.OP_CONNECT;
    }
    return rv;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getRbuf()
   */
  public final ByteBuffer getRbuf() {
    return rbuf;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getWbuf()
   */
  public final ByteBuffer getWbuf() {
    return wbuf;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getSocketAddress()
   */
  public final SocketAddress getSocketAddress() {
    return socketAddress;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#isActive()
   */
  public final boolean isActive() {
    return reconnectAttempt == 0 && getChannel() != null
        && getChannel().isConnected();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#reconnecting()
   */
  public final void reconnecting() {
    reconnectAttempt++;
    continuousTimeout.set(0);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#connected()
   */
  public final void connected() {
    reconnectAttempt = 0;
    continuousTimeout.set(0);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getReconnectCount()
   */
  public final int getReconnectCount() {
    return reconnectAttempt;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#toString()
   */
  @Override
  public final String toString() {
    int sops = 0;
    if (getSk() != null && getSk().isValid()) {
      sops = getSk().interestOps();
    }
    int rsize = readQ.size() + (optimizedOp == null ? 0 : 1);
    int wsize = writeQ.size();
    int isize = inputQueue.size();
    return "{QA sa=" + getSocketAddress() + ", #Rops=" + rsize
        + ", #Wops=" + wsize
        + ", #iq=" + isize
        + ", topRop=" + getCurrentReadOp()
        + ", topWop=" + getCurrentWriteOp()
        + ", toWrite=" + toWrite
        + ", interested=" + sops + "}";
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * net.spy.memcached.MemcachedNode#registerChannel
   * (java.nio.channels.SocketChannel, java.nio.channels.SelectionKey)
   */
  public final void registerChannel(SocketChannel ch, SelectionKey skey) {
    setChannel(ch);
    setSk(skey);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * net.spy.memcached.MemcachedNode#setChannel(java.nio.channels.SocketChannel)
   */
  public final void setChannel(SocketChannel to) {
    assert channel == null || !channel.isOpen()
      : "Attempting to overwrite channel";
    channel = to;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getChannel()
   */
  public final SocketChannel getChannel() {
    return channel;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#setSk(java.nio.channels.SelectionKey)
   */
  public final void setSk(SelectionKey to) {
    sk = to;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getSk()
   */
  public final SelectionKey getSk() {
    return sk;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getBytesRemainingInBuffer()
   */
  public final int getBytesRemainingToWrite() {
    return toWrite;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#writeSome()
   */
  public final int writeSome() throws IOException {
    int wrote = channel.write(wbuf);
    assert wrote >= 0 : "Wrote negative bytes?";
    toWrite -= wrote;
    assert toWrite >= 0 : "toWrite went negative after writing " + wrote
        + " bytes for " + this;
    getLogger().debug("Wrote %d bytes", wrote);
    return wrote;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#setContinuousTimeout
   */
  public void setContinuousTimeout(boolean timedOut) {
    if (timedOut && isActive()) {
      continuousTimeout.incrementAndGet();
    } else {
      continuousTimeout.set(0);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.MemcachedNode#getContinuousTimeout
   */
  public int getContinuousTimeout() {
    return continuousTimeout.get();
  }

  public final void fixupOps() {
    // As the selection key can be changed at any point due to node
    // failure, we'll grab the current volatile value and configure it.
    SelectionKey s = sk;
    if (s != null && s.isValid()) {
      int iops = getSelectionOps();
      getLogger().debug("Setting interested opts to %d", iops);
      s.interestOps(iops);
    } else {
      getLogger().debug("Selection key is not valid.");
    }
  }

  public final void authComplete() {
    if (reconnectBlocked != null && reconnectBlocked.size() > 0) {
      inputQueue.addAll(reconnectBlocked);
    }
    authLatch.countDown();
  }

  public final void setupForAuth() {
    if (shouldAuth) {
      authLatch = new CountDownLatch(1);
      if (inputQueue.size() > 0) {
        reconnectBlocked = new ArrayList<Operation>(inputQueue.size() + 1);
        inputQueue.drainTo(reconnectBlocked);
      }
      assert (inputQueue.size() == 0);
      setupResend();
    } else {
      authLatch = new CountDownLatch(0);
    }
  }
  
  public Map<LocalStatType, String> getLocalStats(){
    Map <LocalStatType, String> localStatMap;
    localStatMap = new HashMap<LocalStatType, String>();
    localStatMap.put(LocalStatType.WRITE_QUEUE_SIZE, String.valueOf(writeQ.size()));
    localStatMap.put(LocalStatType.READ_QUEUE_SIZE, String.valueOf(readQ.size()));
    localStatMap.put(LocalStatType.INPUT_QUEUE_SIZE, String.valueOf(inputQueue.size()));
    
    return localStatMap;
  }
}
