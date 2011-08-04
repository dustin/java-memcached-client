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

package net.spy.memcached.transcoders;

import net.spy.memcached.CachedData;

/**
 * Transcoder is an interface for classes that convert between byte arrays and
 * objects for storage in the cache.
 */
public interface Transcoder<T> {

  /**
   * Should the transcoder be run asyncronously.
   *
   * @return True if the CachedData should be decoded Asyncronously
   */
  boolean asyncDecode(CachedData d);

  /**
   * Encode the given object for storage.
   *
   * @param o the object
   * @return the CachedData representing what should be sent
   */
  CachedData encode(T o);

  /**
   * Decode the cached object into the object it represents.
   *
   * @param d the data
   * @return the return value
   */
  T decode(CachedData d);

  /**
   * Get the maximum size of objects handled by this transcoder.
   */
  int getMaxSize();
}
