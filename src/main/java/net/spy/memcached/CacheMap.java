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

package net.spy.memcached;

/**
 * A Map interface to memcached.
 *
 * <p>
 * Do note that nothing that iterates over the map will work (such is
 * memcached). All iteration mechanisms will return empty iterators and such.
 * </p>
 */
public class CacheMap extends BaseCacheMap<Object> {

  /**
   * Construct a CacheMap over the given MemcachedClient.
   *
   * @param c the client
   * @param expiration the expiration to set for keys written to the cache
   * @param prefix a prefix used to make keys in this map unique
   */
  public CacheMap(MemcachedClientIF c, int expiration, String prefix) {
    super(c, expiration, prefix, c.getTranscoder());
  }

  /**
   * Construct a CacheMap over the given MemcachedClient with no expiration.
   *
   * <p>
   * Keys written into this Map will only expire when the LRU pushes them out.
   * </p>
   *
   * @param c the client
   * @param prefix a prefix used to make keys in this map unique
   */
  public CacheMap(MemcachedClientIF c, String prefix) {
    this(c, 0, prefix);
  }
}
