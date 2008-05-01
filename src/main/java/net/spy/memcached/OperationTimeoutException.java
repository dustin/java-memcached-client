package net.spy.memcached;

/**
 * DOCUMENT ME!
 *
 * @author Ray Krueger
 */
public class OperationTimeoutException extends RuntimeException {
    public OperationTimeoutException(String message) {
        super(message);
    }

    public OperationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationTimeoutException(Throwable cause) {
        super(cause);
    }
}