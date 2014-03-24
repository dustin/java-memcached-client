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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.metrics.DefaultMetricCollector;
import net.spy.memcached.metrics.MetricCollector;
import net.spy.memcached.metrics.MetricType;
import net.spy.memcached.metrics.NoopMetricCollector;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.protocol.ascii.AsciiMemcachedNodeImpl;
import net.spy.memcached.protocol.ascii.AsciiOperationFactory;
import net.spy.memcached.protocol.binary.BinaryMemcachedNodeImpl;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Default implementation of ConnectionFactory.
 *
 * <p>
 * This implementation creates connections where the operation queue is an
 * ArrayBlockingQueue and the read and write queues are unbounded
 * LinkedBlockingQueues. The <code>Redistribute</code> FailureMode is always
 * used. If other FailureModes are needed, look at the ConnectionFactoryBuilder.
 *
 * </p>
 */
public class DefaultConnectionFactory extends SpyObject implements
    ConnectionFactory {

  /**
   * Default failure mode.
   */
  public static final FailureMode DEFAULT_FAILURE_MODE =
      FailureMode.Redistribute;

  /**
   * Default hash algorithm.
   */
  public static final HashAlgorithm DEFAULT_HASH =
    DefaultHashAlgorithm.NATIVE_HASH;

  /**
   * Maximum length of the operation queue returned by this connection factory.
   */
  public static final int DEFAULT_OP_QUEUE_LEN = 16384;

  /**
   * The maximum time to block waiting for op queue operations to complete, in
   * milliseconds. The default has been set with the expectation that most
   * requests are interactive and waiting for more than a few seconds is thus
   * more undesirable than failing the request.
   */
  public static final long DEFAULT_OP_QUEUE_MAX_BLOCK_TIME =
      TimeUnit.SECONDS.toMillis(10);

  /**
   * The read buffer size for each server connection from this factory.
   */
  public static final int DEFAULT_READ_BUFFER_SIZE = 16384;

  /**
   * Default operation timeout in milliseconds.
   */
  public static final long DEFAULT_OPERATION_TIMEOUT = 2500;

  /**
   * Maximum amount of time (in seconds) to wait between reconnect attempts.
   */
  public static final long DEFAULT_MAX_RECONNECT_DELAY = 30;

  /**
   * Maximum number + 2 of timeout exception for shutdown connection.
   */
  public static final int DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD = 998;

  /**
   * Turn off metric collection by default.
   */
  public static final MetricType DEFAULT_METRIC_TYPE = MetricType.OFF;

  /**
   * The time to wait for the authentication to complete when a operation
   * is written in milliseconds.
   */
  public static final long DEFAULT_AUTH_WAIT_TIME = 1000;

  protected final int opQueueLen;
  private final int readBufSize;
  private final HashAlgorithm hashAlg;

  private MetricCollector metrics;

  /**
   * The ExecutorService in which the listener callbacks will be executed.
   */
  private ExecutorService executorService;

  /**
   * Construct a DefaultConnectionFactory with the given parameters.
   *
   * @param qLen the queue length.
   * @param bufSize the buffer size
   * @param hash the algorithm to use for hashing
   */
  public DefaultConnectionFactory(int qLen, int bufSize, HashAlgorithm hash) {
    super();
    opQueueLen = qLen;
    readBufSize = bufSize;
    hashAlg = hash;
    metrics = null;
  }

  /**
   * Create a DefaultConnectionFactory with the given maximum operation queue
   * length, and the given read buffer size.
   */
  public DefaultConnectionFactory(int qLen, int bufSize) {
    this(qLen, bufSize, DEFAULT_HASH);
  }

  /**
   * Create a DefaultConnectionFactory with the default parameters.
   */
  public DefaultConnectionFactory() {
    this(DEFAULT_OP_QUEUE_LEN, DEFAULT_READ_BUFFER_SIZE);
  }

  public MemcachedNode createMemcachedNode(SocketAddress sa, SocketChannel c,
      int bufSize) {

    OperationFactory of = getOperationFactory();
    if (of instanceof AsciiOperationFactory) {
      return new AsciiMemcachedNodeImpl(sa, c, bufSize,
          createReadOperationQueue(),
          createWriteOperationQueue(),
          createOperationQueue(),
          getOpQueueMaxBlockTime(),
          getOperationTimeout(),
          getAuthWaitTime(),
          this);
    } else if (of instanceof BinaryOperationFactory) {
      boolean doAuth = false;
      if (getAuthDescriptor() != null) {
        doAuth = true;
      }
      return new BinaryMemcachedNodeImpl(sa, c, bufSize,
          createReadOperationQueue(),
          createWriteOperationQueue(),
          createOperationQueue(),
          getOpQueueMaxBlockTime(),
          doAuth,
          getOperationTimeout(),
          getAuthWaitTime(),
          this);
    } else {
      throw new IllegalStateException("Unhandled operation factory type " + of);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#createConnection(java.util.List)
   */
  public MemcachedConnection createConnection(List<InetSocketAddress> addrs)
    throws IOException {
    return new MemcachedConnection(getReadBufSize(), this, addrs,
        getInitialObservers(), getFailureMode(), getOperationFactory());
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getFailureMode()
   */
  public FailureMode getFailureMode() {
    return DEFAULT_FAILURE_MODE;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#createOperationQueue()
   */
  public BlockingQueue<Operation> createOperationQueue() {
    return new ArrayBlockingQueue<Operation>(getOpQueueLen());
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#createReadOperationQueue()
   */
  public BlockingQueue<Operation> createReadOperationQueue() {
    return new LinkedBlockingQueue<Operation>();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#createWriteOperationQueue()
   */
  public BlockingQueue<Operation> createWriteOperationQueue() {
    return new LinkedBlockingQueue<Operation>();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#createLocator(java.util.List)
   */
  public NodeLocator createLocator(List<MemcachedNode> nodes) {
    return new ArrayModNodeLocator(nodes, getHashAlg());
  }

  /**
   * Get the op queue length set at construct time.
   */
  public int getOpQueueLen() {
    return opQueueLen;
  }

  /**
   * @return the maximum time to block waiting for op queue operations to
   *         complete, in milliseconds, or null for no waiting.
   */
  public long getOpQueueMaxBlockTime() {
    return DEFAULT_OP_QUEUE_MAX_BLOCK_TIME;
  }

  /**
   * @return the time to wait for the authentication to complete when a
   * operation is written in milliseconds.
   */
  @Override
  public long getAuthWaitTime() {
    return DEFAULT_AUTH_WAIT_TIME;
  }

  /**
   * Returns the stored {@link ExecutorService} for listeners.
   *
   * By default, a {@link ThreadPoolExecutor} is used that acts exactly
   * like a default cachedThreadPool, but defines the upper limit of
   * Threads to be created as the number of available processors to
   * prevent resource exhaustion.
   *
   * @return the stored {@link ExecutorService}.
   */
  @Override
  public ExecutorService getListenerExecutorService() {
    if (executorService == null) {
      ThreadFactory threadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          return new Thread(r, "FutureNotifyListener");
        }
      };

      executorService = new ThreadPoolExecutor(
        0,
        Runtime.getRuntime().availableProcessors(),
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(),
        threadFactory
      );
    }

    return executorService;
  }

  @Override
  public boolean isDefaultExecutorService() {
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getReadBufSize()
   */
  public int getReadBufSize() {
    return readBufSize;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getHashAlg()
   */
  public HashAlgorithm getHashAlg() {
    return hashAlg;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getOperationFactory()
   */
  public OperationFactory getOperationFactory() {
    return new AsciiOperationFactory();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getOperationTimeout()
   */
  public long getOperationTimeout() {
    return DEFAULT_OPERATION_TIMEOUT;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#isDaemon()
   */
  public boolean isDaemon() {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getInitialObservers()
   */
  public Collection<ConnectionObserver> getInitialObservers() {
    return Collections.emptyList();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getDefaultTranscoder()
   */
  public Transcoder<Object> getDefaultTranscoder() {
    return new SerializingTranscoder();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#useNagleAlgorithm()
   */
  public boolean useNagleAlgorithm() {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#shouldOptimize()
   */
  public boolean shouldOptimize() {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getMaxReconnectDelay()
   */
  public long getMaxReconnectDelay() {
    return DEFAULT_MAX_RECONNECT_DELAY;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getAuthDescriptor()
   */
  public AuthDescriptor getAuthDescriptor() {
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#getTimeoutExceptionThreshold()
   */
  public int getTimeoutExceptionThreshold() {
    return DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD;
  }

  @Override
  public MetricType enableMetrics() {
    String metricType = System.getProperty("net.spy.metrics.type");
    return metricType == null
      ? DEFAULT_METRIC_TYPE : MetricType.valueOf(metricType.toUpperCase());
  }

  @Override
  public MetricCollector getMetricCollector() {
    if (metrics != null) {
      return metrics;
    }

    String enableMetrics = System.getProperty("net.spy.metrics.enable");
    if (enableMetrics().equals(MetricType.OFF) || enableMetrics == "false") {
      getLogger().debug("Metric collection disabled.");
      metrics =  new NoopMetricCollector();
    } else {
      getLogger().info("Metric collection enabled (Profile " + enableMetrics() + ").");
      metrics = new DefaultMetricCollector();
    }

    return metrics;
  }

  protected String getName() {
    return "DefaultConnectionFactory";
  }

  @Override
  public String toString() {
    return "Failure Mode: " + getFailureMode().name() + ", Hash Algorithm: "
      + ((DefaultHashAlgorithm)getHashAlg()).name() + " Max Reconnect Delay: "
      + getMaxReconnectDelay() + ", Max Op Timeout: " + getOperationTimeout()
      + ", Op Queue Length: " + getOpQueueLen() + ", Op Max Queue Block Time"
      + getOpQueueMaxBlockTime() + ", Max Timeout Exception Threshold: "
      + getTimeoutExceptionThreshold() + ", Read Buffer Size: "
      + getReadBufSize() + ", Transcoder: " + getDefaultTranscoder()
      + ", Operation Factory: " + getOperationFactory() + " isDaemon: "
      + isDaemon() + ", Optimized: " + shouldOptimize() + ", Using Nagle: "
      + useNagleAlgorithm() + ", ConnectionFactory: " + getName();
  }
}
