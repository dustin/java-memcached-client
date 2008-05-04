package net.spy.memcached;

/**
 * Thrown by {@link MemcachedClientImpl} when any internal operations timeout.
 *
 * @author Ray Krueger
 * @see net.spy.memcached.MemcachedClientImpl#setGlobalOperationTimeout(long)
 */
public class OperationTimeoutException extends RuntimeException {

    public OperationTimeoutException(String message) {
        super(message);
    }

    public OperationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}