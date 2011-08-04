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

package net.spy.memcached.vbucket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import java.util.Collection;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

/**
 * A MemcachedNodeMockImpl.
 */
public class MemcachedNodeMockImpl implements MemcachedNode {
  private SocketAddress socketAddress;

  public void addOp(Operation op) {
  }

  public void authComplete() {
  }

  public void connected() {
  }

  public void copyInputQueue() {
  }

  public Collection<Operation> destroyInputQueue() {
    return null;
  }

  public void fillWriteBuffer(boolean optimizeGets) {
  }

  public void fixupOps() {
  }

  public int getBytesRemainingToWrite() {
    return 0;
  }

  public SocketChannel getChannel() {
    return null;
  }

  public int getContinuousTimeout() {
    return 0;
  }

  public Operation getCurrentReadOp() {
    return null;
  }

  public Operation getCurrentWriteOp() {
    return null;
  }

  public ByteBuffer getRbuf() {
    return null;
  }

  public int getReconnectCount() {
    return 0;
  }

  public int getSelectionOps() {
    return 0;
  }

  public SelectionKey getSk() {
    return null;
  }

  public SocketAddress getSocketAddress() {
    return socketAddress;
  }

  public ByteBuffer getWbuf() {
    return null;
  }

  public boolean hasReadOp() {
    return true;
  }

  public boolean hasWriteOp() {
    return true;
  }

  public void insertOp(Operation o) {
  }

  public boolean isActive() {
    return true;
  }

  public void reconnecting() {
  }

  public void registerChannel(SocketChannel ch, SelectionKey selectionKey) {
  }

  public Operation removeCurrentReadOp() {
    return null;
  }

  public Operation removeCurrentWriteOp() {
    return null;
  }

  public void setChannel(SocketChannel to) {
  }

  public void setContinuousTimeout(boolean timedOut) {
  }

  public void setSk(SelectionKey to) {
  }

  public void setupForAuth() {
  }

  public void setupResend() {
  }

  public void transitionWriteItem() {
  }

  public int writeSome() throws IOException {
    return 0;
  }

  public void setSocketAddress(SocketAddress newSocketAddress) {
    this.socketAddress = newSocketAddress;
  }
}
