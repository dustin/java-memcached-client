package net.spy.memcached;

import java.util.concurrent.CountDownLatch;

import net.spy.memcached.protocol.ascii.OperationImpl;

/**
 * Factory for creating Operations to be broadcast.
 */
public interface OperationFactory {

	/**
	 * Construct a new operation for delivery to the given node.
	 * Each operation should count the given latch down upon completion.
	 */
	OperationImpl newOp(MemcachedNode n, CountDownLatch latch);
}
