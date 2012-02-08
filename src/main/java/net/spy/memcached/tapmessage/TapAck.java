/**
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

package net.spy.memcached.tapmessage;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.TapConnectionProvider;
import net.spy.memcached.ops.OperationCallback;

/**
 * An acknowledgment message used to tell the server we have received a
 * series of messages.
 */
public class TapAck {
  private final TapConnectionProvider conn;
  private final TapOpcode opcode;
  private final int opaque;
  private final MemcachedNode node;
  private final OperationCallback cb;

  public TapAck(TapConnectionProvider conn, MemcachedNode node,
      TapOpcode opcode, int opaque, OperationCallback cb) {
    this.conn = conn;
    this.node = node;
    this.opcode = opcode;
    this.opaque = opaque;
    this.cb = cb;
  }

  public TapConnectionProvider getConn() {
    return conn;
  }

  public MemcachedNode getNode() {
    return node;
  }

  public TapOpcode getOpcode() {
    return opcode;
  }

  public int getOpaque() {
    return opaque;
  }

  public OperationCallback getCallback() {
    return cb;
  }
}
