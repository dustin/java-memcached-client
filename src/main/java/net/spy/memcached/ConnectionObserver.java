package net.spy.memcached;

import java.net.SocketAddress;

/**
 * Users of this interface will be notified when changes to the state of
 * connections take place.
 */
public interface ConnectionObserver {

	/**
	 * A connection has just successfully been established on the given socket.
	 *
	 * @param sa the address of the node whose connection was established
	 * @param reconnectCount the number of attempts before the connection was
	 *                       established
	 */
	void connectionEstablished(SocketAddress sa, int reconnectCount);


	/**
	 * A connection was just lost on the given socket.
	 *
	 * @param sa the address of the node whose connection was lost
	 */
	void connectionLost(SocketAddress sa);
}
