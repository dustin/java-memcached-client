package net.spy.memcached.protocol.binary;

import java.util.Collections;

import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.TapOpcode;
import junit.framework.TestCase;

public class BinaryToStringTest extends TestCase {

	public void testConcatenation() {
		(new ConcatenationOperationImpl(ConcatenationType.append, "key",
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
		(new MutatorOperationImpl(Mutator.decr, "key", 1, 0, 15, null)).toString();
	}

	public void testNoop() {
		(new NoopOperationImpl(null)).toString();
	}

	public void testOptimizedGet() {
		(new OptimizedGetImpl(new GetOperationImpl("key", null))).toString();
	}

	public void testOptimiedSet() {
		(new OptimizedSetImpl(new StoreOperationImpl(StoreType.set, "key", 0, 10,
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
		(new StoreOperationImpl(StoreType.set, "key", 0, 10,
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
