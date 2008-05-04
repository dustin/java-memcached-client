package net.spy.memcached;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import net.spy.memcached.nodes.ArrayModNodeLocator;
import net.spy.memcached.nodes.KetamaNodeLocator;
import net.spy.memcached.ops.OperationFactory;
import net.spy.memcached.util.AddrUtil;

/**
 * Test the various memcached client constructors.
 */
public class MemcachedClientConstructorTest extends TestCase {

	private MemcachedClient client=null;

	@Override
	protected void tearDown() throws Exception {
		if(client != null) {
			try {
				client.shutdown();
			} catch(NullPointerException e) {
				// This is a workaround for a disagreement betweewn how things
				// should work in eclipse and buildr.  My plan is to upgrade to
				// junit4 all around and write some tests that are a bit easier
				// to follow.

				// The actual problem here is a client that isn't properly
				// initialized is attempting to be shut down.
			}
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
		client = new MemcachedClientImpl(
			new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 11211));
		assertWorking();
	}

	public void testEmptyVarargConstructor() throws Exception {
		try {
			client = new MemcachedClientImpl();
			fail("Expected illegal arg exception, got " + client);
		} catch(IllegalArgumentException e) {
			assertArgRequired(e);
		}
	}

	public void testNulListConstructor() throws Exception {
		try {
			List<InetSocketAddress> l=null;
			client = new MemcachedClientImpl(l);
			fail("Expected null pointer exception, got " + client);
		} catch(NullPointerException e) {
			assertEquals("Server list required", e.getMessage());
		}
	}

	public void testEmptyListConstructor() throws Exception {
		try {
			client = new MemcachedClientImpl(
				Collections.<InetSocketAddress>emptyList());
			fail("Expected illegal arg exception, got " + client);
		} catch(IllegalArgumentException e) {
			assertArgRequired(e);
		}
	}

	public void testNullFactoryConstructor() throws Exception {
		try {
			client = new MemcachedClientImpl(null,
				AddrUtil.getAddresses("127.0.0.1:11211"));
			fail("Expected null pointer exception, got " + client);
		} catch(NullPointerException e) {
			assertEquals("Connection factory required", e.getMessage());
		}
	}

	public void testNegativeTimeout() throws Exception {
		try {
			client = new MemcachedClientImpl(new DefaultConnectionFactory() {
				@Override
				public long getOperationTimeout() {
					return -1;
				}},
				AddrUtil.getAddresses("127.0.0.1:11211"));
			fail("Expected null pointer exception, got " + client);
		} catch(IllegalArgumentException e) {
			assertEquals("Operation timeout must be positive.", e.getMessage());
		}
	}

	public void testZeroTimeout() throws Exception {
		try {
			client = new MemcachedClientImpl(new DefaultConnectionFactory() {
				@Override
				public long getOperationTimeout() {
					return 0;
				}},
				AddrUtil.getAddresses("127.0.0.1:11211"));
			fail("Expected null pointer exception, got " + client);
		} catch(IllegalArgumentException e) {
			assertEquals("Operation timeout must be positive.", e.getMessage());
		}
	}

	public void testConnFactoryWithoutOpFactory() throws Exception {
		try {
			client = new MemcachedClientImpl(new DefaultConnectionFactory(){
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
			client = new MemcachedClientImpl(new DefaultConnectionFactory(){
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

	public void testArraymodNodeLocatorAccessor() throws Exception {
		client = new MemcachedClientImpl(AddrUtil.getAddresses("127.0.0.1:11211"));
		assertTrue(client.getNodeLocator() instanceof ArrayModNodeLocator);
		assertTrue(client.getNodeLocator().getPrimary("x")
			instanceof MemcachedNodeROImpl);
	}

	public void testKetamaNodeLocatorAccessor() throws Exception {
		client = new MemcachedClientImpl(new KetamaConnectionFactory(),
			AddrUtil.getAddresses("127.0.0.1:11211"));
		assertTrue(client.getNodeLocator() instanceof KetamaNodeLocator);
		assertTrue(client.getNodeLocator().getPrimary("x")
			instanceof MemcachedNodeROImpl);
	}

}
