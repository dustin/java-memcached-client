package net.spy.memcached;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Test the various memcached client constructors.
 */
public class MemcachedClientConstructorTest extends TestCase {

	private MemcachedClient client=null;

	@Override
	protected void tearDown() throws Exception {
		if(client != null) {
			client.shutdown();
		}
		super.tearDown();
	}

	private void assertWorking() throws Exception {
		Map<SocketAddress, String> versions = client.getVersions();
		assertEquals("/127.0.0.1:11211",
			versions.keySet().iterator().next().toString());
	}

	private void assertArgRequired(IllegalArgumentException e) {
		assertEquals("You must have at least one server to connect to",
			e.getMessage());
	}

	public void testVarargConstructor() throws Exception {
		client = new MemcachedClient(
			new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 11211));
		assertWorking();
	}

	public void testEmptyVarargConstructor() throws Exception {
		try {
			client = new MemcachedClient();
			fail("Expected illegal arg exception, got " + client);
		} catch(IllegalArgumentException e) {
			assertArgRequired(e);
		}
	}

	public void testNulListConstructor() throws Exception {
		try {
			List<InetSocketAddress> l=null;
			client = new MemcachedClient(l);
			fail("Expected null pointer exception, got " + client);
		} catch(NullPointerException e) {
			assertEquals("Server list required", e.getMessage());
		}
	}

	public void testEmptyListConstructor() throws Exception {
		try {
			client = new MemcachedClient(
				Collections.<InetSocketAddress>emptyList());
			fail("Expected illegal arg exception, got " + client);
		} catch(IllegalArgumentException e) {
			assertArgRequired(e);
		}
	}

	public void testNullFactoryConstructor() throws Exception {
		try {
			client = new MemcachedClient(null,
				AddrUtil.getAddresses("127.0.0.1:11211"));
			fail("Expected null pointer exception, got " + client);
		} catch(NullPointerException e) {
			assertEquals("Connection factory required", e.getMessage());
		}
	}

	public void testConnFactoryWithoutOpFactory() throws Exception {
		try {
			client = new MemcachedClient(new DefaultConnectionFactory(){
				@Override
				public OperationFactory getOperationFactory() {
					return null;
				}
			}, AddrUtil.getAddresses("127.0.0.1:11211"));
		} catch(AssertionError e) {
			assertEquals("Connection factory failed to make op factory",
				e.getMessage());
		}
	}

	public void testConnFactoryWithoutConns() throws Exception {
		try {
			client = new MemcachedClient(new DefaultConnectionFactory(){
				@Override
				public MemcachedConnection createConnection(
						List<InetSocketAddress> addrs) throws IOException {
					return null;
				}
			}, AddrUtil.getAddresses("127.0.0.1:11211"));
		} catch(AssertionError e) {
			assertEquals("Connection factory failed to make a connection",
				e.getMessage());
		}

	}

}
