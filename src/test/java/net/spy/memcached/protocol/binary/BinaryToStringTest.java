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

package net.spy.memcached.protocol.binary;

import java.util.Collections;

import junit.framework.TestCase;

import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.TapOpcode;

/**
 * Tests the toString() functions for binary operations.
 */
public class BinaryToStringTest extends TestCase {

  public void testConcatenation() {
    (new ConcatenationOperationImpl(ConcatenationType.APPEND, "key",
        "value".getBytes(), 0, null)).toString();
  }

  public void testDelete() {
    (new DeleteOperationImpl("key", null)).toString();
  }

  public void testFlush() {
    (new FlushOperationImpl(null)).toString();
  }

  public void testGetAndTouch() {
    (new GetAndTouchOperationImpl("key", 15, null)).toString();
  }

  public void testGetl() {
    (new GetlOperationImpl("key", 0, null)).toString();
  }

  public void testGet() {
    (new GetOperationImpl("key", null)).toString();
  }

  public void testGets() {
    (new GetsOperationImpl("key", null)).toString();
  }

  public void testMultiGet() {
    (new MultiGetOperationImpl(Collections.singleton("key"), null)).toString();
  }

  public void testMutator() {
    (new MutatorOperationImpl(Mutator.DECR, "key", 1, 0, 15, null)).toString();
  }

  public void testNoop() {
    (new NoopOperationImpl(null)).toString();
  }

  public void testOptimizedGet() {
    (new OptimizedGetImpl(new GetOperationImpl("key", null))).toString();
  }

  public void testOptimiedSet() {
    (new OptimizedSetImpl(new StoreOperationImpl(StoreType.SET, "key", 0, 10,
        "value".getBytes(), 0, null))).toString();
  }

  public void testSASLAuth() {
    (new SASLAuthOperationImpl(null, null, null, null, null)).toString();
  }

  public void testSASLMechs() {
    (new SASLMechsOperationImpl(null)).toString();
  }

  public void testSASLStep() {
    (new SASLStepOperationImpl(null, null, null, null, null, null)).toString();
  }

  public void testStats() {
    (new StatsOperationImpl("dispatcher", null)).toString();
  }

  public void testStore() {
    (new StoreOperationImpl(StoreType.SET, "key", 0, 10,
        "value".getBytes(), 0, null)).toString();
  }

  public void testTapAck() {
    (new TapAckOperationImpl(TapOpcode.MUTATION, 10, null)).toString();
  }

  public void testTapBackfill() {
    (new TapBackfillOperationImpl(null, 0, null)).toString();
  }

  public void testTapCustom() {
    (new TapCustomOperationImpl(null, new RequestMessage(), null)).toString();
  }

  public void testTapDump() {
    (new TapDumpOperationImpl(null, null)).toString();
  }

  public void testTouch() {
    (new TouchOperationImpl("key", 10, null)).toString();
  }

  public void testTapVersion() {
    (new VersionOperationImpl(null)).toString();
  }
}
