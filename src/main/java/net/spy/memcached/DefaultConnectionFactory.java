package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.protocol.ascii.AsciiMemcachedNodeImpl;
import net.spy.memcached.protocol.ascii.AsciiOperationFactory;
import net.spy.memcached.protocol.binary.BinaryMemcachedNodeImpl;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Default implementation of ConnectionFactory.
 *
 * <p>
 * This implementation creates connections where the operation queue is an
 * ArrayBlockingQueue and the read and write queues are unbounded
 * LinkedBlockingQueues.  The <code>Redistribute</code> FailureMode is used
 * by default.
 * </p>
 */
public class DefaultConnectionFactory extends SpyObject
	implements ConnectionFactory {

	/**
	 * Default failure mode.
	 */
	public static final FailureMode DEFAULT_FAILURE_MODE =
		FailureMode.Redistribute;

	/**
	 * Default hash algorithm.
	 */
	public static final HashAlgorithm DEFAULT_HASH = HashAlgorithm.NATIVE_HASH;

	/**
	 * Maximum length of the operation queue returned by this connection
	 * factory.
	 */
	public static final int DEFAULT_OP_QUEUE_LEN=16384;

	/**
	 * The read buffer size for each server connection from this factory.
	 */
	public static final int DEFAULT_READ_BUFFER_SIZE=16384;

    /**
     * Default operation timeout in milliseconds.
     */
    public static final long DEFAULT_OPERATION_TIMEOUT = 1000;

    /**
     * Maximum amount of time (in seconds) to wait between reconnect attempts.
     */
    public static final long DEFAULT_MAX_RECONNECT_DELAY = 30;

	private final int opQueueLen;
	private final int readBufSize;
	private final HashAlgorithm hashAlg;

	/**
	 * Construct a DefaultConnectionFactory with the given parameters.
	 *
	 * @param qLen the queue length.
	 * @param bufSize the buffer size
	 * @param hash the algorithm to use for hashing
	 */
	public DefaultConnectionFactory(int qLen, int bufSize, HashAlgorithm hash) {
		super();
		opQueueLen=qLen;
		readBufSize=bufSize;
		hashAlg=hash;
	}

	/**
	 * Create a DefaultConnectionFactory with the given maximum operation
	 * queue length, and the given read buffer size.
	 */
	public DefaultConnectionFactory(int qLen, int bufSize) {
		this(qLen, bufSize, DEFAULT_HASH);
	}

	/**
	 * Create a DefaultConnectionFactory with the default parameters.
	 */
	public DefaultConnectionFactory() {
		this(DEFAULT_OP_QUEUE_LEN, DEFAULT_READ_BUFFER_SIZE);
	}

	public MemcachedNode createMemcachedNode(SocketAddress sa,
			SocketChannel c, int bufSize) {

		OperationFactory of = getOperationFactory();
		if(of instanceof AsciiOperationFactory) {
			return new AsciiMemcachedNodeImpl(sa, c, bufSize,
				createReadOperationQueue(),
				createWriteOperationQueue(),
				createOperationQueue());
		} else if(of instanceof BinaryOperationFactory) {
			return new BinaryMemcachedNodeImpl(sa, c, bufSize,
					createReadOperationQueue(),
					createWriteOperationQueue(),
					createOperationQueue());
		} else {
			throw new IllegalStateException(
				"Unhandled operation factory type " + of);
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#createConnection(java.util.List)
	 */
	public MemcachedConnection createConnection(List<InetSocketAddress> addrs)
		throws IOException {
		return new MemcachedConnection(getReadBufSize(), this, addrs,
			getInitialObservers(), getFailureMode(), getOperationFactory());
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getFailureMode()
	 */
	public FailureMode getFailureMode() {
		return DEFAULT_FAILURE_MODE;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#createOperationQueue()
	 */
	public BlockingQueue<Operation> createOperationQueue() {
		return new ArrayBlockingQueue<Operation>(getOpQueueLen());
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#createReadOperationQueue()
	 */
	public BlockingQueue<Operation> createReadOperationQueue() {
		return new LinkedBlockingQueue<Operation>();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#createWriteOperationQueue()
	 */
	public BlockingQueue<Operation> createWriteOperationQueue() {
		return new LinkedBlockingQueue<Operation>();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#createLocator(java.util.List)
	 */
	public NodeLocator createLocator(List<MemcachedNode> nodes) {
		return new ArrayModNodeLocator(nodes, getHashAlg());
	}

	/**
	 * Get the op queue length set at construct time.
	 */
	public int getOpQueueLen() {
		return opQueueLen;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getReadBufSize()
	 */
	public int getReadBufSize() {
		return readBufSize;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getHashAlg()
	 */
	public HashAlgorithm getHashAlg() {
		return hashAlg;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getOperationFactory()
	 */
	public OperationFactory getOperationFactory() {
		return new AsciiOperationFactory();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getOperationTimeout()
	 */
	public long getOperationTimeout() {
		return DEFAULT_OPERATION_TIMEOUT;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#isDaemon()
	 */
	public boolean isDaemon() {
		return false;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getInitialObservers()
	 */
	public Collection<ConnectionObserver> getInitialObservers() {
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getDefaultTranscoder()
	 */
	public Transcoder<Object> getDefaultTranscoder() {
		return new SerializingTranscoder();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#useNagleAlgorithm()
	 */
	public boolean useNagleAlgorithm() {
		return false;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#shouldOptimize()
	 */
	public boolean shouldOptimize() {
		return true;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ConnectionFactory#getMaxReconnectDelay()
	 */
	public long getMaxReconnectDelay() {
		return DEFAULT_MAX_RECONNECT_DELAY;
	}

}
