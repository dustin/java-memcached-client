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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import net.spy.memcached.ops.Operation;

class MemcachedNodeROImpl implements MemcachedNode {

  private final MemcachedNode root;

  public MemcachedNodeROImpl(MemcachedNode n) {
    super();
    root = n;
  }

  @Override
  public String toString() {
    return root.toString();
  }

  public void addOp(Operation op) {
    throw new UnsupportedOperationException();
  }

  public void insertOp(Operation op) {
    throw new UnsupportedOperationException();
  }

  public void connected() {
    throw new UnsupportedOperationException();
  }

  public void copyInputQueue() {
    throw new UnsupportedOperationException();
  }

  public void fillWriteBuffer(boolean optimizeGets) {
    throw new UnsupportedOperationException();
  }

  public void fixupOps() {
    throw new UnsupportedOperationException();
  }

  public int getBytesRemainingToWrite() {
    return root.getBytesRemainingToWrite();
  }

  public SocketChannel getChannel() {
    throw new UnsupportedOperationException();
  }

  public Operation getCurrentReadOp() {
    throw new UnsupportedOperationException();
  }

  public Operation getCurrentWriteOp() {
    throw new UnsupportedOperationException();
  }

  public ByteBuffer getRbuf() {
    throw new UnsupportedOperationException();
  }

  public int getReconnectCount() {
    return root.getReconnectCount();
  }

  public int getSelectionOps() {
    return root.getSelectionOps();
  }

  public SelectionKey getSk() {
    throw new UnsupportedOperationException();
  }

  public SocketAddress getSocketAddress() {
    return root.getSocketAddress();
  }

  public ByteBuffer getWbuf() {
    throw new UnsupportedOperationException();
  }

  public boolean hasReadOp() {
    return root.hasReadOp();
  }

  public boolean hasWriteOp() {
    return root.hasReadOp();
  }

  public boolean isActive() {
    return root.isActive();
  }

  public void reconnecting() {
    throw new UnsupportedOperationException();
  }

  public void registerChannel(SocketChannel ch, SelectionKey selectionKey) {
    throw new UnsupportedOperationException();
  }

  public Operation removeCurrentReadOp() {
    throw new UnsupportedOperationException();
  }

  public Operation removeCurrentWriteOp() {
    throw new UnsupportedOperationException();
  }

  public void setChannel(SocketChannel to) {
    throw new UnsupportedOperationException();
  }

  public void setSk(SelectionKey to) {
    throw new UnsupportedOperationException();
  }

  public void setupResend() {
    throw new UnsupportedOperationException();
  }

  public void transitionWriteItem() {
    throw new UnsupportedOperationException();
  }

  public int writeSome() throws IOException {
    throw new UnsupportedOperationException();
  }

  public Collection<Operation> destroyInputQueue() {
    throw new UnsupportedOperationException();
  }

  public void authComplete() {
    throw new UnsupportedOperationException();
  }

  public void setupForAuth() {
    throw new UnsupportedOperationException();
  }

  public int getContinuousTimeout() {
    throw new UnsupportedOperationException();
  }

  public void setContinuousTimeout(boolean isIncrease) {
    throw new UnsupportedOperationException();
  }

  public boolean isAuthenticated() {
    throw new UnsupportedOperationException();
  }

  public long lastReadDelta() {
    throw new UnsupportedOperationException();
  }

  public void completedRead() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MemcachedConnection getConnection() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setConnection(MemcachedConnection connection) {
    throw new UnsupportedOperationException();
  }

}
