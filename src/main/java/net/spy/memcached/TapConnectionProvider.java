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
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.AuthThreadMonitor;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.transcoders.TranscodeService;

/**
 * A TapConnectionProvider.
 */
public class TapConnectionProvider extends SpyObject implements
    ConnectionObserver {

  protected volatile boolean shuttingDown = false;

  protected final MemcachedConnection conn;

  protected final OperationFactory opFact;

  protected final TranscodeService tcService;

  protected final AuthDescriptor authDescriptor;

  protected final AuthThreadMonitor authMonitor = new AuthThreadMonitor();

  /**
   * Get a tap client operating on the specified memcached locations.
   *
   * @param ia the memcached locations
   * @throws IOException if connections cannot be established
   */
  public TapConnectionProvider(InetSocketAddress... ia) throws IOException {
    this(new BinaryConnectionFactory(), Arrays.asList(ia));
  }

  /**
   * Get a tap client operating on the specified memcached locations.
   *
   * @param addrs the socket addrs
   * @throws IOException if connections cannot be established
   */
  public TapConnectionProvider(List<InetSocketAddress> addrs)
    throws IOException {
    this(new BinaryConnectionFactory(), addrs);
  }

  /**
   * Get a tap client operating on the specified memcached locations.
   *
   * @param cf the connection factory to configure connections for this client
   * @param addrs the socket addresses
   * @throws IOException if connections cannot be established
   */
  public TapConnectionProvider(ConnectionFactory cf,
      List<InetSocketAddress> addrs) throws IOException {
    if (cf == null) {
      throw new NullPointerException("Connection factory required");
    }
    if (addrs == null) {
      throw new NullPointerException("Server list required");
    }
    if (addrs.isEmpty()) {
      throw new IllegalArgumentException(
          "You must have at least one server to connect to");
    }
    if (cf.getOperationTimeout() <= 0) {
      throw new IllegalArgumentException("Operation timeout must be positive.");
    }
    tcService = new TranscodeService(cf.isDaemon());
    cf.getDefaultTranscoder();
    opFact = cf.getOperationFactory();
    assert opFact != null : "Connection factory failed to make op factory";
    conn = cf.createConnection(addrs);
    assert conn != null : "Connection factory failed to make a connection";
    authDescriptor = cf.getAuthDescriptor();
    if (authDescriptor != null) {
      addObserver(this);
    }
  }

  public void addTapAckOp(MemcachedNode node, final Operation op) {
    conn.addOperation(node, op);
  }

  public CountDownLatch broadcastOp(final BroadcastOpFactory of) {
    if (shuttingDown) {
      throw new IllegalStateException("Shutting down");
    }
    return conn.broadcastOperation(of, conn.getLocator().getAll());
  }

  /**
   * Add a connection observer.
   *
   * If connections are already established, your observer will be called with
   * the address and -1.
   *
   * @param obs the ConnectionObserver you wish to add
   * @return true if the observer was added.
   */
  public boolean addObserver(ConnectionObserver obs) {
    boolean rv = conn.addObserver(obs);
    if (rv) {
      for (MemcachedNode node : conn.getLocator().getAll()) {
        if (node.isActive()) {
          obs.connectionEstablished(node.getSocketAddress(), -1);
        }
      }
    }
    return rv;
  }

  /**
   * Remove a connection observer.
   *
   * @param obs the ConnectionObserver you wish to add
   * @return true if the observer existed, but no longer does
   */
  public boolean removeObserver(ConnectionObserver obs) {
    return conn.removeObserver(obs);
  }

  public void connectionEstablished(SocketAddress sa, int reconnectCount) {
    if (authDescriptor != null) {
      if (authDescriptor.authThresholdReached()) {
        this.shutdown();
      } else {
        authMonitor.authConnection(conn, opFact, authDescriptor, findNode(sa));
      }
    }
  }

  private MemcachedNode findNode(SocketAddress sa) {
    MemcachedNode node = null;
    for (MemcachedNode n : conn.getLocator().getAll()) {
      if (n.getSocketAddress().equals(sa)) {
        node = n;
      }
    }
    assert node != null : "Couldn't find node connected to " + sa;
    return node;
  }

  public void connectionLost(SocketAddress sa) {
    // Don't care.
  }

  /**
   * Shut down immediately.
   */
  public void shutdown() {
    shutdown(-1, TimeUnit.MILLISECONDS);
  }

  /**
   * Shut down this client gracefully.
   *
   * @param timeout the amount of time for shutdown
   * @param unit the TimeUnit for the timeout
   * @return result of the shutdown request
   */
  public boolean shutdown(long timeout, TimeUnit unit) {
    // Guard against double shutdowns (bug 8).
    if (shuttingDown) {
      getLogger().info("Suppressing duplicate attempt to shut down");
      return false;
    }
    shuttingDown = true;
    String baseName = conn.getName();
    conn.setName(baseName + " - SHUTTING DOWN");
    boolean rv = false;
    try {
      // Conditionally wait
      if (timeout > 0) {
        conn.setName(baseName + " - SHUTTING DOWN (waiting)");
        rv = waitForQueues(timeout, unit);
      }
    } finally {
      // But always begin the shutdown sequence
      try {
        conn.setName(baseName + " - SHUTTING DOWN (telling client)");
        conn.shutdown();
        conn.setName(baseName + " - SHUTTING DOWN (informed client)");
        tcService.shutdown();
      } catch (IOException e) {
        getLogger().warn("exception while shutting down", e);
      }
    }
    return rv;
  }

  /**
   * Wait for the queues to die down.
   *
   * @param timeout the amount of time time for shutdown
   * @param unit the TimeUnit for the timeout
   * @return result of the request for the wait
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public boolean waitForQueues(long timeout, TimeUnit unit) {
    CountDownLatch blatch = broadcastOp(new BroadcastOpFactory() {
      public Operation newOp(final MemcachedNode n,
          final CountDownLatch latch) {
        return opFact.noop(new OperationCallback() {
          public void complete() {
            latch.countDown();
          }

          public void receivedStatus(OperationStatus s) {
            // Nothing special when receiving status, only
            // necessary to complete the interface
          }
        });
      }
    }, conn.getLocator().getAll(), false);
    try {
      // XXX: Perhaps IllegalStateException should be caught here
      // and the check retried.
      return blatch.await(timeout, unit);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for queues", e);
    }
  }

  private CountDownLatch broadcastOp(BroadcastOpFactory of,
      Collection<MemcachedNode> nodes, boolean checkShuttingDown) {
    if (checkShuttingDown && shuttingDown) {
      throw new IllegalStateException("Shutting down");
    }
    return conn.broadcastOperation(of, nodes);
  }

  public OperationFactory getOpFactory() {
    return opFact;
  }
}
