package net.spy.memcached.vbucket.config;

public class ConfigParsingException extends RuntimeException {

	private static final long serialVersionUID = -8393032485475738369L;

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
