package net.spy.memcached;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.MutatorOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Base class for operation factory tests.
 */
public abstract class OperationFactoryTestBase extends MockObjectTestCase {

	public final static String TEST_KEY = "someKey";
	protected OperationFactory ofact = null;
	protected OperationCallback genericCallback;
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

		testData = new byte[64];
		new Random().nextBytes(testData);
	}

	/**
	 * Get the operation factory used by the tests.
	 */
	protected abstract OperationFactory getOperationFactory();

	public void testDeleteOperationCloning() {
		DeleteOperation op = ofact.delete(TEST_KEY, genericCallback);

		DeleteOperation op2 = cloneOne(DeleteOperation.class, op);
		assertEquals(TEST_KEY, op2.getKeys().iterator().next());
		assertCallback(op2);
	}

	public void testCASOperationCloning() {
		CASOperation op = ofact.cas(StoreType.set,
			"someKey", 727582, 8174, 7175, testData, genericCallback);

		CASOperation op2 = cloneOne(CASOperation.class, op);
		assertKey(op2);
		assertEquals(727582, op2.getCasValue());
		assertEquals(8174, op2.getFlags());
		assertEquals(7175, op2.getExpiration());
		assertBytes(op2.getData());
		assertCallback(op2);
	}

	public void testMutatorOperationIncrCloning() {
		int exp = 823862;
		long def = 28775;
		int by = 7735;
		MutatorOperation op = ofact.mutate(Mutator.incr, TEST_KEY, by, def,
				exp, genericCallback);

		MutatorOperation op2 = cloneOne(MutatorOperation.class, op);
		assertKey(op2);
		assertEquals(exp, op2.getExpiration());
		assertEquals(def, op2.getDefault());
		assertEquals(by, op2.getBy());
		assertSame(Mutator.incr, op2.getType());
		assertCallback(op2);
	}

	public void testMutatorOperationDecrCloning() {
		int exp = 823862;
		long def = 28775;
		int by = 7735;
		MutatorOperation op = ofact.mutate(Mutator.decr, TEST_KEY, by, def,
				exp, genericCallback);

		MutatorOperation op2 = cloneOne(MutatorOperation.class, op);
		assertKey(op2);
		assertEquals(exp, op2.getExpiration());
		assertEquals(def, op2.getDefault());
		assertEquals(by, op2.getBy());
		assertSame(Mutator.decr, op2.getType());
		assertCallback(op2);
	}

	public void testStoreOperationAddCloning() {
		int exp = 823862;
		int flags = 7735;
		StoreOperation op = ofact.store(StoreType.add, TEST_KEY,
				flags, exp, testData, genericCallback);

		StoreOperation op2 = cloneOne(StoreOperation.class, op);
		assertKey(op2);
		assertEquals(exp, op2.getExpiration());
		assertEquals(flags, op2.getFlags());
		assertSame(StoreType.add, op2.getStoreType());
		assertCallback(op2);
	}

	public void testStoreOperationSetCloning() {
		int exp = 823862;
		int flags = 7735;
		StoreOperation op = ofact.store(StoreType.set, TEST_KEY,
				flags, exp, testData, genericCallback);

		StoreOperation op2 = cloneOne(StoreOperation.class, op);
		assertKey(op2);
		assertEquals(exp, op2.getExpiration());
		assertEquals(flags, op2.getFlags());
		assertSame(StoreType.set, op2.getStoreType());
		assertCallback(op2);
	}

	public void testConcatenationOperationAppendCloning() {
		long casId = 82757248;
		ConcatenationOperation op = ofact.cat(ConcatenationType.append, casId,
			TEST_KEY, testData, genericCallback);

		ConcatenationOperation op2 = cloneOne(
				ConcatenationOperation.class, op);
		assertKey(op2);
		assertSame(ConcatenationType.append, op2.getStoreType());
		assertCallback(op2);
	}

	public void testConcatenationOperationPrependCloning() {
		long casId = 82757248;
		ConcatenationOperation op = ofact.cat(ConcatenationType.prepend, casId,
			TEST_KEY, testData, genericCallback);

		ConcatenationOperation op2 = cloneOne(
				ConcatenationOperation.class, op);
		assertKey(op2);
		assertSame(ConcatenationType.prepend, op2.getStoreType());
		assertCallback(op2);
	}

	public void testSingleGetOperationCloning() {
		GetOperation.Callback callback =
			(GetOperation.Callback)mock(GetOperation.Callback.class).proxy();
		GetOperation op = ofact.get(TEST_KEY, callback);

		GetOperation op2 = cloneOne(GetOperation.class, op);
		assertKey(op2);
		assertSame(callback, op.getCallback());
	}

	public void testSingleGetsOperationCloning() {
		GetsOperation.Callback callback =
			(GetsOperation.Callback)mock(GetsOperation.Callback.class).proxy();
		GetsOperation op = ofact.gets(TEST_KEY, callback);

		GetsOperation op2 = cloneOne(GetsOperation.class, op);
		assertKey(op2);
		assertSame(callback, op.getCallback());
	}

	// These are harder cases as they fan out.
	public void testMultipleGetOperationCloning() {
		Collection<String> keys = Arrays.asList("k1", "k2", "k3");
		GetOperation.Callback callback =
			(GetOperation.Callback)mock(GetOperation.Callback.class).proxy();
		GetOperation op = ofact.get(keys, callback);

		Collection<Operation> ops = ofact.clone(op);
		assertEquals(3, ops.size());

		Collection<String> mutableKeys = new ArrayList<String>(keys);
		int i = 3;
		for(Operation o : ops) {
			assertEquals(i, mutableKeys.size()); // Starting size
			GetOperation go = (GetOperation)o;
			mutableKeys.removeAll(go.getKeys());
			// Verify we matched and removed 1
			assertEquals(--i, mutableKeys.size());
		}
	}

	public void testMultipleGetOperationFanout() {
		Collection<String> keys = Arrays.asList("k1", "k2", "k3");
		Mock m = mock(GetOperation.Callback.class);
		OperationStatus st=new OperationStatus(true, "blah");
		m.expects(once()).method("complete");
		m.expects(once()).method("receivedStatus").with(same(st));
		m.expects(once()).method("gotData")
			.with(eq("k1"), eq(1), isA(byte[].class));
		m.expects(once()).method("gotData")
			.with(eq("k2"), eq(2), isA(byte[].class));
		m.expects(once()).method("gotData")
			.with(eq("k3"), eq(3), isA(byte[].class));

		GetOperation.Callback callback = (GetOperation.Callback)m.proxy();
		GetOperation op = ofact.get(keys, callback);

		// Transition each operation callback into the complete state.
		Iterator<String> ki = keys.iterator();
		int i=0;
		for(Operation o : ofact.clone(op)) {
			GetOperation.Callback cb = (GetOperation.Callback)o.getCallback();
			cb.gotData(ki.next(), ++i, new byte[3]);
			cb.receivedStatus(st);
			cb.complete();
		}
	}

	protected void assertKey(KeyedOperation op) {
		assertEquals(TEST_KEY, op.getKeys().iterator().next());
	}

	protected void assertCallback(Operation op) {
		assertSame(genericCallback, op.getCallback());
	}

	private void assertBytes(byte[] bytes) {
		assertTrue(Arrays.equals(testData, bytes));
	}

	@SuppressWarnings("unchecked")
	private <T> T assertOne(Class<T> class1,
			Collection<Operation> ops) {
		assertEquals(1, ops.size());
		Operation op = ops.iterator().next();
		return (T) op;
	}

	protected <T> T cloneOne(Class<T> c, KeyedOperation t) {
		return assertOne(c, ofact.clone(t));
	}
}
