package net.spy.memcached.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.util.CacheLoader;

/**
 * Test a multiget that's sufficiently large as to get data before the
 * transision to read.
 *
 * Note that this is in manual tests currently because, while it predictably
 * demonstrates the problems, I don't believe it generally demonstrates good
 * behavior for a unit test.
 */
public class ExcessivelyLargeGetTest extends SpyObject implements Runnable {

	// How many keys to do
	private static final int N = 25000;

	private final MemcachedClient client;
	private final Collection<String> keys;
	private final byte[] value = new byte[4096];

	public ExcessivelyLargeGetTest() throws Exception {
		client = new MemcachedClient(new ConnectionFactoryBuilder()
				.setProtocol(Protocol.BINARY)
				.setOpTimeout(15000)
				.build(),
				AddrUtil.getAddresses("127.0.0.1:11211"));
		keys = new ArrayList<String>(N);
		new Random().nextBytes(value);
	}

	public void run() {
		int nullKey = 0;
		// Load up a bunch of data.
		CacheLoader cl = new CacheLoader(client);
		for(int i = 0; i<N; i++) {
			String k = "multi." + i;
			keys.add(k);
			cl.push(k, value);
		}

		Map<String, Object> got = client.getBulk(keys);
		for(String k : keys) {
			if(got.containsKey(k)) {
				assert Arrays.equals(value, (byte[])got.get(k))
					: "Incorrect result at " + k;
			} else {
				nullKey++;
			}
		}
		System.out.println("Fetched " + got.size() + "/" + keys.size()
				+ " (" + nullKey + " were null)");
	}

	public static void main(String[] args) throws Exception {
		new ExcessivelyLargeGetTest().run();
	}
}
