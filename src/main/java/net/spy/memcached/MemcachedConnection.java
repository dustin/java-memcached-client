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
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import net.spy.memcached.compat.SpyThread;
import net.spy.memcached.compat.log.LoggerFactory;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.ops.VBucketAware;
import net.spy.memcached.protocol.binary.TapAckOperationImpl;
import net.spy.memcached.util.StringUtils;

/**
 * Connection to a cluster of memcached servers.
 */
public class MemcachedConnection extends SpyThread {

  // The number of empty selects we'll allow before assuming we may have
  // missed one and should check the current selectors. This generally
  // indicates a bug, but we'll check it nonetheless.
  private static final int DOUBLE_CHECK_EMPTY = 256;
  // The number of empty selects we'll allow before blowing up. It's too
  // easy to write a bug that causes it to loop uncontrollably. This helps
  // find those bugs and often works around them.
  private static final int EXCESSIVE_EMPTY = 0x1000000;

  protected volatile boolean shutDown = false;
  // If true, optimization will collapse multiple sequential get ops
  private final boolean shouldOptimize;
  protected Selector selector = null;
  protected final NodeLocator locator;
  protected final FailureMode failureMode;
  // maximum amount of time to wait between reconnect attempts
  private final long maxDelay;
  private int emptySelects = 0;
  private final int bufSize;
  private final ConnectionFactory connectionFactory;
  // AddedQueue is used to track the QueueAttachments for which operations
  // have recently been queued.
  protected final ConcurrentLinkedQueue<MemcachedNode> addedQueue;
  // reconnectQueue contains the attachments that need to be reconnected
  // The key is the time at which they are eligible for reconnect
  private final SortedMap<Long, MemcachedNode> reconnectQueue;

  protected volatile boolean running = true;

  private final Collection<ConnectionObserver> connObservers =
      new ConcurrentLinkedQueue<ConnectionObserver>();
  private final OperationFactory opFact;
  private final int timeoutExceptionThreshold;
  private final Collection<Operation> retryOps;
  protected final ConcurrentLinkedQueue<MemcachedNode> nodesToShutdown;

  /**
   * Construct a memcached connection.
   *
   * @param bufSize the size of the buffer used for reading from the server
   * @param f the factory that will provide an operation queue
   * @param a the addresses of the servers to connect to
   *
   * @throws IOException if a connection attempt fails early
   */
  public MemcachedConnection(int bufSize, ConnectionFactory f,
      List<InetSocketAddress> a, Collection<ConnectionObserver> obs,
      FailureMode fm, OperationFactory opfactory) throws IOException {
    connObservers.addAll(obs);
    reconnectQueue = new TreeMap<Long, MemcachedNode>();
    addedQueue = new ConcurrentLinkedQueue<MemcachedNode>();
    failureMode = fm;
    shouldOptimize = f.shouldOptimize();
    maxDelay = f.getMaxReconnectDelay();
    opFact = opfactory;
    timeoutExceptionThreshold = f.getTimeoutExceptionThreshold();
    selector = Selector.open();
    retryOps = new ArrayList<Operation>();
    nodesToShutdown = new ConcurrentLinkedQueue<MemcachedNode>();
    this.bufSize = bufSize;
    this.connectionFactory = f;
    List<MemcachedNode> connections = createConnections(a);
    locator = f.createLocator(connections);
    setName("Memcached IO over " + this);
    setDaemon(f.isDaemon());
    start();
  }

  protected List<MemcachedNode> createConnections(
      final Collection<InetSocketAddress> a) throws IOException {
    List<MemcachedNode> connections = new ArrayList<MemcachedNode>(a.size());
    for (SocketAddress sa : a) {
      SocketChannel ch = SocketChannel.open();
      ch.configureBlocking(false);
      MemcachedNode qa =
          this.connectionFactory.createMemcachedNode(sa, ch, bufSize);
      int ops = 0;
      ch.socket().setTcpNoDelay(!this.connectionFactory.useNagleAlgorithm());
      // Initially I had attempted to skirt this by queueing every
      // connect, but it considerably slowed down start time.
      try {
        if (ch.connect(sa)) {
          getLogger().info("Connected to %s immediately", qa);
          connected(qa);
        } else {
          getLogger().info("Added %s to connect queue", qa);
          ops = SelectionKey.OP_CONNECT;
        }
        qa.setSk(ch.register(selector, ops, qa));
        assert ch.isConnected()
            || qa.getSk().interestOps() == SelectionKey.OP_CONNECT
            : "Not connected, and not wanting to connect";
      } catch (SocketException e) {
        getLogger().warn("Socket error on initial connect", e);
        queueReconnect(qa);
      }
      connections.add(qa);
    }
    return connections;
  }

