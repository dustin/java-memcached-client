package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import net.spy.SpyObject;
import net.spy.memcached.ops.Operation;

/**
 * Default implementation of ConnectionFactory.
 *
 * <p>
 * This implementation creates connections where each server worker queue is
 * implemented using an ArrayBlockingQueue.
 * </p>
 */
public class DefaultConnectionFactory extends SpyObject
	implements ConnectionFactory {

	/**
	 * Maximum length of the operation queue returned by this connection
	 * factory.
	 */
	public static final int DEFAULT_OP_QUEUE_LEN=16384;

	/**
	 * The read buffer size for each server connection from this factory.
	 */
	public static final int DEFAULT_READ_BUFFER_SIZE=16384;

	private int opQueueLen=DEFAULT_OP_QUEUE_LEN;
	private int readBufSize=DEFAULT_READ_BUFFER_SIZE;

	/**
	 * Create a DefaultConnectionFactory with the default parameters.
	 */
	public DefaultConnectionFactory() {
		this(DEFAULT_OP_QUEUE_LEN, DEFAULT_READ_BUFFER_SIZE);
	}

	/**
	 * Create a DefaultConnectionFactory with the given maximum operation
	 * queue length, and the given read buffer size.
	 */
	public DefaultConnectionFactory(int qLen, int bufSize) {
		super();
		opQueueLen=qLen;
		readBufSize=bufSize;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#createConnection(java.util.List)
	 */
	public MemcachedConnection createConnection(List<InetSocketAddress> addrs)
		throws IOException {
		return new MemcachedConnection(readBufSize, this, addrs);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#createOperationQueue()
	 */
	public BlockingQueue<Operation> createOperationQueue() {
		return new ArrayBlockingQueue<Operation>(opQueueLen);
	}

}
