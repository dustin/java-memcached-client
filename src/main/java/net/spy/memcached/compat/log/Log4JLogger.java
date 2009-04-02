// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.compat.log;

/**
 * Logging implementation using
 * <a href="http://jakarta.apache.org/log4j/docs/">log4j</a>.
 */
public class Log4JLogger extends AbstractLogger {

	// Can't really import this without confusion as there's another thing
	// by this name in here.
	private final org.apache.log4j.Logger l4jLogger;

	/**
	 * Get an instance of Log4JLogger.
	 */
	public Log4JLogger(String name) {
		super(name);

		// Get the log4j logger instance.
		l4jLogger=org.apache.log4j.Logger.getLogger(name);
	}

	/**
	 * True if the underlying logger would allow debug messages through.
	 */
	@Override
	public boolean isDebugEnabled() {
		return(l4jLogger.isDebugEnabled());
	}

	/**
	 * True if the underlying logger would allow info messages through.
	 */
	@Override
	public boolean isInfoEnabled() {
		return(l4jLogger.isInfoEnabled());
	}

	/**
	 * Wrapper around log4j.
	 *
	 * @param level net.spy.compat.log.AbstractLogger level.
	 * @param message object message
	 * @param e optional throwable
	 */
	@Override
	public void log(Level level, Object message, Throwable e) {
		org.apache.log4j.Level pLevel=org.apache.log4j.Level.DEBUG;

		switch(level == null ? Level.FATAL : level) {
			case DEBUG:
				pLevel=org.apache.log4j.Level.DEBUG;
				break;
			case INFO:
				pLevel=org.apache.log4j.Level.INFO;
				break;
			case WARN:
				pLevel=org.apache.log4j.Level.WARN;
				break;
			case ERROR:
				pLevel=org.apache.log4j.Level.ERROR;
				break;
			case FATAL:
				pLevel=org.apache.log4j.Level.FATAL;
				break;
			default:
				// I don't know what this is, so consider it fatal
				pLevel=org.apache.log4j.Level.FATAL;
				l4jLogger.log("net.spy.compat.log.AbstractLogger", pLevel,
					"Unhandled log level:  " + level
						+ " for the following message", null);
		}

		l4jLogger.log("net.spy.compat.log.AbstractLogger", pLevel, message, e);
	}

}
