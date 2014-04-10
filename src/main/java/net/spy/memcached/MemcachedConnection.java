/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2013 Couchbase, Inc.
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.compat.SpyThread;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.metrics.MetricCollector;
import net.spy.memcached.metrics.MetricType;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.NoopOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.ops.VBucketAware;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.protocol.binary.MultiGetOperationImpl;
import net.spy.memcached.protocol.binary.TapAckOperationImpl;
import net.spy.memcached.util.StringUtils;

/**
 * Main class for handling connections to a memcached cluster.
 */
public class MemcachedConnection extends SpyThread {

  /**
   * The number of empty selects we'll allow before assuming we may have
   * missed one and should check the current selectors. This generally
   * indicates a bug, but we'll check it nonetheless.
   */
  private static final int DOUBLE_CHECK_EMPTY = 256;

  /**
   * The number of empty selects we'll allow before blowing up. It's too
   * easy to write a bug that causes it to loop uncontrollably. This helps
   * find those bugs and often works around them.
   */
  private static final int EXCESSIVE_EMPTY = 0x1000000;

  /**
   * If an operation gets cloned more than this ceiling, cancel it for
   * safety reasons.
   */
  private static final int MAX_CLONE_COUNT = 100;

  private static final String RECON_QUEUE_METRIC =
    "[MEM] Reconnecting Nodes (ReconnectQueue)";
  private static final String SHUTD_QUEUE_METRIC =
    "[MEM] Shutting Down Nodes (NodesToShutdown)";
  private static final String OVERALL_REQUEST_METRIC =
    "[MEM] Request Rate: All";
  private static final String OVERALL_AVG_BYTES_WRITE_METRIC =
    "[MEM] Average Bytes written to OS per write";
  private static final String OVERALL_AVG_BYTES_READ_METRIC =
    "[MEM] Average Bytes read from OS per read";
  private static final String OVERALL_AVG_TIME_ON_WIRE_METRIC =
    "[MEM] Average Time on wire for operations (µs)";
  private static final String OVERALL_RESPONSE_METRIC =
    "[MEM] Response Rate: All (Failure + Success + Retry)";
  private static final String OVERALL_RESPONSE_RETRY_METRIC =
    "[MEM] Response Rate: Retry";
  private static final String OVERALL_RESPONSE_FAIL_METRIC =
    "[MEM] Response Rate: Failure";
  private static final String OVERALL_RESPONSE_SUCC_METRIC =
    "[MEM] Response Rate: Success";

  /**
   * If the connection is alread shut down or shutting down.
   */
  protected volatile boolean shutDown = false;

  /**
   * If true, optimization will collapse multiple sequential get ops.
   */
  private final boolean shouldOptimize;

  /**
   * Holds the current {@link Selector} to use.
   */
  protected Selector selector = null;

  /**
   * The {@link NodeLocator} to use for this connection.
   */
  protected final NodeLocator locator;

  /**
   * The configured {@link FailureMode}.
   */
  protected final FailureMode failureMode;

  /**
   * Maximum amount of time to wait between reconnect attempts.
   */
  private final long maxDelay;

  /**
   * Contains the current number of empty select() calls, which could indicate
   * bugs.
   */
  private int emptySelects = 0;

  /**
   * The buffer size that will be used when reading from the server.
   */
  private final int bufSize;

  /**
   * The connection factory to create {@link MemcachedNode}s from.
   */
  private final ConnectionFactory connectionFactory;

  /**
   * AddedQueue is used to track the QueueAttachments for which operations
   * have recently been queued.
   */
  protected final ConcurrentLinkedQueue<MemcachedNode> addedQueue;

  /**
   * reconnectQueue contains the attachments that need to be reconnected.
   * The key is the time at which they are eligible for reconnect.
   */
  private final SortedMap<Long, MemcachedNode> reconnectQueue;

  /**
   * True if not shutting down or shut down.
   */
  protected volatile boolean running = true;

  /**
   * Holds all connection observers that get notified on connection status
   * changes.
   */
  private final Collection<ConnectionObserver> connObservers =
    new ConcurrentLinkedQueue<ConnectionObserver>();

  /**
   * The {@link OperationFactory} to clone or create operations.
   */
  private final OperationFactory opFact;

  /**
   * The threshold for timeout exceptions.
   */
  private final int timeoutExceptionThreshold;

  /**
   * Holds operations that need to be retried.
   */
  private final List<Operation> retryOps;

  /**
   * Holds all nodes that are scheduled for shutdown.
   */
  protected final ConcurrentLinkedQueue<MemcachedNode> nodesToShutdown;

  /**
   * If set to true, a proper check after finish connecting is done to see
   * if the node is not responding but really alive.
   */
  private final boolean verifyAliveOnConnect;

  /**
   * The {@link ExecutorService} to use for callbacks.
   */
  private final ExecutorService listenerExecutorService;

