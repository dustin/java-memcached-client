package net.spy.memcached.transcoders;

import junit.framework.TestCase;

/**
 * Test the long transcoder.
 */
public class LongTranscoderTest extends TestCase {

	private LongTranscoder tc=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tc=new LongTranscoder();
	}

	public void testLong() throws Exception {
		assertEquals(923, tc.decode(tc.encode(923L)).longValue());
	}
}
