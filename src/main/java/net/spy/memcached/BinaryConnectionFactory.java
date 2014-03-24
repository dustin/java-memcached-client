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

package net.spy.memcached;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import net.spy.memcached.protocol.binary.BinaryMemcachedNodeImpl;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;

/**
 * Default connection factory for binary wire protocol connections.
 */
public class BinaryConnectionFactory extends DefaultConnectionFactory {

  /**
   * Create a DefaultConnectionFactory with the default parameters.
   */
  public BinaryConnectionFactory() {
    super();
  }

  /**
   * Create a BinaryConnectionFactory with the given maximum operation queue
   * length, and the given read buffer size.
   */
  public BinaryConnectionFactory(int len, int bufSize) {
    super(len, bufSize);
  }

  /**
   * Construct a BinaryConnectionFactory with the given parameters.
   *
   * @param len the queue length.
   * @param bufSize the buffer size
   * @param hash the algorithm to use for hashing
   */
  public BinaryConnectionFactory(int len, int bufSize, HashAlgorithm hash) {
    super(len, bufSize, hash);
  }

  @Override
  public MemcachedNode createMemcachedNode(SocketAddress sa, SocketChannel c,
      int bufSize) {
    boolean doAuth = false;
    if (getAuthDescriptor() != null) {
        doAuth = true;
    }
    return new BinaryMemcachedNodeImpl(sa, c, bufSize,
        createReadOperationQueue(), createWriteOperationQueue(),
        createOperationQueue(), getOpQueueMaxBlockTime(), doAuth,
        getOperationTimeout(), getAuthWaitTime(), this);
  }

  @Override
  public OperationFactory getOperationFactory() {
    return new BinaryOperationFactory();
  }

  @Override
  protected String getName() {
    return "BinaryConnectionFactory";
  }
}
