package net.spy.memcached.transcoders;

import junit.framework.TestCase;
import net.spy.memcached.CachedData;

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

	public void testBadFlags() throws Exception {
		CachedData cd=tc.encode(9284L);
		assertNull(tc.decode(new CachedData(cd.getFlags()+1, cd.getData(),
				CachedData.MAX_SIZE)));
	}
}
