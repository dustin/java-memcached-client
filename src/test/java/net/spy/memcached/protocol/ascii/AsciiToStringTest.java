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

package net.spy.memcached.protocol.ascii;

import junit.framework.TestCase;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.StoreType;

/**
 * Tests the toString() functions for ASCII operations.
 */
public class AsciiToStringTest extends TestCase{

  public void testConcatentaion() {
    (new ConcatenationOperationImpl(ConcatenationType.append, "key",
        "value".getBytes(), null)).toString();
  }

  public void testDelete() {
    (new DeleteOperationImpl("key", null)).toString();
  }

  public void testFlush() {
    (new FlushOperationImpl(0, null)).toString();
  }

  public void testGetAndTouch() {
    (new GetAndTouchOperationImpl("gat", 15, null, "key")).toString();
  }

  public void testTouch() {
    (new TouchOperationImpl("key", 2, null)).toString();
  }

  public void testGetl() {
    (new GetlOperationImpl("key", 10, null)).toString();
  }

  public void testGet() {
    (new GetOperationImpl("key", null)).toString();
  }

  public void testGets() {
    (new GetsOperationImpl("key", null)).toString();
  }

  public void testMutator() {
    (new MutatorOperationImpl(Mutator.decr, "key", 1, null)).toString();
  }

  public void testOptimizedGet() {
    (new OptimizedGetImpl(new GetOperationImpl("key", null))).toString();
  }

  public void testStats() {
    (new StatsOperationImpl("hash", null)).toString();
  }

  public void testStore() {
    (new StoreOperationImpl(StoreType.set, "key", 0, 10,
        "value".getBytes(), null)).toString();
  }

  public void testVersion() {
    (new VersionOperationImpl(null)).toString();
  }
}
