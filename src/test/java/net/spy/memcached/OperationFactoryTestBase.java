/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.GetAndTouchOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.MutatorOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Base class for operation factory tests.
 */
public abstract class OperationFactoryTestBase extends MockObjectTestCase {

  public static final String TEST_KEY = "someKey";
  protected OperationFactory ofact = null;
  protected OperationCallback genericCallback;
  protected StoreOperation.Callback storeCallback;
  protected DeleteOperation.Callback deleteCallback;
  private byte[] testData;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ofact = getOperationFactory();
    genericCallback = new OperationCallback() {
      public void complete() {
        fail("Unexpected invocation");
      }

      public void receivedStatus(OperationStatus status) {
        fail("Unexpected status:  " + status);
      }
    };
    storeCallback = new StoreOperation.Callback() {
      public void complete() {
        fail("Unexpected invocation");
      }

      public void gotData(String key, long cas) {
      }

      public void receivedStatus(OperationStatus status) {
        fail("Unexpected status:  " + status);
      }
    };
    deleteCallback = new DeleteOperation.Callback() {
      public void complete() {
        fail("Unexpected invocation");
      }

      public void gotData(long cas) {
      }

      public void receivedStatus(OperationStatus status) {
        fail("Unexpected status:  " + status);
      }
    };
    testData = new byte[64];
    new Random().nextBytes(testData);
  }

  /**
   * Get the operation factory used by the tests.
   */
  protected abstract OperationFactory getOperationFactory();

  public void testDeleteOperationCloning() {
    DeleteOperation op = ofact.delete(TEST_KEY, deleteCallback);

    DeleteOperation op2 = cloneOne(DeleteOperation.class, op);
    assertEquals(TEST_KEY, op2.getKeys().iterator().next());
    assertDeleteCallback(op2);
  }

  public void testCASOperationCloning() {
    CASOperation op = ofact.cas(StoreType.SET, "someKey", 727582, 8174, 7175,
      testData, storeCallback);

    CASOperation op2 = cloneOne(CASOperation.class, op);
    assertKey(op2);
    assertEquals(727582, op2.getCasValue());
    assertEquals(8174, op2.getFlags());
    assertEquals(7175, op2.getExpiration());
    assertBytes(op2.getData());
    assertStoreCallback(op2);
  }

  public void testMutatorOperationIncrCloning() {
    int exp = 823862;
    long def = 28775;
    long by = 7735;
    MutatorOperation op = ofact.mutate(Mutator.INCR, TEST_KEY, by, def, exp,
      genericCallback);

    MutatorOperation op2 = cloneOne(MutatorOperation.class, op);
    assertKey(op2);
    assertEquals(exp, op2.getExpiration());
    assertEquals(def, op2.getDefault());
    assertEquals(by, op2.getBy());
    assertSame(Mutator.INCR, op2.getType());
    assertCallback(op2);
  }

  public void testMutatorOperationDecrCloning() {
    int exp = 823862;
    long def = 28775;
    long by = 7735;
    MutatorOperation op = ofact.mutate(Mutator.DECR, TEST_KEY, by, def, exp,
        genericCallback);

    MutatorOperation op2 = cloneOne(MutatorOperation.class, op);
    assertKey(op2);
    assertEquals(exp, op2.getExpiration());
    assertEquals(def, op2.getDefault());
    assertEquals(by, op2.getBy());
    assertSame(Mutator.DECR, op2.getType());
    assertCallback(op2);
  }

  public void testStoreOperationAddCloning() {
    int exp = 823862;
    int flags = 7735;
    StoreOperation op = ofact.store(StoreType.ADD, TEST_KEY, flags, exp,
        testData, storeCallback);

    StoreOperation op2 = cloneOne(StoreOperation.class, op);
    assertKey(op2);
    assertEquals(exp, op2.getExpiration());
    assertEquals(flags, op2.getFlags());
    assertSame(StoreType.ADD, op2.getStoreType());
    assertStoreCallback(op2);
  }

  public void testStoreOperationSetCloning() {
    int exp = 823862;
    int flags = 7735;
    StoreOperation op = ofact.store(StoreType.SET, TEST_KEY, flags, exp,
        testData, storeCallback);

    StoreOperation op2 = cloneOne(StoreOperation.class, op);
    assertKey(op2);
    assertEquals(exp, op2.getExpiration());
    assertEquals(flags, op2.getFlags());
    assertSame(StoreType.SET, op2.getStoreType());
    assertStoreCallback(op2);
  }

  public void testConcatenationOperationAppendCloning() {
    long casId = 82757248;
    ConcatenationOperation op = ofact.cat(ConcatenationType.APPEND, casId,
        TEST_KEY, testData, genericCallback);

    ConcatenationOperation op2 = cloneOne(ConcatenationOperation.class, op);
    assertKey(op2);
    assertSame(ConcatenationType.APPEND, op2.getStoreType());
    assertCallback(op2);
  }

  public void testConcatenationOperationPrependCloning() {
    long casId = 82757248;
    ConcatenationOperation op = ofact.cat(ConcatenationType.PREPEND, casId,
        TEST_KEY, testData, genericCallback);

    ConcatenationOperation op2 = cloneOne(ConcatenationOperation.class, op);
    assertKey(op2);
    assertSame(ConcatenationType.PREPEND, op2.getStoreType());
    assertCallback(op2);
  }

  public void testSingleGetOperationCloning() {
    GetOperation.Callback callback =
        (GetOperation.Callback) mock(GetOperation.Callback.class).proxy();
    GetOperation op = ofact.get(TEST_KEY, callback);

    GetOperation op2 = cloneOne(GetOperation.class, op);
    assertKey(op2);
    assertSame(callback, op.getCallback());
  }

  public void testSingleGetsOperationCloning() {
    GetsOperation.Callback callback =
        (GetsOperation.Callback) mock(GetsOperation.Callback.class).proxy();
    GetsOperation op = ofact.gets(TEST_KEY, callback);

    GetsOperation op2 = cloneOne(GetsOperation.class, op);
    assertKey(op2);
    assertSame(callback, op.getCallback());
  }

  // These are harder cases as they fan out.
  public void testMultipleGetOperationCloning() {
    Collection<String> keys = Arrays.asList("k1", "k2", "k3");
    GetOperation.Callback callback =
        (GetOperation.Callback) mock(GetOperation.Callback.class).proxy();
    GetOperation op = ofact.get(keys, callback);

    Collection<Operation> ops = ofact.clone(op);
    assertEquals(3, ops.size());

    Collection<String> mutableKeys = new ArrayList<String>(keys);
    int i = 3;
    for (Operation o : ops) {
      assertEquals(i, mutableKeys.size()); // Starting size
      GetOperation go = (GetOperation) o;
      mutableKeys.removeAll(go.getKeys());
      // Verify we matched and removed 1
      assertEquals(--i, mutableKeys.size());
    }
  }

  public void testMultipleGetOperationFanout() {
    Collection<String> keys = Arrays.asList("k1", "k2", "k3");
    Mock m = mock(GetOperation.Callback.class);
    OperationStatus st = new OperationStatus(true, "blah", StatusCode.SUCCESS);
    m.expects(once()).method("complete");
    m.expects(once()).method("receivedStatus").with(same(st));
    m.expects(once()).method("gotData").with(eq("k1"), eq(1),
        isA(byte[].class));
    m.expects(once()).method("gotData").with(eq("k2"), eq(2),
        isA(byte[].class));
    m.expects(once()).method("gotData").with(eq("k3"), eq(3),
        isA(byte[].class));

    GetOperation.Callback callback = (GetOperation.Callback) m.proxy();
    GetOperation op = ofact.get(keys, callback);

    // Transition each operation callback into the complete state.
    Iterator<String> ki = keys.iterator();
    int i = 0;
    for (Operation o : ofact.clone(op)) {
      GetOperation.Callback cb = (GetOperation.Callback) o.getCallback();
      cb.gotData(ki.next(), ++i, new byte[3]);
      cb.receivedStatus(st);
      cb.complete();
    }
  }

  public void testNotGrowingCallstack() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    GetOperation.Callback cb = new GetOperation.Callback() {
      @Override
      public void receivedStatus(OperationStatus status) {
      }

      @Override
      public void complete() {
        latch.countDown();
      }

      @Override
      public void gotData(String key, int flags, byte[] data) {
      }
    };

    GetOperation operation = ofact.get("key", cb);
    int nestingDepth = 10000000;
    for (int i = 0; i < nestingDepth; i++) {
      List<Operation> clonedOps = (List<Operation>) ofact.clone(operation);
      operation = (GetOperation) clonedOps.get(0);
    }

    operation.getCallback().complete();
    assertTrue(latch.await(1, TimeUnit.SECONDS));
  }

  protected void assertKey(KeyedOperation op) {
    assertEquals(TEST_KEY, op.getKeys().iterator().next());
  }

  protected void assertCallback(Operation op) {
    assertSame(genericCallback, op.getCallback());
  }

  protected void assertStoreCallback(Operation op) {
    assertSame(storeCallback, op.getCallback());
  }

  protected void assertDeleteCallback(Operation op) {
    assertSame(deleteCallback, op.getCallback());
  }

  private void assertBytes(byte[] bytes) {
    assertTrue(Arrays.equals(testData, bytes));
  }

  @SuppressWarnings("unchecked")
  private <T> T assertOne(Class<T> class1, Collection<Operation> ops) {
    assertEquals(1, ops.size());
    Operation op = ops.iterator().next();
    return (T) op;
  }

  protected <T> T cloneOne(Class<T> c, KeyedOperation t) {
    return assertOne(c, ofact.clone(t));
  }
}
