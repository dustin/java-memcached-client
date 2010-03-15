package net.spy.memcached.auth;

import java.util.HashMap;
import java.util.Map;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.compat.SpyObject;

/**
 * This will ensure no more than one AuthThread will exist for a given
 * MemcachedNode.
 */
public class AuthThreadMonitor extends SpyObject {

	private Map<Object, AuthThread> nodeMap;

	public AuthThreadMonitor() {
		nodeMap = new HashMap<Object, AuthThread>();
	}

	/**
	 *
	 * Authenticate a new connection.  This is typically used by a
	 * MemcachedNode in order to authenticate a connection right after it
	 * has been established.
	 *
	 * If an old, but not yet completed authentication exists this will
	 * stop it in order to create a new authentication attempt.
	 *
	 * @param conn
	 * @param opFact
	 * @param authDescriptor
	 * @param node
	 */
	public synchronized void authConnection(MemcachedConnection conn,
		OperationFactory opFact, AuthDescriptor authDescriptor, MemcachedNode node) {
			interruptOldAuth(node);
			AuthThread newSASLAuthenticator = new AuthThread(conn, opFact,
				authDescriptor, node);
			nodeMap.put(node, newSASLAuthenticator);
	}

	private void interruptOldAuth(MemcachedNode nodeToStop) {
		AuthThread toStop = nodeMap.get(nodeToStop);
		if (toStop != null) {
			if (toStop.isAlive()) {
				getLogger().warn("Incomplete authentication interrupted for node " +
					nodeToStop);
				toStop.interrupt();
			}

			nodeMap.remove(nodeToStop);
		}
	}
}
