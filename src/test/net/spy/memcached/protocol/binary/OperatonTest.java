package net.spy.memcached.protocol.binary;

import static net.spy.memcached.protocol.binary.OperationImpl.decodeInt;
import static net.spy.memcached.protocol.binary.OperationImpl.decodeUnsignedInt;
import junit.framework.TestCase;

/**
 * Test operation stuff.
 */
public class OperatonTest extends TestCase {

	public void testIntegerDecode() {
		assertEquals(129,
			decodeInt(new byte[]{0, 0, 0, (byte)0x81}, 0));
		assertEquals(129 * 256,
				decodeInt(new byte[]{0, 0, (byte)0x81, 0}, 0));
		assertEquals(129 * 256 * 256,
				decodeInt(new byte[]{0, (byte)0x81, 0, 0}, 0));
		assertEquals(129 * 256 * 256 * 256,
				decodeInt(new byte[]{(byte)0x81, 0, 0, 0}, 0));
	}

	public void testUnsignedIntegerDecode() {
		assertEquals(129,
			decodeUnsignedInt(new byte[]{0, 0, 0, (byte)0x81}, 0));
		assertEquals(129 * 256,
				decodeUnsignedInt(new byte[]{0, 0, (byte)0x81, 0}, 0));
		assertEquals(129 * 256 * 256,
				decodeUnsignedInt(new byte[]{0, (byte)0x81, 0, 0}, 0));
		assertEquals(129L * 256L * 256L * 256L,
				decodeUnsignedInt(new byte[]{(byte)0x81, 0, 0, 0}, 0));
	}
}
