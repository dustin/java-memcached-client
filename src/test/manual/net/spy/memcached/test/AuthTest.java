package net.spy.memcached.test;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.OperationException;

/**
 * Loader performance test.
 */
public class AuthTest extends SpyObject implements Runnable {

	private final String username;
	private final String password;
	private MemcachedClient client;

	public AuthTest(String u, String p) {
		username = u;
		password = p;
	}

	public void init() throws Exception {
		client = new MemcachedClient(
				new BinaryConnectionFactory(),
				AddrUtil.getAddresses("localhost:11212"));
	}

	public void shutdown() throws Exception {
		client.shutdown();
	}

	public void run() {
		try {
			client.authenticate(username, password);
		} catch(OperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] a) throws Exception {
		AuthTest lt = new AuthTest("testuser", "testpass");
		lt.init();
		long start = System.currentTimeMillis();
		try {
			lt.run();
		} finally {
			lt.shutdown();
		}
		long end = System.currentTimeMillis();
		System.out.println("Runtime:  " + (end - start) + "ms");
	}

}
