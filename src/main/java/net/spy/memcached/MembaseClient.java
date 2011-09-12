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
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.GetlOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.memcached.vbucket.Reconfigurable;
import net.spy.memcached.vbucket.config.Bucket;

/**
 * A client for Membase Server.
 */
public class MembaseClient extends MemcachedClient implements MembaseClientIF,
    Reconfigurable {

  protected volatile boolean reconfiguring = false;

  /**
   * Get a MembaseClient based on the REST response from a Membase server.
   *
   * This constructor is merely a convenience for situations where the bucket
   * name is the same as the user name. This is commonly the case.
   *
   * To connect to the "default" special bucket for a given cluster, use an
   * empty string as the password.
   *
   * If a password has not been assigned to the bucket, it is typically an empty
   * string.
   *
   * @param baseList the URI list of one or more servers from the cluster
   * @param bucketName the bucket name in the cluster you wish to use
   * @param pwd the password for the bucket
   * @throws IOException if connections could not be made
   * @throws ConfigurationException if the configuration provided by the server
   *           has issues or is not compatible
   */
  public MembaseClient(List<URI> baseList, String bucketName, String pwd)
    throws IOException {
    this(new MembaseConnectionFactory(baseList, bucketName, bucketName, pwd));
  }

  /**
   * Get a MembaseClient based on the REST response from a Membase server where
   * the username is different than the bucket name.
   *
   * To connect to the "default" special bucket for a given cluster, use an
   * empty string as the password.
   *
   * If a password has not been assigned to the bucket, it is typically an empty
   * string.
   *
   * @param baseList the URI list of one or more servers from the cluster
   * @param bucketName the bucket name in the cluster you wish to use
   * @param usr the username for the bucket; this nearly always be the same as
   *          the bucket name
   * @param pwd the password for the bucket
   * @throws IOException if connections could not be made
   * @throws ConfigurationException if the configuration provided by the server
   *           has issues or is not compatible
   */
  public MembaseClient(final List<URI> baseList, final String bucketName,
      final String usr, final String pwd) throws IOException {
    this(new MembaseConnectionFactory(baseList, bucketName, usr, pwd));
  }

  /**
   * Get a MembaseClient based on the REST response from a Membase server where
   * the username is different than the bucket name.
   *
   * Note that when specifying a ConnectionFactory you must specify a
   * BinaryConnectionFactory. Also the ConnectionFactory's protocol and locator
   * values are always overwritten. The protocol will always be binary and the
   * locator will be chosen based on the bucket type you are connecting to.
   *
   * To connect to the "default" special bucket for a given cluster, use an
   * empty string as the password.
   *
   * If a password has not been assigned to the bucket, it is typically an empty
   * string.
   *
   * @param cf the ConnectionFactory to use to create connections
   * @throws IOException if connections could not be made
   * @throws ConfigurationException if the configuration provided by the server
   *           has issues or is not compatible
   */
  public MembaseClient(MembaseConnectionFactory cf) throws IOException {
    this(cf, true);
  }

  /**
   * Get a MembaseClient based on the REST response from a Membase server where
   * the username is different than the bucket name.
   *
   * Note that when specifying a ConnectionFactory you must specify a
   * BinaryConnectionFactory. Also the ConnectionFactory's protocol and locator
   * values are always overwritten. The protocol will always be binary and the
   * locator will be chosen based on the bucket type you are connecting to.
   *
   * To connect to the "default" special bucket for a given cluster, use an
   * empty string as the password.
   *
   * If a password has not been assigned to the bucket, it is typically an empty
   * string.
   *
   * The subscribe variable is determines whether or not we will subscribe to
   * the configuration changes feed. This constructor should be used when
   * calling super from subclasses of MembaseClient since the subclass might
   * want to start the changes feed later.
   *
   * @param cf the ConnectionFactory to use to create connections
   * @param subscribe whether or not to subscribe to config changes
   * @throws IOException if connections could not be made
   * @throws ConfigurationException if the configuration provided by the server
   *           has issues or is not compatible
   */
  protected MembaseClient(MembaseConnectionFactory cf, boolean subscribe)
    throws IOException {
    super(cf, AddrUtil.getAddresses(cf.getVBucketConfig().getServers()));
    if (subscribe) {
      cf.getConfigurationProvider().subscribe(cf.getBucket(), this);
    }
  }

  /**
   * This function is called when there is a topology change in the cluster.
   * This function is intended for internal use only.
   */
  public void reconfigure(Bucket bucket) {
    reconfiguring = true;
    try {
      mconn.reconfigure(bucket);
    } catch (IllegalArgumentException ex) {
      getLogger().warn(
          "Failed to reconfigure client, staying with previous configuration.",
          ex);
    } finally {
      reconfiguring = false;
    }
  }

  /**
   * Gets and locks the given key asynchronously. By default the maximum allowed
   * timeout is 30 seconds. Timeouts greater than this will be set to 30
   * seconds.
   *
   * @param key the key to fetch and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @param tc the transcoder to serialize and unserialize value
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public <T> OperationFuture<CASValue<T>> asyncGetAndLock(final String key,
      int exp, final Transcoder<T> tc) {
    final CountDownLatch latch = new CountDownLatch(1);
    final OperationFuture<CASValue<T>> rv =
        new OperationFuture<CASValue<T>>(key, latch, operationTimeout);

    Operation op = opFact.getl(key, exp, new GetlOperation.Callback() {
      private CASValue<T> val = null;

      public void receivedStatus(OperationStatus status) {
        rv.set(val, status);
      }

      public void gotData(String k, int flags, long cas, byte[] data) {
        assert key.equals(k) : "Wrong key returned";
        assert cas > 0 : "CAS was less than zero:  " + cas;
        val =
            new CASValue<T>(cas, tc.decode(new CachedData(flags, data, tc
                .getMaxSize())));
      }

      public void complete() {
        latch.countDown();
      }
    });
    rv.setOperation(op);
    addOp(key, op);
    return rv;
  }

  /**
   * Get and lock the given key asynchronously and decode with the default
   * transcoder. By default the maximum allowed timeout is 30 seconds. Timeouts
   * greater than this will be set to 30 seconds.
   *
   * @param key the key to fetch and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public OperationFuture<CASValue<Object>> asyncGetAndLock(final String key,
      int exp) {
    return asyncGetAndLock(key, exp, transcoder);
  }

  /**
   * Getl with a single key. By default the maximum allowed timeout is 30
   * seconds. Timeouts greater than this will be set to 30 seconds.
   *
   * @param key the key to get and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @param tc the transcoder to serialize and unserialize value
   * @return the result from the cache (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public <T> CASValue<T> getAndLock(String key, int exp, Transcoder<T> tc) {
    try {
      return asyncGetAndLock(key, exp, tc).get(operationTimeout,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Exception waiting for value", e);
    } catch (TimeoutException e) {
      throw new OperationTimeoutException("Timeout waiting for value", e);
    }
  }

  /**
   * Get and lock with a single key and decode using the default transcoder. By
   * default the maximum allowed timeout is 30 seconds. Timeouts greater than
   * this will be set to 30 seconds.
   *
   * @param key the key to get and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @return the result from the cache (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public CASValue<Object> getAndLock(String key, int exp) {
    return getAndLock(key, exp, transcoder);
  }

  /**
   * Gets the number of vBuckets that are contained in the cluster. This
   * function is for internal use only and should rarely be since there
   * are few use cases in which it is necessary.
   */
  @Override
  public int getNumVBuckets() {
    return ((MembaseConnectionFactory)connFactory).getVBucketConfig()
      .getVbucketsCount();
  }

  @Override
  public boolean shutdown(long timeout, TimeUnit unit) {
    boolean shutdownResult = super.shutdown(timeout, unit);
    MembaseConnectionFactory cf = (MembaseConnectionFactory) connFactory;
    if (cf.getConfigurationProvider() != null) {
      cf.getConfigurationProvider().shutdown();
    }
    return shutdownResult;
  }
}
