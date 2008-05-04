package net.spy.memcached;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

/**
 * Test stuff that can be tested within a MemcachedConnection separately.
 */
public class MemcachedConnectionTest extends TestCase {

	public void testDebugBuffer() throws Exception {
		String input="this is a test _";
		ByteBuffer bb=ByteBuffer.wrap(input.getBytes());
		String s=MemcachedLowLevelIO.dbgBuffer(bb, input.length());
		assertEquals("this is a test \\x5f", s);
	}

}
