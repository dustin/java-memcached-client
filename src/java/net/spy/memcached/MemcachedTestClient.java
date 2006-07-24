// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 53D321C2-853C-4A80-8045-9C46E350CAC7

package net.spy.memcached;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.spy.SpyObject;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.StoreOperation;

public class MemcachedTestClient extends SpyObject {

	public static void main(String args[]) throws Exception {
		final MemcachedClient c=new MemcachedClient(
				new InetSocketAddress("localhost", 11211));
		try {
			assert false;
			throw new RuntimeException("Assertions not enabled.");
		} catch(AssertionError e) {
			// ok
		}
		c.start();

		// Small delay to make sure we aren't getting something in the queue
		// so early that the write just happens to be available.  We want to
		// make sure the write is available for the next add only.
		Thread.sleep(100);

		System.out.println(c.getVersions());

		// Do a big'un
		byte b[]=new byte[128*1024];
		Arrays.fill(b, (byte)'a');
		c.storeAsync(StoreOperation.StoreType.add, "big", 60,
				Arrays.toString(b),
				new StoreOperation.Callback() {
					public void storeResult(String val) {
						System.out.println("Big store result:  " + val);
					}
			});

		for(String s : new String[]{"a", "b", "c"}) {
			System.out.println(c.add(s, 10, "hello\r\n" + s));
		}

		c.asyncGet(new GetOperation.Callback() {
			public void gotData(String key, int flags, byte[] data) {
				System.out.printf("Got data for %s (%d): %s\n",
						key, flags, new String(data));
			}
			public void getComplete() {
				System.out.println("Get complete!");
			}}, "a", "b", "c");

		System.out.println("Sync get(a): " + c.get("a"));
		assert c.get("r") == null;
		System.out.println("Bulk get: " + c.get("a", "b", "c"));

		c.delete("b");
		c.delete("r");
		System.out.println("Bulk get2: " + c.get("a", "b", "c"));

		System.out.println("Stats: " + c.getStats());

		c.delete("i");
		c.delete("d");
		System.out.println("incr(1):  " + c.incr("i", 3, 7));
		System.out.println("incr(2):  " + c.incr("i", 3, 7));
		System.out.println("decr(1):  " + c.decr("d", 3, 7));
		System.out.println("decr(2):  " + c.decr("d", 3, 7));

		Map<String, Date> m=new HashMap<String, Date>();
		m.put("o1", new Date());
		m.put("o2", new Date());
		m.put("o3", new Date());
		c.set("testobj", 60, m);
		Object o=c.get("testobj");
		System.out.printf("Object retrieval: %s (%s)\n", o, o.getClass());

		// c.flush(5);

		// Give a bit of time to try to not be in an operation when this goes.
		Thread.sleep(100);
		c.shutdown();
	}
}
