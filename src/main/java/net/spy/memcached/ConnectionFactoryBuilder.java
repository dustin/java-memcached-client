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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.metrics.MetricCollector;
import net.spy.memcached.metrics.MetricType;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationQueueFactory;
import net.spy.memcached.protocol.ascii.AsciiOperationFactory;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Builder for more easily configuring a ConnectionFactory.
 */
public class ConnectionFactoryBuilder {

  protected OperationQueueFactory opQueueFactory;
  protected OperationQueueFactory readQueueFactory;
  protected OperationQueueFactory writeQueueFactory;

  protected Transcoder<Object> transcoder;

  protected FailureMode failureMode;

  protected Collection<ConnectionObserver> initialObservers =
      Collections.emptyList();

  protected OperationFactory opFact;

  protected Locator locator = Locator.ARRAY_MOD;
  protected long opTimeout = -1;
  protected boolean isDaemon = false;
  protected boolean shouldOptimize = false;
  protected boolean useNagle = false;
  protected long maxReconnectDelay =
      DefaultConnectionFactory.DEFAULT_MAX_RECONNECT_DELAY;

  protected int readBufSize = -1;
  protected HashAlgorithm hashAlg;
  protected AuthDescriptor authDescriptor = null;
  protected long opQueueMaxBlockTime = -1;

  protected int timeoutExceptionThreshold =
      DefaultConnectionFactory.DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD;

  protected MetricType metricType = null;
  protected MetricCollector collector = null;
  protected ExecutorService executorService = null;
  protected long authWaitTime = DefaultConnectionFactory.DEFAULT_AUTH_WAIT_TIME;

  /**
   * Set the operation queue factory.
   */

  public ConnectionFactoryBuilder() {
    // empty
  }

  public ConnectionFactoryBuilder(ConnectionFactory cf) {
    setAuthDescriptor(cf.getAuthDescriptor());
    setDaemon(cf.isDaemon());
    setFailureMode(cf.getFailureMode());
    setHashAlg(cf.getHashAlg());
    setInitialObservers(cf.getInitialObservers());
    setMaxReconnectDelay(cf.getMaxReconnectDelay());
    setOpQueueMaxBlockTime(cf.getOpQueueMaxBlockTime());
    setOpTimeout(cf.getOperationTimeout());
    setReadBufferSize(cf.getReadBufSize());
    setShouldOptimize(cf.shouldOptimize());
    setTimeoutExceptionThreshold(cf.getTimeoutExceptionThreshold());
    setTranscoder(cf.getDefaultTranscoder());
    setUseNagleAlgorithm(cf.useNagleAlgorithm());
    setEnableMetrics(cf.enableMetrics());
    setListenerExecutorService(cf.getListenerExecutorService());
    setAuthWaitTime(cf.getAuthWaitTime());
  }

  public ConnectionFactoryBuilder setOpQueueFactory(OperationQueueFactory q) {
    opQueueFactory = q;
    return this;
  }

  /**
   * Set the read queue factory.
   */
  public ConnectionFactoryBuilder
  setReadOpQueueFactory(OperationQueueFactory q) {
    readQueueFactory = q;
    return this;
  }

  /**
   * Set the write queue factory.
   */
  public ConnectionFactoryBuilder
  setWriteOpQueueFactory(OperationQueueFactory q) {
    writeQueueFactory = q;
    return this;
  }

  /**
   * Set the maximum amount of time (in milliseconds) a client is willing to
   * wait for space to become available in an output queue.
   */
  public ConnectionFactoryBuilder setOpQueueMaxBlockTime(long t) {
    opQueueMaxBlockTime = t;
    return this;
  }

  /**
   * Set the default transcoder.
   */
  public ConnectionFactoryBuilder setTranscoder(Transcoder<Object> t) {
    transcoder = t;
    return this;
  }

  /**
   * Set the failure mode.
   */
  public ConnectionFactoryBuilder setFailureMode(FailureMode fm) {
    failureMode = fm;
    return this;
  }

