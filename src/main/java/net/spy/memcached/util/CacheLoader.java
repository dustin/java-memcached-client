/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.internal.ImmediateFuture;

/**
 * CacheLoader provides efficient mechanisms for storing lots of data.
 */
public class CacheLoader extends SpyObject {

  private final ExecutorService executorService;
  private final StorageListener storageListener;
  private final MemcachedClientIF client;
  private final int expiration;

  /**
   * Simple CacheLoader constructor that doesn't provide any feedback and caches
   * forever.
   *
   * @param c a client
   */
  public CacheLoader(MemcachedClientIF c) {
    this(c, null, null, 0);
  }

  /**
   * Get a CacheLoader with all the options.
   *
   * @param c a client
   * @param es an ExecutorService (e.g. thread pool) to dispatch results (may be
   *          null, in which case no listener may be provided)
   * @param sl a storage listener (may be null)
   * @param exp expiration to use while loading
   */
  public CacheLoader(MemcachedClientIF c, ExecutorService es,
      StorageListener sl, int exp) {
    super();
    client = c;
    executorService = es;
    storageListener = sl;
    expiration = exp;
  }

  /**
   * Load data from the given iterator.
   *
   * @param <T> type of data being loaded
   * @param i the iterator of data to load
   */
  public <T> Future<?> loadData(Iterator<Map.Entry<String, T>> i) {
    Future<Boolean> mostRecent = null;
    while (i.hasNext()) {
      Map.Entry<String, T> e = i.next();
      mostRecent = push(e.getKey(), e.getValue());
      watch(e.getKey(), mostRecent);
    }

    return mostRecent == null ? new ImmediateFuture(true) : mostRecent;
  }

  /**
   * Load data from the given map.
   *
   * @param <T> type of data being loaded
   * @param map the map of keys to values that needs to be loaded
   */
  public <T> Future<?> loadData(Map<String, T> map) {
    return loadData(map.entrySet().iterator());
  }

  /**
   * Push a value into the cache.
   *
   * This is a wrapper around set that throttles and retries on full queues.
   *
   * @param <T> the type being stored
   * @param k the key
   * @param value the value
   * @return the future representing the stored data
   */
  public <T> Future<Boolean> push(String k, T value) {
    Future<Boolean> rv = null;
    while (rv == null) {
      try {
        rv = client.set(k, expiration, value);
      } catch (IllegalStateException ex) {
        // Need to slow down a bit when we start getting rejections.
        try {
          if (rv != null) {
            rv.get(250, TimeUnit.MILLISECONDS);
          } else {
            Thread.sleep(250);
          }
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        } catch (Exception e2) {
          // Ignore exceptions here. We're just trying to slow
          // down input.
        }
      }

    }
    return rv;
  }

  private void watch(final String key, final Future<Boolean> f) {
    if (executorService != null && storageListener != null) {
      executorService.execute(new Runnable() {
        public void run() {
          try {
            storageListener.storeResult(key, f.get());
          } catch (Exception e) {
            storageListener.errorStoring(key, e);
          }
        }
      });
    }
  }

  /**
   * If you are interested in the results of your data load, this interface will
   * receive them.
   */
  public interface StorageListener {

    /**
     * Normal path response for a set.
     *
     * @param k the key that was being set
     * @param result true if the set changed the DB value
     */
    void storeResult(String k, boolean result);

    /**
     * @param k the key that was attempting to be stored
     * @param e the error received while storing that key
     */
    void errorStoring(String k, Exception e);
  }
}
