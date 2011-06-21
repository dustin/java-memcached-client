package net.spy.memcached.vbucket;

public class ConnectionException extends RuntimeException {

    private static final long serialVersionUID = -6513198067127603979L;

    public ConnectionException() {
        super();
    }

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
