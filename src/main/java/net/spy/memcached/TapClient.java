/**
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
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.naming.ConfigurationException;

import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.ResponseMessage;
import net.spy.memcached.tapmessage.TapOpcode;

/**
 * A tap client for memcached.
 */
public class TapClient {
  protected BlockingQueue<Object> rqueue;
  protected HashMap<Operation, TapConnectionProvider> omap;
  protected long messagesRead;
  private List<InetSocketAddress> addrs;

  /**
   * Creates a tap client against the specified servers.
   *
   * This type of TapClient will TAP the specified servers, but will not be able
   * to react to changes in the number of cluster nodes. Using the constructor
   * which bootstraps itself from the cluster REST interface is preferred.
   *
   * @param ia the addresses of each node in the cluster.
   */
  public TapClient(InetSocketAddress... ia) {
    this(Arrays.asList(ia));
  }

  /**
   * Creates a tap client against the specified servers.
   *
   * This type of TapClient will TAP the specified servers, but will not be able
   * to react to changes in the number of cluster nodes. Using the constructor
   * which bootstraps itself from the cluster REST interface is preferred.
   *
   * @param addrs a list of addresses containing each node in the cluster.
   */
  public TapClient(List<InetSocketAddress> addrs) {
    this.rqueue = new LinkedBlockingQueue<Object>();
    this.omap = new HashMap<Operation, TapConnectionProvider>();
    this.addrs = addrs;
    this.messagesRead = 0;
  }

  /**
   * Gets the next tap message from the queue of received tap messages.
   *
   * @return The tap message at the head of the queue or null if the queue is
   *         empty for more than one second.
   */
  public ResponseMessage getNextMessage() {
    return getNextMessage(1, TimeUnit.SECONDS);
  }

  /**
   * Gets the next tap message from the queue of received tap messages.
   *
   * @param time the amount of time to wait for a message.
   * @param timeunit the unit of time to use.
   * @return The tap message at the head of the queue or null if the queue is
   *         empty for the given amount of time.
   */
  public ResponseMessage getNextMessage(long time, TimeUnit timeunit) {
    try {
      Object m = rqueue.poll(time, timeunit);
      if (m == null) {
        return null;
      } else if (m instanceof ResponseMessage) {
        return (ResponseMessage) m;
      } else if (m instanceof TapAck) {
        TapAck ack = (TapAck) m;
        tapAck(ack.getConn(), ack.getOpcode(), ack.getOpaque(),
            ack.getCallback());
        return null;
      } else {
        throw new RuntimeException("Unexpected tap message type");
      }
    } catch (InterruptedException e) {
      shutdown();
      return null;
    }
  }

  /**
   * Decides whether the client has received tap messages or will receive more
   * messages in the future.
   *
   * @return true if the client has tap responses or expects to have responses
   *         in the future. False otherwise.
   */
  public boolean hasMoreMessages() {
    if (!rqueue.isEmpty()) {
      return true;
    } else {
      synchronized (omap) {
        Iterator<Operation> itr = omap.keySet().iterator();
        while (itr.hasNext()) {
          Operation op = itr.next();
          if (op.getState().equals(OperationState.COMPLETE) || op.isCancelled()
              || op.hasErrored()) {
            omap.get(op).shutdown();
            omap.remove(op);
          }
        }
        if (omap.size() > 0) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Allows the user to specify a custom tap message.
   *
   * @param id the named tap id that can be used to resume a disconnected tap
   *          stream
   * @param message the custom tap message that will be used to initiate the tap
   *          stream.
   * @return the operation that controls the tap stream.
   * @throws ConfigurationException a bad configuration was received from the
   *           memcached cluster.
   * @throws IOException if there are errors connecting to the cluster.
   */
  public Operation tapCustom(String id, RequestMessage message)
    throws ConfigurationException, IOException {
    final TapConnectionProvider conn = new TapConnectionProvider(addrs);
    final CountDownLatch latch = new CountDownLatch(1);
    final Operation op = conn.getOpFactory().tapCustom(id, message,
        new TapOperation.Callback() {
          public void receivedStatus(OperationStatus status) {
          }

          public void gotData(ResponseMessage tapMessage) {
            rqueue.add(tapMessage);
            messagesRead++;
          }

          public void gotAck(TapOpcode opcode, int opaque) {
            rqueue.add(new TapAck(conn, opcode, opaque, this));
          }

          public void complete() {
            latch.countDown();
          }
        });
    synchronized (omap) {
      omap.put(op, conn);
    }
    conn.addOp(op);
    return op;
  }

  /**
   * Specifies a tap stream that will take a snapshot of items in memcached and
   * send them through a tap stream.
   *
   * @param id the named tap id that can be used to resume a disconnected tap
   *          stream
   * @return the operation that controls the tap stream.
   * @throws ConfigurationException a bad configuration was received from the
   *           memcached cluster.
   * @throws IOException If there are errors connecting to the cluster.
   */
  public Operation tapDump(final String id) throws IOException,
      ConfigurationException {
    final TapConnectionProvider conn = new TapConnectionProvider(addrs);
    final CountDownLatch latch = new CountDownLatch(1);
    final Operation op = conn.getOpFactory().tapDump(id,
        new TapOperation.Callback() {
        public void receivedStatus(OperationStatus status) {
        }
        public void gotData(ResponseMessage tapMessage) {
          rqueue.add(tapMessage);
          messagesRead++;
        }
        public void gotAck(TapOpcode opcode, int opaque) {
          rqueue.add(new TapAck(conn, opcode, opaque, this));
        }
        public void complete() {
          latch.countDown();
        }
      });
    synchronized (omap) {
      omap.put(op, conn);
    }
    conn.addOp(op);
    return op;
  }

  private void tapAck(TapConnectionProvider conn, TapOpcode opcode, int opaque,
      OperationCallback cb) {
    final Operation op = conn.getOpFactory().tapAck(opcode, opaque, cb);
    conn.addOp(op);
  }

  /**
   * Shuts down all tap streams that are currently running.
   */
  public void shutdown() {
    synchronized (omap) {
      for (Map.Entry<Operation, TapConnectionProvider> me : omap.entrySet()) {
        me.getValue().shutdown();
      }
    }
  }

  /**
   * The number of messages read by all of the tap streams created with this
   * client. This will include a count of all tap response types.
   *
   * @return The number of messages read
   */
  public long getMessagesRead() {
    return messagesRead;
  }

  static class TapAck {
    private TapConnectionProvider conn;
    private TapOpcode opcode;
    private int opaque;
    private OperationCallback cb;

    public TapAck(TapConnectionProvider conn, TapOpcode opcode, int opaque,
        OperationCallback cb) {
      this.conn = conn;
      this.opcode = opcode;
      this.opaque = opaque;
      this.cb = cb;
    }

    public TapConnectionProvider getConn() {
      return conn;
    }

    public TapOpcode getOpcode() {
      return opcode;
    }

    public int getOpaque() {
      return opaque;
    }

    public OperationCallback getCallback() {
      return cb;
    }
  }
}

