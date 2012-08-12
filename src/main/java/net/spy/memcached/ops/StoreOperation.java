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

package net.spy.memcached.ops;

/**
 * Operation that represents object storage.
 */
public interface StoreOperation extends KeyedOperation {

  /**
   * Get the store type used by this operation.
   */
  StoreType getStoreType();

  /**
   * Get the flags to be set.
   */
  int getFlags();

  /**
   * Get the expiration value to be set.
   */
  int getExpiration();

  /**
   * Get the bytes to be set during this operation.
   *
   * <p>
   * Note, this returns an exact reference to the bytes and the data
   * <em>must not</em> be modified.
   * </p>
   */
  byte[] getData();
  /**
   * Operation callback to get the CAS value.
   */
  interface Callback extends OperationCallback {
    /**
     * Callback for each result from a Store.
     *
     * @param key the key that was retrieved
     * @param cas the CAS value for this record
     */
    void gotData(String key, long cas);
  }
}
