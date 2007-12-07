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
	COMPLETE
}