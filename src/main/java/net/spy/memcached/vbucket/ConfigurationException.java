package net.spy.memcached.vbucket;

public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = -9180877058910807939L;

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
