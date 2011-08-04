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
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import net.spy.memcached.couch.AsyncConnectionManager;
import net.spy.memcached.protocol.couch.HttpOperation;

/**
 * Creates a connection to Couchbase.
 */
public class CouchbaseConnectionFactory extends MembaseConnectionFactory {

  public CouchbaseConnectionFactory(List<URI> baseList, String bucketName,
      String usr, String pwd) throws IOException {
    super(baseList, bucketName, usr, pwd);
  }

  public CouchbaseNode createCouchDBNode(InetSocketAddress addr,
      AsyncConnectionManager connMgr) {
    return new CouchbaseNode(addr, connMgr,
        new LinkedBlockingQueue<HttpOperation>(opQueueLen),
        getOpQueueMaxBlockTime(), getOperationTimeout());
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * net.spy.memcached.ConnectionFactory#createMemcachedConnection(java.util
   * .List)
   */
  public MemcachedConnection createMemcachedConnection(
      List<InetSocketAddress> addrs) throws IOException {
    return new MemcachedConnection(getReadBufSize(), this, addrs,
        getInitialObservers(), getFailureMode(), getOperationFactory());
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#createCouchDBConnection(java.util
   * .List)
   */
  public CouchbaseConnection createCouchDBConnection(
      List<InetSocketAddress> addrs) throws IOException {
    return new CouchbaseConnection(this, addrs, getInitialObservers());
  }

}