  private boolean selectorsMakeSense() {
    for (MemcachedNode qa : locator.getAll()) {
      if (qa.getSk() != null && qa.getSk().isValid()) {
        if (qa.getChannel().isConnected()) {
          int sops = qa.getSk().interestOps();
          int expected = 0;
          if (qa.hasReadOp()) {
            expected |= SelectionKey.OP_READ;
          }
          if (qa.hasWriteOp()) {
            expected |= SelectionKey.OP_WRITE;
          }
          if (qa.getBytesRemainingToWrite() > 0) {
            expected |= SelectionKey.OP_WRITE;
          }
          assert sops == expected : "Invalid ops:  " + qa + ", expected "
              + expected + ", got " + sops;
        } else {
          int sops = qa.getSk().interestOps();
          assert sops == SelectionKey.OP_CONNECT
            : "Not connected, and not watching for connect: " + sops;
        }
      }
    }
    getLogger().debug("Checked the selectors.");
    return true;
  }

  /**
   * MemcachedClient calls this method to handle IO over the connections.
   */
  public void handleIO() throws IOException {
    if (shutDown) {
      throw new IOException("No IO while shut down");
    }

    // Deal with all of the stuff that's been added, but may not be marked
    // writable.
    handleInputQueue();
    getLogger().debug("Done dealing with queue.");

    long delay = 0;
    if (!reconnectQueue.isEmpty()) {
      long now = System.currentTimeMillis();
      long then = reconnectQueue.firstKey();
      delay = Math.max(then - now, 1);
    }
    getLogger().debug("Selecting with delay of %sms", delay);
    assert selectorsMakeSense() : "Selectors don't make sense.";
    int selected = selector.select(delay);
    Set<SelectionKey> selectedKeys = selector.selectedKeys();

    if (selectedKeys.isEmpty() && !shutDown) {
      getLogger().debug("No selectors ready, interrupted: "
          + Thread.interrupted());
      if (++emptySelects > DOUBLE_CHECK_EMPTY) {
        for (SelectionKey sk : selector.keys()) {
          getLogger().debug("%s has %s, interested in %s", sk, sk.readyOps(),
              sk.interestOps());
          if (sk.readyOps() != 0) {
            getLogger().debug("%s has a ready op, handling IO", sk);
            handleIO(sk);
          } else {
            lostConnection((MemcachedNode) sk.attachment());
          }
        }
        assert emptySelects < EXCESSIVE_EMPTY : "Too many empty selects";
      }
    } else {
      getLogger().debug("Selected %d, selected %d keys", selected,
          selectedKeys.size());
      emptySelects = 0;

      for (SelectionKey sk : selectedKeys) {
        handleIO(sk);
      }
      selectedKeys.clear();
    }

    // see if any connections blew up with large number of timeouts
    for (SelectionKey sk : selector.keys()) {
      MemcachedNode mn = (MemcachedNode) sk.attachment();
      if (mn.getContinuousTimeout() > timeoutExceptionThreshold) {
        getLogger().warn("%s exceeded continuous timeout threshold", sk);
        lostConnection(mn);
      }
    }

    if (!shutDown && !reconnectQueue.isEmpty()) {
      attemptReconnects();
    }
    // rehash any operations that are in retry state
    redistributeOperations(retryOps);
    retryOps.clear();

    // try to shutdown odd nodes
    for (MemcachedNode qa : nodesToShutdown) {
      if (!addedQueue.contains(qa)) {
        nodesToShutdown.remove(qa);
        Collection<Operation> notCompletedOperations = qa.destroyInputQueue();
        if (qa.getChannel() != null) {
          qa.getChannel().close();
          qa.setSk(null);
          if (qa.getBytesRemainingToWrite() > 0) {
            getLogger().warn("Shut down with %d bytes remaining to write",
                qa.getBytesRemainingToWrite());
          }
          getLogger().debug("Shut down channel %s", qa.getChannel());
        }
        redistributeOperations(notCompletedOperations);
      }
    }
  }

