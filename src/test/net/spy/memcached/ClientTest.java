package net.spy.memcached;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
		assertTrue(client.flush().get());
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
		assertTrue(client.set("test1", 5, "test1value").get());
		assertEquals("test1value", client.get("test1"));
		assertFalse(client.add("test1", 5, "ignoredvalue").get());
		// Should return the original value
		assertEquals("test1value", client.get("test1"));
	}

	public void testAddNotSerializable() throws Exception {
		try {
			client.add("t1", 5, new Object());
			fail("expected illegal argument exception");
		} catch(IllegalArgumentException e) {
			assertEquals("Non-serializable object", e.getMessage());
		}
	}

	public void testSetNotSerializable() throws Exception {
		try {
			client.set("t1", 5, new Object());
			fail("expected illegal argument exception");
		} catch(IllegalArgumentException e) {
			assertEquals("Non-serializable object", e.getMessage());
		}
	}

	public void testReplaceNotSerializable() throws Exception {
		try {
			client.replace("t1", 5, new Object());
			fail("expected illegal argument exception");
		} catch(IllegalArgumentException e) {
			assertEquals("Non-serializable object", e.getMessage());
		}
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

	public void testDeleteFuture() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.get("test1"));
		Future<Boolean> f=client.delete("test1");
		assertNull(client.get("test1"));
		assertTrue("Deletion didn't return true", f.get());
		assertFalse("Second deletion returned true",
			client.delete("test1").get());
	}

	public void testDelayedDelete() throws Exception {
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

	public void testDelayedFlush() throws Exception {
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

	public void testFlush() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		client.set("test2", 5, "test2value");
		assertEquals("test1value", client.get("test1"));
		assertEquals("test2value", client.get("test2"));
		assertTrue(client.flush().get());
		assertNull(client.get("test1"));
		assertNull(client.get("test2"));
	}

	public void testGetKeys() throws Exception {
		client.set("test1", 5, "test1value");
		client.set("test2", 5, "test2value");
		Collection<String> ks=client.findKeys("");
		assertEquals(2, ks.size());
		assertTrue(ks.contains("test1"));
		assertTrue(ks.contains("test2"));
	}

	public void testGracefulShutdown() throws Exception {
		for(int i=0; i<1000; i++) {
			client.set("t" + i, 10, i);
		}
		assertTrue("Couldn't shut down within five seconds",
			client.shutdown(5, TimeUnit.SECONDS));

		// Get a new client
		client=new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
		Collection<String> keys=new ArrayList<String>();
		for(int i=0; i<1000; i++) {
			keys.add("t" + i);
		}
		Map<String, Object> m=client.getBulk(keys);
		assertEquals(1000, m.size());
		for(int i=0; i<1000; i++) {
			assertEquals(i, m.get("t" + i));
		}
	}

	public void testGracefulShutdownTooSlow() throws Exception {
		for(int i=0; i<1000; i++) {
			client.set("t" + i, 10, i);
		}
		assertFalse("Weird, shut down too fast",
			client.shutdown(3, TimeUnit.MILLISECONDS));

		try {
			Map<SocketAddress, String> m = client.getVersions();
			fail("Expected failure, got " + m);
		} catch(IllegalStateException e) {
			assertEquals("Shutting down", e.getMessage());
		}

		// Get a new client
		client=new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
	}
}
