package net.spy.memcached.protocol.ascii;

import java.io.IOException;

/**
 * Exceptions thrown when protocol errors occur.
 */
public class OperationException extends IOException {

	private final OperationImpl.ErrorType type;

	/**
	 * General exception (no message).
	 */
	public OperationException() {
		super();
		type=OperationImpl.ErrorType.GENERAL;
	}

	/**
	 * Exception with a message.
	 *
	 * @param eType the type of error that occurred
	 * @param msg the error message
	 */
	public OperationException(OperationImpl.ErrorType eType, String msg) {
		super(msg.substring(eType.getSize()));
		type=eType;
	}

	/**
	 * Get the type of error.
	 */
	public OperationImpl.ErrorType getType() {
		return type;
	}

	@Override
	public String toString() {
		String rv=null;
		if(type == OperationImpl.ErrorType.GENERAL) {
			rv="OperationException: " + type;
		} else {
			rv="OperationException: " + type + ": " + getMessage();
		}
		return rv;
	}
}
