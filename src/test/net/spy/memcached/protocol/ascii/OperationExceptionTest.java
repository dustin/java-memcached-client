package net.spy.memcached.protocol.ascii;

import junit.framework.TestCase;

/**
 * Test operation exception constructors and accessors and stuff.
 */
public class OperationExceptionTest extends TestCase {

	public void testEmpty() {
		OperationException oe=new OperationException();
		assertSame(Operation.ErrorType.GENERAL, oe.getType());
		assertEquals("OperationException: GENERAL", String.valueOf(oe));
	}

	public void testServer() {
		OperationException oe=new OperationException(
			Operation.ErrorType.SERVER, "SERVER_ERROR figures");
		assertSame(Operation.ErrorType.SERVER, oe.getType());
		assertEquals("OperationException: SERVER: figures", String.valueOf(oe));
	}

	public void testClient() {
		OperationException oe=new OperationException(
			Operation.ErrorType.CLIENT, "CLIENT_ERROR nope");
		assertSame(Operation.ErrorType.CLIENT, oe.getType());
		assertEquals("OperationException: CLIENT: nope", String.valueOf(oe));
	}

	public void testGeneral() {
		// General type doesn't have additional info
		OperationException oe=new OperationException(
			Operation.ErrorType.GENERAL, "GENERAL wtf");
		assertSame(Operation.ErrorType.GENERAL, oe.getType());
		assertEquals("OperationException: GENERAL", String.valueOf(oe));
	}
}
