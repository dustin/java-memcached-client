package net.spy.memcached.ops;

/**
 * Status indicator.
 */
public class OperationStatus {

	private final boolean isSuccess;
	private final String message;

	public OperationStatus(boolean success, String msg) {
		super();
		isSuccess = success;
		message = msg;
	}

	/**
	 * Does this status indicate success?
	 */
	public boolean isSuccess() {
		return isSuccess;
	}

	/**
	 * Get the message included as part of this status.
	 */
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "{OperationStatus success=" + isSuccess + ":  " + message + "}";
	}
}
