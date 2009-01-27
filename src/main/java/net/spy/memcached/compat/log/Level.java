// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.compat.log;

/**
 * Levels for logging.
 */
public enum Level {

	/**
	 * Debug level.
	 */
	DEBUG,
	/**
	 * Info level.
	 */
	INFO,
	/**
	 * Warning level.
	 */
	WARN,
	/**
	 * Error level.
	 */
	ERROR,
	/**
	 * Fatal level.
	 */
	FATAL;

	/**
	 * Get a string representation of this level.
	 */
	@Override
	public String toString() {
		return("{LogLevel:  " + name() + "}");
	}

}
