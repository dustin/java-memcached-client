// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.ops;

/**
 * Callback that's invoked with the response of an operation.
 */
public interface OperationCallback {

	/**
	 * Method invoked with the status when the operation is complete.
	 *
	 * @param line the line containing the final status of the operation
	 */
	void receivedStatus(OperationStatus status);

	/**
	 * Called whenever an operation completes.
	 */
	void complete();
}
