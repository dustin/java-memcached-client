package net.spy.memcached.test;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.compat.SpyObject;

/**
 * Authentication functional test.
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
				new ConnectionFactoryBuilder()
					.setProtocol(Protocol.BINARY)
					.setAuthDescriptor(AuthDescriptor.typical(username, password))
					.build(),
				AddrUtil.getAddresses("localhost:11212"));
	}

	public void shutdown() throws Exception {
		client.shutdown();
	}

	public void run() {
		System.out.println("Available mechs:  "
				+ client.listSaslMechanisms());
		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		client.getVersions();
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
