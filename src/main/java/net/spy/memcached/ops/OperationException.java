package net.spy.memcached.ops;

import java.io.IOException;


/**
 * Exceptions thrown when protocol errors occur.
 */
public final class OperationException extends IOException {

	private final OperationErrorType type;

	/**
	 * General exception (no message).
	 */
	public OperationException() {
		super();
		type=OperationErrorType.GENERAL;
	}

	/**
	 * Exception with a message.
	 *
	 * @param eType the type of error that occurred
	 * @param msg the error message
	 */
	public OperationException(OperationErrorType eType, String msg) {
		super(msg);
		type=eType;
	}

	/**
	 * Get the type of error.
	 */
	public OperationErrorType getType() {
		return type;
	}

	@Override
	public String toString() {
		String rv=null;
		if(type == OperationErrorType.GENERAL) {
			rv="OperationException: " + type;
		} else {
			rv="OperationException: " + type + ": " + getMessage();
		}
		return rv;
	}
}
