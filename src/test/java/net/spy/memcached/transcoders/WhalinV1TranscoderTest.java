package net.spy.memcached.transcoders;

import net.spy.memcached.CachedData;

public class WhalinV1TranscoderTest extends BaseTranscoderCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setTranscoder(new WhalinV1Transcoder());
	}

	@Override
	public void testByteArray() throws Exception {
		byte[] a={'a', 'b', 'c'};
		try {
			CachedData cd=getTranscoder().encode(a);
			fail("Expected IllegalArgumentException, got " + cd);
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

}
