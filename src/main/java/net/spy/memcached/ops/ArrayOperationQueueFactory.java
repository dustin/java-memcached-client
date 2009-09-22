package net.spy.memcached.ops;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * OperationQueueFactory that uses an ArrayBlockingQueue.
 */
public class ArrayOperationQueueFactory implements OperationQueueFactory {

	private final int capacity;

	/**
	 * Create an ArrayOperationQueueFactory that creates blocking queues with
	 * the given capacity.
	 *
	 * @param cap maximum size of a queue produced by this factory
	 */
	public ArrayOperationQueueFactory(int cap) {
		super();
		capacity = cap;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.ops.OperationQueueFactory#create()
	 */
	public BlockingQueue<Operation> create() {
		return new ArrayBlockingQueue<Operation>(capacity);
	}

}
