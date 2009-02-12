package net.spy.memcached;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import junit.framework.TestCase;
import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

/**
 * Base class for operation factory tests.
 */
public abstract class OperationFactoryTestBase extends TestCase {

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
		assertBytes(op2.getBytes());
		assertCallback(op2);
	}

	public void testMutatorOperationIncrCloning() {
		int exp = 823862;
		long def = 28775;
		int by = 7735;
		MutatatorOperation op = ofact.mutate(Mutator.incr, TEST_KEY, by, def,
				exp, genericCallback);

		MutatatorOperation op2 = cloneOne(MutatatorOperation.class, op);
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
		MutatatorOperation op = ofact.mutate(Mutator.decr, TEST_KEY, by, def,
				exp, genericCallback);

		MutatatorOperation op2 = cloneOne(MutatatorOperation.class, op);
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