  // Handle any requests that have been made against the client.
  private void handleInputQueue() {
    if (!addedQueue.isEmpty()) {
      getLogger().debug("Handling queue");
      // If there's stuff in the added queue. Try to process it.
      Collection<MemcachedNode> toAdd = new HashSet<MemcachedNode>();
      // Transfer the queue into a hashset. There are very likely more
      // additions than there are nodes.
      Collection<MemcachedNode> todo = new HashSet<MemcachedNode>();
      MemcachedNode qaNode = null;
      while ((qaNode = addedQueue.poll()) != null) {
        todo.add(qaNode);
      }

      // Now process the queue.
      for (MemcachedNode qa : todo) {
        boolean readyForIO = false;
        if (qa.isActive()) {
          if (qa.getCurrentWriteOp() != null) {
            readyForIO = true;
            getLogger().debug("Handling queued write %s", qa);
          }
        } else {
          toAdd.add(qa);
        }
        qa.copyInputQueue();
        if (readyForIO) {
          try {
            if (qa.getWbuf().hasRemaining()) {
              handleWrites(qa.getSk(), qa);
            }
          } catch (IOException e) {
            getLogger().warn("Exception handling write", e);
            lostConnection(qa);
          }
        }
        qa.fixupOps();
      }
      addedQueue.addAll(toAdd);
    }
  }

  /**
   * Add a connection observer.
   *
   * @return whether the observer was successfully added
   */
  public boolean addObserver(ConnectionObserver obs) {
    return connObservers.add(obs);
  }

  /**
   * Remove a connection observer.
   *
   * @return true if the observer existed and now doesn't
   */
  public boolean removeObserver(ConnectionObserver obs) {
    return connObservers.remove(obs);
  }

  private void connected(MemcachedNode qa) {
    assert qa.getChannel().isConnected() : "Not connected.";
    int rt = qa.getReconnectCount();
    qa.connected();
    for (ConnectionObserver observer : connObservers) {
      observer.connectionEstablished(qa.getSocketAddress(), rt);
    }
  }

  private void lostConnection(MemcachedNode qa) {
    queueReconnect(qa);
    for (ConnectionObserver observer : connObservers) {
      observer.connectionLost(qa.getSocketAddress());
    }
  }

  // Handle IO for a specific selector. Any IOException will cause a
  // reconnect
  private void handleIO(SelectionKey sk) {
    MemcachedNode qa = (MemcachedNode) sk.attachment();
    try {
      getLogger().debug("Handling IO for:  %s (r=%s, w=%s, c=%s, op=%s)", sk,
              sk.isReadable(), sk.isWritable(), sk.isConnectable(),
              sk.attachment());
      if (sk.isConnectable()) {
        getLogger().info("Connection state changed for %s", sk);
        final SocketChannel channel = qa.getChannel();
        if (channel.finishConnect()) {
          connected(qa);
          addedQueue.offer(qa);
          if (qa.getWbuf().hasRemaining()) {
            handleWrites(sk, qa);
          }
        } else {
          assert !channel.isConnected() : "connected";
        }
      } else {
        if (sk.isValid() && sk.isReadable()) {
          handleReads(sk, qa);
        }
        if (sk.isValid() && sk.isWritable()) {
          handleWrites(sk, qa);
        }
      }
    } catch (ClosedChannelException e) {
      // Note, not all channel closes end up here
      if (!shutDown) {
        getLogger().info("Closed channel and not shutting down. Queueing"
            + " reconnect on %s", qa, e);
        lostConnection(qa);
      }
    } catch (ConnectException e) {
      // Failures to establish a connection should attempt a reconnect
      // without signaling the observers.
      getLogger().info("Reconnecting due to failure to connect to %s", qa, e);
      queueReconnect(qa);
    } catch (OperationException e) {
      qa.setupForAuth(); // noop if !shouldAuth
      getLogger().info("Reconnection due to exception handling a memcached "
          + "operation on %s. This may be due to an authentication failure.",
          qa, e);
      lostConnection(qa);
    } catch (Exception e) {
      // Any particular error processing an item should simply
      // cause us to reconnect to the server.
      //
      // One cause is just network oddness or servers
      // restarting, which lead here with IOException
      qa.setupForAuth(); // noop if !shouldAuth
      getLogger().info("Reconnecting due to exception on %s", qa, e);
      lostConnection(qa);
    }
    qa.fixupOps();
  }

  private void handleWrites(SelectionKey sk, MemcachedNode qa)
    throws IOException {
    qa.fillWriteBuffer(shouldOptimize);
    boolean canWriteMore = qa.getBytesRemainingToWrite() > 0;
    while (canWriteMore) {
      int wrote = qa.writeSome();
      qa.fillWriteBuffer(shouldOptimize);
      canWriteMore = wrote > 0 && qa.getBytesRemainingToWrite() > 0;
    }
  }

