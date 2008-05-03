package net.spy.memcached;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.test.SyncThread;


public abstract class ProtocolBaseCase extends ClientBaseCase {

	public void testAssertions() {
		try {
			assert false;
			fail("Assertions are not enabled.");
		} catch(AssertionError e) {
			// ok
		}
	}

	public void testNoop() {
		// This runs through the startup/flush cycle
	}

	public void testDoubleShutdown() {
		client.shutdown();
		client.shutdown();
	}

	public void testSimpleGet() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.get("test1"));
	}

	public void testSimpleCASGets() throws Exception {
		assertNull(client.gets("test1"));
		client.set("test1", 5, "test1value");
		assertEquals("test1value", client.gets("test1").getValue());
	}

	public void testCAS() throws Exception {
		final String key="castestkey";
		// First, make sure it doesn't work for a non-existing value.
		assertSame("Expected error CASing with no existing value.",
			CASResponse.NOT_FOUND,
			client.cas(key, 0x7fffffffffl, "bad value"));

		// OK, stick a value in here.
		assertTrue(client.add(key, 5, "original value").get());
		CASValue<?> getsVal = client.gets(key);
		assertEquals("original value", getsVal.getValue());

		// Now try it with an existing value, but wrong CAS id
		assertSame("Expected error CASing with invalid id",
			CASResponse.EXISTS,
			client.cas(key, getsVal.getCas() + 1, "broken value"));
		// Validate the original value is still in tact.
		assertEquals("original value", getsVal.getValue());

		// OK, now do a valid update
		assertSame("Expected successful CAS with correct id ("
			+ getsVal.getCas() + ")",
			CASResponse.OK,
			client.cas(key, getsVal.getCas(), "new value"));
		assertEquals("new value", client.get(key));

		// Test a CAS replay
		assertSame("Expected unsuccessful CAS with replayed id",
			CASResponse.EXISTS,
			client.cas(key, getsVal.getCas(), "crap value"));
		assertEquals("new value", client.get(key));
	}

	public void testExtendedUTF8Key() throws Exception {
		String key="\u2013\u00ba\u2013\u220f\u2014\u00c4";
		assertNull(client.get(key));
		client.set(key, 5, "test1value");
		assertEquals("test1value", client.get(key));
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

	public void testInvalidKey3() throws Exception {
		try {
			Object val=client.get("Key\n");
			fail("Expected IllegalArgumentException, got " + val);
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testInvalidKey4() throws Exception {
		try {
			Object val=client.get("Key\r");
			fail("Expected IllegalArgumentException, got " + val);
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testInvalidKey5() throws Exception {
		try {
			Object val=client.get("Key\0");
			fail("Expected IllegalArgumentException, got " + val);
		} catch(IllegalArgumentException e) {
			// pass
		}
	}


	public void testInvalidKeyBulk() throws Exception {
		try {
			Object val=client.getBulk("Key key2");
			fail("Expected IllegalArgumentException, got " + val);
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testInvalidTranscoder() {
		try {
			client.setTranscoder(null);
			fail("Allowed null transcoder.");
		} catch(NullPointerException e) {
			assertEquals("Can't use a null transcoder", e.getMessage());
		}
	}

	public void testSetTranscoder() {
		Transcoder<Object> tc=client.getTranscoder();
		assertTrue(tc instanceof SerializingTranscoder);
		Transcoder<Object> tmptc=new Transcoder<Object>(){
			public Object decode(CachedData d) {
				throw new RuntimeException("Not implemented.");
			}
			public CachedData encode(Object o) {
				throw new RuntimeException("Not implemented.");
			}};
		client.setTranscoder(tmptc);
		assertSame(tmptc, client.getTranscoder());
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

	public void testParallelSetMultiGet() throws Throwable {
		int cnt=SyncThread.getDistinctResultCount(10, new Callable<Boolean>(){
			public Boolean call() throws Exception {
				for(int i=0; i<10; i++) {
					client.set("test" + i, 5, "value" + i);
					assertEquals("value" + i, client.get("test" + i));
				}
				Map<String, Object> m=client.getBulk("test0", "test1", "test2",
					"test3", "test4", "test5", "test6", "test7", "test8",
					"test9", "test10"); // Yes, I intentionally ran over.
				for(int i=0; i<10; i++) {
					assertEquals("value" + i, m.get("test" + i));
				}
				return Boolean.TRUE;
			}});
		assertEquals(1, cnt);
	}

	public void testParallelSetAutoMultiGet() throws Throwable {
		int cnt=SyncThread.getDistinctResultCount(10, new Callable<Boolean>(){
			public Boolean call() throws Exception {
				client.set("testparallel", 5, "parallelvalue");
				for(int i=0; i<10; i++) {
					assertEquals("parallelvalue", client.get("testparallel"));
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

	public void testAddWithTranscoder() throws Exception {
		Transcoder<String> t=new TestTranscoder();
		assertNull(client.get("test1", t));
		assertTrue(client.set("test1", 5, "test1value", t).get());
		assertEquals("test1value", client.get("test1", t));
		assertFalse(client.add("test1", 5, "ignoredvalue", t).get());
		// Should return the original value
		assertEquals("test1value", client.get("test1", t));
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

	public void testUpdateWithTranscoder() throws Exception {
		Transcoder<String> t=new TestTranscoder();
		assertNull(client.get("test1", t));
		client.replace("test1", 5, "test1value", t);
		assertNull(client.get("test1", t));
	}

	// Just to make sure the sequence is being handled correctly
	public void testMixedSetsAndUpdates() throws Exception {
		Collection<Future<Boolean>> futures=new ArrayList<Future<Boolean>>();
		Collection<String> keys=new ArrayList<String>();
		for(int i=0; i<100; i++) {
			String key="k" + i;
			futures.add(client.set(key, 10, key));
			futures.add(client.add(key, 10, "a" + i));
			keys.add(key);
		}
		Map<String, Object> m=client.getBulk(keys);
		assertEquals(100, m.size());
		for(Map.Entry<String, Object> me : m.entrySet()) {
			assertEquals(me.getKey(), me.getValue());
		}
		for(Iterator<Future<Boolean>> i=futures.iterator();i.hasNext();) {
			assertTrue(i.next().get());
			assertFalse(i.next().get());
		}
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

	public void testGetBulkVarargWithTranscoder() throws Exception {
		Transcoder<String> t=new TestTranscoder();
		assertEquals(0, client.getBulk(t, "test1", "test2", "test3").size());
		client.set("test1", 5, "val1", t);
		client.set("test2", 5, "val2", t);
		Map<String, String> vals=client.getBulk(t, "test1", "test2", "test3");
		assertEquals(2, vals.size());
		assertEquals("val1", vals.get("test1"));
		assertEquals("val2", vals.get("test2"));
	}

	protected abstract String getExpectedVersionSource();

	public void testGetVersions() throws Exception {
		Map<SocketAddress, String> vs=client.getVersions();
		assertEquals(1, vs.size());
		Map.Entry<SocketAddress, String> me=vs.entrySet().iterator().next();
		assertEquals(getExpectedVersionSource(), me.getKey().toString());
		assertNotNull(me.getValue());
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
		String key="deleteFuture";
		assertNull(client.get(key));
		client.set(key, 5, "test1value");
		assertEquals("test1value", client.get(key));
		Future<Boolean> f=client.delete(key);
		assertNull(client.get(key));
		assertTrue("Deletion didn't return true", f.get());
		assertFalse("Second deletion returned true",
			client.delete(key).get());
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

	public void testGracefulShutdown() throws Exception {
		for(int i=0; i<1000; i++) {
			client.set("t" + i, 10, i);
		}
		assertTrue("Couldn't shut down within five seconds",
			client.shutdown(5, TimeUnit.SECONDS));

		// Get a new client
		initClient();
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
		for(int i=0; i<10000; i++) {
			client.set("t" + i, 10, i);
		}
		assertFalse("Weird, shut down too fast",
			client.shutdown(1, TimeUnit.MILLISECONDS));

		try {
			Map<SocketAddress, String> m = client.getVersions();
			fail("Expected failure, got " + m);
		} catch(IllegalStateException e) {
			assertEquals("Shutting down", e.getMessage());
		}

		// Get a new client
		initClient();
	}

	public void testStupidlyLargeSet() throws Exception {
		Random r=new Random();
		SerializingTranscoder st=new SerializingTranscoder();
		st.setCompressionThreshold(Integer.MAX_VALUE);
		client.setTranscoder(st);

		byte data[]=new byte[10*1024*1024];
		r.nextBytes(data);

		try {
			client.set("bigassthing", 60, data).get();
			fail("Didn't fail setting bigass thing.");
		} catch(ExecutionException e) {
			e.printStackTrace();
			OperationException oe=(OperationException)e.getCause();
			assertSame(OperationErrorType.SERVER, oe.getType());
		}

		// But I should still be able to do something.
		client.set("k", 5, "Blah");
		assertEquals("Blah", client.get("k"));
	}

	public void testQueueAfterShutdown() throws Exception {
		client.shutdown();
		try {
			Object o=client.get("k");
			fail("Expected IllegalStateException, got " + o);
		} catch(IllegalStateException e) {
			// OK
		} finally {
			initClient(); // init for tearDown
		}
	}

	public void testMultiReqAfterShutdown() throws Exception {
		client.shutdown();
		try {
			Map<String, ?> m=client.getBulk("k1", "k2", "k3");
			fail("Expected IllegalStateException, got " + m);
		} catch(IllegalStateException e) {
			// OK
		} finally {
			initClient(); // init for tearDown
		}
	}

	public void testBroadcastAfterShutdown() throws Exception {
		client.shutdown();
		try {
			Future<?> f=client.flush();
			fail("Expected IllegalStateException, got " + f.get());
		} catch(IllegalStateException e) {
			// OK
		} finally {
			initClient(); // init for tearDown
		}
	}

	public void testABunchOfCancelledOperations() throws Exception {
		Collection<Future<?>> futures=new ArrayList<Future<?>>();
		for(int i=0; i<1000; i++) {
			futures.add(client.set("x", 5, "xval"));
			futures.add(client.asyncGet("x"));
		}
		Future<Boolean> sf=client.set("x", 5, "myxval");
		Future<Object> gf=client.asyncGet("x");
		for(Future<?> f : futures) {
			f.cancel(true);
		}
		assertTrue(sf.get());
		assertEquals("myxval", gf.get());
	}

	public void testUTF8Key() throws Exception {
		final String key = "junit.Здравствуйте." + System.currentTimeMillis();
		final String value = "Skiing rocks if you can find the time to go!";

		assertTrue(client.set(key, 6000, value).get());
		Object output = client.get(key);
		assertNotNull("output is null", output);
		assertEquals("output is not equal", value, output);
	}

	public void testUTF8KeyDelete() throws Exception {
		final String key = "junit.Здравствуйте." + System.currentTimeMillis();
		final String value = "Skiing rocks if you can find the time to go!";

		assertTrue(client.set(key, 6000, value).get());
		assertTrue(client.delete(key).get());
		assertNull(client.get(key));
	}

	public void testUTF8MultiGet() throws Exception {
		final String value = "Skiing rocks if you can find the time to go!";
		Collection<String> keys=new ArrayList<String>();
		for(int i=0; i<50; i++) {
			final String key = "junit.Здравствуйте."
				+ System.currentTimeMillis() + "." + i;
			assertTrue(client.set(key, 6000, value).get());
			keys.add(key);
		}

		Map<String, Object> vals = client.getBulk(keys);
		assertEquals(keys.size(), vals.size());
		for(Object o : vals.values()) {
			assertEquals(value, o);
		}
		assertTrue(keys.containsAll(vals.keySet()));
	}

	public void testUTF8Value() throws Exception {
		final String key = "junit.plaintext." + System.currentTimeMillis();
		final String value = "Здравствуйте Здравствуйте Здравствуйте "
			+ "Skiing rocks if you can find the time to go!";

		assertTrue(client.set(key, 6000, value).get());
		Object output = client.get(key);
		assertNotNull("output is null", output);
		assertEquals("output is not equal", value, output);
	}

	public void testAppend() throws Exception {
		final String key="append.key";
		assertTrue(client.set(key, 5, "test").get());
		assertTrue(client.append(0, key, "es").get());
		assertEquals("testes", client.get(key));
	}

	public void testPrepend() throws Exception {
		final String key="prepend.key";
		assertTrue(client.set(key, 5, "test").get());
		assertTrue(client.prepend(0, key, "es").get());
		assertEquals("estest", client.get(key));
	}

	private static class TestTranscoder implements Transcoder<String> {
		private final int flags=238885206;

		public String decode(CachedData d) {
			assert d.getFlags() == flags
				: "expected " + flags + " got " + d.getFlags();
			return new String(d.getData());
		}

		public CachedData encode(String o) {
			return new CachedData(flags, o.getBytes());
		}
	}
}
