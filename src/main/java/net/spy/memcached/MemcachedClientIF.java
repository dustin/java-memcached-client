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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.transcoders.Transcoder;

/**
 * This interface is provided as a helper for testing clients of the
 * MemcachedClient.
 */
public interface MemcachedClientIF {
  /**
   * Maximum supported key length.
   */
  int MAX_KEY_LENGTH = 250;

  Collection<SocketAddress> getAvailableServers();

  Collection<SocketAddress> getUnavailableServers();

  Transcoder<Object> getTranscoder();

  NodeLocator getNodeLocator();

  Future<Boolean> append(long cas, String key, Object val);

  Future<Boolean> append(String key, Object val);

  <T> Future<Boolean> append(long cas, String key, T val, Transcoder<T> tc);

  <T> Future<Boolean> append(String key, T val, Transcoder<T> tc);

  Future<Boolean> prepend(long cas, String key, Object val);

  Future<Boolean> prepend(String key, Object val);

  <T> Future<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc);

  <T> Future<Boolean> prepend(String key, T val, Transcoder<T> tc);

  <T> Future<CASResponse> asyncCAS(String key, long casId, T value,
      Transcoder<T> tc);

  Future<CASResponse> asyncCAS(String key, long casId, Object value);

  Future<CASResponse> asyncCAS(String key, long casId, int exp, Object value);

  <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, int exp,
    T value, Transcoder<T> tc);

  <T> CASResponse cas(String key, long casId, int exp, T value,
      Transcoder<T> tc);

  CASResponse cas(String key, long casId, Object value);

  CASResponse cas(String key, long casId, int exp, Object value);

  <T> CASResponse cas(String key, long casId, T value, Transcoder<T> tc);

  <T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc);

  Future<Boolean> add(String key, int exp, Object o);

  <T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc);

  Future<Boolean> set(String key, int exp, Object o);

  <T> Future<Boolean> replace(String key, int exp, T o, Transcoder<T> tc);

  Future<Boolean> replace(String key, int exp, Object o);

  <T> Future<T> asyncGet(String key, Transcoder<T> tc);

  Future<Object> asyncGet(String key);

  Future<CASValue<Object>> asyncGetAndTouch(final String key, final int exp);

  <T> Future<CASValue<T>> asyncGetAndTouch(final String key, final int exp,
      final Transcoder<T> tc);

  CASValue<Object> getAndTouch(String key, int exp);

  <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc);

  <T> Future<CASValue<T>> asyncGets(String key, Transcoder<T> tc);

  Future<CASValue<Object>> asyncGets(String key);

  <T> CASValue<T> gets(String key, Transcoder<T> tc);

  CASValue<Object> gets(String key);

  <T> T get(String key, Transcoder<T> tc);

  Object get(String key);

  <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keys,
      Iterator<Transcoder<T>> tcs);
  <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
      Iterator<Transcoder<T>> tcs);

  <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keys,
      Transcoder<T> tc);
  <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
      Transcoder<T> tc);

  BulkFuture<Map<String, Object>> asyncGetBulk(Iterator<String> keys);
  BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys);

  <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc, String... keys);

  BulkFuture<Map<String, Object>> asyncGetBulk(String... keys);

  <T> Map<String, T> getBulk(Iterator<String> keys, Transcoder<T> tc);
  <T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc);

  Map<String, Object> getBulk(Iterator<String> keys);
  Map<String, Object> getBulk(Collection<String> keys);

  <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys);

  Map<String, Object> getBulk(String... keys);

  <T> Future<Boolean> touch(final String key, final int exp,
      final Transcoder<T> tc);

  <T> Future<Boolean> touch(final String key, final int exp);

  Map<SocketAddress, String> getVersions();

  Map<SocketAddress, Map<String, String>> getStats();

  Map<SocketAddress, Map<String, String>> getStats(String prefix);

  long incr(String key, long by);

  long incr(String key, int by);

  long decr(String key, long by);

  long decr(String key, int by);

  Future<Long> asyncIncr(String key, long by);

  Future<Long> asyncIncr(String key, int by);

  Future<Long> asyncDecr(String key, long by);

  Future<Long> asyncDecr(String key, int by);

  long incr(String key, long by, long def, int exp);

  long incr(String key, int by, long def, int exp);

  long decr(String key, long by, long def, int exp);

  long decr(String key, int by, long def, int exp);

  Future<Long> asyncIncr(String key, long by, long def, int exp);

  Future<Long> asyncIncr(String key, int by, long def, int exp);

  Future<Long> asyncDecr(String key, long by, long def, int exp);

  Future<Long> asyncDecr(String key, int by, long def, int exp);

  long incr(String key, long by, long def);

  long incr(String key, int by, long def);

  long decr(String key, long by, long def);

  long decr(String key, int by, long def);

  Future<Long> asyncIncr(String key, long by, long def);

  Future<Long> asyncIncr(String key, int by, long def);

  Future<Long> asyncDecr(String key, long by, long def);

  Future<Long> asyncDecr(String key, int by, long def);

  Future<Boolean> delete(String key);

  Future<Boolean> delete(String key, long cas);

  Future<Boolean> flush(int delay);

  Future<Boolean> flush();

  void shutdown();

  boolean shutdown(long timeout, TimeUnit unit);

  boolean waitForQueues(long timeout, TimeUnit unit);

  boolean addObserver(ConnectionObserver obs);

  boolean removeObserver(ConnectionObserver obs);

  CountDownLatch broadcastOp(final BroadcastOpFactory of);
  CountDownLatch broadcastOp(final BroadcastOpFactory of,
    Collection<MemcachedNode> nodes);

  /**
   * Get the set of SASL mechanisms supported by the servers.
   *
   * @return the union of all SASL mechanisms supported by the servers.
   */
  Set<String> listSaslMechanisms();
}