  private void handleReads(SelectionKey sk, MemcachedNode qa)
    throws IOException {
    Operation currentOp = qa.getCurrentReadOp();
    // If it's a tap ack there is no response
    if (currentOp instanceof TapAckOperationImpl) {
      qa.removeCurrentReadOp();
      return;
    }
    ByteBuffer rbuf = qa.getRbuf();
    final SocketChannel channel = qa.getChannel();
    int read = channel.read(rbuf);
    if (read < 0) {
      if (currentOp instanceof TapOperation) {
        // If were doing tap then we won't throw an exception
        currentOp.getCallback().complete();
        ((TapOperation) currentOp).streamClosed(OperationState.COMPLETE);
        getLogger().debug("Completed read op: %s and giving the next %d bytes",
            currentOp, rbuf.remaining());
        Operation op = qa.removeCurrentReadOp();
        assert op == currentOp : "Expected to pop " + currentOp + " got " + op;
        currentOp = qa.getCurrentReadOp();
      } else {
        // our model is to keep the connection alive for future ops
        // so we'll queue a reconnect if disconnected via an IOException
        throw new IOException("Disconnected unexpected, will reconnect.");
      }
    }
    while (read > 0) {
      getLogger().debug("Read %d bytes", read);
      rbuf.flip();
      while (rbuf.remaining() > 0) {
        if (currentOp == null) {
          throw new IllegalStateException("No read operation.");
        }
        synchronized(currentOp) {
          currentOp.readFromBuffer(rbuf);
          if (currentOp.getState() == OperationState.COMPLETE) {
            getLogger().debug("Completed read op: %s and giving the next %d "
                + "bytes", currentOp, rbuf.remaining());
            Operation op = qa.removeCurrentReadOp();
            assert op == currentOp : "Expected to pop " + currentOp + " got "
                + op;
          } else if (currentOp.getState() == OperationState.RETRY) {
            getLogger().debug("Reschedule read op due to NOT_MY_VBUCKET error: "
                + "%s ", currentOp);
            ((VBucketAware) currentOp).addNotMyVbucketNode(
                currentOp.getHandlingNode());
            Operation op = qa.removeCurrentReadOp();
            assert op == currentOp : "Expected to pop " + currentOp + " got "
                + op;
            retryOps.add(currentOp);
          }
        }
        currentOp=qa.getCurrentReadOp();
      }
      rbuf.clear();
      read = channel.read(rbuf);
    }
  }

  // Make a debug string out of the given buffer's values
  static String dbgBuffer(ByteBuffer b, int size) {
    StringBuilder sb = new StringBuilder();
    byte[] bytes = b.array();
    for (int i = 0; i < size; i++) {
      char ch = (char) bytes[i];
      if (Character.isWhitespace(ch) || Character.isLetterOrDigit(ch)) {
        sb.append(ch);
      } else {
        sb.append("\\x");
        sb.append(Integer.toHexString(bytes[i] & 0xff));
      }
    }
    return sb.toString();
  }

  protected void queueReconnect(MemcachedNode qa) {
    if (!shutDown) {
      getLogger().warn("Closing, and reopening %s, attempt %d.", qa,
          qa.getReconnectCount());
      if (qa.getSk() != null) {
        qa.getSk().cancel();
        assert !qa.getSk().isValid() : "Cancelled selection key is valid";
      }
      qa.reconnecting();
      try {
        if (qa.getChannel() != null && qa.getChannel().socket() != null) {
          qa.getChannel().socket().close();
        } else {
          getLogger().info("The channel or socket was null for %s", qa);
        }
      } catch (IOException e) {
        getLogger().warn("IOException trying to close a socket", e);
      }
      qa.setChannel(null);

      long delay = (long) Math.min(maxDelay, Math.pow(2,
          qa.getReconnectCount())) * 1000;
      long reconTime = System.currentTimeMillis() + delay;

      // Avoid potential condition where two connections are scheduled
      // for reconnect at the exact same time. This is expected to be
      // a rare situation.
      while (reconnectQueue.containsKey(reconTime)) {
        reconTime++;
      }

      reconnectQueue.put(reconTime, qa);

      // Need to do a little queue management.
      qa.setupResend();

      if (failureMode == FailureMode.Redistribute) {
        redistributeOperations(qa.destroyInputQueue());
      } else if (failureMode == FailureMode.Cancel) {
        cancelOperations(qa.destroyInputQueue());
      }
    }
  }

