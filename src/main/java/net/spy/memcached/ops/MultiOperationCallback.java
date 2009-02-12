package net.spy.memcached.ops;

/**
 * An operation callback that will capture receivedStatus and complete
 * invocations and dispatch to a single callback.
 *
 * <p>
 *  This is useful for the cases where a single request gets split into
 *  multiple requests and the callback needs to not know the difference.
 * </p>
 */
public abstract class MultiOperationCallback implements OperationCallback {

	private OperationStatus mostRecentStatus = null;
	private int remaining=0;
	protected final OperationCallback originalCallback;

	/**
	 * Get a MultiOperationCallback over the given callback for the specified
	 * number of replicates.
	 *
	 * @param original the original callback
	 * @param todo how many complete() calls we expect before dispatching.
	 */
	public MultiOperationCallback(OperationCallback original, int todo) {
		originalCallback = original;
		remaining = todo;
	}

	public void complete() {
		if(--remaining == 0) {
			originalCallback.receivedStatus(mostRecentStatus);
			originalCallback.complete();
		}
	}

	public void receivedStatus(OperationStatus status) {
		mostRecentStatus = status;
	}

}
