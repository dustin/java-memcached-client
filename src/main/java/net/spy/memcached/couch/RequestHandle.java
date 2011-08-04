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

package net.spy.memcached.couch;

import org.apache.http.nio.NHttpClientConnection;

/**
 * A connection request.
 */
public class RequestHandle {

  private final AsyncConnectionManager connMgr;
  private final NHttpClientConnection conn;

  private volatile boolean completed;

  public RequestHandle(AsyncConnectionManager connMgr,
      NHttpClientConnection conn) {
    super();
    this.connMgr = connMgr;
    this.conn = conn;
  }

  public boolean isCompleted() {
    return this.completed;
  }

  public void completed() {
    if (this.completed) {
      return;
    }
    this.completed = true;
    this.connMgr.releaseConnection(this.conn);
    synchronized (this) {
      notifyAll();
    }
  }

  public void cancel() {
    if (this.completed) {
      return;
    }
    this.completed = true;
    synchronized (this) {
      notifyAll();
    }
  }

  public void waitFor() throws InterruptedException {
    if (this.completed) {
      return;
    }
    synchronized (this) {
      while (!this.completed) {
        wait();
      }
    }
  }
}
