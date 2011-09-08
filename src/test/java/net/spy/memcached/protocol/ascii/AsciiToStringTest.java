package net.spy.memcached.protocol.ascii;

import junit.framework.TestCase;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.StoreType;

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
