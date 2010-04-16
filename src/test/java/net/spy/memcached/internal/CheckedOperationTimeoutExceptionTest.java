package net.spy.memcached.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;
import net.spy.memcached.MockMemcachedNode;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.protocol.BaseOperationImpl;

public class CheckedOperationTimeoutExceptionTest extends TestCase {

	public void testSingleOperation() {
		Operation op = buildOp(11211);
		assertEquals(CheckedOperationTimeoutException.class.getName()
				+ ": test - failing node: localhost:11211",
				new CheckedOperationTimeoutException("test", op).toString());
	}

	public void testNullNode() {
		Operation op = new TestOperation();
		assertEquals(CheckedOperationTimeoutException.class.getName()
				+ ": test - failing node: <unknown>",
				new CheckedOperationTimeoutException("test", op).toString());
	}

	public void testNullOperation() {
		assertEquals(CheckedOperationTimeoutException.class.getName()
				+ ": test - failing node: <unknown>",
				new CheckedOperationTimeoutException("test",
						(Operation)null).toString());
	}


	public void testMultipleOperation() {
		Collection<Operation> ops = new ArrayList<Operation>();
		ops.add(buildOp(11211));
		ops.add(buildOp(64212));
		assertEquals(CheckedOperationTimeoutException.class.getName()
				+ ": test - failing nodes: localhost:11211, localhost:64212",
				new CheckedOperationTimeoutException("test", ops).toString());
	}

	private TestOperation buildOp(int portNum) {
		TestOperation op = new TestOperation();
		MockMemcachedNode node = new MockMemcachedNode(
				InetSocketAddress.createUnresolved("localhost", portNum));
		op.setHandlingNode(node);
		return op;
	}

	static class TestOperation extends BaseOperationImpl implements Operation {

		@Override
		public void initialize() {
			throw new RuntimeException("Not implemented.");
		}

		@Override
		public void readFromBuffer(ByteBuffer data) throws IOException {
			throw new RuntimeException("Not implemented");
		}

	}
}
