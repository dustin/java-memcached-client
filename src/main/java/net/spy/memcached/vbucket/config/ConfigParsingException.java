package net.spy.memcached.vbucket.config;

public class ConfigParsingException extends RuntimeException {

    public ConfigParsingException() {
        super();
    }

    public ConfigParsingException(String message) {
        super(message);
    }

    public ConfigParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigParsingException(Throwable cause) {
        super(cause);
    }
}
