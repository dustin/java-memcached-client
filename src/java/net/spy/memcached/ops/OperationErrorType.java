package net.spy.memcached.ops;

/**
 * Error classification.
 */
public enum OperationErrorType {
	/**
	 * General error.
	 */
	GENERAL(0),
	/**
	 * Error that occurred because the client did something stupid.
	 */
	CLIENT("CLIENT_ERROR ".length()),
	/**
	 * Error that occurred because the server did something stupid.
	 */
	SERVER("SERVER_ERROR ".length());

	private final int size;

	OperationErrorType(int s) {
		size=s;
	}

	public int getSize() {
		return size;
	}
}