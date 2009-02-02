package net.spy.memcached;

import java.net.SocketAddress;

import net.spy.memcached.compat.SpyObject;

/**
 * Test observer hooks.
 */
public class ObserverTest extends ClientBaseCase {

	public void testConnectionObserver() throws Exception {
		ConnectionObserver obs = new LoggingObserver();
		assertTrue(client.addObserver(obs));
		assertTrue(client.removeObserver(obs));
		assertFalse(client.removeObserver(obs));
	}

	static class LoggingObserver extends SpyObject
		implements ConnectionObserver {
		public void connectionEstablished(SocketAddress sa,
				int reconnectCount) {
			getLogger().info("Connection established to %s (%s)",
				sa, reconnectCount);
		}

		public void connectionLost(SocketAddress sa) {
			getLogger().info("Connection lost from %s", sa);
		}

	}
}
