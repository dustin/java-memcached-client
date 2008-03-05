package net.spy.memcached.transcoders;

import junit.framework.TestCase;

/**
 * Test the integer transcoder.
 */
public class IntegerTranscoderTest extends TestCase {

	private IntegerTranscoder tc=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tc=new IntegerTranscoder();
	}

	public void testInt() throws Exception {
		assertEquals(923, tc.decode(tc.encode(923)).intValue());
	}
}
