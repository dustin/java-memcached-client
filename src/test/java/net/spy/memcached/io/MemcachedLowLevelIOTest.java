package net.spy.memcached.io;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

/**
 * Test stuff that can be tested within a MemcachedLowLevelIO separately.
 */
public class MemcachedLowLevelIOTest extends TestCase {

	public void testDebugBuffer() throws Exception {
		String input="this is a test _";
		ByteBuffer bb=ByteBuffer.wrap(input.getBytes());
		String s=MemcachedLowLevelIO.dbgBuffer(bb, input.length());
		assertEquals("this is a test \\x5f", s);
	}

}
