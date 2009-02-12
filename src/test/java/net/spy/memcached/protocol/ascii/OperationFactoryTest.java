package net.spy.memcached.protocol.ascii;

import net.spy.memcached.OperationFactory;
import net.spy.memcached.OperationFactoryTestBase;
import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Mutator;

public class OperationFactoryTest extends OperationFactoryTestBase {

	@Override
	protected OperationFactory getOperationFactory() {
		return new AsciiOperationFactory();
	}

	@Override
	public void testMutatorOperationIncrCloning() {
		int exp = 823862;
		long def = 28775;
		int by = 7735;
		MutatatorOperation op = ofact.mutate(Mutator.incr, TEST_KEY, by, def,
				exp, genericCallback);

		MutatatorOperation op2 = cloneOne(MutatatorOperation.class, op);
		assertKey(op2);
		assertEquals(-1, op2.getExpiration());
		assertEquals(-1, op2.getDefault());
		assertEquals(by, op2.getBy());
		assertSame(Mutator.incr, op2.getType());
		assertCallback(op2);
	}

	@Override
	public void testMutatorOperationDecrCloning() {
		int exp = 823862;
		long def = 28775;
		int by = 7735;
		MutatatorOperation op = ofact.mutate(Mutator.decr, TEST_KEY, by, def,
				exp, genericCallback);

		MutatatorOperation op2 = cloneOne(MutatatorOperation.class, op);
		assertKey(op2);
		assertEquals(-1, op2.getExpiration());
		assertEquals(-1, op2.getDefault());
		assertEquals(by, op2.getBy());
		assertSame(Mutator.decr, op2.getType());
		assertCallback(op2);
	}

}
