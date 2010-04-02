package net.spy.memcached.transcoders;

import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.spy.memcached.CachedData;

/**
 * Test the transcode service.
 */
public class TranscodeServiceTest extends TestCase {

	private TranscodeService ts = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ts = new TranscodeService(false);
	}

	@Override
	protected void tearDown() throws Exception {
		ts.shutdown();
		assertTrue(ts.isShutdown());
		super.tearDown();
	}

	public void testNonExecuting() throws Exception {
		CachedData cd = new CachedData(0, new byte[0], 0);
		Future<String> fs = ts.decode(new TestTranscoder(), cd);
		assertEquals("Stuff!", fs.get());
	}

	public void testExecuting() throws Exception {
		CachedData cd = new CachedData(1, new byte[0], 0);
		Future<String> fs = ts.decode(new TestTranscoder(), cd);
		assertEquals("Stuff!", fs.get());
	}

	private static final class TestTranscoder implements Transcoder<String> {

		public boolean asyncDecode(CachedData d) {
			return d.getFlags() == 1;
		}

		public String decode(CachedData d) {
			return "Stuff!";
		}

		public CachedData encode(String o) {
			throw new RuntimeException("Not invoked.");
		}

		public int getMaxSize() {
			return 5;
		}

	}
}
