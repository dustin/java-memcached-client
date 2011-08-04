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

package net.spy.memcached.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ops.OperationStatus;

/**
 * Additional flexibility for asyncGetBulk
 *
 * <p>
 * This interface is now returned from all asyncGetBulk methods. Unlike
 * {@link #get(long, TimeUnit)}, {@link #getSome(long, TimeUnit)} does not throw
 * CheckedOperationTimeoutException, thus allowing retrieval of partial results
 * after timeout occurs. This behavior is especially useful in case of large
 * multi gets.
 * </p>
 *
 * @author boris.partensky@gmail.com
 * @param <V>
 */
public interface BulkFuture<V> extends Future<V> {

  /**
   * @return true if timeout was reached, false otherwise
   */
  boolean isTimeout();

  /**
   * Wait for the operation to complete and return results
   *
   * If operation could not complete within specified timeout, partial result is
   * returned. Otherwise, the behavior is identical to
   * {@link #get(long, TimeUnit)}
   *
   * @param timeout
   * @param unit
   * @return a partial get bulk result
   * @throws InterruptedException
   * @throws ExecutionException
   */
  V getSome(long timeout, TimeUnit unit) throws InterruptedException,
      ExecutionException;

  /**
   * Gets the status of the operation upon completion.
   *
   * @return the operation status.
   */
  OperationStatus getStatus();
}
