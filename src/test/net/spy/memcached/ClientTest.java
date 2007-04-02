package net.spy.memcached;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import junit.framework.TestCase;
import net.spy.test.SyncThread;

/**
 * This test assumes a client is running on localhost:11211.
 */
public class ClientTest extends TestCase {

	MemcachedClient client=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		client=new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
	}

	@Override
	protected void tearDown() throws Exception {
		client.flush();
		// XXX:  This exists because flush is async.  We need to wait for the
		// flush to finish before shutting down.
		client.getVersions();
		client.shutdown();
		client=null;
		super.tearDown();
	}

	public void testAssertions() {
		try {
			assert false;
			fail("Assertions are not enabled.");
		} catch(AssertionError e) {
			// ok
		}
	}

	public void testSimpleGet() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.get("test1"));
	}

	public void testInvalidKey1() throws Exception {
		try {
			client.get("key with spaces");
			fail("Expected IllegalArgumentException getting key with spaces");
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testInvalidKey2() throws Exception {
		try {
			StringBuilder longKey=new StringBuilder();
			for(int i=0; i<251; i++) {
				longKey.append("a");
			}
			client.get(longKey.toString());
			fail("Expected IllegalArgumentException getting too long of a key");
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testParallelSetGet() throws Throwable {
		int cnt=SyncThread.getDistinctResultCount(10, new Callable<Boolean>(){
			public Boolean call() throws Exception {
				for(int i=0; i<10; i++) {
					client.set("test" + i, 5, "value" + i);
					assertEquals("value" + i, client.get("test" + i));
				}
				for(int i=0; i<10; i++) {
					assertEquals("value" + i, client.get("test" + i));
				}
				return Boolean.TRUE;
			}});
		assertEquals(1, cnt);
	}

	public void testAdd() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.get("test1"));
		client.add("test1", 5, "ignoredvalue");
		// Should return the original value
		assertEquals("test1value", client.get("test1"));
	}

	public void testUpdate() throws Exception {
		assertNull(client.get("test1"));
		client.replace("test1", 5, "test1value");
		assertNull(client.get("test1"));
	}

	public void testGetBulk() throws Exception {
		Collection<String> keys=Arrays.asList("test1", "test2", "test3");
		assertEquals(0, client.getBulk(keys).size());
		client.set("test1", 5, "val1");
		client.set("test2", 5, "val2");
		Map<String, Object> vals=client.getBulk(keys);
		assertEquals(2, vals.size());
		assertEquals("val1", vals.get("test1"));
		assertEquals("val2", vals.get("test2"));
	}

	public void testGetBulkVararg() throws Exception {
		assertEquals(0, client.getBulk("test1", "test2", "test3").size());
		client.set("test1", 5, "val1");
		client.set("test2", 5, "val2");
		Map<String, Object> vals=client.getBulk("test1", "test2", "test3");
		assertEquals(2, vals.size());
		assertEquals("val1", vals.get("test1"));
		assertEquals("val2", vals.get("test2"));
	}

	public void testGetVersions() throws Exception {
		Map<SocketAddress, String> vs=client.getVersions();
		assertEquals(1, vs.size());
		Map.Entry<SocketAddress, String> me=vs.entrySet().iterator().next();
		assertEquals("/127.0.0.1:11211", me.getKey().toString());
		assertNotNull(me.getValue());
	}

	public void testGetStats() throws Exception {
		Map<SocketAddress, Map<String, String>> stats = client.getStats();
		assertEquals(1, stats.size());
		Map<String, String> oneStat=stats.values().iterator().next();
		assertTrue(oneStat.containsKey("total_items"));
	}

	public void testNonexistentMutate() throws Exception {
		assertEquals(-1, client.incr("nonexistent", 1));
		assertEquals(-1, client.decr("nonexistent", 1));
	}

	public void testMutateWithDefault() throws Exception {
		assertEquals(3, client.incr("mtest", 1, 3));
		assertEquals(4, client.incr("mtest", 1, 3));
		assertEquals(3, client.decr("mtest", 1, 9));
		assertEquals(9, client.decr("mtest2", 1, 9));
	}

	public void testConcurrentMutation() throws Throwable {
		int num=SyncThread.getDistinctResultCount(10, new Callable<Long>(){
			public Long call() throws Exception {
				return client.incr("mtest", 1, 11);
			}});
		assertEquals(10, num);
	}

	public void testImmediateDelete() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.get("test1"));
		client.delete("test1");
		assertNull(client.get("test1"));
	}

	public void testFutureDelete() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.get("test1"));
		client.delete("test1", 5);
		assertNull(client.get("test1"));
		// Add should fail, even though the get returns null
		client.add("test1", 5, "test1value");
		assertNull(client.get("test1"));
		// Replace should also fail
		client.replace("test1", 5, "test1value");
		assertNull(client.get("test1"));
		// Set should be fine, though.
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.get("test1"));
	}

	public void testFutureFlush() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		client.set("test2", 5, "test2value");
		assertEquals("test1value", client.get("test1"));
		assertEquals("test2value", client.get("test2"));
		client.flush(2);
		Thread.sleep(2100);
		assertNull(client.get("test1"));
		assertNull(client.get("test2"));
	}
}
