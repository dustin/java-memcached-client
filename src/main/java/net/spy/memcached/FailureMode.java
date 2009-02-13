package net.spy.memcached;

/**
 * Failure modes for node failures.
 */
public enum FailureMode {

	/**
	 * Move on to functional nodes when nodes fail.
	 *
	 * <p>
	 *  In this failure mode, the failure of a node will cause its current
	 *  queue and future requests to move to the next logical node in the
	 *  cluster for a given key.
	 * </p>
	 */
	Redistribute,
	/**
	 * Continue to retry a failing node until it comes back up.
	 *
	 * <p>
	 *  This failure mode is appropriate when you have a rare short downtime
	 *  of a memcached node that will be back quickly, and your app is written
	 *  to not wait very long for async command completion.
	 * </p>
	 */
	Retry,

	/**
	 * Automatically cancel all operations heading towards a downed node.
	 */
	Cancel

}