  private void cancelOperations(Collection<Operation> ops) {
    for (Operation op : ops) {
      op.cancel();
    }
  }

  private void redistributeOperations(Collection<Operation> ops) {
    for (Operation op : ops) {
      if (op.isCancelled() || op.isTimedOut()) {
        continue;
      }
      if (op instanceof KeyedOperation) {
        KeyedOperation ko = (KeyedOperation) op;
        int added = 0;
        for (String k : ko.getKeys()) {
          for (Operation newop : opFact.clone(ko)) {
            addOperation(k, newop);
            added++;
          }
        }
        assert added > 0 : "Didn't add any new operations when redistributing";
      } else {
        // Cancel things that don't have definite targets.
        op.cancel();
      }
    }
  }

  private void attemptReconnects() throws IOException {
    final long now = System.currentTimeMillis();
    final Map<MemcachedNode, Boolean> seen =
        new IdentityHashMap<MemcachedNode, Boolean>();
    final List<MemcachedNode> rereQueue = new ArrayList<MemcachedNode>();
    SocketChannel ch = null;
    for (Iterator<MemcachedNode> i =
        reconnectQueue.headMap(now).values().iterator(); i.hasNext();) {
      final MemcachedNode qa = i.next();
      i.remove();
      try {
        if (!seen.containsKey(qa)) {
          seen.put(qa, Boolean.TRUE);
          getLogger().info("Reconnecting %s", qa);
          ch = SocketChannel.open();
          ch.configureBlocking(false);
          int ops = 0;
          if (ch.connect(qa.getSocketAddress())) {
            connected(qa);
            addedQueue.offer(qa);
            getLogger().info("Immediately reconnected to %s", qa);
            assert ch.isConnected();
          } else {
            ops = SelectionKey.OP_CONNECT;
          }
          qa.registerChannel(ch, ch.register(selector, ops, qa));
          assert qa.getChannel() == ch : "Channel was lost.";
        } else {
          getLogger().debug("Skipping duplicate reconnect request for %s", qa);
        }
      } catch (SocketException e) {
        getLogger().warn("Error on reconnect", e);
        rereQueue.add(qa);
      } catch (Exception e) {
        getLogger().error("Exception on reconnect, lost node %s", qa, e);
      } finally {
        // it's possible that above code will leak file descriptors under
        // abnormal
        // conditions (when ch.open() fails and throws IOException.
        // always close non connected channel
        if (ch != null && !ch.isConnected() && !ch.isConnectionPending()) {
          try {
            ch.close();
          } catch (IOException x) {
            getLogger().error("Exception closing channel: %s", qa, x);
          }
        }
      }
    }
    // Requeue any fast-failed connects.
    for (MemcachedNode n : rereQueue) {
      queueReconnect(n);
    }
  }

  /**
   * Get the node locator used by this connection.
   */
  public NodeLocator getLocator() {
    return locator;
  }

  public void enqueueOperation(String key, Operation o) {
    StringUtils.validateKey(key);
    checkState();
    addOperation(key, o);
  }

  /**
   * Add an operation to the given connection.
   *
   * @param key the key the operation is operating upon
   * @param o the operation
   */
  protected void addOperation(final String key, final Operation o) {

    MemcachedNode placeIn = null;
    MemcachedNode primary = locator.getPrimary(key);
    if (primary.isActive() || failureMode == FailureMode.Retry) {
      placeIn = primary;
    } else if (failureMode == FailureMode.Cancel) {
      o.cancel();
    } else {
      // Look for another node in sequence that is ready.
      for (Iterator<MemcachedNode> i = locator.getSequence(key); placeIn == null
          && i.hasNext();) {
        MemcachedNode n = i.next();
        if (n.isActive()) {
          placeIn = n;
        }
      }
      // If we didn't find an active node, queue it in the primary node
      // and wait for it to come back online.
      if (placeIn == null) {
        placeIn = primary;
        this.getLogger().warn(
            "Could not redistribute "
                + "to another node, retrying primary node for %s.", key);
      }
    }

    assert o.isCancelled() || placeIn != null : "No node found for key " + key;
    if (placeIn != null) {
      addOperation(placeIn, o);
    } else {
      assert o.isCancelled() : "No node found for " + key
          + " (and not immediately cancelled)";
    }
  }

