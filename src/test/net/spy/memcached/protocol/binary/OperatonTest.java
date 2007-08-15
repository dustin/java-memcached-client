package net.spy.memcached.protocol.binary;

import junit.framework.TestCase;

/**
 * Test operation stuff.
 */
public class OperatonTest extends TestCase {

	public void testIntegerDecode() {
		assertEquals(129,
			OperationImpl.decodeInt(new byte[]{0, 0, 0, (byte)0x81}, 0));
		assertEquals(129 * 256,
				OperationImpl.decodeInt(new byte[]{0, 0, (byte)0x81, 0}, 0));
		assertEquals(129 * 256 * 256,
				OperationImpl.decodeInt(new byte[]{0, (byte)0x81, 0, 0}, 0));
		assertEquals(129 * 256 * 256 * 256,
				OperationImpl.decodeInt(new byte[]{(byte)0x81, 0, 0, 0}, 0));
	}
}
