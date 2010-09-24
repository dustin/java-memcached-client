package net.spy.memcached.ops;

/**
 * State of this operation.
 */
public enum OperationState {
	/**
	 * State indicating this operation is writing data to the server.
	 */
	WRITING,
	/**
	 * State indicating this operation is reading data from the server.
	 */
	READING,
	/**
	 * State indicating this operation is complete.
	 */
	COMPLETE,
	/**
	 * State indicating this operation timed out without completing.
	 */
	TIMEDOUT,
	/**
	 * State indicating this operation needs to be resent.  Typically
	 * this means vbucket hashing and there is a topology change.
	 */
	RETRY
}