  public void insertOperation(final MemcachedNode node, final Operation o) {
    o.setHandlingNode(node);
    o.initialize();
    node.insertOp(o);
    addedQueue.offer(node);
    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    getLogger().debug("Added %s to %s", o, node);
  }

  protected void addOperation(final MemcachedNode node, final Operation o) {
    o.setHandlingNode(node);
    o.initialize();
    node.addOp(o);
    addedQueue.offer(node);
    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    getLogger().debug("Added %s to %s", o, node);
  }

  public void addOperations(final Map<MemcachedNode, Operation> ops) {

    for (Map.Entry<MemcachedNode, Operation> me : ops.entrySet()) {
      final MemcachedNode node = me.getKey();
      Operation o = me.getValue();
      o.setHandlingNode(node);
      o.initialize();
      node.addOp(o);
      addedQueue.offer(node);
    }
    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
  }

  /**
   * Broadcast an operation to all nodes.
   */
  public CountDownLatch broadcastOperation(BroadcastOpFactory of) {
    return broadcastOperation(of, locator.getAll());
  }

  /**
   * Broadcast an operation to a specific collection of nodes.
   */
  public CountDownLatch broadcastOperation(final BroadcastOpFactory of,
      Collection<MemcachedNode> nodes) {
    final CountDownLatch latch = new CountDownLatch(locator.getAll().size());
    for (MemcachedNode node : nodes) {
      getLogger().debug("broadcast Operation: node = " + node);
      Operation op = of.newOp(node, latch);
      op.initialize();
      node.addOp(op);
      op.setHandlingNode(node);
      addedQueue.offer(node);
    }
    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    return latch;
  }

  /**
   * Shut down all of the connections.
   */
  public void shutdown() throws IOException {
    shutDown = true;
    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    for (MemcachedNode qa : locator.getAll()) {
      if (qa.getChannel() != null) {
        qa.getChannel().close();
        qa.setSk(null);
        if (qa.getBytesRemainingToWrite() > 0) {
          getLogger().warn("Shut down with %d bytes remaining to write",
              qa.getBytesRemainingToWrite());
        }
        getLogger().debug("Shut down channel %s", qa.getChannel());
      }
    }
    running = false;
    selector.close();
    getLogger().debug("Shut down selector %s", selector);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{MemcachedConnection to");
    for (MemcachedNode qa : locator.getAll()) {
      sb.append(" ");
      sb.append(qa.getSocketAddress());
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * helper method: increase timeout count on node attached to this op.
   *
   * @param op
   */
  public static void opTimedOut(Operation op) {
    MemcachedConnection.setTimeout(op, true);
  }

  /**
   * helper method: reset timeout counter.
   *
   * @param op
   */
  public static void opSucceeded(Operation op) {
    MemcachedConnection.setTimeout(op, false);
  }

  /**
   * helper method: do some error checking and set timeout boolean.
   *
   * @param op
   * @param isTimeout
   */
  private static void setTimeout(Operation op, boolean isTimeout) {
    try {
      if (op == null || op.isTimedOutUnsent()) {
        return; // op may be null in some cases, e.g. flush
      }
      MemcachedNode node = op.getHandlingNode();
      if (node == null) {
        LoggerFactory.getLogger(MemcachedConnection.class).warn(
            "handling node for operation is not set");
      } else {
        node.setContinuousTimeout(isTimeout);
      }
    } catch (Exception e) {
      LoggerFactory.getLogger(MemcachedConnection.class).error(e.getMessage());
    }
  }

  protected void checkState() {
    if (shutDown) {
      throw new IllegalStateException("Shutting down");
    }
    assert isAlive() : "IO Thread is not running.";
  }

  /**
   * Infinitely loop processing IO.
   */
  @Override
  public void run() {
    while (running) {
      try {
        handleIO();
      } catch (IOException e) {
        logRunException(e);
      } catch (CancelledKeyException e) {
        logRunException(e);
      } catch (ClosedSelectorException e) {
        logRunException(e);
      } catch (IllegalStateException e) {
        logRunException(e);
      }
    }
    getLogger().info("Shut down memcached client");
  }

  private void logRunException(Exception e) {
    if (shutDown) {
      // There are a couple types of errors that occur during the
      // shutdown sequence that are considered OK. Log at debug.
      getLogger().debug("Exception occurred during shutdown", e);
    } else {
      getLogger().warn("Problem handling memcached IO", e);
    }
  }
}