  /**
   * Set the initial connection observers (will observe initial connection).
   */
  public ConnectionFactoryBuilder setInitialObservers(
      Collection<ConnectionObserver> obs) {
    initialObservers = obs;
    return this;
  }

  /**
   * Set the operation factory.
   *
   * Note that the operation factory is used to also imply the type of nodes to
   * create.
   *
   * @see MemcachedNode
   */
  public ConnectionFactoryBuilder setOpFact(OperationFactory f) {
    opFact = f;
    return this;
  }

  /**
   * Set the default operation timeout in milliseconds.
   */
  public ConnectionFactoryBuilder setOpTimeout(long t) {
    opTimeout = t;
    return this;
  }

  /**
   * Set the daemon state of the IO thread (defaults to true).
   */
  public ConnectionFactoryBuilder setDaemon(boolean d) {
    isDaemon = d;
    return this;
  }

  /**
   * Set to false if the default operation optimization is not desirable.
   */
  public ConnectionFactoryBuilder setShouldOptimize(boolean o) {
    shouldOptimize = o;
    return this;
  }

  /**
   * Set the read buffer size.
   */
  public ConnectionFactoryBuilder setReadBufferSize(int to) {
    readBufSize = to;
    return this;
  }

  /**
   * Set the hash algorithm.
   */
  public ConnectionFactoryBuilder setHashAlg(HashAlgorithm to) {
    hashAlg = to;
    return this;
  }

  /**
   * Set to true if you'd like to enable the Nagle algorithm.
   */
  public ConnectionFactoryBuilder setUseNagleAlgorithm(boolean to) {
    useNagle = to;
    return this;
  }

  /**
   * Convenience method to specify the protocol to use.
   */
  public ConnectionFactoryBuilder setProtocol(Protocol prot) {
    switch (prot) {
    case TEXT:
      opFact = new AsciiOperationFactory();
      break;
    case BINARY:
      opFact = new BinaryOperationFactory();
      break;
    default:
      assert false : "Unhandled protocol: " + prot;
    }
    return this;
  }

  /**
   * Set the locator type.
   */
  public ConnectionFactoryBuilder setLocatorType(Locator l) {
    locator = l;
    return this;
  }

  /**
   * Set the maximum reconnect delay.
   */
  public ConnectionFactoryBuilder setMaxReconnectDelay(long to) {
    assert to > 0 : "Reconnect delay must be a positive number";
    maxReconnectDelay = to;
    return this;
  }

  /**
   * Set the auth descriptor to enable authentication on new connections.
   */
  public ConnectionFactoryBuilder setAuthDescriptor(AuthDescriptor to) {
    authDescriptor = to;
    return this;
  }

  /**
   * Set the maximum timeout exception threshold.
   */
  public ConnectionFactoryBuilder setTimeoutExceptionThreshold(int to) {
    assert to > 1 : "Minimum timeout exception threshold is 2";
    if (to > 1) {
      timeoutExceptionThreshold = to - 2;
    }
    return this;
  }

  /**
   * Enable or disable metric collection.
   *
   * @param type the metric type to use (or disable).
   */
  public ConnectionFactoryBuilder setEnableMetrics(MetricType type) {
    metricType = type;
    return this;
  }

  /**
   * Set a custom {@link MetricCollector}.
   *
   * @param collector the metric collector to use.
   */
  public ConnectionFactoryBuilder setMetricCollector(MetricCollector collector) {
    this.collector = collector;
    return this;
  }

  /**
   * Set a custom {@link ExecutorService} to execute the listener callbacks.
   *
   * Note that if a custom {@link ExecutorService} is passed in, it also needs to be properly
   * shut down by the caller. The library itself treats it as a outside managed resource.
   * Therefore, also make sure to not shut it down before all instances that use it are
   * shut down.
   *
   * @param executorService the ExecutorService to use.
   */
  public ConnectionFactoryBuilder setListenerExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  /**
   * Set a custom wait time for the authentication on connect/reconnect.
   *
   * @param authWaitTime the time in milliseconds.
   */
  public ConnectionFactoryBuilder setAuthWaitTime(long authWaitTime) {
    this.authWaitTime = authWaitTime;
    return this;
  }

