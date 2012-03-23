/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

import net.spy.memcached.ops.OperationStatus;

import java.util.concurrent.Future;

/**
 * Interface provided to handle operations completions on memcached client.
 * It allows you not to block on Future(T) object waiting for any result.
 * @param <T> Type of data returned by Future(T).
 */
public interface OperationListener<T> {
    /**
     * This method calls when MemcachedClient got a completion event on it's operation.
     * Implementations of this method should never block it's processing thread.
     * @param client Client that was processing the operation.
     * @param status Operation processing status.
     * @param future Future(T) object holding result of operation.
     */
    public void onComplete(MemcachedClientIF client, OperationStatus status, Future<T> future);
}