  /**
   * The {@link MetricCollector} to accumulate metrics (or dummy).
   */
  protected final MetricCollector metrics;

  /**
   * The current type of metrics to collect.
   */
  protected final MetricType metricType;

  /**
   * Construct a {@link MemcachedConnection}.
   *
   * @param bufSize the size of the buffer used for reading from the server.
   * @param f the factory that will provide an operation queue.
   * @param a the addresses of the servers to connect to.
   * @param obs the initial observers to add.
   * @param fm the failure mode to use.
   * @param opfactory the operation factory.
   * @throws IOException if a connection attempt fails early
   */
  public MemcachedConnection(final int bufSize, final ConnectionFactory f,
      final List<InetSocketAddress> a, final Collection<ConnectionObserver> obs,
      final FailureMode fm, final OperationFactory opfactory) throws IOException {
    connObservers.addAll(obs);
    reconnectQueue = new TreeMap<Long, MemcachedNode>();
    addedQueue = new ConcurrentLinkedQueue<MemcachedNode>();
    failureMode = fm;
    shouldOptimize = f.shouldOptimize();
    maxDelay = TimeUnit.SECONDS.toMillis(f.getMaxReconnectDelay());
    opFact = opfactory;
    timeoutExceptionThreshold = f.getTimeoutExceptionThreshold();
    selector = Selector.open();
    retryOps = Collections.synchronizedList(new ArrayList<Operation>());
    nodesToShutdown = new ConcurrentLinkedQueue<MemcachedNode>();
    listenerExecutorService = f.getListenerExecutorService();
    this.bufSize = bufSize;
    this.connectionFactory = f;

    String verifyAlive = System.getProperty("net.spy.verifyAliveOnConnect");
    if(verifyAlive != null && verifyAlive.equals("true")) {
      verifyAliveOnConnect = true;
    } else {
      verifyAliveOnConnect = false;
    }

    List<MemcachedNode> connections = createConnections(a);
    locator = f.createLocator(connections);

    metrics = f.getMetricCollector();
    metricType = f.enableMetrics();

    registerMetrics();

    setName("Memcached IO over " + this);
    setDaemon(f.isDaemon());
    start();
  }

  /**
   * Register Metrics for collection.
   *
   * Note that these Metrics may or may not take effect, depending on the
   * {@link MetricCollector} implementation. This can be controlled from
   * the {@link DefaultConnectionFactory}.
   */
  protected void registerMetrics() {
    if (metricType.equals(MetricType.DEBUG)
      || metricType.equals(MetricType.PERFORMANCE)) {
      metrics.addHistogram(OVERALL_AVG_BYTES_READ_METRIC);
      metrics.addHistogram(OVERALL_AVG_BYTES_WRITE_METRIC);
      metrics.addHistogram(OVERALL_AVG_TIME_ON_WIRE_METRIC);
      metrics.addMeter(OVERALL_RESPONSE_METRIC);
      metrics.addMeter(OVERALL_REQUEST_METRIC);

      if (metricType.equals(MetricType.DEBUG)) {
        metrics.addCounter(RECON_QUEUE_METRIC);
        metrics.addCounter(SHUTD_QUEUE_METRIC);
        metrics.addMeter(OVERALL_RESPONSE_RETRY_METRIC);
        metrics.addMeter(OVERALL_RESPONSE_SUCC_METRIC);
        metrics.addMeter(OVERALL_RESPONSE_FAIL_METRIC);
      }
    }
  }