  /**
   * Get the ConnectionFactory set up with the provided parameters.
   */
  public ConnectionFactory build() {
    return new DefaultConnectionFactory() {

      @Override
      public BlockingQueue<Operation> createOperationQueue() {
        return opQueueFactory == null ? super.createOperationQueue()
            : opQueueFactory.create();
      }

      @Override
      public BlockingQueue<Operation> createReadOperationQueue() {
        return readQueueFactory == null ? super.createReadOperationQueue()
            : readQueueFactory.create();
      }

      @Override
      public BlockingQueue<Operation> createWriteOperationQueue() {
        return writeQueueFactory == null ? super.createReadOperationQueue()
            : writeQueueFactory.create();
      }

      @Override
      public NodeLocator createLocator(List<MemcachedNode> nodes) {
        switch (locator) {
        case ARRAY_MOD:
          return new ArrayModNodeLocator(nodes, getHashAlg());
        case CONSISTENT:
          return new KetamaNodeLocator(nodes, getHashAlg());
        default:
          throw new IllegalStateException("Unhandled locator type: " + locator);
        }
      }

      @Override
      public Transcoder<Object> getDefaultTranscoder() {
        return transcoder == null ? super.getDefaultTranscoder() : transcoder;
      }

      @Override
      public FailureMode getFailureMode() {
        return failureMode == null ? super.getFailureMode() : failureMode;
      }

      @Override
      public HashAlgorithm getHashAlg() {
        return hashAlg == null ? super.getHashAlg() : hashAlg;
      }

      public Collection<ConnectionObserver> getInitialObservers() {
        return initialObservers;
      }

      @Override
      public OperationFactory getOperationFactory() {
        return opFact == null ? super.getOperationFactory() : opFact;
      }

      @Override
      public long getOperationTimeout() {
        return opTimeout == -1 ? super.getOperationTimeout() : opTimeout;
      }

      @Override
      public int getReadBufSize() {
        return readBufSize == -1 ? super.getReadBufSize() : readBufSize;
      }

      @Override
      public boolean isDaemon() {
        return isDaemon;
      }

      @Override
      public boolean shouldOptimize() {
        return shouldOptimize;
      }

      @Override
      public boolean useNagleAlgorithm() {
        return useNagle;
      }

      @Override
      public long getMaxReconnectDelay() {
        return maxReconnectDelay;
      }

      @Override
      public AuthDescriptor getAuthDescriptor() {
        return authDescriptor;
      }

      @Override
      public long getOpQueueMaxBlockTime() {
        return opQueueMaxBlockTime > -1 ? opQueueMaxBlockTime
            : super.getOpQueueMaxBlockTime();
      }

      @Override
      public int getTimeoutExceptionThreshold() {
        return timeoutExceptionThreshold;
      }

      @Override
      public MetricType enableMetrics() {
        return metricType == null ? super.enableMetrics() : metricType;
      }

      @Override
      public MetricCollector getMetricCollector() {
        return collector == null ? super.getMetricCollector() : collector;
      }

      @Override
      public ExecutorService getListenerExecutorService() {
        return executorService == null ? super.getListenerExecutorService() : executorService;
      }

      @Override
      public boolean isDefaultExecutorService() {
        return executorService == null;
      }

      @Override
      public long getAuthWaitTime() {
        return authWaitTime;
      }
    };

  }

  /**
   * Type of protocol to use for connections.
   */
  public static enum Protocol {
    /**
     * Use the text (ascii) protocol.
     */
    TEXT,
    /**
     * Use the binary protocol.
     */
    BINARY
  }

  /**
   * Type of node locator to use.
   */
  public static enum Locator {
    /**
     * Array modulus - the classic node location algorithm.
     */
    ARRAY_MOD,
    /**
     * Consistent hash algorithm.
     *
     * This uses ketema's distribution algorithm, but may be used with any hash
     * algorithm.
     */
    CONSISTENT,
    /**
     * VBucket support.
     */
    VBUCKET
  }
}
