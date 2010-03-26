package net.spy.memcached;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import net.spy.memcached.protocol.binary.BinaryMemcachedNodeImpl;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;

/**
 * Default connection factory for binary wire protocol connections.
 */
public class BinaryConnectionFactory extends DefaultConnectionFactory {

	/**
	 * Create a DefaultConnectionFactory with the default parameters.
	 */
	public BinaryConnectionFactory() {
		super();
	}

	/**
	 * Create a BinaryConnectionFactory with the given maximum operation
	 * queue length, and the given read buffer size.
	 */
	public BinaryConnectionFactory(int len, int bufSize) {
		super(len, bufSize);
	}

	/**
	 * Construct a BinaryConnectionFactory with the given parameters.
	 *
	 * @param len the queue length.
	 * @param bufSize the buffer size
 	 * @param hash the algorithm to use for hashing
	 */
	public BinaryConnectionFactory(int len, int bufSize, HashAlgorithm hash) {
		super(len, bufSize, hash);
	}

	@Override
	public MemcachedNode createMemcachedNode(SocketAddress sa,
			SocketChannel c, int bufSize) {
		boolean doAuth = false;
		return new BinaryMemcachedNodeImpl(sa, c, bufSize,
			createReadOperationQueue(),
			createWriteOperationQueue(),
			createOperationQueue(),
			getOpQueueMaxBlockTime(),
			doAuth);
	}

	@Override
	public OperationFactory getOperationFactory() {
		return new BinaryOperationFactory();
	}

}
