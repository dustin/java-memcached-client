package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Factory for creating instances of MemcachedConnection.
 * This is used to provide more fine-grained configuration of connections.
 */
public interface ConnectionFactory {

	/**
	 * Create a MemcachedConnection for the given SocketAddresses.
	 *
	 * @param addrs the addresses of the memcached servers
	 * @return a new MemcachedConnection connected to those addresses
	 * @throws IOException for problems initializing the memcached connections
	 */
	MemcachedConnection createConnection(List<InetSocketAddress> addrs)
		throws IOException;

	/**
	 * Create a new memcached node.
	 */
	MemcachedNode createMemcachedNode(SocketAddress sa,
			SocketChannel c, int bufSize);

	/**
	 * Create a BlockingQueue for operations for a connection.
	 */
	BlockingQueue<Operation> createOperationQueue();

	/**
	 * Create a BlockingQueue for the operations currently expecting to read
	 * responses from memcached.
	 */
	BlockingQueue<Operation> createReadOperationQueue();

	/**
	 * Create a BlockingQueue for the operations currently expecting to write
	 * requests to memcached.
	 */
	BlockingQueue<Operation> createWriteOperationQueue();

	/**
	 * Get the maximum amount of time (in milliseconds) a client is willing
	 * to wait to add a new item to a queue.
	 */
	long getOpQueueMaxBlockTime();

	/**
	 * Create a NodeLocator instance for the given list of nodes.
	 */
	NodeLocator createLocator(List<MemcachedNode> nodes);

	/**
	 * Get the operation factory for connections built by this connection
	 * factory.
	 */
	OperationFactory getOperationFactory();

	/**
	 * Get the operation timeout used by this connection.
	 */
	long getOperationTimeout();

	/**
	 * If true, the IO thread should be a daemon thread.
	 */
	boolean isDaemon();

	/**
	 * If true, the nagle algorithm will be used on connected sockets.
	 *
	 * <p>
	 * See {@link java.net.Socket#setTcpNoDelay(boolean)} for more information.
	 * </p>
	 */
	boolean useNagleAlgorithm();

	/**
	 * Observers that should be established at the time of connection
	 * instantiation.
	 *
	 * These observers will see the first connection established.
	 */
	Collection<ConnectionObserver> getInitialObservers();

	/**
	 * Get the default failure mode for the underlying connection.
	 */
	FailureMode getFailureMode();

	/**
	 * Get the default transcoder to be used in connections created by this
	 * factory.
	 */
	Transcoder<Object> getDefaultTranscoder();

	/**
	 * If true, low-level optimization is in effect.
	 */
	boolean shouldOptimize();

	/*
	 * Get the read buffer size set at construct time.
	 */
	int getReadBufSize();

	/**
	 * Get the hash algorithm to be used.
	 */
	public HashAlgorithm getHashAlg();

	/**
	 * Maximum number of milliseconds to wait between reconnect attempts.
	 */
	long getMaxReconnectDelay();

	/**
	 * Authenticate connections using the given auth descriptor.
	 *
	 * @return null if no authentication should take place
	 */
	AuthDescriptor getAuthDescriptor();

	/**
	 * Maximum number of timeout exception for shutdown connection
	 */
	int getTimeoutExceptionThreshold();
}