  /**
   * Create connections for the given list of addresses.
   *
   * @param addrs the list of addresses to connect to.
   * @return addrs list of {@link MemcachedNode}s.
   * @throws IOException if connecting was not successful.
   */
  protected List<MemcachedNode> createConnections(
    final Collection<InetSocketAddress> addrs) throws IOException {
    List<MemcachedNode> connections = new ArrayList<MemcachedNode>(addrs.size());

    for (SocketAddress sa : addrs) {
      SocketChannel ch = SocketChannel.open();
      ch.configureBlocking(false);
      MemcachedNode qa = connectionFactory.createMemcachedNode(sa, ch, bufSize);
      qa.setConnection(this);
      int ops = 0;
      ch.socket().setTcpNoDelay(!connectionFactory.useNagleAlgorithm());

      try {
        if (ch.connect(sa)) {
          getLogger().info("Connected to %s immediately", qa);
          connected(qa);
        } else {
          getLogger().info("Added %s to connect queue", qa);
          ops = SelectionKey.OP_CONNECT;
        }

        selector.wakeup();
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

  /**
   * Make sure that the current selectors make sense.
   *
   * @return true if they do.
   */
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
   * Handle all IO that flows through the connection.
   *
   * This method is called in an endless loop, listens on NIO selectors and
   * dispatches the underlying read/write calls if needed.
   */
  public void handleIO() throws IOException {
    if (shutDown) {
      throw new IOException("No IO while shut down");
    }

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
      handleEmptySelects();
    } else {
      getLogger().debug("Selected %d, selected %d keys", selected,
        selectedKeys.size());
      emptySelects = 0;

      for (SelectionKey sk : selectedKeys) {
        handleIO(sk);
      }
      selectedKeys.clear();
    }

    handleOperationalTasks();
  }

  /**
   * Helper method for {@link #handleIO()} to encapsulate everything that
   * needs to be checked on a regular basis that has nothing to do directly
   * with reading and writing data.
   *
   * @throws IOException if an error happens during shutdown queue handling.
   */
  private void handleOperationalTasks() throws IOException {
    checkPotentiallyTimedOutConnection();

    if (!shutDown && !reconnectQueue.isEmpty()) {
      attemptReconnects();
    }

    if (!retryOps.isEmpty()) {
      redistributeOperations(new ArrayList<Operation>(retryOps));
      retryOps.clear();
    }

    handleShutdownQueue();
  }

  /**
   * Helper method for {@link #handleIO()} to handle empty select calls.
   */
  private void handleEmptySelects() {
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
  }

  /**
   * Check if nodes need to be shut down and do so if needed.
   *
   * @throws IOException if the channel could not be closed properly.
   */
  private void handleShutdownQueue() throws IOException {
    for (MemcachedNode qa : nodesToShutdown) {
      if (!addedQueue.contains(qa)) {
        nodesToShutdown.remove(qa);
        metrics.decrementCounter(SHUTD_QUEUE_METRIC);
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

  /**
   * Check if one or more nodes exceeded the timeout Threshold.
   */
  private void checkPotentiallyTimedOutConnection() {
    boolean stillCheckingTimeouts = true;
    while (stillCheckingTimeouts) {
      try {
        for (SelectionKey sk : selector.keys()) {
          MemcachedNode mn = (MemcachedNode) sk.attachment();
          if (mn.getContinuousTimeout() > timeoutExceptionThreshold) {
            getLogger().warn("%s exceeded continuous timeout threshold", sk);
            lostConnection(mn);
          }
        }
        stillCheckingTimeouts = false;
      } catch(ConcurrentModificationException e) {
        getLogger().warn("Retrying selector keys after "
          + "ConcurrentModificationException caught", e);
        continue;
      }
    }
  }

  /**
   * Handle any requests that have been made against the client.
   */
  private void handleInputQueue() {
    if (!addedQueue.isEmpty()) {
      getLogger().debug("Handling queue");
      Collection<MemcachedNode> toAdd = new HashSet<MemcachedNode>();
      Collection<MemcachedNode> todo = new HashSet<MemcachedNode>();

      MemcachedNode qaNode;
      while ((qaNode = addedQueue.poll()) != null) {
        todo.add(qaNode);
      }

      for (MemcachedNode node : todo) {
        boolean readyForIO = false;
        if (node.isActive()) {
          if (node.getCurrentWriteOp() != null) {
            readyForIO = true;
            getLogger().debug("Handling queued write %s", node);
          }
        } else {
          toAdd.add(node);
        }
        node.copyInputQueue();
        if (readyForIO) {
          try {
            if (node.getWbuf().hasRemaining()) {
              handleWrites(node);
            }
          } catch (IOException e) {
            getLogger().warn("Exception handling write", e);
            lostConnection(node);
          }
        }
        node.fixupOps();
      }
      addedQueue.addAll(toAdd);
    }
  }

  /**
   * Add a connection observer.
   *
   * @return whether the observer was successfully added.
   */
  public boolean addObserver(final ConnectionObserver obs) {
    return connObservers.add(obs);
  }

  /**
   * Remove a connection observer.
   *
   * @return true if the observer existed and now doesn't.
   */
  public boolean removeObserver(final ConnectionObserver obs) {
    return connObservers.remove(obs);
  }

  /**
   * Indicate a successful connect to the given node.
   *
   * @param node the node which was successfully connected.
   */
  private void connected(final MemcachedNode node) {
    assert node.getChannel().isConnected() : "Not connected.";
    int rt = node.getReconnectCount();
    node.connected();

    for (ConnectionObserver observer : connObservers) {
      observer.connectionEstablished(node.getSocketAddress(), rt);
    }
  }

  /**
   * Indicate a lost connection to the given node.
   *
   * @param node the node where the connection was lost.
   */
  private void lostConnection(final MemcachedNode node) {
    queueReconnect(node);
    for (ConnectionObserver observer : connObservers) {
      observer.connectionLost(node.getSocketAddress());
    }
  }

  /**
   * Makes sure that the given node belongs to the current cluster.
   *
   * Before trying to connect to a node, make sure it actually belongs to the
   * currently connected cluster.
   */
  boolean belongsToCluster(final MemcachedNode node) {
    for (MemcachedNode n : locator.getAll()) {
      if (n.getSocketAddress().equals(node.getSocketAddress())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Handle IO for a specific selector.
   *
   * Any IOException will cause a reconnect. Note that this code makes sure
   * that the corresponding node is not only able to connect, but also able to
   * respond in a correct fashion (if verifyAliveOnConnect is set to true
   * through a property). This is handled by issuing a dummy
   * version/noop call and making sure it returns in a correct and timely
   * fashion.
   *
   * @param sk the selector to handle IO against.
   */
  private void handleIO(final SelectionKey sk) {
    MemcachedNode node = (MemcachedNode) sk.attachment();

    try {
      getLogger().debug("Handling IO for:  %s (r=%s, w=%s, c=%s, op=%s)", sk,
        sk.isReadable(), sk.isWritable(), sk.isConnectable(),
        sk.attachment());
      if (sk.isConnectable() && belongsToCluster(node)) {
        getLogger().info("Connection state changed for %s", sk);
        final SocketChannel channel = node.getChannel();
        if (channel.finishConnect()) {
          finishConnect(sk, node);
        } else {
          assert !channel.isConnected() : "connected";
        }
      } else {
        handleReadsAndWrites(sk, node);
      }
    } catch (ClosedChannelException e) {
      if (!shutDown) {
        getLogger().info("Closed channel and not shutting down. Queueing"
            + " reconnect on %s", node, e);
        lostConnection(node);
      }
    } catch (ConnectException e) {
      getLogger().info("Reconnecting due to failure to connect to %s", node, e);
      queueReconnect(node);
    } catch (OperationException e) {
      node.setupForAuth();
      getLogger().info("Reconnection due to exception handling a memcached "
        + "operation on %s. This may be due to an authentication failure.",
        node, e);
      lostConnection(node);
    } catch (Exception e) {
      node.setupForAuth();
      getLogger().info("Reconnecting due to exception on %s", node, e);
      lostConnection(node);
    }
    node.fixupOps();
  }

  /**
   * A helper method for {@link #handleIO(java.nio.channels.SelectionKey)} to
   * handle reads and writes if appropriate.
   *
   * @param sk the selection key to use.
   * @param node th enode to read write from.
   * @throws IOException if an error occurs during read/write.
   */
  private void handleReadsAndWrites(final SelectionKey sk,
    final MemcachedNode node) throws IOException {
    if (sk.isValid()) {
      if (sk.isReadable()) {
        handleReads(node);
      }
      if (sk.isWritable()) {
        handleWrites(node);
      }
    }
  }

  /**
   * Finish the connect phase and potentially verify its liveness.
   *
   * @param sk the selection key for the node.
   * @param node the actual node.
   * @throws IOException if something goes wrong during reading/writing.
   */
  private void finishConnect(final SelectionKey sk, final MemcachedNode node)
    throws IOException {
    if (verifyAliveOnConnect) {
      final CountDownLatch latch = new CountDownLatch(1);
      final OperationFuture<Boolean> rv = new OperationFuture<Boolean>("noop",
        latch, 2500, listenerExecutorService);
      NoopOperation testOp = opFact.noop(new OperationCallback() {
        public void receivedStatus(OperationStatus status) {
          rv.set(status.isSuccess(), status);
        }

        @Override
        public void complete() {
          latch.countDown();
        }
      });

      testOp.setHandlingNode(node);
      testOp.initialize();
      checkState();
      insertOperation(node, testOp);
      node.copyInputQueue();

      boolean done = false;
      if (sk.isValid()) {
        long timeout = TimeUnit.MILLISECONDS.toNanos(
          connectionFactory.getOperationTimeout());

        long stop = System.nanoTime() + timeout;
        while (stop > System.nanoTime()) {
          handleWrites(node);
          handleReads(node);
          if(done = (latch.getCount() == 0)) {
            break;
          }
        }
      }

      if (!done || testOp.isCancelled() || testOp.hasErrored()
        || testOp.isTimedOut()) {
        throw new ConnectException("Could not send noop upon connect! "
          + "This may indicate a running, but not responding memcached "
          + "instance.");
      }
    }

    connected(node);
    addedQueue.offer(node);
    if (node.getWbuf().hasRemaining()) {
      handleWrites(node);
    }
  }

  /**
   * Handle pending writes for the given node.
   *
   * @param node the node to handle writes for.
   * @throws IOException can be raised during writing failures.
   */
  private void handleWrites(final MemcachedNode node) throws IOException {
    node.fillWriteBuffer(shouldOptimize);
    boolean canWriteMore = node.getBytesRemainingToWrite() > 0;
    while (canWriteMore) {
      int wrote = node.writeSome();
      metrics.updateHistogram(OVERALL_AVG_BYTES_WRITE_METRIC, wrote);
      node.fillWriteBuffer(shouldOptimize);
      canWriteMore = wrote > 0 && node.getBytesRemainingToWrite() > 0;
    }
  }

  /**
   * Handle pending reads for the given node.
   *
   * @param node the node to handle reads for.
   * @throws IOException can be raised during reading failures.
   */
  private void handleReads(final MemcachedNode node) throws IOException {
    Operation currentOp = node.getCurrentReadOp();
    if (currentOp instanceof TapAckOperationImpl) {
      node.removeCurrentReadOp();
      return;
    }

    ByteBuffer rbuf = node.getRbuf();
    final SocketChannel channel = node.getChannel();
    int read = channel.read(rbuf);
    metrics.updateHistogram(OVERALL_AVG_BYTES_READ_METRIC, read);
    if (read < 0) {
      currentOp = handleReadsWhenChannelEndOfStream(currentOp, node, rbuf);
    }

    while (read > 0) {
      getLogger().debug("Read %d bytes", read);
      rbuf.flip();
      while (rbuf.remaining() > 0) {
        if (currentOp == null) {
          throw new IllegalStateException("No read operation.");
        }

        long timeOnWire =
          System.nanoTime() - currentOp.getWriteCompleteTimestamp();
        metrics.updateHistogram(OVERALL_AVG_TIME_ON_WIRE_METRIC,
          (int)(timeOnWire / 1000));
        metrics.markMeter(OVERALL_RESPONSE_METRIC);
        synchronized(currentOp) {
          readBufferAndLogMetrics(currentOp, rbuf, node);
        }

        currentOp = node.getCurrentReadOp();
      }
      rbuf.clear();
      read = channel.read(rbuf);
      node.completedRead();
    }
  }

  /**
   * Read from the buffer and add metrics information.
   *
   * @param currentOp the current operation to read.
   * @param rbuf the read buffer to read from.
   * @param node the node to read from.
   * @throws IOException if reading was not successful.
   */
  private void readBufferAndLogMetrics(final Operation currentOp,
    final ByteBuffer rbuf, final MemcachedNode node) throws IOException {
    currentOp.readFromBuffer(rbuf);
    if (currentOp.getState() == OperationState.COMPLETE) {
      getLogger().debug("Completed read op: %s and giving the next %d "
        + "bytes", currentOp, rbuf.remaining());
      Operation op = node.removeCurrentReadOp();
      assert op == currentOp : "Expected to pop " + currentOp + " got "
        + op;

      if (op.hasErrored()) {
        metrics.markMeter(OVERALL_RESPONSE_FAIL_METRIC);
      } else {
        metrics.markMeter(OVERALL_RESPONSE_SUCC_METRIC);
      }
    } else if (currentOp.getState() == OperationState.RETRY) {
      handleRetryInformation(currentOp.getErrorMsg());
      getLogger().debug("Reschedule read op due to NOT_MY_VBUCKET error: "
        + "%s ", currentOp);
      ((VBucketAware) currentOp).addNotMyVbucketNode(
        currentOp.getHandlingNode());
      Operation op = node.removeCurrentReadOp();
      assert op == currentOp : "Expected to pop " + currentOp + " got "
        + op;

      retryOps.add(currentOp);
      metrics.markMeter(OVERALL_RESPONSE_RETRY_METRIC);
    }
  }

  /**
   * Deal with an operation where the channel reached the end of a stream.
   *
   * @param currentOp the current operation to read.
   * @param node the node for that operation.
   * @param rbuf the read buffer.
   *
   * @return the next operation on the node to read.
   * @throws IOException if disconnect while reading.
   */
  private Operation handleReadsWhenChannelEndOfStream(final Operation currentOp,
    final MemcachedNode node, final ByteBuffer rbuf) throws IOException {
    if (currentOp instanceof TapOperation) {
      currentOp.getCallback().complete();
      ((TapOperation) currentOp).streamClosed(OperationState.COMPLETE);

      getLogger().debug("Completed read op: %s and giving the next %d bytes",
        currentOp, rbuf.remaining());
      Operation op = node.removeCurrentReadOp();
      assert op == currentOp : "Expected to pop " + currentOp + " got " + op;
      return node.getCurrentReadOp();
    } else {
      throw new IOException("Disconnected unexpected, will reconnect.");
    }
  }

  /**
   * Convert the {@link ByteBuffer} into a string for easier debugging.
   *
   * @param b the buffer to debug.
   * @param size the size of the buffer.
   * @return the stringified {@link ByteBuffer}.
   */
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

  /**
   * Optionally handle retry (NOT_MY_VBUKET) responses.
   *
   * This method can be overridden in subclasses to handle the content
   * of the retry message appropriately.
   *
   * @param retryMessage the body of the retry message.
   */
  protected void handleRetryInformation(final byte[] retryMessage) {
    getLogger().debug("Got RETRY message: " + new String(retryMessage)
      + ", but not handled.");
  }

  /**
   * Enqueue the given {@link MemcachedNode} for reconnect.
   *
   * @param node the node to reconnect.
   */
  protected void queueReconnect(final MemcachedNode node) {
    if (shutDown) {
      return;
    }
    getLogger().warn("Closing, and reopening %s, attempt %d.", node,
      node.getReconnectCount());

    if (node.getSk() != null) {
      node.getSk().cancel();
      assert !node.getSk().isValid() : "Cancelled selection key is valid";
    }
    node.reconnecting();

    try {
      if (node.getChannel() != null && node.getChannel().socket() != null) {
        node.getChannel().socket().close();
      } else {
        getLogger().info("The channel or socket was null for %s", node);
      }
    } catch (IOException e) {
      getLogger().warn("IOException trying to close a socket", e);
    }
    node.setChannel(null);

    long delay = (long) Math.min(maxDelay, Math.pow(2,
        node.getReconnectCount())) * 1000;
    long reconnectTime = System.currentTimeMillis() + delay;
    while (reconnectQueue.containsKey(reconnectTime)) {
      reconnectTime++;
    }

    reconnectQueue.put(reconnectTime, node);
    metrics.incrementCounter(RECON_QUEUE_METRIC);

    node.setupResend();
    if (failureMode == FailureMode.Redistribute) {
      redistributeOperations(node.destroyInputQueue());
    } else if (failureMode == FailureMode.Cancel) {
      cancelOperations(node.destroyInputQueue());
    }
  }

  /**
   * Cancel the given collection of operations.
   *
   * @param ops the list of operations to cancel.
   */
  private void cancelOperations(final Collection<Operation> ops) {
    for (Operation op : ops) {
      op.cancel();
    }
  }

  /**
   * Redistribute the given list of operations to (potentially) other nodes.
   *
   * Note that operations can only be redistributed if they have not been
   * cancelled already, timed out already or do not have definite targets
   * (a key).
   *
   * @param ops the operations to redistribute.
   */
  public void redistributeOperations(final Collection<Operation> ops) {
    for (Operation op : ops) {
      redistributeOperation(op);
    }
  }

  /**
   * Redistribute the given operation to (potentially) other nodes.
   *
   * Note that operations can only be redistributed if they have not been
   * cancelled already, timed out already or do not have definite targets
   * (a key).
   *
   * @param op the operation to redistribute.
   */
  public void redistributeOperation(Operation op) {
    if (op.isCancelled() || op.isTimedOut()) {
      return;
    }

    if (op.getCloneCount() >= MAX_CLONE_COUNT) {
      getLogger().warn("Cancelling operation " + op + "because it has been "
        + "retried (cloned) more than " + MAX_CLONE_COUNT + "times.");
      op.cancel();
      return;
    }

    // The operation gets redistributed but has never been actually written,
    // it we just straight re-add it without cloning.
    if (op.getState() == OperationState.WRITE_QUEUED) {
      addOperation(op.getHandlingNode(), op);
    }

    if (op instanceof MultiGetOperationImpl) {
      for (String key : ((MultiGetOperationImpl) op).getRetryKeys()) {
        addOperation(key, opFact.get(key,
          (GetOperation.Callback) op.getCallback()));
      }
    } else if (op instanceof KeyedOperation) {
      KeyedOperation ko = (KeyedOperation) op;
      int added = 0;
      for (Operation newop : opFact.clone(ko)) {
        if (newop instanceof KeyedOperation) {
          KeyedOperation newKeyedOp = (KeyedOperation) newop;
          for (String k : newKeyedOp.getKeys()) {
            addOperation(k, newop);
            op.addClone(newop);
            newop.setCloneCount(op.getCloneCount()+1);
          }
        } else {
          newop.cancel();
          getLogger().warn("Could not redistribute cloned non-keyed " +
            "operation", newop);
        }
        added++;
      }
      assert added > 0 : "Didn't add any new operations when redistributing";
    } else {
      op.cancel();
    }
  }

  /**
   * Attempt to reconnect {@link MemcachedNode}s in the reconnect queue.
   *
   * If the {@link MemcachedNode} does not belong to the cluster list anymore,
   * the reconnect attempt is cancelled. If it does, the code tries to
   * reconnect immediately and if this is not possible it waits until the
   * connection information arrives.
   *
   * Note that if a socket error arises during reconnect, the node is scheduled
   * for re-reconnect immediately.
   */
  private void attemptReconnects() {
    final long now = System.currentTimeMillis();
    final Map<MemcachedNode, Boolean> seen =
      new IdentityHashMap<MemcachedNode, Boolean>();
    final List<MemcachedNode> rereQueue = new ArrayList<MemcachedNode>();
    SocketChannel ch = null;


    Iterator<MemcachedNode> i = reconnectQueue.headMap(now).values().iterator();
    while(i.hasNext()) {
      final MemcachedNode node = i.next();
      i.remove();
      metrics.decrementCounter(RECON_QUEUE_METRIC);

      try {
        if (!belongsToCluster(node)) {
          getLogger().debug("Node does not belong to cluster anymore, "
            + "skipping reconnect: %s", node);
          continue;
        }

        if (!seen.containsKey(node)) {
          seen.put(node, Boolean.TRUE);
          getLogger().info("Reconnecting %s", node);

          ch = SocketChannel.open();
          ch.configureBlocking(false);
          ch.socket().setTcpNoDelay(!connectionFactory.useNagleAlgorithm());
          int ops = 0;
          if (ch.connect(node.getSocketAddress())) {
            connected(node);
            addedQueue.offer(node);
            getLogger().info("Immediately reconnected to %s", node);
            assert ch.isConnected();
          } else {
            ops = SelectionKey.OP_CONNECT;
          }
          node.registerChannel(ch, ch.register(selector, ops, node));
          assert node.getChannel() == ch : "Channel was lost.";
        } else {
          getLogger().debug("Skipping duplicate reconnect request for %s",
            node);
        }
      } catch (SocketException e) {
        getLogger().warn("Error on reconnect", e);
        rereQueue.add(node);
      } catch (Exception e) {
        getLogger().error("Exception on reconnect, lost node %s", node, e);
      } finally {
        potentiallyCloseLeakingChannel(ch, node);
      }
    }

    for (MemcachedNode n : rereQueue) {
      queueReconnect(n);
    }
  }

  /**
   * Make sure channel connections are not leaked and properly close under
   * faulty reconnect cirumstances.
   *
   * @param ch the channel to potentially close.
   * @param node the node to which the channel should be bound to.
   */
  private void potentiallyCloseLeakingChannel(final SocketChannel ch,
    final MemcachedNode node) {
    if (ch != null && !ch.isConnected() && !ch.isConnectionPending()) {
      try {
        ch.close();
      } catch (IOException e) {
        getLogger().error("Exception closing channel: %s", node, e);
      }
    }
  }

  /**
   * Returns the {@link NodeLocator} in use for this connection.
   *
   * @return  the current {@link NodeLocator}.
   */
  public NodeLocator getLocator() {
    return locator;
  }

  /**
   * Enqueue the given {@link Operation} with the used key.
   *
   * @param key the key to use.
   * @param o the {@link Operation} to enqueue.
   */
  public void enqueueOperation(final String key, final Operation o) {
    checkState();
    StringUtils.validateKey(key, opFact instanceof BinaryOperationFactory);
    addOperation(key, o);
  }

  /**
   * Add an operation to a connection identified by the given key.
   *
   * If the {@link MemcachedNode} is active or the {@link FailureMode} is set
   * to retry, the primary node will be used for that key. If the primary
   * node is not available and the {@link FailureMode} cancel is used, the
   * operation will be cancelled without further retry.
   *
   * For any other {@link FailureMode} mechanisms (Redistribute), another
   * possible node is used (only if its active as well). If no other active
   * node could be identified, the original primary node is used and retried.
   *
   * @param key the key the operation is operating upon.
   * @param o the operation to add.
   */
  protected void addOperation(final String key, final Operation o) {
    MemcachedNode placeIn = null;
    MemcachedNode primary = locator.getPrimary(key);

    if (primary.isActive() || failureMode == FailureMode.Retry) {
      placeIn = primary;
    } else if (failureMode == FailureMode.Cancel) {
      o.cancel();
    } else {
      Iterator<MemcachedNode> i = locator.getSequence(key);
      while (placeIn == null && i.hasNext()) {
        MemcachedNode n = i.next();
        if (n.isActive()) {
          placeIn = n;
        }
      }

      if (placeIn == null) {
        placeIn = primary;
        this.getLogger().warn("Could not redistribute to another node, "
          + "retrying primary node for %s.", key);
      }
    }

    assert o.isCancelled() || placeIn != null : "No node found for key " + key;
    if (placeIn != null) {
      addOperation(placeIn, o);
    } else {
      assert o.isCancelled() : "No node found for " + key + " (and not "
        + "immediately cancelled)";
    }
  }

  /**
   * Insert an operation on the given node to the beginning of the queue.
   *
   * @param node the node where to insert the {@link Operation}.
   * @param o the operation to insert.
   */
  public void insertOperation(final MemcachedNode node, final Operation o) {
    o.setHandlingNode(node);
    o.initialize();
    node.insertOp(o);
    addedQueue.offer(node);
    metrics.markMeter(OVERALL_REQUEST_METRIC);

    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    getLogger().debug("Added %s to %s", o, node);
  }

  /**
   * Enqueue an operation on the given node.
   *
   * @param node the node where to enqueue the {@link Operation}.
   * @param o the operation to add.
   */
  protected void addOperation(final MemcachedNode node, final Operation o) {
    o.setHandlingNode(node);
    o.initialize();
    node.addOp(o);
    addedQueue.offer(node);
    metrics.markMeter(OVERALL_REQUEST_METRIC);

    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    getLogger().debug("Added %s to %s", o, node);
  }

  /**
   * Enqueue the given list of operations on each handling node.
   *
   * @param ops the operations for each node.
   */
  public void addOperations(final Map<MemcachedNode, Operation> ops) {
    for (Map.Entry<MemcachedNode, Operation> me : ops.entrySet()) {
      addOperation(me.getKey(), me.getValue());
    }
  }

  /**
   * Broadcast an operation to all nodes.
   *
   * @return a {@link CountDownLatch} that will be counted down when the
   *         operations are complete.
   */
  public CountDownLatch broadcastOperation(final BroadcastOpFactory of) {
    return broadcastOperation(of, locator.getAll());
  }

  /**
   * Broadcast an operation to a collection of nodes.
   *
   * @return a {@link CountDownLatch} that will be counted down when the
   *         operations are complete.
   */
  public CountDownLatch broadcastOperation(final BroadcastOpFactory of,
    final Collection<MemcachedNode> nodes) {
    final CountDownLatch latch = new CountDownLatch(nodes.size());

    for (MemcachedNode node : nodes) {
      getLogger().debug("broadcast Operation: node = " + node);
      Operation op = of.newOp(node, latch);
      op.initialize();
      node.addOp(op);
      op.setHandlingNode(node);
      addedQueue.offer(node);
      metrics.markMeter(OVERALL_REQUEST_METRIC);
    }

    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    return latch;
  }

  /**
   * Shut down all connections and do not accept further incoming ops.
   */
  public void shutdown() throws IOException {
    shutDown = true;

    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
    for (MemcachedNode node : locator.getAll()) {
      if (node.getChannel() != null) {
        node.getChannel().close();
        node.setSk(null);
        if (node.getBytesRemainingToWrite() > 0) {
          getLogger().warn("Shut down with %d bytes remaining to write",
              node.getBytesRemainingToWrite());
        }
        getLogger().debug("Shut down channel %s", node.getChannel());
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
      sb.append(" ").append(qa.getSocketAddress());
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Construct a String containing information about all nodes and their state.
   *
   * @return a stringified representation of the connection status.
   */
  public String connectionsStatus() {
    StringBuilder connStatus = new StringBuilder();
    connStatus.append("Connection Status {");
    for (MemcachedNode node : locator.getAll()) {
      connStatus
        .append(" ")
        .append(node.getSocketAddress())
        .append(" active: ")
        .append(node.isActive())
        .append(", authed: ")
        .append(node.isAuthenticated())
        .append(MessageFormat.format(", last read: {0} ms ago",
          node.lastReadDelta()));
    }
    connStatus.append(" }");
    return connStatus.toString();
  }

  /**
   * Increase the timeout counter for the given handling node.
   *
   * @param op the operation to grab the node from.
   */
  public static void opTimedOut(final Operation op) {
    MemcachedConnection.setTimeout(op, true);
  }

  /**
   * Reset the timeout counter for the given handling node.
   *
   * @param op the operation to grab the node from.
   */
  public static void opSucceeded(final Operation op) {
    MemcachedConnection.setTimeout(op, false);
  }

  /**
   * Set the continous timeout on an operation.
   *
   * @param op the operation to use.
   * @param isTimeout is timed out or not.
   */
  private static void setTimeout(final Operation op, final boolean isTimeout) {
    Logger logger = LoggerFactory.getLogger(MemcachedConnection.class);

    try {
      if (op == null || op.isTimedOutUnsent()) {
        return;
      }

      MemcachedNode node = op.getHandlingNode();
      if (node == null) {
        logger.warn("handling node for operation is not set");
      } else {
        node.setContinuousTimeout(isTimeout);
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * Check to see if this connection is shutting down.
   *
   * @throws IllegalStateException when shutting down.
   */
  protected void checkState() {
    if (shutDown) {
      throw new IllegalStateException("Shutting down");
    }
    assert isAlive() : "IO Thread is not running.";
  }

  /**
   * Handle IO as long as the application is running.
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
      } catch (ConcurrentModificationException e) {
        logRunException(e);
      }
    }
    getLogger().info("Shut down memcached client");
  }

  /**
   * Log a exception to different levels depending on the state.
   *
   * Exceptions get logged at debug level when happening during shutdown, but
   * at warning level when operating normally.
   *
   * @param e the exception to log.
   */
  private void logRunException(final Exception e) {
    if (shutDown) {
      getLogger().debug("Exception occurred during shutdown", e);
    } else {
      getLogger().warn("Problem handling memcached IO", e);
    }
  }

  /**
   * Returns whether the connection is shut down or not.
   *
   * @return true if the connection is shut down, false otherwise.
   */
  public boolean isShutDown() {
    return shutDown;
  }

  /**
   * Add a operation to the retry queue.
   *
   * @param op the operation to retry.
   */
  public void retryOperation(Operation op) {
    retryOps.add(op);
  }

}
