package net.spy.memcached;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.util.AddrUtil;

/**
 * Small test program that does a bunch of sets in a tight loop.
 */
public class DoLotsOfSets {

	public static void main(String[] args) throws Exception {
		// Create a client with a queue big enough to hold the 300,000 items
		// we're going to add.
		MemcachedClient client=new MemcachedClientImpl(
			new DefaultConnectionFactory(350000, 32768),
			AddrUtil.getAddresses("localhost:11211"));
		long start=System.currentTimeMillis();
		byte[] toStore=new byte[26];
		Arrays.fill(toStore, (byte)'a');
		for(int i=0; i<300000; i++) {
			client.set("k" + i, 300, toStore);
		}
		long added=System.currentTimeMillis();
		System.err.printf("Finished queuing in %sms%n", added-start);
		client.waitForQueues(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		long end=System.currentTimeMillis();
		System.err.printf("Completed everything in %sms (%sms to flush)%n",
				end-start, end-added);
		Map<String, Object> m = client.getBulk("k1", "k2", "k3", "k4", "k5",
				"k299999", "k299998", "k299997", "k299996");
		assert m.size() == 9 : "Expected 9 results, got " + m;
		client.shutdown();
	}
}
