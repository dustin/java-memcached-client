package net.spy.memcached.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.compat.BaseMockCase;
import net.spy.memcached.internal.ImmediateFuture;

import org.jmock.Mock;

/**
 * Test the cache loader.
 */
public class CacheLoaderTest extends BaseMockCase {

	private ExecutorService es = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		BlockingQueue<Runnable> wq = new LinkedBlockingQueue<Runnable>();
		es = new ThreadPoolExecutor(10, 10, 5*60, TimeUnit.SECONDS, wq);
	}

	@Override
	protected void tearDown() throws Exception {
		es.shutdownNow();
		super.tearDown();
	}

	public void testSimpleLoading() throws Exception {
		Mock m = mock(MemcachedClientIF.class);

		LoadCounter sl = new LoadCounter();
		CacheLoader cl = new CacheLoader((MemcachedClientIF)m.proxy(),
				es, sl, 0);

		m.expects(once()).method("set").with(eq("a"), eq(0), eq(1))
			.will(returnValue(new ImmediateFuture(true)));
		m.expects(once()).method("set").with(eq("a"), eq(0), eq(1))
			.will(throwException(new IllegalStateException("Full up")));
		m.expects(once()).method("set").with(eq("b"), eq(0), eq(2))
			.will(returnValue(new ImmediateFuture(new RuntimeException("blah"))));
		m.expects(once()).method("set").with(eq("c"), eq(0), eq(3))
			.will(returnValue(new ImmediateFuture(false)));

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("a", 1);
		map.put("b", 2);
		map.put("c", 3);

		// Load the cache and wait for it to finish.
		cl.loadData(map).get();
		es.shutdown();
		es.awaitTermination(1, TimeUnit.SECONDS);

		assertEquals(1, sl.success);
		assertEquals(1, sl.exceptions);
		assertEquals(1, sl.failure);
	}

	static class LoadCounter implements CacheLoader.StorageListener {

		public volatile int exceptions = 0;
		public volatile int success = 0;
		public volatile int failure = 0;

		public void errorStoring(String k, Exception e) {
			exceptions++;
		}

		public void storeResult(String k, boolean result) {
			if(result) {
				success++;
			} else {
				failure++;
			}
		}

	}

}
