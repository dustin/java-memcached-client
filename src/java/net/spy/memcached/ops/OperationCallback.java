// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 47B3D166-3964-49D4-9B4C-A46B749B12A5

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
	void receivedStatus(String line);
}
