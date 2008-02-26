package net.spy.memcached.ops;

/**
 * Operation status indicating an operation was cancelled.
 */
public class CancelledOperationStatus extends OperationStatus {

	public CancelledOperationStatus() {
		super(false, "cancelled");
	}

}
